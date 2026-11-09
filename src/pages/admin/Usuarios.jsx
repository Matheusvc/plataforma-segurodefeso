import { useState } from 'react'
import { api } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import { Botao, Campo, Card, Td, Th, Titulo, useAcao, useDados } from '../../components/ui.jsx'
import { br } from '../../util.js'

export default function Usuarios() {
  const { usuario, meta } = useAuth()
  const { dados, recarregar } = useDados('/admin/usuarios')
  const { executar, ocupado } = useAcao()
  const [novo, setNovo] = useState({ nome: '', email: '', senha: '', papel: 'ADVOGADO' })
  const [senhas, setSenhas] = useState({})

  async function criar(e) {
    e.preventDefault()
    await executar(() => api('/admin/usuarios', { metodo: 'POST', corpo: novo }), () => {
      setNovo({ nome: '', email: '', senha: '', papel: 'ADVOGADO' })
      recarregar()
    })
  }

  const alterar = (id, corpo) => executar(() => api(`/admin/usuarios/${id}`, { metodo: 'PUT', corpo }), () => {
    setSenhas({})
    recarregar()
  })

  return (
    <>
      <Titulo titulo="Usuários" sub="Cada pessoa com o seu login. Administrador: tudo. Advogado: tudo, menos usuários e configurações. Equipe: operação do dia a dia, sem honorários, relatórios e exclusões." />
      <Card>
        <form onSubmit={criar} className="flex flex-col gap-3.5">
          <h2 className="text-base font-semibold">Novo usuário</h2>
          <div className="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-3.5">
            <Campo rotulo="Nome" obrigatorio><input type="text" required value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} /></Campo>
            <Campo rotulo="E-mail" obrigatorio><input type="email" required value={novo.email} onChange={(e) => setNovo({ ...novo, email: e.target.value })} /></Campo>
            <Campo rotulo="Senha inicial (mín. 10, letras e números)" obrigatorio><input type="password" required autoComplete="new-password" value={novo.senha} onChange={(e) => setNovo({ ...novo, senha: e.target.value })} /></Campo>
            <Campo rotulo="Papel"><select value={novo.papel} onChange={(e) => setNovo({ ...novo, papel: e.target.value })}>{meta.papeis.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}</select></Campo>
          </div>
          <div><Botao type="submit" disabled={ocupado}>Criar usuário</Botao></div>
        </form>
      </Card>
      <Card semPad className="overflow-x-auto">
        <table className="w-full min-w-[900px] border-collapse">
          <thead><tr><Th>Nome</Th><Th>E-mail</Th><Th>Papel</Th><Th>Último acesso</Th><Th>Situação</Th><Th>Redefinir senha</Th></tr></thead>
          <tbody>
            {(dados || []).map((u) => {
              const proprio = u.id === usuario.id
              return (
                <tr key={u.id}>
                  <Td><strong>{u.nome}</strong></Td><Td>{u.email}</Td>
                  <Td><select aria-label="Papel" disabled={proprio || ocupado} value={u.papel} onChange={(e) => alterar(u.id, { papel: e.target.value })}>
                    {meta.papeis.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}</select></Td>
                  <Td>{br(u.ultimoLogin) || 'nunca'}</Td>
                  <Td><Botao variante={u.ativo ? 'perigo' : 'sec'} pequeno disabled={proprio || ocupado} onClick={() => alterar(u.id, { ativo: !u.ativo })}>{u.ativo ? 'Desativar' : 'Reativar'}</Botao></Td>
                  <Td><div className="flex items-center gap-2">
                    <input type="password" aria-label="Nova senha" placeholder="nova senha" autoComplete="new-password" className="!w-[150px]" value={senhas[u.id] || ''} onChange={(e) => setSenhas({ ...senhas, [u.id]: e.target.value })} />
                    <Botao variante="neutro" pequeno disabled={!senhas[u.id] || ocupado} onClick={() => alterar(u.id, { senha: senhas[u.id] })}>Redefinir</Botao>
                  </div></Td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </Card>
    </>
  )
}
