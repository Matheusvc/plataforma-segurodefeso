import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import ConferenciaCpf from '../components/ConferenciaCpf.jsx'
import Documentos from '../components/Documentos.jsx'
import SeletorEntrevista from '../components/SeletorEntrevista.jsx'
import { Alerta, Botao, Card, Carregando, Chip, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { limparRascunho, lerRascunho, salvarRascunho } from '../rascunho.js'
import ClienteForm from './ClienteForm.jsx'
import Triagem from './Triagem.jsx'

const PASSOS = ['Triagem', 'Cadastro', 'Documentos', 'Entrevista']

function Passos({ atual, aoIr, criado }) {
  return (
    <ol className="flex items-center gap-2 overflow-x-auto" aria-label="Passos do cadastro">
      {PASSOS.map((nome, i) => {
        const n = i + 1
        const feito = n < atual
        // Depois de salvo, os passos 1 e 2 travam; 3 e 4 ficam livres para ir e voltar.
        const pode = criado ? n >= 3 && n !== atual : feito
        return (
          <li key={nome} className="flex shrink-0 items-center gap-2" aria-current={n === atual ? 'step' : undefined}>
            <button type="button" disabled={!pode} onClick={() => aoIr(n)}
              className={`flex items-center gap-2 rounded-full border px-3 py-1.5 text-[13px] font-semibold disabled:cursor-default ${n === atual
                ? 'border-marca-700 bg-marca-700 text-white'
                : feito ? 'border-green-300 bg-green-50 text-green-800' : 'border-slate-300 bg-white text-slate-500'}`}>
              <span aria-hidden="true">{feito ? '✓' : n}</span>{nome}
            </button>
            {n < PASSOS.length && <span className="h-px w-4 bg-slate-300" aria-hidden="true" />}
          </li>
        )
      })}
    </ol>
  )
}

const avisarMudanca = () => window.dispatchEvent(new Event('cadastros-mudaram'))

/**
 * Cadastro de um novo cliente do começo ao fim: triagem, dados, documentos e entrevista.
 * O progresso não se perde ao trocar de menu: antes de salvar fica num rascunho desta aba;
 * depois de salvo, o cliente aparece em Cadastro > Falta finalizar e dá para retomar (?cliente=ID).
 */
export default function NovoCadastro() {
  const { usuario } = useAuth()
  const navegar = useNavigate()
  const [params] = useSearchParams()
  const retomar = params.get('cliente')
  const { executar, ocupado } = useAcao()
  const [rascunho] = useState(() => (retomar ? null : lerRascunho(usuario.id)))
  const [recuperado, setRecuperado] = useState(Boolean(rascunho))
  const [passo, setPasso] = useState(retomar ? 3 : rascunho?.passo || 1)
  const [respostas, setRespostas] = useState(rascunho?.respostas || {})
  const [triagem, setTriagem] = useState(rascunho?.triagem || null)
  const [formAtual, setFormAtual] = useState(rascunho?.form || null)
  const [clienteId, setClienteId] = useState(retomar ? Number(retomar) : null)
  const { dados, recarregar } = useDados(clienteId ? `/clientes/${clienteId}` : null)

  const a = dados?.analise
  const c = a?.cliente

  useEffect(() => {
    if (clienteId) return
    const { senhaGov, senhaCadastrada, ...semSenha } = formAtual || {}
    salvarRascunho(usuario.id, { passo, respostas, triagem, form: semSenha })
  }, [passo, respostas, triagem, formAtual, clienteId, usuario.id])

  function recomecar() {
    limparRascunho(usuario.id)
    setRecuperado(false)
    setPasso(1)
    setRespostas({})
    setTriagem(null)
    setFormAtual(null)
    setClienteId(null)
    navegar('/clientes/novo', { replace: true })
  }

  const finalizar = (depois) =>
    executar(() => api(`/clientes/${clienteId}/concluir-cadastro`, { metodo: 'PUT' }), () => {
      avisarMudanca()
      depois()
    })

  const mudarEntrevista = (situacao) =>
    executar(() => api(`/clientes/${clienteId}/entrevista`, { metodo: 'PUT', corpo: { situacao } }), recarregar)

  const continuarDepois = () => navegar('/clientes?filtro=andamento')

  return (
    <>
      <Titulo titulo="Novo cadastro" sub="Passo a passo completo: triagem, dados do cliente, documentos e entrevista. O que você preenche fica guardado se precisar sair." />
      <Passos atual={passo} aoIr={setPasso} criado={Boolean(clienteId)} />

      {recuperado && !clienteId && (
        <Alerta cor="info">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span>Rascunho recuperado: você parou no passo {passo} ({PASSOS[passo - 1]}).</span>
            <Botao variante="neutro" pequeno onClick={recomecar}>Descartar e começar de novo</Botao>
          </div>
        </Alerta>
      )}

      {passo === 1 && (
        <Triagem respostasIniciais={respostas} aoMudar={setRespostas}
          aoContinuar={(r, titulo) => { setRespostas(r); setTriagem(titulo); setPasso(2) }} />
      )}

      {passo === 2 && (
        <>
          <div>
            <Botao variante="neutro" pequeno onClick={() => setPasso(1)}>‹ Voltar à triagem</Botao>
          </div>
          <ClienteForm respostas={respostas} triagem={triagem} rascunho={formAtual} aoRascunho={setFormAtual}
            aoSalvar={(id) => { limparRascunho(usuario.id); setRecuperado(false); setClienteId(id); setPasso(3); avisarMudanca() }} />
        </>
      )}

      {passo === 3 && (!dados ? <Carregando /> : (
        <>
          <Alerta cor="ok">
            {retomar ? 'Retomando o cadastro de ' : ''}<strong>{c.nome}</strong>{retomar ? '.' : ' cadastrado.'}{' '}
            Anexe os documentos agora ou deixe para depois: ele fica em Cadastro › Falta finalizar.
          </Alerta>
          <Card>
            <Documentos a={a} id={clienteId} docs={dados.documentos} dae={dados.dae} recarregar={recarregar} />
          </Card>
          <div className="flex flex-wrap items-center gap-3">
            <Botao onClick={() => setPasso(4)}>Continuar para a entrevista ›</Botao>
            <Botao variante="neutro" onClick={continuarDepois}>Salvar e continuar depois</Botao>
            {a.documentosPendentes.length > 0 && <Chip cor="aviso">{a.documentosPendentes.length} documento(s) pendente(s)</Chip>}
          </div>
        </>
      ))}

      {passo === 4 && (!dados ? <Carregando /> : (
        <>
          <Card className="flex flex-col gap-4">
            <div>
              <h2 className="text-base font-semibold">Entrevista</h2>
              <p className="text-sm text-slate-500">O DAE só é lançado para clientes com a entrevista confirmada.</p>
            </div>
            <SeletorEntrevista valor={c.entrevista} nome={c.nome} desabilitado={ocupado} aoMudar={mudarEntrevista} />
            <ConferenciaCpf c={c} por={dados.cpfConferidoPor} recarregar={recarregar} />
          </Card>
          {a.apto
            ? <Alerta cor="ok"><strong>Apto para requerer.</strong> Faça o pedido no Emprega Brasil e registre o protocolo em Atualizar status.</Alerta>
            : <Alerta cor="aviso"><strong>Ainda falta para requerer:</strong> {a.bloqueios.join('; ')}.</Alerta>}
          <div className="flex flex-wrap gap-2">
            <Botao disabled={ocupado} onClick={() => finalizar(() => navegar(`/clientes/${clienteId}`))}>Finalizar cadastro</Botao>
            <Botao variante="sec" disabled={ocupado} onClick={() => finalizar(recomecar)}>Finalizar e cadastrar outro</Botao>
            <Botao variante="neutro" onClick={continuarDepois}>Salvar e continuar depois</Botao>
          </div>
        </>
      ))}
    </>
  )
}
