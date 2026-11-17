import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Botao, BotaoLink, Campo, Card, Carregando, Chip, Paginacao, Pills, Titulo, useAcao, useAviso, useDados } from '../components/ui.jsx'
import { mascaras, validadores } from '../formato.js'
import { br, cpf } from '../util.js'

const TAMANHO = 20
const FILTROS = [['andamento', 'Requerido e em exigência'], ['a_requerer', 'A requerer'], ['deferidos', 'Deferidos com parcelas a liberar'], ['todos', 'Todos os clientes']]
const SITUACOES = [['analise', 'Em análise'], ['exigencia', 'Exigência'], ['deferido', 'Deferido'], ['indeferido', 'Indeferido'], ['naoreq', 'Ainda não requerido'], ['naoachou', 'Pedido não encontrado']]

/** Parâmetros que fazem o servidor montar a fila de conferência. */
function paramsDoFiltro(filtro) {
  if (filtro === 'andamento') return { etapas: 'REQUERIDO,EXIGENCIA' }
  if (filtro === 'a_requerer') return { etapas: 'A_REQUERER' }
  if (filtro === 'deferidos') return { filtro: 'parcelas' }
  return {}
}

export default function Status() {
  const { usuario, meta } = useAuth()
  const aviso = useAviso()
  const [params, setParams] = useSearchParams()
  const pedido = params.get('cliente') ? Number(params.get('cliente')) : null
  const [filtro, setFiltro] = useState(pedido ? 'todos' : 'andamento')
  const [busca, setBusca] = useState('')
  const [termo, setTermo] = useState('')
  const [pagina, setPagina] = useState(0)
  useEffect(() => {
    const t = setTimeout(() => { setTermo(busca); setPagina(0) }, 350)
    return () => clearTimeout(t)
  }, [busca])
  const q = new URLSearchParams({ q: termo, pagina: String(pagina), tamanho: String(TAMANHO), ...paramsDoFiltro(filtro) })
  const { dados, recarregar } = useDados('/clientes?' + q.toString())
  const { dados: fichaPedido } = useDados(pedido ? `/clientes/${pedido}` : null)
  const { executar, ocupado } = useAcao()
  const [form, setForm] = useState({})
  const emprega = meta.sites.find((s) => s.id === 'emprega')?.extra

  const fila = useMemo(() => dados?.itens || [], [dados])
  const atual = useMemo(() => {
    if (pedido) return fichaPedido?.analise || fila.find((a) => a.cliente.id === pedido) || null
    return fila[0] || null
  }, [pedido, fichaPedido, fila])

  useEffect(() => {
    if (atual) setForm({ protocolo: atual.cliente.protocolo || '', parcelasTotal: atual.cliente.parcelasTotal || 4, parcelasLiberadas: atual.cliente.parcelasLiberadas || 0 })
  }, [atual])

  function ir(id) {
    setParams(id ? { cliente: String(id) } : {})
  }

  const proximoDaFila = () => {
    const ids = fila.map((a) => a.cliente.id)
    const idx = ids.indexOf(atual.cliente.id)
    return idx >= 0 && idx + 1 < ids.length ? ids[idx + 1] : null
  }

  async function salvar(e) {
    e.preventDefault()
    const problema = form.sit === 'exigencia' ? validadores.data({ min: 2000, max: 2100 })(form.prazo)
      : form.sit === 'indeferido' ? validadores.data({ passado: true, min: 2000 })(form.ciencia) : ''
    if (problema) {
      aviso.erro(problema)
      return
    }
    const proximo = proximoDaFila()
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
      <div className="flex flex-wrap items-end justify-between gap-3">
        <Pills opcoes={FILTROS} valor={filtro} onChange={(v) => { setFiltro(v); setPagina(0); ir(null) }} />
        <label className="flex min-w-[200px] flex-[0_1_300px] flex-col gap-1.5 text-xs font-semibold text-slate-600">
          Buscar na fila
          <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Nome, CPF, município ou colônia" />
        </label>
      </div>
      {!dados || (pedido && !atual) ? <Carregando /> : !atual ? (
        <Card><p className="text-sm text-slate-600">Nenhum cliente neste filtro. Escolha "Todos os clientes" para conferir qualquer um.</p></Card>
      ) : (
        <div className="flex flex-wrap items-start gap-4">
          <Card semPad className="flex max-h-[760px] min-w-0 flex-[1_1_260px] flex-col overflow-y-auto">
            <div className="flex flex-col gap-2 p-5">
              <h2 className="text-base font-semibold">Fila de conferência</h2>
              <span className="text-xs text-slate-600">{conferidos} conferido(s) hoje nesta página</span>
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
              {fila.length === 0 && <p className="text-sm text-slate-500">Nenhum cliente nesta fila.</p>}
            </div>
            <Paginacao pagina={dados.pagina} tamanho={dados.tamanho} total={dados.total} aoMudar={(p) => { setPagina(p); ir(null) }} />
          </Card>
          <Card className="min-w-0 flex-[3_1_520px]">
            <form onSubmit={salvar} noValidate className="flex flex-col gap-5">
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
                    <label key={v} className={`flex min-h-[40px] cursor-pointer items-center gap-2 rounded-lg border px-3 text-[13px] font-semibold ${form.sit === v ? 'border-marca-700 bg-marca-50 text-marca-900' : 'border-slate-300 bg-white text-slate-700'}`}>
                      <input type="radio" name="sit" value={v} checked={form.sit === v} onChange={() => mudar('sit', v)} required /> {n}
                    </label>
                  ))}
                </div>
              </fieldset>

              {form.sit && form.sit !== 'naoreq' && form.sit !== 'naoachou' && (
                <Campo rotulo="Número do protocolo (confira com o site)" className="max-w-sm">
                  <input type="text" maxLength={60} value={form.protocolo || ''} onChange={(e) => mudar('protocolo', e.target.value)} />
                </Campo>
              )}
              {form.sit === 'exigencia' && (
                <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5 rounded-lg border border-amber-300 bg-amber-50 p-3.5">
                  <Campo rotulo="O que foi pedido na exigência?" obrigatorio><input type="text" maxLength={500} placeholder="Ex.: comprovante de residência atualizado" value={form.exigenciaTexto || ''} onChange={(e) => mudar('exigenciaTexto', e.target.value)} /></Campo>
                  <Campo rotulo="Prazo para cumprir" obrigatorio><input type="text" inputMode="numeric" maxLength={10} placeholder="dd/mm/aaaa" value={form.prazo || ''} onChange={(e) => mudar('prazo', mascaras.data(e.target.value))} /></Campo>
                </div>
              )}
              {form.sit === 'deferido' && (
                <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5 rounded-lg border border-green-300 bg-green-50 p-3.5">
                  <Campo rotulo="Total de parcelas do seguro"><input type="number" min="1" max="12" inputMode="numeric" value={form.parcelasTotal} onChange={(e) => mudar('parcelasTotal', Number(e.target.value))} /></Campo>
                  <Campo rotulo="Parcelas já liberadas"><input type="number" min="0" max="12" inputMode="numeric" value={form.parcelasLiberadas} onChange={(e) => mudar('parcelasLiberadas', Number(e.target.value))} /></Campo>
                </div>
              )}
              {form.sit === 'indeferido' && (
                <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5 rounded-lg border border-red-300 bg-red-50 p-3.5">
                  <Campo rotulo="Motivo do indeferimento (como está no site)" obrigatorio><input type="text" maxLength={500} placeholder="Ex.: vínculo de emprego no CNIS" value={form.motivo || ''} onChange={(e) => mudar('motivo', e.target.value)} /></Campo>
                  <Campo rotulo="Data em que o cliente tomou ciência"><input type="text" inputMode="numeric" maxLength={10} placeholder="dd/mm/aaaa" value={form.ciencia || ''} onChange={(e) => mudar('ciencia', mascaras.data(e.target.value))} /></Campo>
                </div>
              )}
              {form.sit === 'naoachou' && (
                <p className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">A plataforma vai registrar o alerta para confirmar com o cliente se o pedido foi enviado e com qual login.</p>
              )}
              <Campo rotulo="Observação (opcional)"><input type="text" maxLength={300} value={form.obs || ''} onChange={(e) => mudar('obs', e.target.value)} /></Campo>
              <div className="flex flex-wrap gap-2 border-t border-slate-200 pt-3.5">
                <Botao type="submit" disabled={ocupado || !form.sit}>3. Salvar e ir para o próximo</Botao>
                <Botao variante="neutro" onClick={() => { const p = proximoDaFila(); if (p) ir(p) }}>Pular</Botao>
              </div>
            </form>
          </Card>
        </div>
      )}
    </>
  )
}
