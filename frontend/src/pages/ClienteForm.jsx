import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { Alerta, Botao, BotaoLink, Campo, Card, Carregando, Titulo, useAcao, useAviso, useDados } from '../components/ui.jsx'
import { mascaras, validadores } from '../formato.js'
import { br, cpf } from '../util.js'

const SEXOS = ['Masculino', 'Feminino', 'Outro']
const STATUS_CIVIS = ['Solteiro(a)', 'Casado(a)', 'União estável', 'Separado(a)', 'Divorciado(a)', 'Viúvo(a)']
const NIVEIS_GOV = [['bronze', 'Bronze'], ['prata', 'Prata'], ['ouro', 'Ouro']]

// Blocos e campos na ordem e com os nomes definidos pelo escritório.
// Campo: [chave, rótulo, obrigatório, dica, opções ou 'senha']
const SECOES = [
  ['Cadastro', [
    ['nome', 'Nome completo', true],
    ['nascimento', 'Data de nascimento', false, 'dd/mm/aaaa'],
    ['sexo', 'Sexo', false, null, SEXOS],
    ['cpf', 'CPF', true, '000.000.000-00'],
    ['rg', 'RG'],
    ['orgaoEmissor', 'Órgão do emissor', false, 'ex.: SSP'],
    ['dataExpedicao', 'Data da expedição', false, 'dd/mm/aaaa'],
    ['nomePai', 'Nome do pai'],
    ['nomeMae', 'Nome da mãe'],
    ['estadoCivil', 'Status civil', false, null, STATUS_CIVIS],
    ['estadoOrigem', 'Estado de origem', false, 'UF, ex.: MA'],
    ['cidadeOrigem', 'Cidade de origem'],
    ['tituloEleitor', 'Título'],
    ['rgpNumero', 'RGP', true],
    ['senhaGov', 'Senha gov', false, null, 'senha'],
  ]],
  ['Dados do afiliado', [
    ['inscricaoNumero', 'Numeração da inscrição'],
    ['dataAfiliacao', 'Data da afiliação', false, 'dd/mm/aaaa'],
  ]],
  ['Auxiliar jurídico', 'auxiliar'],
  ['Endereço do associado', [
    ['cep', 'CEP', false, '00000-000'],
    ['bairro', 'Bairro'],
    ['numero', 'Número'],
    ['uf', 'Estado', false, 'UF, ex.: MA'],
    ['complemento', 'Complemento'],
    ['municipio', 'Cidade', true],
    ['logradouro', 'Logradouro'],
  ]],
  ['Contato', [
    ['telefone', 'Número do telefone', true],
  ]],
  ['Dados do pedido', [
    ['nit', 'NIT/PIS'],
    ['colonia', 'Colônia'],
    ['rgpDesde', 'RGP registrado desde (mês/ano)', false, 'mm/aaaa'],
    ['rgpValidade', 'Validade do RGP', false, 'dd/mm/aaaa'],
    ['especie', 'Espécie / modalidade de pesca'],
    ['procuracaoValidade', 'Validade da procuração e-CAC', false, 'dd/mm/aaaa'],
    ['govNivel', 'Nível da conta gov.br', false, null, NIVEIS_GOV],
    ['honorariosPct', 'Honorários (% por parcela; vazio = padrão)'],
  ]],
  ['Campo de observação', 'observacao'],
]
const CHAVES = SECOES.flatMap(([, campos]) => (Array.isArray(campos) ? campos.map(([k]) => k) : []))
const DATAS = ['nascimento', 'rgpValidade', 'procuracaoValidade', 'dataExpedicao', 'dataAfiliacao']
const OBRIGATORIOS = new Set(SECOES.flatMap(([, campos]) => (Array.isArray(campos) ? campos.filter((f) => f[2]).map((f) => f[0]) : [])))

