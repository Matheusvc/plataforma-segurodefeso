import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Card, Carregando, Chip, Pills, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'

const ATIVO = {
  ok: 'border-green-700 bg-green-700 text-white',
  erro: 'border-red-700 bg-red-700 text-white',
  aviso: 'border-amber-500 bg-amber-500 text-white',
}

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
                    <div className="flex flex-wrap gap-1.5" role="group" aria-label={`Entrevista de ${c.nome}`}>
                      {meta.entrevistas.map((e) => (
                        <button key={e.id} type="button" disabled={ocupado} onClick={() => marcar(c, e.id)}
                          aria-pressed={c.entrevista === e.id}
                          className={`min-h-[34px] rounded-lg border px-3 text-[13px] font-semibold disabled:opacity-60 ${c.entrevista === e.id
                            ? ATIVO[e.cor] : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'}`}>
                          {e.nome}
                        </button>
                      ))}
                    </div>
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
