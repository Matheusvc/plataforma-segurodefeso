import { Link } from 'react-router-dom'
import { Card, Carregando, Chip, Td, Th, Titulo, useDados } from '../../components/ui.jsx'
import { br, hora } from '../../util.js'

export default function Atividades() {
  const { dados } = useDados('/admin/atividades')
  return (
    <>
      <Titulo titulo="Registro de atividades" sub="Quem fez o quê e quando (últimos 500 registros). Útil para auditoria e para a LGPD." />
      {!dados ? <Carregando /> : (
        <Card semPad className="overflow-x-auto">
          <table className="w-full min-w-[820px] border-collapse">
            <thead><tr><Th>Quando</Th><Th>Usuário</Th><Th>Cliente</Th><Th>Tipo</Th><Th>Descrição</Th></tr></thead>
            <tbody>
              {dados.map((e, i) => (
                <tr key={i}>
                  <Td className="whitespace-nowrap">{br(e.momento)} {hora(e.momento)}</Td>
                  <Td>{e.usuario || 'sistema'}</Td>
                  <Td>{e.clienteId ? <Link to={`/clientes/${e.clienteId}`} className="text-marca-700 hover:underline">{e.cliente}</Link> : '—'}</Td>
                  <Td><Chip>{e.tipo}</Chip></Td>
                  <Td>{e.descricao}</Td>
                </tr>
              ))}
              {dados.length === 0 && <tr><Td className="py-6 text-slate-500">Sem registros.</Td></tr>}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
