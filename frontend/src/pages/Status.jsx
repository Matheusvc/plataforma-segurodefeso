import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Botao, BotaoLink, Campo, Card, Carregando, Chip, Pills, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { br, cpf } from '../util.js'

const FILTROS = [['andamento', 'Requerido e em exigência'], ['a_requerer', 'A requerer'], ['deferidos', 'Deferidos com parcelas a liberar'], ['todos', 'Todos os clientes']]
const SITUACOES = [['analise', 'Em análise'], ['exigencia', 'Exigência'], ['deferido', 'Deferido'], ['indeferido', 'Indeferido'], ['naoreq', 'Ainda não requerido'], ['naoachou', 'Pedido não encontrado']]

function naFila(a, filtro) {
  const e = a.cliente.etapa
  if (filtro === 'andamento') return e === 'REQUERIDO' || e === 'EXIGENCIA'
  if (filtro === 'a_requerer') return e === 'A_REQUERER'
  if (filtro === 'deferidos') return e === 'DEFERIDO' && a.cliente.parcelasLiberadas < a.cliente.parcelasTotal
  return true
}

export default function Status() {
  const { usuario, meta } = useAuth()
  const [params, setParams] = useSearchParams()
  const pedido = params.get('cliente') ? Number(params.get('cliente')) : null
  const [filtro, setFiltro] = useState(pedido ? 'todos' : 'andamento')
  const { dados, recarregar } = useDados('/clientes')
  const { executar, ocupado } = useAcao()
  const [form, setForm] = useState({})
  const emprega = meta.sites.find((s) => s.id === 'emprega')?.extra

  const fila = useMemo(() => (dados || []).filter((a) => naFila(a, filtro)), [dados, filtro])
  const atual = useMemo(() => {
    if (!dados) return null
    return (pedido && dados.find((a) => a.cliente.id === pedido)) || fila[0] || null
  }, [dados, fila, pedido])

  useEffect(() => {
    if (atual) setForm({ protocolo: atual.cliente.protocolo || '', parcelasTotal: atual.cliente.parcelasTotal || 4, parcelasLiberadas: atual.cliente.parcelasLiberadas || 0 })
  }, [atual])

  function ir(id) {
    setParams(id ? { cliente: String(id) } : {})
  }

  async function salvar(e) {
    e.preventDefault()
    const ids = fila.map((a) => a.cliente.id)
    const idx = ids.indexOf(atual.cliente.id)
    const proximo = idx >= 0 && idx + 1 < ids.length ? ids[idx + 1] : null
    await executar(() => api(`/clientes/${atual.cliente.id}/status`, { metodo: 'POST', corpo: form }), () => {
      recarregar()
      ir(proximo)
    })
  }

  const conferidos = fila.filter((a) => a.cliente.ultimaConferencia === usuario.hoje).length
  const mudar = (k, v) => setForm((f) => ({ ...f, [k]: v }))

  return (
    <>
      <Titulo titulo="Atualizar status" sub="Abra o Emprega Brasil, veja o pedido e responda as perguntas. A plataforma atualiza a etapa, cria os prazos e deixa a mensagem ao cliente pronta." />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Pills opcoes={FILTROS} valor={filtro} onChange={(v) => { setFiltro(v); ir(null) }} />
        <span className="text-xs text-slate-600">{conferidos} de {fila.length} conferido(s) hoje</span>
      </div>
      {!dados ? <Carregando /> : !atual ? (
        <Card><p className="text-sm text-slate-600">Nenhum cliente neste filtro. Escolha "Todos os clientes" para conferir qualquer um.</p></Card>
      ) : (
        <div className="flex flex-wrap items-start gap-4">
          <Card className="flex max-h-[760px] min-w-0 flex-[1_1_260px] flex-col gap-2 overflow-y-auto">
            <h2 className="text-base font-semibold">Fila de conferência</h2>
            {fila.map((a) => (
              <button key={a.cliente.id} type="button" onClick={() => ir(a.cliente.id)}
                className={`flex flex-col items-start gap-1 rounded-lg border px-3 py-2.5 text-left ${a.cliente.id === atual.cliente.id ? 'border-marca-700 bg-marca-50' : 'border-slate-200 bg-slate-50'}`}>
                <span className="font-semibold">{a.cliente.nome}</span>
                <span className="text-xs text-slate-600">{a.etapaNome}{a.cliente.protocolo && ` · ${a.cliente.protocolo}`}</span>
                <Chip cor={a.cliente.ultimaConferencia === usuario.hoje ? 'ok' : 'neutro'}>
                  {a.cliente.ultimaConferencia ? `Conferido em ${br(a.cliente.ultimaConferencia)}` : 'Ainda não conferido'}
                </Chip>
              </button>
            ))}
          </Card>
          <Card className="min-w-0 flex-[3_1_520px]">
            <form onSubmit={salvar} className="flex flex-col gap-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <Link to={`/clientes/${atual.cliente.id}`} className="text-lg font-bold text-marca-700 underline">{atual.cliente.nome}</Link>
                  <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-600">
                    CPF {cpf(atual.cliente.cpf)} · Protocolo {atual.cliente.protocolo || 'sem protocolo'} <Chip cor={atual.etapaCor}>{atual.etapaNome}</Chip>
                  </div>
                  <div className="mt-1 text-xs text-slate-600">Hoje na plataforma: {atual.cliente.situacao || 'sem registro'}</div>
                </div>
                <BotaoLink href={emprega} target="_blank" rel="noopener noreferrer" variante="escuro">1. Abrir Emprega Brasil ›</BotaoLink>
              </div>

              <fieldset className="flex flex-col gap-2.5">
                <legend className="mb-2 font-bold">2. Qual é a situação do pedido no site?</legend>
                <div className="flex flex-wrap gap-2">
                  {SITUACOES.map(([v, n]) => (
                    <label key={v} className={`flex min-h-[38px] cursor-pointer items-center gap-2 rounded-lg border px-3 text-[13px] font-semibold ${form.sit === v ? 'border-marca-700 bg-marca-50 text-marca-900' : 'border-slate-300 bg-white text-slate-700'}`}>
                      <input type="radio" name="sit" value={v} checked={form.sit === v} onChange={() => mudar('sit', v)} required /> {n}
                    </label>
                  ))}
                </div>
              </fieldset>

              {form.sit && form.sit !== 'naoreq' && form.sit !== 'naoachou' && (
                <Campo rotulo="Número do protocolo (confira com o site)" className="max-w-sm">
                  <input type="text" value={form.protocolo || ''} onChange={(e) => mudar('protocolo', e.target.value)} />
                </Campo>
              )}
              {form.sit === 'exigencia' && (
                <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5 rounded-lg border border-amber-300 bg-amber-50 p-3.5">
                  <Campo rotulo="O que foi pedido na exigência?" obrigatorio><input type="text" placeholder="Ex.: comprovante de residência atualizado" value={form.exigenciaTexto || ''} onChange={(e) => mudar('exigenciaTexto', e.target.value)} /></Campo>
                  <Campo rotulo="Prazo para cumprir" obrigatorio><input type="text" placeholder="dd/mm/aaaa" value={form.prazo || ''} onChange={(e) => mudar('prazo', e.target.value)} /></Campo>
                </div>
              )}
              {form.sit === 'deferido' && (
                <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5 rounded-lg border border-green-300 bg-green-50 p-3.5">
                  <Campo rotulo="Total de parcelas do seguro"><input type="number" min="1" max="12" value={form.parcelasTotal} onChange={(e) => mudar('parcelasTotal', Number(e.target.value))} /></Campo>
                  <Campo rotulo="Parcelas já liberadas"><input type="number" min="0" max="12" value={form.parcelasLiberadas} onChange={(e) => mudar('parcelasLiberadas', Number(e.target.value))} /></Campo>
                </div>
              )}
              {form.sit === 'indeferido' && (
                <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5 rounded-lg border border-red-300 bg-red-50 p-3.5">
                  <Campo rotulo="Motivo do indeferimento (como está no site)" obrigatorio><input type="text" placeholder="Ex.: vínculo de emprego no CNIS" value={form.motivo || ''} onChange={(e) => mudar('motivo', e.target.value)} /></Campo>
                  <Campo rotulo="Data em que o cliente tomou ciência"><input type="text" placeholder="dd/mm/aaaa" value={form.ciencia || ''} onChange={(e) => mudar('ciencia', e.target.value)} /></Campo>
                </div>
              )}
              {form.sit === 'naoachou' && (
                <p className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">A plataforma vai registrar o alerta para confirmar com o cliente se o pedido foi enviado e com qual login.</p>
              )}
              <Campo rotulo="Observação (opcional)"><input type="text" value={form.obs || ''} onChange={(e) => mudar('obs', e.target.value)} /></Campo>
              <div className="flex flex-wrap gap-2 border-t border-slate-200 pt-3.5">
                <Botao type="submit" disabled={ocupado}>3. Salvar e ir para o próximo</Botao>
                <Botao variante="neutro" onClick={() => {
                  const ids = fila.map((a) => a.cliente.id)
                  const i = ids.indexOf(atual.cliente.id)
                  if (i >= 0 && i + 1 < ids.length) ir(ids[i + 1])
                }}>Pular</Botao>
              </div>
            </form>
          </Card>
        </div>
      )}
    </>
  )
}
