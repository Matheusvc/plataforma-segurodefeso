import { Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { BotaoLink, Card, Carregando, Chip, Item, Titulo, useDados } from '../components/ui.jsx'

export default function Inicio() {
  const { meta } = useAuth()
  const { dados, erro } = useDados('/inicio')
  return (
    <>
      <Titulo titulo="Início" sub="O que precisa ser feito hoje, em ordem de prioridade. Tudo é calculado a partir dos dados lançados." />
      {erro && <p className="text-sm text-red-700">{erro}</p>}
      {!dados ? <Carregando /> : (
        <>
          <div className="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-3.5">
            {dados.kpis.map((k) => (
              <Link key={k.rotulo} to={k.link.startsWith('#') ? '/' : k.link}
                className="rounded-xl border border-borda bg-white p-4 hover:border-marca-700">
                <div className="text-[13px] text-slate-600">{k.rotulo}</div>
                <div className="mt-1 text-[27px] font-bold">{k.valor}</div>
                <div className="text-xs text-slate-500">{k.dica}</div>
              </Link>
            ))}
          </div>
          <div className="flex flex-wrap items-start gap-4">
            <Card className="min-w-0 flex-[3_1_520px]" id="fila">
              <h2 className="mb-3 text-base font-semibold">Fila do dia, por prioridade</h2>
              {dados.fila.length === 0 && <p className="text-sm text-slate-500">Nada pendente. Bom trabalho.</p>}
              <div className="flex flex-col gap-2">
                {dados.fila.map((f, i) => (
                  <Item key={i}>
                    <Chip cor={f.cor} className="w-[76px] text-center">{f.prioridade}</Chip>
                    <div className="min-w-0 flex-[1_1_280px]">
                      <div className="font-semibold">{f.titulo}</div>
                      <div className="mt-0.5 text-[13px] text-slate-600">{f.detalhe}</div>
                    </div>
                    <span className="whitespace-nowrap text-xs text-slate-600">{f.prazo}</span>
                    <BotaoLink to={f.url} variante="sec" pequeno>{f.acao}</BotaoLink>
                  </Item>
                ))}
              </div>
            </Card>
            <div className="flex min-w-0 flex-[2_1_320px] flex-col gap-4">
              <Card>
                <h2 className="mb-3 text-base font-semibold">Funil dos requerimentos</h2>
                <div className="flex flex-col gap-2.5">
                  {dados.funil.map((f) => (
                    <div key={f.etapa} className="grid grid-cols-[120px_1fr_30px] items-center gap-2.5 text-[13px]">
                      <span>{f.etapa}</span>
                      <div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-marca-700" style={{ width: `${f.pct}%` }} /></div>
                      <strong className="text-right">{f.qtd}</strong>
                    </div>
                  ))}
                </div>
              </Card>
              <Card>
                <h2 className="mb-3 text-base font-semibold">Acesso rápido aos sites</h2>
                <div className="flex flex-col gap-2">
                  {meta.sites.map((s) => (
                    <a key={s.id} href={s.extra} target="_blank" rel="noopener noreferrer"
                      className="rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5 hover:border-marca-700">
                      <span className="font-semibold text-marca-700">{s.nome} ›</span>
                    </a>
                  ))}
                </div>
              </Card>
            </div>
          </div>
        </>
      )}
    </>
  )
}