// Máscara (formata e bloqueia o que não cabe), validação e teclado de cada campo.
const dataPassada = validadores.data({ passado: true })
const dataLonge = validadores.data({ min: 2000, max: 2100 })
const DATA = { inputMode: 'numeric', dica: 'dd/mm/aaaa', max: 10 }
const UF = { mascara: mascaras.uf, validar: validadores.uf, dica: 'UF, ex.: MA', max: 2 }
const REGRAS = {
  nome: { mascara: mascaras.nome, validar: validadores.nomeCompleto, autoComplete: 'name' },
  nascimento: { ...DATA, mascara: mascaras.data, validar: dataPassada },
  cpf: { mascara: mascaras.cpf, validar: validadores.cpf, inputMode: 'numeric', dica: '000.000.000-00', max: 14 },
  rg: { mascara: mascaras.rg, max: 15 },
  orgaoEmissor: { mascara: mascaras.orgaoEmissor, dica: 'ex.: SSP/MA', max: 12 },
  dataExpedicao: { ...DATA, mascara: mascaras.data, validar: dataPassada },
  nomePai: { mascara: mascaras.nome },
  nomeMae: { mascara: mascaras.nome },
  estadoOrigem: UF,
  cidadeOrigem: { mascara: mascaras.nome },
  tituloEleitor: { mascara: mascaras.numeros12, inputMode: 'numeric', max: 12 },
  rgpNumero: { mascara: mascaras.rgp, dica: 'ex.: MA-000111', max: 20 },
  inscricaoNumero: { mascara: mascaras.numeros12, inputMode: 'numeric', max: 12 },
  dataAfiliacao: { ...DATA, mascara: mascaras.data, validar: dataPassada },
  cep: { mascara: mascaras.cep, validar: validadores.cep, inputMode: 'numeric', dica: '00000-000', max: 9 },
  bairro: { mascara: mascaras.texto(100) },
  numero: { mascara: mascaras.numeroEndereco, dica: 'ex.: 120 ou S/N', max: 6 },
  uf: UF,
  complemento: { mascara: mascaras.texto(60) },
  municipio: { mascara: mascaras.nome },
  logradouro: { mascara: mascaras.texto(120) },
  telefone: { mascara: mascaras.telefone, validar: validadores.telefone, inputMode: 'tel', autoComplete: 'tel', dica: '(98) 98888-7777', max: 15 },
  nit: { mascara: mascaras.nit, validar: validadores.nit, inputMode: 'numeric', dica: '000.00000.00-0', max: 14 },
  colonia: { mascara: mascaras.texto(80) },
  rgpDesde: { mascara: mascaras.mesAno, validar: validadores.mesAno, inputMode: 'numeric', dica: 'mm/aaaa', max: 7 },
  rgpValidade: { ...DATA, mascara: mascaras.data, validar: dataLonge },
  especie: { mascara: mascaras.texto(80) },
  procuracaoValidade: { ...DATA, mascara: mascaras.data, validar: dataLonge },
  honorariosPct: { mascara: mascaras.percentual, validar: validadores.percentual, inputMode: 'decimal', dica: 'ex.: 30', max: 6 },
}

function erroDe(k, valor) {
  if (OBRIGATORIOS.has(k) && !String(valor ?? '').trim()) return 'Preencha este campo.'
  return REGRAS[k]?.validar ? REGRAS[k].validar(valor) : ''
}

function Secao({ titulo, children }) {
  return (
    <fieldset className="flex flex-col gap-3 border-t border-slate-200 pt-4 first:border-0 first:pt-0">
      <legend className="mb-1 text-sm font-bold text-marca-900">{titulo}</legend>
      <div className="grid grid-cols-[repeat(auto-fit,minmax(230px,1fr))] gap-x-4 gap-y-3.5">{children}</div>
    </fieldset>
  )
}

