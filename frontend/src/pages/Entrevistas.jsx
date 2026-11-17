import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import SeletorEntrevista from '../components/SeletorEntrevista.jsx'
import { Card, Carregando, Chip, Paginacao, Pills, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'

const TAMANHO = 25

export default function Entrevistas() {
  const { meta } = useAuth()
  const [filtro, setFiltro] = useState('todos')
  const [busca, setBusca] = useState('')
  const [termo, setTermo] = useState('')
  const [pagina, setPagina] = useState(0)
  useEffect(() => {
    const t = setTimeout(() => { setTermo(busca); setPagina(0) }, 350)
    return () => clearTimeout(t)
  }, [busca])
  const q = new URLSearchParams({ q: termo, situacao: filtro === 'todos' ? '' : filtro, pagina: String(pagina), tamanho: String(TAMANHO) })
  const { dados, erro, recarregar } = useDados('/entrevistas?' + q.toString())
  const { executar, ocupado } = useAcao()

  const totais = dados?.totais || {}
  const geral = Object.values(totais).reduce((soma, n) => soma + n, 0)

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
          <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Nome, CPF, município ou colônia" />
        </label>
        <Pills valor={filtro} onChange={(v) => { setFiltro(v); setPagina(0) }}
          opcoes={[['todos', `Todos (${geral.toLocaleString('pt-BR')})`], ...meta.entrevistas.map((e) => [e.id, `${e.nome} (${(totais[e.id] ?? 0).toLocaleString('pt-BR')})`])]} />
      </Card>
      {erro && <p className="text-sm text-red-700">{erro}</p>}
      {!dados ? <Carregando /> : (
        <Card semPad className="overflow-x-auto">
          <table className="w-full min-w-[720px] border-collapse">
            <thead><tr><Th>Cliente</Th><Th>Etapa</Th><Th>Entrevista</Th></tr></thead>
            <tbody>
              {dados.itens.map((c) => (
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
              {dados.itens.length === 0 && <tr><Td className="py-6 text-slate-500">Nenhum cliente neste filtro.</Td></tr>}
            </tbody>
          </table>
          <Paginacao pagina={dados.pagina} tamanho={dados.tamanho} total={dados.total} aoMudar={setPagina} />
        </Card>
      )}
    </>
  )
}
