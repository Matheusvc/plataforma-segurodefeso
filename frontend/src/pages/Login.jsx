import { useState } from 'react'
import { useAuth } from '../auth.jsx'
import { Alerta, Botao, Campo } from '../components/ui.jsx'

export default function Login() {
  const { entrar } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
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
    <div className="mx-auto mt-[10vh] max-w-sm px-4">
      <form onSubmit={enviar} className="flex flex-col gap-4 rounded-xl border border-borda bg-white p-6">
        <div>
          <h1 className="text-2xl font-bold">Seguro-Defeso</h1>
          <p className="mt-1 text-sm text-slate-600">Entre com o seu e-mail e senha do escritório.</p>
        </div>
        {erro && <Alerta cor="erro">{erro}</Alerta>}
        <Campo rotulo="E-mail"><input type="email" required autoFocus autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} /></Campo>
        <Campo rotulo="Senha"><input type="password" required autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} /></Campo>
        <Botao type="submit" disabled={ocupado}>{ocupado ? 'Entrando…' : 'Entrar'}</Botao>
        <p className="text-xs text-slate-500">Esqueceu a senha? Peça ao administrador do escritório para redefinir.</p>
      </form>
    </div>
  )
}
