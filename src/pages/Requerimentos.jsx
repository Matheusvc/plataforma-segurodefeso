import { Link } from 'react-router-dom'
import { urlApi } from '../api.js'
import { useAuth } from '../auth.jsx'
import { BotaoLink, Carregando, Chip, Titulo, useDados } from '../components/ui.jsx'
import { br } from '../util.js'

export default function Requerimentos() {
  const { meta } = useAuth()
  const { dados } = useDados('/clientes')
  return (
    <>
      <Titulo titulo="Requerimentos" sub="Cada pedido do seguro-defeso por etapa. Clique no cliente para abrir a ficha." />
      <div className="flex flex-wrap gap-2">
        <BotaoLink to="/status">Conferir status dos pedidos</BotaoLink>
        <BotaoLink href={urlApi('/planilhas/exportar/requerimentos.xlsx')} variante="neutro">Exportar planilha</BotaoLink>
      </div>
      {!dados ? <Carregando /> : (
        <div className="overflow-x-auto">
          <div className="grid min-w-[1340px] grid-cols-6 gap-3">
            {meta.etapas.map((e) => {
              const lista = dados.filter((a) => a.cliente.etapa === e.id)
              return (
                <section key={e.id} className="flex min-h-[300px] flex-col gap-2.5 rounded-xl bg-slate-200/60 p-3">
                  <div className="flex items-center justify-between"><h2 className="text-sm font-semibold">{e.nome}</h2><Chip className="bg-white">{lista.length}</Chip></div>
                  {lista.map((a) => <Cartao key={a.cliente.id} a={a} etapa={e.id} />)}
                </section>
              )
            })}
          </div>
        </div>
      )}
    </>
  )
}

function Cartao({ a, etapa }) {
  const c = a.cliente
  return (
    <div className="flex flex-col gap-1.5 rounded-lg border border-borda bg-white p-3">
      <Link to={`/clientes/${c.id}`} className="font-semibold text-marca-700 hover:underline">{c.nome}</Link>
      <div className="text-xs text-slate-500">{c.municipio}{c.colonia && ` · ${c.colonia}`}</div>
      {(etapa === 'DOCUMENTACAO' || etapa === 'A_REQUERER') && (a.apto
        ? <><Chip cor="ok">Apto</Chip><BotaoLink to={`/status?cliente=${c.id}`} pequeno>Fiz o pedido: registrar protocolo</BotaoLink></>
        : <><div className="text-xs text-slate-600">Pendências: {a.bloqueios.join('; ')}</div><BotaoLink to={`/clientes/${c.id}?aba=requisitos`} variante="neutro" pequeno>Ver pendências</BotaoLink></>)}
      {etapa === 'REQUERIDO' && <><div className="text-xs text-slate-600">Protocolo {c.protocolo || '—'} · conferido {br(c.ultimaConferencia) || 'nunca'}</div>
        <BotaoLink to={`/status?cliente=${c.id}`} variante="sec" pequeno>Atualizar status</BotaoLink></>}
      {etapa === 'EXIGENCIA' && <><div className="text-xs text-slate-600">{c.exigenciaTexto}</div><Chip cor="erro">Cumprir até {br(c.prazoData) || '?'}</Chip>
        <BotaoLink to={`/mensagens?cliente=${c.id}&modelo=EXIGENCIA`} pequeno>Pedir documento</BotaoLink>
        <BotaoLink to={`/status?cliente=${c.id}`} variante="sec" pequeno>Atualizar status</BotaoLink></>}
      {etapa === 'DEFERIDO' && <><div className="text-xs text-slate-600">{c.parcelasLiberadas} de {c.parcelasTotal} parcelas liberadas</div>
        <BotaoLink to={`/status?cliente=${c.id}`} variante="sec" pequeno>Atualizar parcelas</BotaoLink></>}
      {etapa === 'INDEFERIDO' && <><div className="text-xs text-slate-600">{c.indeferimentoMotivo}</div><Chip cor="erro">Prazo recursal: conferir na decisão</Chip>
        <BotaoLink to={`/clientes/${c.id}?aba=linha`} variante="sec" pequeno>Ver histórico</BotaoLink></>}
    </div>
  )
}
