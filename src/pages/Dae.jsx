import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, urlApi } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Botao, BotaoLink, Card, Carregando, Chip, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { br, brl, competencia, numeroBr } from '../util.js'

export default function Dae() {
  const { meta } = useAuth()
  const [comp, setComp] = useState('')
  const { dados, recarregar } = useDados('/dae' + (comp ? `?competencia=${comp}` : ''))
  const { executar, ocupado } = useAcao()
  const [linhas, setLinhas] = useState({})
  const st = Object.fromEntries(meta.statusDae.map((s) => [s.id, s]))
  const esocial = meta.sites.find((s) => s.id === 'esocial')?.extra

  useEffect(() => {
    if (!dados) return
    const l = {}
    dados.clientes.forEach((a) => {
      const d = a.cliente.daes[dados.competencia]
      l[a.cliente.id] = { venda: d && d.venda != null ? numeroBr(d.venda) : '', status: d ? d.status : '' }
    })
    setLinhas(l)
  }, [dados])

  async function salvar() {
    const itens = Object.entries(linhas).map(([id, v]) => ({ clienteId: Number(id), venda: v.venda, status: v.status }))
    await executar(() => api('/dae', { metodo: 'POST', corpo: { competencia: dados.competencia, itens } }), recarregar)
  }

  const aliq = dados ? Number(dados.aliquota) : 0
  const estimado = (txt) => {
    const n = Number(String(txt).replace(/\./g, '').replace(',', '.'))
    return txt && !Number.isNaN(n) ? brl((n * aliq) / 100) : '—'
  }

  return (
    <>
      <Titulo titulo="DAE no eSocial" sub={`Lance a venda do mês de cada cliente e acompanhe as guias. Valor estimado com a alíquota de ${String(aliq).replace('.', ',')}%; o oficial é o da guia emitida no eSocial.`} />
      {!dados ? <Carregando /> : (
        <>
          <Card className="flex flex-wrap items-end justify-between gap-3">
            <label className="flex flex-col gap-1.5 text-xs font-semibold text-slate-600">Competência
              <select value={dados.competencia} onChange={(e) => setComp(e.target.value)}>
                {[...dados.competencias].reverse().map((c) => <option key={c} value={c}>{competencia(c)}</option>)}
              </select>
            </label>
            <div className="flex flex-wrap items-center gap-2">
              {meta.statusDae.map((s) => <Chip key={s.id} cor={s.cor}>{s.nome}: {dados.resumo[s.id]}</Chip>)}
              <span className="text-xs text-slate-500">Vencimento estimado: {br(dados.vencimento)} (conferir na guia)</span>
            </div>
            <div className="flex flex-wrap gap-2">
              <BotaoLink href={esocial} target="_blank" rel="noopener noreferrer" variante="sec">Abrir eSocial ›</BotaoLink>
              <BotaoLink href={urlApi('/planilhas/exportar/dae.xlsx')} variante="neutro">Exportar planilha</BotaoLink>
            </div>
          </Card>
          <Card semPad className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse">
              <thead><tr><Th>Cliente</Th><Th>Últimos meses</Th><Th>Venda em {competencia(dados.competencia)} (R$)</Th><Th>DAE estimado</Th><Th>Situação</Th><Th>Procuração</Th></tr></thead>
              <tbody>
                {dados.clientes.map((a) => {
                  const c = a.cliente
                  const l = linhas[c.id] || { venda: '', status: '' }
                  const mudar = (k, v) => setLinhas((x) => ({ ...x, [c.id]: { ...l, [k]: v } }))
                  return (
                    <tr key={c.id}>
                      <Td><Link to={`/clientes/${c.id}?aba=dae`} className="font-semibold text-marca-700 hover:underline">{c.nome}</Link><div className="text-xs text-slate-500">{c.colonia}</div></Td>
                      <Td className="whitespace-nowrap">{a.daeUltimosMeses.slice(-4).map((m) => (
                        <Chip key={m.competencia} cor={m.status ? st[m.status].cor : 'neutro'} className="mr-0.5">{m.label.slice(0, 3)}</Chip>
                      ))}</Td>
                      <Td className="min-w-[140px]"><input type="text" inputMode="decimal" placeholder="0,00" className="text-right" value={l.venda} onChange={(e) => mudar('venda', e.target.value)} /></Td>
                      <Td>{estimado(l.venda)}</Td>
                      <Td className="min-w-[150px]">
                        <select value={l.status || ''} onChange={(e) => mudar('status', e.target.value)}>
                          <option value="">sem lançamento</option>
                          {meta.statusDae.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}
                        </select>
                      </Td>
                      <Td><Chip cor={a.procuracaoValida ? 'ok' : 'erro'}>{a.procuracaoValida ? `até ${br(c.procuracaoValidade)}` : 'sem procuração válida'}</Chip></Td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            <div className="flex flex-wrap items-center gap-3 border-t border-slate-200 px-4 py-3.5">
              <Botao onClick={salvar} disabled={ocupado}>Salvar lançamentos de {competencia(dados.competencia)}</Botao>
              <span className="text-xs text-slate-500">Fluxo: lance a venda → emita a guia no eSocial e marque "Gerado" → envie ao cliente ("Enviado") → anexe o comprovante na ficha ("Pago").</span>
            </div>
          </Card>
        </>
      )}
    </>
  )
}
