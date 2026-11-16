import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import SeletorEntrevista from '../components/SeletorEntrevista.jsx'
import { Card, Carregando, Chip, Pills, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'

export default function Entrevistas() {
  const { meta } = useAuth()
  const { dados, erro, recarregar } = useDados('/entrevistas')
  const { executar, ocupado } = useAcao()
  const [filtro, setFiltro] = useState('todos')
  const [busca, setBusca] = useState('')

  const termo = busca.trim().toLowerCase()
  const lista = (dados || []).filter((c) => (filtro === 'todos' || c.entrevista === filtro)
    && (!termo || c.nome.toLowerCase().includes(termo)))
  const contagem = (id) => (dados || []).filter((c) => c.entrevista === id).length

  function marcar(cliente, situacao) {
    if (cliente.entrevista === situacao) return
    executar(() => api(`/clientes/${cliente.id}/entrevista`, { metodo: 'PUT', corpo: { situacao } }), recarregar)
  }

  return (
    <>
      <Titulo titulo="Entrevistas"
        sub="Marque a situação da entrevista de cada cliente. O DAE só é lançado para quem estiver com a entrevista confirmada." />
      <Card className="flex flex-col gap-3.5">
        <label className="flex flex-col gap-1.5 text-xs font-semibold text-slate-600">
          Buscar cliente
          <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Nome" />
        </label>
        <Pills valor={filtro} onChange={setFiltro}
          opcoes={[['todos', `Todos (${(dados || []).length})`], ...meta.entrevistas.map((e) => [e.id, `${e.nome} (${contagem(e.id)})`])]} />
      </Card>
      {erro && <p className="text-sm text-red-700">{erro}</p>}
      {!dados ? <Carregando /> : (
        <Card semPad className="overflow-x-auto">
          <table className="w-full min-w-[720px] border-collapse">
            <thead><tr><Th>Cliente</Th><Th>Etapa</Th><Th>Entrevista</Th></tr></thead>
            <tbody>
              {lista.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50">
                  <Td>
                    <Link to={`/clientes/${c.id}`} className="font-semibold text-marca-700 hover:underline">{c.nome}</Link>
                    <div className="text-xs text-slate-500">{[c.colonia, c.municipio].filter(Boolean).join(' · ')}</div>
                  </Td>
                  <Td><Chip cor={c.etapaCor}>{c.etapa}</Chip></Td>
                  <Td>
                    <SeletorEntrevista valor={c.entrevista} nome={c.nome} desabilitado={ocupado} aoMudar={(s) => marcar(c, s)} />
                  </Td>
                </tr>
              ))}
              {lista.length === 0 && <tr><Td className="py-6 text-slate-500">Nenhum cliente neste filtro.</Td></tr>}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