/** Cadastro completo. Novo: usado como passo 2 do assistente (recebe as respostas da triagem). Edição: rota própria. */
export default function ClienteForm({ clienteId, respostas, triagem, rascunho, aoRascunho, aoSalvar, aoCancelar }) {
  const { id: idDaRota } = useParams()
  const id = clienteId ?? idDaRota
  const novo = !id
  const navegar = useNavigate()
  const { executar, ocupado } = useAcao()
  const [form, setForm] = useState(rascunho || (novo ? {} : null))

  useEffect(() => { if (form) aoRascunho?.(form) }, [form])
  const [erros, setErros] = useState({})
  const aviso = useAviso()
  const { dados: auxiliares } = useDados('/auxiliares')

  useEffect(() => {
    if (novo || rascunho) return
    api(`/clientes/${id}`).then((f) => {
      const c = f.analise.cliente
      const dados = { ...c, ...c.pessoal }
      const v = {}
      CHAVES.forEach((k) => { v[k] = dados[k] ?? '' })
      DATAS.forEach((k) => { v[k] = br(dados[k]) })
      v.cpf = cpf(c.cpf)
      v.honorariosPct = c.honorariosPct != null ? String(c.honorariosPct).replace('.', ',') : ''
      CHAVES.forEach((k) => { if (REGRAS[k]?.mascara) v[k] = REGRAS[k].mascara(v[k]) })
      v.observacoes = c.observacoes || ''
      v.auxiliarId = c.pessoal.auxiliarId ?? ''
      v.senhaGov = ''
      v.senhaCadastrada = c.pessoal.senhaGovCadastrada
      if (c.endereco) v.endereco = c.endereco
      setForm(v)
    })
  }, [id, novo])

  function mudar(k, v) {
    setForm((f) => ({ ...f, [k]: v }))
    setErros((x) => (x[k] ? { ...x, [k]: erroDe(k, v) } : x))
  }

  async function salvar(e) {
    e.preventDefault()
    const novosErros = {}
    CHAVES.forEach((k) => {
      const mensagem = erroDe(k, form[k])
      if (mensagem) novosErros[k] = mensagem
    })
    setErros(novosErros)
    const primeiro = CHAVES.find((k) => novosErros[k])
    if (primeiro) {
      aviso.erro('Corrija os campos destacados em vermelho.')
      document.getElementById(`campo-${primeiro}`)?.focus()
      return
    }
    const { senhaCadastrada, ...resto } = form
    const corpo = { ...resto, auxiliarId: form.auxiliarId ? Number(form.auxiliarId) : null, ...(novo ? { respostas, emAndamento: true } : {}) }
    await executar(
      () => (novo ? api('/clientes', { metodo: 'POST', corpo }) : api(`/clientes/${id}`, { metodo: 'PUT', corpo })),
      (r) => (aoSalvar ? aoSalvar(novo ? r.id : id) : navegar(`/clientes/${id}`)),
    )
  }

  if (!form) return <Carregando />

  function campo([k, rotulo, obrig, dica, tipo]) {
    let entrada
    if (tipo === 'senha') {
      entrada = (
        <input type="password" autoComplete="new-password" value={form[k] || ''}
          placeholder={form.senhaCadastrada ? 'Já cadastrada: deixe em branco para manter' : 'Guardada criptografada'}
          onChange={(e) => mudar(k, e.target.value)} />
      )
    } else if (tipo) {
      entrada = (
        <select value={form[k] || ''} onChange={(e) => mudar(k, e.target.value)}>
          <option value="">Não informado</option>
          {tipo.map((o) => {
            const [valor, nome] = Array.isArray(o) ? o : [o, o]
            return <option key={valor} value={valor}>{nome}</option>
          })}
        </select>
      )
    } else {
      const regra = REGRAS[k] || {}
      entrada = (
        <input id={`campo-${k}`} type="text" value={form[k] || ''} placeholder={regra.dica || dica || undefined}
          inputMode={regra.inputMode} autoComplete={regra.autoComplete} maxLength={regra.max}
          aria-invalid={erros[k] ? 'true' : undefined}
          onChange={(e) => mudar(k, regra.mascara ? regra.mascara(e.target.value) : e.target.value)}
          onBlur={() => setErros((x) => ({ ...x, [k]: erroDe(k, form[k]) }))} />
      )
    }
    return <Campo key={k} rotulo={rotulo} obrigatorio={obrig} erro={erros[k]}>{entrada}</Campo>
  }

  return (
    <>
      <Titulo titulo={novo ? 'Cadastro' : 'Editar cadastro'} sub="Campos com * são obrigatórios. Datas no formato dd/mm/aaaa." />
      {novo && triagem && <Alerta cor="info">Triagem concluída: {triagem}. Agora complete o cadastro.</Alerta>}
      <Card>
        <form onSubmit={salvar} noValidate className="flex flex-col gap-5">
          {SECOES.map(([titulo, conteudo]) => (
            <Secao key={titulo} titulo={titulo}>
              {conteudo === 'auxiliar' && (
                <Campo rotulo="Auxiliar jurídico">
                  <select value={form.auxiliarId ?? ''} onChange={(e) => mudar('auxiliarId', e.target.value)}>
                    <option value="">Não definido</option>
                    {(auxiliares || []).filter((a) => a.ativo || a.id === Number(form.auxiliarId))
                      .map((a) => <option key={a.id} value={a.id}>{a.nome}</option>)}
                  </select>
                </Campo>
              )}
              {conteudo === 'observacao' && (
                <Campo rotulo="Observações" className="col-span-full">
                  <textarea rows={3} maxLength={2000} value={form.observacoes || ''} onChange={(e) => mudar('observacoes', e.target.value)} />
                </Campo>
              )}
              {Array.isArray(conteudo) && conteudo.map(campo)}
              {titulo === 'Endereço do associado' && form.endereco && (
                <Campo rotulo="Endereço anterior (importado)">
                  <input type="text" value={form.endereco} onChange={(e) => mudar('endereco', e.target.value)} />
                </Campo>
              )}
            </Secao>
          ))}
          <div className="sticky bottom-0 -mx-5 -mb-5 flex flex-wrap gap-2 rounded-b-xl border-t border-slate-200 bg-white/95 px-5 py-3 backdrop-blur">
            <Botao type="submit" disabled={ocupado}>{novo ? 'Cadastrar' : 'Salvar alterações'}</Botao>
            {aoCancelar
              ? <Botao variante="neutro" onClick={aoCancelar}>Cancelar</Botao>
              : <BotaoLink to={novo ? '/clientes' : `/clientes/${id}`} variante="neutro">Cancelar</BotaoLink>}
          </div>
        </form>
      </Card>
    </>
  )
}
