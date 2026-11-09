import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, urlApi } from '../api.js'
import { Botao, BotaoLink, Card, Carregando, Chip, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { brl, numeroBr } from '../util.js'

export default function Honorarios() {
  const { dados, recarregar } = useDados('/honorarios')
  const { executar, ocupado } = useAcao()
  const [valores, setValores] = useState({})

  async function registrar(a) {
    const valor = valores[a.cliente.id] ?? numeroBr(a.honorariosPorParcela)
    await executar(() => api(`/clientes/${a.cliente.id}/honorarios`, { metodo: 'POST', corpo: { valor } }), recarregar)
  }

  if (!dados) return <Carregando />
  return (
    <>
      <Titulo titulo="Honorários" sub={`Clientes deferidos: parcelas liberadas, honorários devidos, recebidos e saldo. Cálculo: % do contrato sobre o salário mínimo vigente (${brl(dados.salarioMinimo)}) por parcela.`} />
      <div className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-3.5">
        {[['Saldo a receber', dados.saldo, 'Parcelas já liberadas'], ['Recebido', dados.recebido, 'Clientes listados'], ['A liberar', dados.futuro, 'Parcelas futuras']].map(([k, v, d]) => (
          <Card key={k}><div className="text-[13px] text-slate-600">{k}</div><div className="mt-1 text-[27px] font-bold">{brl(v)}</div><div className="text-xs text-slate-500">{d}</div></Card>
        ))}
      </div>
      <div><BotaoLink href={urlApi('/planilhas/exportar/honorarios.xlsx')} variante="neutro">Exportar planilha</BotaoLink></div>
      <Card semPad className="overflow-x-auto">
        <table className="w-full min-w-[940px] border-collapse">
          <thead><tr><Th>Cliente</Th><Th>Parcelas</Th><Th>Por parcela</Th><Th>Devido</Th><Th>Recebido</Th><Th>Saldo</Th><Th>Registrar recebimento</Th></tr></thead>
          <tbody>
            {dados.clientes.map((a) => {
              const saldo = Number(a.honorariosSaldo)
              return (
                <tr key={a.cliente.id}>
                  <Td><Link to={`/clientes/${a.cliente.id}?aba=financeiro`} className="font-semibold text-marca-700 hover:underline">{a.cliente.nome}</Link>
                    <div className="text-xs text-slate-500">{Number(a.honorariosPct).toLocaleString('pt-BR')}%</div></Td>
                  <Td>{a.cliente.parcelasLiberadas} de {a.cliente.parcelasTotal}</Td>
                  <Td>{brl(a.honorariosPorParcela)}</Td><Td>{brl(a.honorariosDevidos)}</Td><Td>{brl(a.honorariosRecebidos)}</Td>
                  <Td><Chip cor={saldo > 0.009 ? 'aviso' : 'ok'}>{brl(saldo)}</Chip></Td>
                  <Td>{saldo > 0.009 ? (
                    <div className="flex items-center gap-2">
                      <input type="text" aria-label="Valor recebido" className="!w-[110px]" value={valores[a.cliente.id] ?? numeroBr(a.honorariosPorParcela)}
                        onChange={(e) => setValores({ ...valores, [a.cliente.id]: e.target.value })} />
                      <Botao pequeno disabled={ocupado} onClick={() => registrar(a)}>Registrar</Botao>
                    </div>
                  ) : <span className="text-xs text-slate-500">quitado</span>}</Td>
                </tr>
              )
            })}
            {dados.clientes.length === 0 && <tr><Td className="py-6 text-slate-500">Nenhum cliente deferido com parcelas registradas.</Td></tr>}
          </tbody>
        </table>
      </Card>
    </>
  )
}
