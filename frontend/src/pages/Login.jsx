import { useState } from 'react'
import { useAuth } from '../auth.jsx'
import { Alerta, Botao, Campo } from '../components/ui.jsx'

export default function Login() {
  const { entrar } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [mostrar, setMostrar] = useState(false)
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setErro(null)
    setOcupado(true)
    try {
      await entrar(email, senha)
    } catch (err) {
      setErro(err.message)
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="grid min-h-screen md:grid-cols-[1fr_minmax(420px,520px)]">
      <aside className="hidden flex-col justify-between bg-marca-900 p-12 text-white md:flex">
        <div className="text-xl font-bold">Seguro-Defeso</div>
        <div>
          <h2 className="max-w-md text-3xl font-bold leading-tight">Cada pescador no prazo certo, sem planilhas soltas.</h2>
          <p className="mt-4 max-w-md text-[15px] text-[#B9D3DA]">
            Triagem, cadastro, entrevistas, DAE e acompanhamento do seguro-defeso num só lugar.
          </p>
        </div>
        <div className="text-xs text-[#8FB5BF]">Painel interno do escritório</div>
      </aside>

      <main className="flex items-center justify-center px-5 py-10">
        <form onSubmit={enviar} className="flex w-full max-w-sm flex-col gap-4">
          <div>
            <div className="text-sm font-bold text-marca-700 md:hidden">Seguro-Defeso</div>
            <h1 className="mt-1 text-2xl font-bold">Entrar</h1>
            <p className="mt-1 text-sm text-slate-600">Use o e-mail e a senha do escritório.</p>
          </div>
          {erro && <Alerta cor="erro">{erro}</Alerta>}
          <Campo rotulo="E-mail">
            <input type="email" required autoFocus autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
          </Campo>
          <Campo rotulo="Senha">
            <input type={mostrar ? 'text' : 'password'} required autoComplete="current-password" value={senha}
              onChange={(e) => setSenha(e.target.value)} />
          </Campo>
          <label className="flex items-center gap-2 text-xs text-slate-600">
            <input type="checkbox" checked={mostrar} onChange={(e) => setMostrar(e.target.checked)} /> Mostrar senha
          </label>
          <Botao type="submit" disabled={ocupado} className="w-full">{ocupado ? 'Entrando…' : 'Entrar'}</Botao>
          <p className="text-xs text-slate-500">Esqueceu a senha? Peça ao administrador do escritório para redefinir.</p>
        </form>
      </main>
    </div>
  )
}
