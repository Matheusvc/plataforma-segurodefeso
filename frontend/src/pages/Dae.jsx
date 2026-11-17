import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, urlApi } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Botao, BotaoLink, Card, Carregando, Chip, Paginacao, Td, Th, Titulo, useAcao, useAviso, useDados } from '../components/ui.jsx'
import { mascaras, validadores } from '../formato.js'
import { br, brl, competencia, numeroBr } from '../util.js'

const TAMANHO = 25

export default function Dae() {
  const { meta } = useAuth()
  const aviso = useAviso()
  const [comp, setComp] = useState('')
  const [busca, setBusca] = useState('')
  const [termo, setTermo] = useState('')
  const [pagina, setPagina] = useState(0)
  const [edits, setEdits] = useState({})   // só as linhas alteradas, guardadas mesmo ao trocar de página
  useEffect(() => {
    const t = setTimeout(() => { setTermo(busca); setPagina(0) }, 350)
    return () => clearTimeout(t)
  }, [busca])
  const q = new URLSearchParams({ q: termo, pagina: String(pagina), tamanho: String(TAMANHO) })
  if (comp) q.set('competencia', comp)
  const { dados, recarregar } = useDados('/dae?' + q.toString())
  const { executar, ocupado } = useAcao()
  const st = Object.fromEntries(meta.statusDae.map((s) => [s.id, s]))
  const esocial = meta.sites.find((s) => s.id === 'esocial')?.extra

  const doServidor = (a) => {
    const d = dados.competencia && a.cliente.daes[dados.competencia]
    return { venda: d && d.venda != null ? numeroBr(d.venda) : '', status: d ? d.status : '' }
  }
  const linhaDe = (a) => ({ ...doServidor(a), ...(edits[a.cliente.id] || {}) })
  const alteradas = Object.keys(edits).length

  async function salvar() {
    if (alteradas === 0) {
      aviso.erro('Nenhuma linha foi alterada.')
      return
    }
    const itens = []
    for (const [id, e] of Object.entries(edits)) {
      const problema = validadores.dinheiro(e.venda)
      if (problema) {
        aviso.erro(problema)
        return
      }
      itens.push({ clienteId: Number(id), venda: e.venda ?? '', status: e.status ?? '' })
    }
    await executar(() => api('/dae', { metodo: 'POST', corpo: { competencia: dados.competencia, itens } }), () => {
      setEdits({})
      recarregar()
    })
  }

  const aliq = dados ? Number(dados.aliquota) : 0
  const estimado = (txt) => {
    const n = Number(String(txt).replace(/\./g, '').replace(',', '.'))
    return txt && !Number.isNaN(n) ? brl((n * aliq) / 100) : '—'
  }

  return (
    <>
      <Titulo titulo="DAE no eSocial" sub={`Controle manual desde maio, só de clientes com entrevista confirmada. Valor estimado com a alíquota de ${String(aliq).replace('.', ',')}%; o oficial é o da guia emitida no eSocial.`} />
      {!dados ? <Carregando /> : (
        <>
          <Card className="flex flex-wrap items-end justify-between gap-3">
            <label className="flex flex-col gap-1.5 text-xs font-semibold text-slate-600">Competência
              <select value={dados.competencia} onChange={(e) => { setComp(e.target.value); setPagina(0); setEdits({}) }}>
                {[...dados.competencias].reverse().map((c) => <option key={c} value={c}>{competencia(c)}</option>)}
              </select>
            </label>
            <label className="flex min-w-[200px] flex-[1_1_220px] flex-col gap-1.5 text-xs font-semibold text-slate-600">Buscar cliente
              <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Nome, CPF, município ou colônia" />
            </label>
            <div className="flex flex-wrap items-center gap-2">
              <Chip cor="erro">Em aberto: {dados.resumo.EM_ABERTO.toLocaleString('pt-BR')}</Chip>
              {meta.statusDae.map((s) => <Chip key={s.id} cor={s.cor}>{s.nome}: {dados.resumo[s.id].toLocaleString('pt-BR')}</Chip>)}
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
                {dados.clientes.length === 0 && (
                  <tr><Td className="py-6 text-slate-500" colSpan={6}>
                    Nenhum cliente com entrevista confirmada{termo ? ' para esta busca' : ''}. Confirme a entrevista na tela <Link to="/entrevistas" className="text-marca-700 underline">Entrevistas</Link>.
                  </Td></tr>
                )}
                {dados.clientes.map((a) => {
                  const c = a.cliente
                  const l = linhaDe(a)
                  const mudar = (k, v) => setEdits((x) => ({ ...x, [c.id]: { ...(x[c.id] || {}), [k]: v } }))
                  const erroVenda = validadores.dinheiro(l.venda)
                  return (
                    <tr key={c.id} className={edits[c.id] ? 'bg-amber-50/60' : undefined}>
                      <Td><Link to={`/clientes/${c.id}?aba=dae`} className="font-semibold text-marca-700 hover:underline">{c.nome}</Link><div className="text-xs text-slate-500">{c.colonia}</div></Td>
                      <Td className="whitespace-nowrap">{a.daeUltimosMeses.map((m) => (
                        <Chip key={m.competencia} cor={m.status ? st[m.status].cor : 'neutro'} className="mr-0.5">{m.label.slice(0, 3)}</Chip>
                      ))}</Td>
                      <Td className="min-w-[140px]">
                        <input type="text" inputMode="decimal" placeholder="0,00" className="text-right" aria-label={`Venda de ${c.nome}`}
                          aria-invalid={erroVenda ? 'true' : undefined} maxLength={13} value={l.venda}
                          onChange={(e) => mudar('venda', mascaras.dinheiro(e.target.value))} />
                        {erroVenda && <div role="alert" className="mt-1 text-xs text-red-700">{erroVenda}</div>}
                      </Td>
                      <Td>{estimado(l.venda)}</Td>
                      <Td className="min-w-[150px]">
                        <select value={l.status || ''} aria-label={`Situação do DAE de ${c.nome}`} onChange={(e) => mudar('status', e.target.value)}>
                          <option value="">Em aberto</option>
                          {meta.statusDae.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}
                        </select>
                      </Td>
                      <Td><Chip cor={a.procuracaoValida ? 'ok' : 'erro'}>{a.procuracaoValida ? `até ${br(c.procuracaoValidade)}` : 'sem procuração válida'}</Chip></Td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            <Paginacao pagina={dados.pagina} tamanho={dados.tamanho} total={dados.total} aoMudar={setPagina} />
            <div className="flex flex-wrap items-center gap-3 border-t border-slate-200 px-4 py-3.5">
              <Botao onClick={salvar} disabled={ocupado || alteradas === 0}>Salvar lançamentos de {competencia(dados.competencia)}{alteradas ? ` (${alteradas})` : ''}</Botao>
              {alteradas > 0 && <Botao variante="neutro" onClick={() => setEdits({})}>Descartar alterações</Botao>}
              <span className="text-xs text-slate-500">Fluxo: lance a venda → emita e declare a guia no eSocial ("Declarado") → quando pagar, marque "Pago" ou anexe o comprovante na ficha. As linhas alteradas ficam guardadas ao trocar de página.</span>
            </div>
          </Card>
        </>
      )}
    </>
  )
}
