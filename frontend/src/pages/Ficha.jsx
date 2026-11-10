import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { api, urlApi } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Alerta, Botao, BotaoLink, Campo, Card, Carregando, Chip, Item, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { br, brl, competencia, cpf, hora } from '../util.js'

const ABAS = [['resumo', 'Resumo'], ['requisitos', 'Requisitos'], ['documentos', 'Documentos'], ['dae', 'DAE'], ['linha', 'Linha do tempo']]

export default function Ficha() {
  const { id } = useParams()
  const [params, setParams] = useSearchParams()
  const { gestor, meta } = useAuth()
  const { dados, erro, recarregar } = useDados(`/clientes/${id}`)
  const { executar, ocupado } = useAcao()
  const aba = params.get('aba') || 'resumo'
  const abas = gestor ? [...ABAS, ['financeiro', 'Financeiro']] : ABAS

  if (erro) return <p className="text-sm text-red-700">{erro}</p>
  if (!dados) return <Carregando />
  const a = dados.analise
  const c = a.cliente
  const docNome = Object.fromEntries(meta.documentos.map((d) => [d.id, d.nome]))
  const statusDae = Object.fromEntries(meta.statusDae.map((s) => [s.id, s]))
  const entrevista = meta.entrevistas.find((e) => e.id === c.entrevista) || { nome: '—', cor: 'neutro' }
  const daeAtivo = c.entrevista === 'CONFIRMADO'

  async function arquivar() {
    if (!window.confirm(c.ativo ? 'Arquivar este cliente?' : 'Reativar este cliente?')) return
    await executar(() => api(`/clientes/${id}/arquivar`, { metodo: 'POST' }), recarregar)
  }

  return (
    <>
      <Titulo titulo={c.nome}>
        <div className="flex flex-wrap gap-2">
          <BotaoLink to={`/status?cliente=${c.id}`}>Atualizar status</BotaoLink>
          <BotaoLink to={`/clientes/${c.id}/editar`} variante="sec">Editar cadastro</BotaoLink>
          <BotaoLink to={`/mensagens?cliente=${c.id}&modelo=${a.documentosPendentes.length ? 'DOCS' : 'DAE'}`} variante="sec">Mensagem</BotaoLink>
          {dados.whatsapp && <BotaoLink href={`https://wa.me/${dados.whatsapp}`} target="_blank" rel="noopener noreferrer" variante="neutro">Abrir WhatsApp</BotaoLink>}
          {gestor && <Botao variante="perigo" onClick={arquivar} disabled={ocupado}>{c.ativo ? 'Arquivar' : 'Reativar'}</Botao>}
        </div>
      </Titulo>
      <p className="-mt-3 flex flex-wrap items-center gap-2 text-sm text-slate-600">
        CPF {cpf(c.cpf)} · {c.municipio || 'município não informado'}{c.colonia && ` · ${c.colonia}`}
        <Chip cor={a.etapaCor}>{a.etapaNome}</Chip>
        <Link to="/entrevistas" title="Alterar na tela Entrevistas"><Chip cor={entrevista.cor}>Entrevista: {entrevista.nome}</Chip></Link>
        {!c.ativo && <Chip>Arquivado</Chip>}
      </p>

      {(c.etapa === 'DOCUMENTACAO' || c.etapa === 'A_REQUERER') && (a.apto
        ? <Alerta cor="ok"><strong>Apto para requerer.</strong> Faça o pedido no Emprega Brasil e registre o protocolo em Atualizar status.</Alerta>
        : <Alerta cor="aviso"><strong>Pendências para requerer:</strong> {a.bloqueios.join('; ')}.</Alerta>)}

      <Card className="flex flex-col gap-4">
        <nav aria-label="Seções da ficha" className="flex flex-wrap gap-1.5 border-b border-slate-200 pb-2.5">
          {abas.map(([k, nome]) => (
            <button key={k} type="button" onClick={() => setParams({ aba: k })}
              className={`min-h-[36px] rounded-lg px-3 text-[13px] ${aba === k ? 'bg-marca-900 font-semibold text-white' : 'bg-slate-100 text-slate-700'}`}>
              {nome}
            </button>
          ))}
        </nav>

        {aba === 'resumo' && <Resumo a={a} />}
        {aba === 'requisitos' && <Requisitos a={a} id={id} recarregar={recarregar} />}
        {aba === 'documentos' && <Documentos a={a} id={id} docs={dados.documentos} dae={dados.dae} docNome={docNome} recarregar={recarregar} />}
        {aba === 'dae' && (
          <div className="flex flex-col gap-3">
            {!daeAtivo && <Alerta cor="aviso">DAE só para clientes com entrevista confirmada. Confirme na tela <Link to="/entrevistas" className="underline">Entrevistas</Link>.</Alerta>}
            <p className="text-xs text-slate-500">Lance vendas e situação na tela <Link to="/dae" className="text-marca-700 underline">DAE (eSocial)</Link>. Ao anexar o comprovante (aba Documentos), a guia é marcada como paga.</p>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] border-collapse">
                <thead><tr><Th>Competência</Th><Th>Venda</Th><Th>DAE</Th><Th>Vencimento estimado</Th><Th>Situação</Th></tr></thead>
                <tbody>
                  {dados.dae.map((d) => (
                    <tr key={d.competencia}>
                      <Td>{d.label}</Td><Td>{d.venda != null ? brl(d.venda) : '—'}</Td><Td>{d.valor != null ? brl(d.valor) : '—'}</Td>
                      <Td>{br(d.vencimento)}</Td>
                      <Td>{d.status ? <Chip cor={statusDae[d.status].cor}>{statusDae[d.status].nome}</Chip> : <span className="text-xs text-slate-500">{daeAtivo ? 'em aberto' : '—'}</span>}</Td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
        {aba === 'linha' && (
          <ol className="flex flex-col">
            {dados.eventos.map((e, i) => (
              <li key={i} className="grid grid-cols-[150px_12px_1fr] gap-2.5 pb-3 text-sm">
                <span className="text-xs text-slate-500"><strong>{br(e.momento)}</strong> {hora(e.momento)}<br />{e.usuario || 'sistema'}</span>
                <span className="mt-1.5 h-2.5 w-2.5 rounded-full bg-marca-700" />
                <span>{e.descricao}</span>
              </li>
            ))}
            {dados.eventos.length === 0 && <li className="text-sm text-slate-500">Sem registros.</li>}
          </ol>
        )}
        {aba === 'financeiro' && gestor && <Financeiro a={a} id={id} receb={dados.honorarios || []} recarregar={recarregar} />}
      </Card>
    </>
  )
}

function Resumo({ a }) {
  const c = a.cliente
  const p = c.pessoal
  const { gestor } = useAuth()
  const { dados: auxiliares } = useDados('/auxiliares')
  const { executar, ocupado } = useAcao()
  const [senha, setSenha] = useState(null)
  const aux = (auxiliares || []).find((x) => x.id === p.auxiliarId)
  const grupos = [
    ['Cadastro', [
      ['Data de nascimento', br(c.nascimento) || '—'], ['Sexo', p.sexo || '—'], ['CPF', cpf(c.cpf)], ['RG', p.rg || '—'],
      ['Órgão do emissor', p.orgaoEmissor || '—'], ['Data da expedição', br(p.dataExpedicao) || '—'],
      ['Nome do pai', p.nomePai || '—'], ['Nome da mãe', p.nomeMae || '—'], ['Status civil', p.estadoCivil || '—'],
      ['Estado de origem', p.estadoOrigem || '—'], ['Cidade de origem', p.cidadeOrigem || '—'],
      ['Título', p.tituloEleitor || '—'], ['RGP', c.rgpNumero || '—'],
    ]],
    ['Dados do afiliado', [
      ['Numeração da inscrição', p.inscricaoNumero || '—'], ['Data da afiliação', br(p.dataAfiliacao) || '—'],
    ]],
    ['Auxiliar jurídico', [['Auxiliar jurídico', aux ? aux.nome : 'não definido']]],
    ['Endereço do associado', [
      ['CEP', p.cep || '—'], ['Bairro', p.bairro || '—'], ['Número', p.numero || '—'], ['Estado', p.uf || '—'],
      ['Complemento', p.complemento || '—'], ['Cidade', c.municipio || '—'], ['Logradouro', p.logradouro || c.endereco || '—'],
    ]],
    ['Contato', [['Número do telefone', c.telefone || '—']]],
    ['Dados do pedido', [
      ['NIT/PIS', c.nit || '—'], ['Colônia', c.colonia || '—'], ['RGP desde', c.rgpDesde || '—'],
      ['Validade do RGP', br(c.rgpValidade) || '—'], ['Espécie', c.especie || '—'],
      ['Procuração e-CAC', br(c.procuracaoValidade) || 'sem procuração'], ['Conta gov.br', c.govNivel || 'não informado'],
      ['Protocolo', c.protocolo || '—'], ['Última conferência', br(c.ultimaConferencia) || 'nunca'],
    ]],
  ]
  return (
    <div className="flex flex-col gap-4">
      {grupos.map(([titulo, campos]) => (
        <section key={titulo} className="flex flex-col gap-2">
          <h3 className="text-xs font-bold uppercase tracking-wide text-marca-900">{titulo}</h3>
          <div className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-x-4 gap-y-3">
            {campos.map(([k, v]) => <div key={k}><div className="text-xs text-slate-500">{k}</div><div className="font-semibold">{v}</div></div>)}
          </div>
          {titulo === 'Cadastro' && p.senhaGovCadastrada && (
            <Item>
              <div className="flex-1">
                <div className="font-semibold">Senha gov</div>
                <div className="font-mono text-[13px] text-slate-600">{senha ?? '••••••••••'}</div>
              </div>
              {gestor && (senha === null
                ? <Botao variante="neutro" pequeno disabled={ocupado}
                  onClick={() => executar(() => api(`/clientes/${c.id}/senha-gov`), (r) => setSenha(r.senha))}>Revelar (fica registrado)</Botao>
                : <Botao variante="neutro" pequeno onClick={() => setSenha(null)}>Ocultar</Botao>)}
            </Item>
          )}
        </section>
      ))}
      <Item>
        <div>
          <div className="font-semibold">Situação do pedido</div>
          <div className="text-[13px] text-slate-600">
            {c.situacao || 'Sem registro.'}
            {c.etapa === 'EXIGENCIA' && <><br />Exigência: {c.exigenciaTexto} · prazo {br(c.prazoData)}</>}
            {c.etapa === 'INDEFERIDO' && <><br />Motivo: {c.indeferimentoMotivo}{c.cienciaData && ` · ciência em ${br(c.cienciaData)}`}</>}
            {c.etapa === 'DEFERIDO' && <><br />{c.parcelasLiberadas} de {c.parcelasTotal} parcelas liberadas.</>}
          </div>
        </div>
      </Item>
      <section className="flex flex-col gap-2">
        <h3 className="text-xs font-bold uppercase tracking-wide text-marca-900">Campo de observação</h3>
        <div className="whitespace-pre-line text-sm text-slate-700">{c.observacoes || '—'}</div>
      </section>
    </div>
  )
}

function Requisitos({ a, id, recarregar }) {
  const { meta } = useAuth()
  const { executar, ocupado } = useAcao()
  const [resp, setResp] = useState({})
  useEffect(() => {
    setResp(Object.fromEntries(a.requisitos.map((r) => [r.requisito, r.atende])))
  }, [a])

  async function salvar() {
    await executar(() => api(`/clientes/${id}/requisitos`, { metodo: 'PUT', corpo: resp }), recarregar)
  }

  return (
    <div className="flex flex-col gap-2.5">
      {a.requisitos.map((r) => (
        <Item key={r.requisito}>
          <div className="min-w-0 flex-[1_1_300px]">
            <div className="font-semibold">{r.texto} {r.essencial && <Chip>essencial</Chip>}</div>
            <div className="text-[13px] text-slate-600">Como comprovar: {r.prova}</div>
          </div>
          <div className="flex gap-1.5">
            {[[true, 'Sim'], [false, 'Não'], [null, '?']].map(([v, n]) => (
              <button key={n} type="button" onClick={() => setResp((x) => ({ ...x, [r.requisito]: v }))}
                className={`min-h-[38px] min-w-[52px] rounded-lg border px-3 text-[13px] font-semibold ${resp[r.requisito] === v
                  ? (v === true ? 'border-green-700 bg-green-700 text-white' : v === false ? 'border-red-700 bg-red-700 text-white' : 'border-slate-500 bg-slate-500 text-white')
                  : 'border-slate-300 bg-white text-slate-700'}`}>
                {n}
              </button>
            ))}
          </div>
        </Item>
      ))}
      <p className="text-xs text-slate-500">{meta.baseLegal}</p>
      <div><Botao onClick={salvar} disabled={ocupado}>Salvar requisitos</Botao></div>
    </div>
  )
}

function Documentos({ a, id, docs, dae, docNome, recarregar }) {
  const { meta, gestor } = useAuth()
  const { executar, ocupado } = useAcao()
  const [tipo, setTipo] = useState('RG')
  const [comp, setComp] = useState(dae[0]?.competencia || '')
  const [arquivo, setArquivo] = useState(null)
  const recebidos = new Set(a.cliente.documentos)

  async function anexar(e) {
    e.preventDefault()
    if (!arquivo) return
    const form = new FormData()
    form.append('tipo', tipo)
    if (tipo === 'COMPROVANTE_DAE') form.append('competencia', comp)
    form.append('arquivo', arquivo)
    await executar(() => api(`/clientes/${id}/documentos`, { metodo: 'POST', form }), () => {
      e.target.reset()
      setArquivo(null)
      recarregar()
    })
  }

  async function excluir(doc) {
    if (!window.confirm(`Excluir ${doc.nome}?`)) return
    await executar(() => api(`/documentos/${doc.id}`, { metodo: 'DELETE' }), recarregar)
  }

  return (
    <div className="flex flex-wrap items-start gap-4">
      <div className="flex min-w-0 flex-[2_1_380px] flex-col gap-2">
        <h2 className="text-base font-semibold">Documentos obrigatórios</h2>
        {meta.documentos.filter((d) => d.flag).map((d) => (
          <Item key={d.id}>
            <span className="flex-1 font-semibold">{d.nome}</span>
            <Chip cor={recebidos.has(d.id) ? 'ok' : 'erro'}>{recebidos.has(d.id) ? 'Recebido' : 'Pendente'}</Chip>
          </Item>
        ))}
        {a.documentosPendentes.length > 0 && <BotaoLink to={`/mensagens?cliente=${id}&modelo=DOCS`} variante="sec">Pedir pendentes no WhatsApp</BotaoLink>}
      </div>
      <div className="flex min-w-0 flex-[2_1_380px] flex-col gap-3">
        <h2 className="text-base font-semibold">Anexar arquivo</h2>
        <form onSubmit={anexar} className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-slate-50 p-3.5">
          <Campo rotulo="Tipo de documento">
            <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
              {meta.documentos.map((d) => <option key={d.id} value={d.id}>{d.nome}</option>)}
            </select>
          </Campo>
          {tipo === 'COMPROVANTE_DAE' && (
            <Campo rotulo="Competência do DAE pago">
              <select value={comp} onChange={(e) => setComp(e.target.value)}>
                {dae.map((d) => <option key={d.competencia} value={d.competencia}>{d.label}</option>)}
              </select>
            </Campo>
          )}
          <Campo rotulo="Arquivo (PDF ou foto, até 15 MB)">
            <input type="file" required accept=".pdf,.jpg,.jpeg,.png,.webp,.heic" onChange={(e) => setArquivo(e.target.files[0] || null)} />
          </Campo>
          <Botao type="submit" disabled={ocupado || !arquivo}>Anexar</Botao>
        </form>
        <h2 className="mt-1 text-base font-semibold">Arquivos na pasta do cliente</h2>
        {docs.map((d) => (
          <Item key={d.id}>
            <div className="min-w-0 flex-1">
              <a href={urlApi(`/documentos/${d.id}`)} target="_blank" rel="noopener noreferrer" className="font-semibold text-marca-700 underline">{d.nome}</a>
              <div className="text-[13px] text-slate-600">{docNome[d.tipo]}{d.competencia && ` ${competencia(d.competencia)}`} · {br(d.enviadoEm)} · {d.usuario || ''}</div>
            </div>
            {gestor && <Botao variante="perigo" pequeno onClick={() => excluir(d)}>Excluir</Botao>}
          </Item>
        ))}
        {docs.length === 0 && <p className="text-sm text-slate-500">Nenhum arquivo ainda.</p>}
      </div>
    </div>
  )
}

function Financeiro({ a, id, receb, recarregar }) {
  const { executar, ocupado } = useAcao()
  const [valor, setValor] = useState('')
  const [data, setData] = useState('')
  const [obs, setObs] = useState('')
  const c = a.cliente
  const cards = [
    ['Parcelas liberadas', `${c.parcelasLiberadas} de ${c.parcelasTotal}`],
    ['Honorários por parcela', `${brl(a.honorariosPorParcela)} (${Number(a.honorariosPct).toLocaleString('pt-BR')}%)`],
    ['Devido', brl(a.honorariosDevidos)], ['Recebido', brl(a.honorariosRecebidos)],
    ['Saldo a receber', brl(a.honorariosSaldo)], ['A liberar (futuro)', brl(a.honorariosFuturos)],
  ]
  async function registrar(e) {
    e.preventDefault()
    await executar(() => api(`/clientes/${id}/honorarios`, { metodo: 'POST', corpo: { valor, data, observacao: obs } }), () => {
      setValor(''); setData(''); setObs(''); recarregar()
    })
  }
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-3">
        {cards.map(([k, v]) => <Item key={k} className="flex-col items-start"><div className="text-xs text-slate-500">{k}</div><div className="text-lg font-bold">{v}</div></Item>)}
      </div>
      <form onSubmit={registrar} className="flex flex-wrap items-end gap-3 rounded-lg border border-slate-200 bg-slate-50 p-3.5">
        <Campo rotulo="Valor recebido (R$)" className="flex-[1_1_160px]"><input type="text" inputMode="decimal" value={valor} placeholder={String(a.honorariosPorParcela).replace('.', ',')} onChange={(e) => setValor(e.target.value)} /></Campo>
        <Campo rotulo="Data" className="flex-[1_1_160px]"><input type="text" value={data} placeholder="dd/mm/aaaa (vazio = hoje)" onChange={(e) => setData(e.target.value)} /></Campo>
        <Campo rotulo="Observação" className="flex-[2_1_220px]"><input type="text" value={obs} onChange={(e) => setObs(e.target.value)} /></Campo>
        <Botao type="submit" disabled={ocupado}>Registrar recebimento</Botao>
      </form>
      <div className="flex flex-col gap-2">
        {receb.map((h, i) => <Item key={i}><div><div className="font-semibold">{brl(h.valor)} em {br(h.data)}</div><div className="text-[13px] text-slate-600">{h.observacao || ''} {h.usuario && `· ${h.usuario}`}</div></div></Item>)}
        {receb.length === 0 && <p className="text-sm text-slate-500">Nenhum recebimento registrado.</p>}
      </div>
      <p className="text-xs text-slate-500">Parcelas liberadas são registradas em Atualizar status (situação "Deferido").</p>
    </div>
  )
}
