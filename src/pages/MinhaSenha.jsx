import { useState } from 'react'
import { api } from '../api.js'
import { Botao, Campo, Card, Titulo, useAcao } from '../components/ui.jsx'

export default function MinhaSenha() {
  const { executar, ocupado } = useAcao()
  const [f, setF] = useState({ atual: '', nova: '', confirma: '' })
  async function salvar(e) {
    e.preventDefault()
    await executar(() => api('/auth/senha', { metodo: 'POST', corpo: f }), () => setF({ atual: '', nova: '', confirma: '' }))
  }
  return (
    <>
      <Titulo titulo="Trocar senha" />
      <Card className="max-w-md">
        <form onSubmit={salvar} className="flex flex-col gap-3">
          <Campo rotulo="Senha atual"><input type="password" required autoComplete="current-password" value={f.atual} onChange={(e) => setF({ ...f, atual: e.target.value })} /></Campo>
          <Campo rotulo="Nova senha (mín. 10 caracteres, letras e números)"><input type="password" required autoComplete="new-password" value={f.nova} onChange={(e) => setF({ ...f, nova: e.target.value })} /></Campo>
          <Campo rotulo="Confirme a nova senha"><input type="password" required autoComplete="new-password" value={f.confirma} onChange={(e) => setF({ ...f, confirma: e.target.value })} /></Campo>
          <div><Botao type="submit" disabled={ocupado}>Salvar</Botao></div>
        </form>
      </Card>
    </>
  )
}
