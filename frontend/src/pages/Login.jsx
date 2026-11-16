import { useRef, useState } from 'react'
import { useAuth } from '../auth.jsx'
import { Alerta, Botao } from '../components/ui.jsx'
import Icone from '../components/Icone.jsx'

const DESTAQUES = [
  ['cadastro', 'Cadastro completo', 'Triagem, dados, documentos e entrevista num só passo a passo.'],
  ['prazos', 'Prazos sob controle', 'Exigências, defesos e vencimentos calculados para você.'],
  ['dae', 'DAE organizado', 'Lançamento por competência, só para quem tem entrevista confirmada.'],
]

function Ondas() {
  return (
    <svg viewBox="0 0 800 300" preserveAspectRatio="none" aria-hidden="true"
      className="pointer-events-none absolute inset-x-0 bottom-0 h-56 w-full text-white opacity-[0.07]">
      <path fill="currentColor" d="M0 150 C 120 100, 240 200, 400 150 S 680 100, 800 150 V300 H0z" />
      <path fill="currentColor" d="M0 200 C 140 150, 260 250, 420 200 S 700 160, 800 205 V300 H0z" />
      <path fill="currentColor" d="M0 250 C 160 215, 280 285, 440 250 S 700 225, 800 255 V300 H0z" />
    </svg>
  )
}

export default function Login() {
  const { entrar } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [mostrar, setMostrar] = useState(false)
  const [capsLigado, setCapsLigado] = useState(false)
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const senhaRef = useRef(null)

  async function enviar(e) {
    e.preventDefault()
    setErro(null)
    setOcupado(true)
    try {
      await entrar(email, senha)
    } catch (err) {
      setErro(err.message)
      senhaRef.current?.focus()
      senhaRef.current?.select()
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <aside className="relative hidden overflow-hidden bg-marca-900 text-white lg:flex lg:flex-col lg:justify-between lg:px-16 lg:py-12 xl:px-24">
        <div className="pointer-events-none absolute -right-32 -top-32 h-96 w-96 rounded-full bg-marca-700/40 blur-3xl" aria-hidden="true" />
        <Ondas />

        <div className="relative flex items-center gap-3 text-lg font-bold">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 ring-1 ring-white/20">
            <Icone nome="defesos" tamanho={22} />
          </span>
          Seguro-Defeso
        </div>

        <div className="relative max-w-xl">
          <h2 className="text-4xl font-bold leading-tight tracking-tight xl:text-5xl">
            Cada pescador no prazo certo, sem planilhas soltas.
          </h2>
          <ul className="mt-10 flex flex-col gap-5">
            {DESTAQUES.map(([icone, titulo, texto]) => (
              <li key={titulo} className="flex items-start gap-4">
                <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white/10 text-[#BFE0E8]">
                  <Icone nome={icone} tamanho={20} />
                </span>
                <div>
                  <div className="font-semibold">{titulo}</div>
                  <div className="text-sm text-[#B9D3DA]">{texto}</div>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="relative text-xs text-[#8FB5BF]">Painel interno do escritório</div>
      </aside>

      <main className="flex items-center justify-center px-5 py-10 sm:px-8">
        <div className="w-full max-w-[420px]">
          <div className="mb-8 flex items-center gap-2.5 font-bold text-marca-900 lg:hidden">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-marca-900 text-white">
              <Icone nome="defesos" tamanho={20} />
            </span>
            Seguro-Defeso
          </div>

          <form onSubmit={enviar} className="flex flex-col gap-5 rounded-2xl border border-borda bg-white p-7 shadow-sm sm:p-9">
            <div>
              <h1 className="text-[28px] font-bold tracking-tight">Entrar</h1>
              <p className="mt-1.5 text-[15px] text-slate-600">Use o e-mail e a senha do escritório.</p>
            </div>

            {erro && <div role="alert"><Alerta cor="erro">{erro}</Alerta></div>}

            <div className="flex flex-col gap-1.5">
              <label htmlFor="login-email" className="text-sm font-semibold text-slate-700">E-mail</label>
              <input id="login-email" type="email" required autoFocus autoComplete="username" inputMode="email"
                placeholder="nome@escritorio.com.br" value={email} onChange={(e) => setEmail(e.target.value)}
                className="!min-h-[48px] !text-base" />
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="login-senha" className="text-sm font-semibold text-slate-700">Senha</label>
              <div className="relative">
                <input id="login-senha" ref={senhaRef} type={mostrar ? 'text' : 'password'} required autoComplete="current-password"
                  value={senha} onChange={(e) => setSenha(e.target.value)}
                  onKeyUp={(e) => setCapsLigado(e.getModifierState && e.getModifierState('CapsLock'))}
                  className="!min-h-[48px] !pr-12 !text-base" />
                <button type="button" onClick={() => setMostrar((v) => !v)} aria-pressed={mostrar}
                  aria-label={mostrar ? 'Ocultar senha' : 'Mostrar senha'}
                  className="absolute inset-y-0 right-0 flex w-12 items-center justify-center rounded-r-lg text-slate-500 hover:text-marca-700">
                  <Icone nome={mostrar ? 'olhoFechado' : 'olho'} tamanho={20} />
                </button>
              </div>
              {capsLigado && <p className="text-xs font-medium text-amber-700" role="status">Caps Lock está ligado.</p>}
            </div>

            <Botao type="submit" disabled={ocupado} className="!min-h-[48px] w-full !text-base">
              {ocupado ? 'Entrando…' : 'Entrar'}
            </Botao>

            <p className="text-center text-[13px] text-slate-500">
              Esqueceu a senha? Peça ao administrador do escritório para redefinir.
            </p>
          </form>

          <p className="mt-6 text-center text-xs text-slate-500">Acesso restrito a usuários autorizados.</p>
        </div>
      </main>
    </div>
  )
}
