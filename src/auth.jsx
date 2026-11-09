import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, prepararCsrf } from './api.js'

const AuthCtx = createContext(null)

/** Guarda o usuário logado e as tabelas fixas (requisitos, documentos, etapas...). */
export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [meta, setMeta] = useState(null)
  const [pronto, setPronto] = useState(false)

  const carregar = useCallback(async () => {
    try {
      await prepararCsrf()
      const eu = await api('/auth/eu')
      setUsuario(eu)
      setMeta(await api('/meta'))
    } catch {
      setUsuario(null)
    } finally {
      setPronto(true)
    }
  }, [])

  useEffect(() => {
    carregar()
    const expirou = () => setUsuario(null)
    window.addEventListener('sessao-expirada', expirou)
    return () => window.removeEventListener('sessao-expirada', expirou)
  }, [carregar])

  async function entrar(email, senha) {
    await prepararCsrf()
    const eu = await api('/auth/login', { metodo: 'POST', corpo: { email, senha } })
    setUsuario(eu)
    setMeta(await api('/meta'))
  }

  async function sair() {
    try {
      await api('/auth/logout', { metodo: 'POST' })
    } finally {
      setUsuario(null)
      await prepararCsrf()
    }
  }

  const gestor = usuario && (usuario.papel === 'ADMIN' || usuario.papel === 'ADVOGADO')
  const admin = usuario && usuario.papel === 'ADMIN'

  return (
    <AuthCtx.Provider value={{ usuario, meta, pronto, entrar, sair, gestor, admin }}>
      {children}
    </AuthCtx.Provider>
  )
}

export function useAuth() {
  return useContext(AuthCtx)
}
