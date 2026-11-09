import { useState } from 'react'
import { Link } from 'react-router-dom'
import { urlApi } from '../api.js'
import { BotaoLink, Card, Carregando, Chip, Pills, Td, Th, Titulo, useDados } from '../components/ui.jsx'

const TIPOS = ['Todos', 'Exigência', 'Recurso', 'DAE', 'RGP', 'Procuração', 'Defeso']

export default function Prazos() {
  const [tipo, setTipo] = useState('Todos')
  const { dados } = useDados('/prazos?tipo=' + encodeURIComponent(tipo))
  return (
    <>
      <Titulo titulo="Prazos" sub="Todos os vencimentos em ordem: exigências, recursos, DAE, RGP, procurações e defesos. Prazos sem data (recurso) aparecem primeiro." />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Pills opcoes={TIPOS.map((t) => [t, t])} valor={tipo} onChange={setTipo} />
        <BotaoLink href={urlApi('/planilhas/exportar/prazos.xlsx')} variante="neutro">Exportar planilha</BotaoLink>
      </div>
      {!dados ? <Carregando /> : (
        <Card semPad className="overflow-x-auto">
          <table className="w-full min-w-[760px] border-collapse">
            <thead><tr><Th>Data</Th><Th>Tipo</Th><Th>Cliente</Th><Th>Descrição</Th><Th>Situação</Th></tr></thead>
            <tbody>
              {dados.map((p, i) => (
                <tr key={i}>
                  <Td><strong>{p.dataBr}</strong></Td>
                  <Td><Chip cor={p.cor === 'neutro' ? 'info' : p.cor}>{p.tipo}</Chip></Td>
                  <Td>{p.clienteId ? <Link to={`/clientes/${p.clienteId}`} className="text-marca-700 hover:underline">{p.cliente}</Link> : p.cliente}</Td>
                  <Td>{p.texto}</Td>
                  <Td><Chip cor={p.cor}>{p.falta}</Chip></Td>
                </tr>
              ))}
              {dados.length === 0 && <tr><Td className="py-6 text-slate-500">Nenhum prazo neste filtro.</Td></tr>}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
