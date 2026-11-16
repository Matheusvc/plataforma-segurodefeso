import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import ConferenciaCpf from '../components/ConferenciaCpf.jsx'
import Documentos from '../components/Documentos.jsx'
import SeletorEntrevista from '../components/SeletorEntrevista.jsx'
import { Alerta, Botao, Card, Carregando, Chip, Titulo, useAcao, useDados } from '../components/ui.jsx'
import ClienteForm from './ClienteForm.jsx'
import Triagem from './Triagem.jsx'

const PASSOS = ['Triagem', 'Cadastro', 'Documentos', 'Entrevista']

function Passos({ atual, aoIr, travados }) {
  return (
    <ol className="flex items-center gap-2 overflow-x-auto" aria-label="Passos do cadastro">
      {PASSOS.map((nome, i) => {
        const n = i + 1
        const feito = n < atual
        const pode = feito && !travados.includes(n)
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

/** Cadastro de um novo cliente do começo ao fim, sem sair da tela: triagem, dados, documentos e entrevista. */
export default function NovoCadastro() {
  const navegar = useNavigate()
  const { executar, ocupado } = useAcao()
  const [passo, setPasso] = useState(1)
  const [triagem, setTriagem] = useState(null)
  const [clienteId, setClienteId] = useState(null)
  const { dados, recarregar } = useDados(clienteId ? `/clientes/${clienteId}` : null)

  const a = dados?.analise
  const c = a?.cliente
  const criado = clienteId ? [1, 2] : []

  function recomecar() {
    setPasso(1)
    setTriagem(null)
    setClienteId(null)
  }

  const mudarEntrevista = (situacao) =>
    executar(() => api(`/clientes/${clienteId}/entrevista`, { metodo: 'PUT', corpo: { situacao } }), recarregar)

  return (
    <>
      <Titulo titulo="Novo cadastro" sub="Passo a passo completo: triagem, dados do cliente, documentos e entrevista." />
      <Passos atual={passo} aoIr={setPasso} travados={criado} />

      {passo === 1 && (
        <Triagem aoContinuar={(respostas, titulo) => { setTriagem({ respostas, titulo }); setPasso(2) }} />
      )}

      {passo === 2 && (
        <>
          <div>
            <Botao variante="neutro" pequeno onClick={() => setPasso(1)}>‹ Voltar à triagem</Botao>
          </div>
          <ClienteForm respostas={triagem.respostas} triagem={triagem.titulo}
            aoSalvar={(id) => { setClienteId(id); setPasso(3) }} />
        </>
      )}

      {passo === 3 && (!dados ? <Carregando /> : (
        <>
          <Alerta cor="ok"><strong>{c.nome}</strong> cadastrado. Anexe os documentos agora ou deixe para depois.</Alerta>
          <Card>
            <Documentos a={a} id={clienteId} docs={dados.documentos} dae={dados.dae} recarregar={recarregar} />
          </Card>
          <div className="flex flex-wrap items-center gap-3">
            <Botao onClick={() => setPasso(4)}>Continuar para a entrevista ›</Botao>
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
            <Botao onClick={() => navegar(`/clientes/${clienteId}`)}>Concluir e abrir a ficha</Botao>
            <Botao variante="sec" onClick={recomecar}>Cadastrar outro cliente</Botao>
          </div>
        </>
      ))}
    </>
  )
}
