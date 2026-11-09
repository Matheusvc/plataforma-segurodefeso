import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'

// ------------------------------------------------------------------ cores das etiquetas
const CORES = {
  ok: 'bg-green-100 text-green-800',
  aviso: 'bg-amber-100 text-amber-900',
  erro: 'bg-red-100 text-red-800',
  info: 'bg-sky-100 text-sky-900',
  azul: 'bg-indigo-100 text-indigo-800',
  neutro: 'bg-slate-100 text-slate-700',
}

export function Chip({ cor = 'neutro', children, className = '' }) {
  return (
    <span className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ${CORES[cor] || CORES.neutro} ${className}`}>
      {children}
    </span>
  )
}

export function Card({ children, className = '', semPad = false, ...resto }) {
  return (
    <section className={`rounded-xl border border-borda bg-white ${semPad ? '' : 'p-5'} ${className}`} {...resto}>
      {children}
    </section>
  )
}

const BOTAO = {
  pri: 'bg-marca-700 text-white hover:bg-marca-800 border border-marca-700',
  sec: 'bg-white text-marca-700 border border-marca-700 hover:bg-marca-50',
  neutro: 'bg-white text-slate-700 border border-slate-300 hover:bg-slate-50',
  perigo: 'bg-white text-red-800 border border-red-300 hover:bg-red-50',
  escuro: 'bg-marca-900 text-white border border-marca-900 hover:bg-marca-950',
}

function classesBotao(variante, pequeno) {
  return `inline-flex items-center justify-center gap-1.5 rounded-lg font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${
    pequeno ? 'min-h-[34px] px-3 text-[13px]' : 'min-h-[40px] px-4 text-sm'
  } ${BOTAO[variante] || BOTAO.pri}`
}

export function Botao({ variante = 'pri', pequeno = false, className = '', ...resto }) {
  return <button type="button" className={`${classesBotao(variante, pequeno)} ${className}`} {...resto} />
}

export function BotaoLink({ to, href, variante = 'pri', pequeno = false, className = '', children, ...resto }) {
  const cls = `${classesBotao(variante, pequeno)} ${className}`
  if (href) return <a href={href} className={cls} {...resto}>{children}</a>
  return <Link to={to} className={cls} {...resto}>{children}</Link>
}

export function Campo({ rotulo, obrigatorio, children, className = '' }) {
  return (
    <label className={`flex flex-col gap-1.5 text-xs font-semibold text-slate-600 ${className}`}>
      <span>{rotulo}{obrigatorio && ' *'}</span>
      {children}
    </label>
  )
}

export function Pills({ opcoes, valor, onChange }) {
  return (
    <div className="flex flex-wrap gap-2">
      {opcoes.map(([v, nome]) => (
        <button key={v} type="button" onClick={() => onChange(v)}
          className={`min-h-[36px] rounded-full border px-3.5 text-[13px] ${valor === v
            ? 'border-marca-700 bg-marca-700 font-semibold text-white'
            : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'}`}>
          {nome}
        </button>
      ))}
    </div>
  )
}

export function Alerta({ cor = 'info', children }) {
  const c = {
    ok: 'border-green-300 bg-green-50 text-green-900',
    erro: 'border-red-300 bg-red-50 text-red-900',
    aviso: 'border-amber-300 bg-amber-50 text-amber-900',
    info: 'border-marca-100 bg-marca-50 text-marca-900',
  }[cor]
  return <div role="status" className={`rounded-lg border px-4 py-3 text-sm ${c}`}>{children}</div>
}

export function Titulo({ titulo, sub, children }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-[25px] font-bold tracking-tight">{titulo}</h1>
        {sub && <p className="mt-1.5 max-w-3xl text-sm text-slate-600">{sub}</p>}
      </div>
      {children}
    </div>
  )
}

export function Carregando() {
  return <p className="py-6 text-sm text-slate-500">Carregando…</p>
}

export function Item({ children, className = '' }) {
  return <div className={`flex flex-wrap items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-3 ${className}`}>{children}</div>
}

export function Th({ children }) {
  return <th className="whitespace-nowrap border-b border-slate-200 px-3.5 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-600">{children}</th>
}

export function Td({ children, className = '' }) {
  return <td className={`border-b border-slate-100 px-3.5 py-2.5 align-middle text-sm ${className}`}>{children}</td>
}

// ------------------------------------------------------------------ avisos (mensagens de sucesso/erro)
const AvisoCtx = createContext(null)

export function AvisoProvider({ children }) {
  const [aviso, setAviso] = useState(null)
  const mostrar = useCallback((cor, texto) => {
    setAviso({ cor, texto, id: Date.now() })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])
  const valor = { ok: (t) => mostrar('ok', t), erro: (t) => mostrar('erro', t), limpar: () => setAviso(null) }
  return (
    <AvisoCtx.Provider value={valor}>
      {aviso && (
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1"><Alerta cor={aviso.cor}>{aviso.texto}</Alerta></div>
          <Botao variante="neutro" pequeno onClick={() => setAviso(null)}>Fechar</Botao>
        </div>
      )}
      {children}
    </AvisoCtx.Provider>
  )
}

export function useAviso() {
  return useContext(AvisoCtx)
}

// ------------------------------------------------------------------ carregar dados da API
export function useDados(caminho) {
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    if (!caminho) return undefined
    let ativo = true
    setErro(null)
    api(caminho).then((d) => ativo && setDados(d)).catch((e) => ativo && setErro(e.message))
    return () => { ativo = false }
  }, [caminho, versao])
  return { dados, erro, recarregar: () => setVersao((v) => v + 1), setDados }
}

/** Executa uma ação na API mostrando o resultado no aviso. */
export function useAcao() {
  const aviso = useAviso()
  const [ocupado, setOcupado] = useState(false)
  async function executar(fn, aoTerminar) {
    setOcupado(true)
    try {
      const r = await fn()
      if (r && r.mensagem) aviso.ok(r.mensagem)
      if (aoTerminar) await aoTerminar(r)
      return r
    } catch (e) {
      aviso.erro(e.message)
      return null
    } finally {
      setOcupado(false)
    }
  }
  return { executar, ocupado }
}
