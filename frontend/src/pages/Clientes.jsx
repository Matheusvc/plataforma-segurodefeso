import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { urlApi } from '../api.js'
import { BotaoLink, Card, Carregando, Chip, Pills, Td, Th, Titulo, useDados } from '../components/ui.jsx'
import { cpfMascarado } from '../util.js'

export default function Clientes() {
  const { meta } = useAuth()
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState('todos')
  const [arquivados, setArquivados] = useState(false)
  const [termo, setTermo] = useState('')
  useEffect(() => {
    const t = setTimeout(() => setTermo(busca), 350)
    return () => clearTimeout(t)
  }, [busca])
  const q = new URLSearchParams({ q: termo, filtro, arquivados: String(arquivados) })
  const { dados, erro } = useDados('/clientes?' + q.toString())
  const opcoes = [['todos', 'Todos'], ['pendencia', 'Com pendência'], ...meta.etapas.map((e) => [e.id, e.nome])]
  const nReq = meta.requisitos.length
  const nDoc = meta.documentos.filter((d) => d.flag).length

  return (
    <>
      <Titulo titulo="Clientes" sub="Cadastro único de cada pescador: requisitos, documentos, DAE, linha do tempo e honorários." />
      <Card className="flex flex-col gap-3.5">
        <div className="flex flex-wrap items-end gap-2">
          <label className="flex flex-[1_1_280px] flex-col gap-1.5 text-xs font-semibold text-slate-600">
            Buscar
            <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Nome, CPF, município ou colônia" />
          </label>
          <BotaoLink to="/clientes/novo">+ Novo cliente</BotaoLink>
          <BotaoLink href={urlApi('/planilhas/exportar/clientes.xlsx')} variante="neutro">Exportar planilha</BotaoLink>
        </div>
        <Pills opcoes={opcoes} valor={filtro} onChange={setFiltro} />
        <label className="flex items-center gap-2 text-xs text-slate-600">
          <input type="checkbox" checked={arquivados} onChange={(e) => setArquivados(e.target.checked)} /> incluir arquivados
        </label>
      </Card>
      {erro && <p className="text-sm text-red-700">{erro}</p>}
      {!dados ? <Carregando /> : (
        <Card semPad className="overflow-x-auto">
          <table className="w-full min-w-[860px] border-collapse">
            <thead><tr><Th>Cliente</Th><Th>Colônia</Th><Th>Requisitos</Th><Th>Documentos</Th><Th>DAE em aberto</Th><Th>Etapa</Th><Th /></tr></thead>
            <tbody>
              {dados.map((a) => {
                const c = a.cliente
                const nDae = a.daeVencidos.length + a.daeAbertos.length
                const docsOk = nDoc - a.documentosPendentes.length
                return (
                  <tr key={c.id} className="hover:bg-slate-50">
                    <Td>
                      <Link to={`/clientes/${c.id}`} className="font-semibold text-marca-700 hover:underline">{c.nome}</Link>
                      <div className="text-xs text-slate-500">CPF {cpfMascarado(c.cpf)}{!c.ativo && ' · arquivado'}</div>
                    </Td>
                    <Td>{c.colonia || '—'}<div className="text-xs text-slate-500">{c.municipio}</div></Td>
                    <Td><Chip cor={a.requisitosNaoAtendidos.length === 0 && a.requisitosSemResposta === 0 ? 'ok' : a.essencialFalha ? 'erro' : 'aviso'}>{a.requisitosOk}/{nReq}</Chip></Td>
                    <Td><Chip cor={a.documentosPendentes.length === 0 ? 'ok' : a.documentosPendentes.length > 2 ? 'erro' : 'aviso'}>{docsOk}/{nDoc}</Chip></Td>
                    <Td><Chip cor={a.daeVencidos.length ? 'erro' : nDae ? 'aviso' : 'ok'}>{nDae ? `${nDae}${a.daeVencidos.length ? ' (vencido)' : ''}` : 'Em dia'}</Chip></Td>
                    <Td><Chip cor={a.etapaCor}>{a.etapaNome}</Chip></Td>
                    <Td><BotaoLink to={`/clientes/${c.id}`} variante="neutro" pequeno>Ficha</BotaoLink></Td>
                  </tr>
                )
              })}
              {dados.length === 0 && (
                <tr><Td className="py-6 text-slate-500">Nenhum cliente encontrado. Cadastre um novo ou importe a planilha em Planilhas.</Td></tr>
              )}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
