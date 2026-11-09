import { Card, Carregando, Item, Titulo, useDados } from '../components/ui.jsx'

export default function Relatorios() {
  const { dados } = useDados('/relatorios')
  if (!dados) return <Carregando />
  return (
    <>
      <Titulo titulo="Relatórios" sub="Desempenho por colônia, indicadores do escritório e o que mais trava os pedidos." />
      <div className="flex flex-wrap items-start gap-4">
        <Card className="min-w-0 flex-[3_1_480px]">
          <h2 className="mb-3 text-base font-semibold">Por colônia</h2>
          <div className="flex flex-col gap-3">
            {dados.colonias.map((o) => (
              <div key={o.colonia} className="grid grid-cols-[180px_1fr_170px] items-center gap-3 text-[13px]">
                <div><strong>{o.colonia}</strong><div className="text-xs text-slate-500">{o.municipio}</div></div>
                <div className="h-3 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-marca-700" style={{ width: `${o.pct}%` }} /></div>
                <span className="text-right">{o.clientes} cliente(s) · {o.deferimento}</span>
              </div>
            ))}
            {dados.colonias.length === 0 && <p className="text-sm text-slate-500">Sem clientes.</p>}
          </div>
        </Card>
        <div className="flex min-w-0 flex-[2_1_320px] flex-col gap-4">
          <Card>
            <h2 className="mb-3 text-base font-semibold">Indicadores</h2>
            <div className="grid grid-cols-2 gap-2.5">
              {dados.indicadores.map((i) => <Item key={i.rotulo} className="flex-col items-start"><div className="text-xs text-slate-500">{i.rotulo}</div><div className="text-[22px] font-bold">{i.valor}</div></Item>)}
            </div>
          </Card>
          <Card>
            <h2 className="mb-2 text-base font-semibold">O que mais trava os pedidos</h2>
            {dados.motivos.map((m) => (
              <div key={m.motivo} className="flex justify-between border-b border-slate-100 py-1.5 text-[13px]"><span className="first-letter:uppercase">{m.motivo}</span><strong>{m.qtd}</strong></div>
            ))}
            {dados.motivos.length === 0 && <p className="text-sm text-slate-500">Nenhuma pendência.</p>}
          </Card>
        </div>
      </div>
    </>
  )
}
