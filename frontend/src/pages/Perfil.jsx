import { useRef, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import Avatar from '../components/Avatar.jsx'
import { Botao, Campo, Card, Titulo, useAcao } from '../components/ui.jsx'

const SENHA_VAZIA = { atual: '', nova: '', confirma: '' }

export default function Perfil() {
  const { usuario, atualizarUsuario } = useAuth()
  const { executar, ocupado } = useAcao()
  const arquivo = useRef(null)
  const [dados, setDados] = useState({ nome: usuario.nome, telefone: usuario.telefone || '' })
  const [senha, setSenha] = useState(SENHA_VAZIA)

  async function enviarFoto(e) {
    const f = e.target.files[0]
    e.target.value = ''
    if (!f) return
    const form = new FormData()
    form.append('arquivo', f)
    await executar(() => api('/auth/foto', { metodo: 'POST', form }), (eu) => { atualizarUsuario(eu) })
  }

  const removerFoto = () => executar(() => api('/auth/foto', { metodo: 'DELETE' }), (eu) => { atualizarUsuario(eu) })

  async function salvarDados(e) {
    e.preventDefault()
    await executar(async () => {
      const eu = await api('/auth/perfil', { metodo: 'PUT', corpo: dados })
      atualizarUsuario(eu)
      return { mensagem: 'Perfil atualizado.' }
    })
  }

  async function salvarSenha(e) {
    e.preventDefault()
    await executar(() => api('/auth/senha', { metodo: 'POST', corpo: senha }), () => setSenha(SENHA_VAZIA))
  }

  return (
    <>
      <Titulo titulo="Meu perfil" sub="Seus dados de acesso. A foto e o nome aparecem no menu e nos registros de atividade." />
      <div className="flex flex-wrap items-start gap-4">
        <Card className="flex w-full max-w-sm flex-col items-center gap-3 text-center">
          <Avatar usuario={usuario} tamanho={112} />
          <div>
            <div className="text-base font-bold">{usuario.nome}</div>
            <div className="text-xs text-slate-500">{usuario.papelNome}</div>
          </div>
          <input ref={arquivo} type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={enviarFoto} />
          <div className="flex flex-wrap justify-center gap-2">
            <Botao pequeno disabled={ocupado} onClick={() => arquivo.current.click()}>{usuario.foto ? 'Trocar foto' : 'Enviar foto'}</Botao>
            {usuario.foto && <Botao pequeno variante="neutro" disabled={ocupado} onClick={removerFoto}>Remover</Botao>}
          </div>
          <p className="text-xs text-slate-500">JPG, PNG ou WEBP, até 3 MB.</p>
        </Card>

        <div className="flex min-w-0 flex-[1_1_380px] flex-col gap-4">
          <Card>
            <form onSubmit={salvarDados} className="flex max-w-xl flex-col gap-3">
              <h2 className="text-base font-semibold">Meus dados</h2>
              <Campo rotulo="Nome" obrigatorio>
                <input type="text" required maxLength={150} value={dados.nome} onChange={(e) => setDados({ ...dados, nome: e.target.value })} />
              </Campo>
              <Campo rotulo="Telefone">
                <input type="text" maxLength={40} value={dados.telefone} onChange={(e) => setDados({ ...dados, telefone: e.target.value })} />
              </Campo>
              <Campo rotulo="E-mail (usado no login)">
                <input type="text" value={usuario.email} readOnly className="bg-slate-50 text-slate-600" />
              </Campo>
              <p className="text-xs text-slate-500">Para trocar o e-mail ou o perfil de acesso, peça ao administrador (Configurações › Usuários).</p>
              <div><Botao type="submit" disabled={ocupado}>Salvar dados</Botao></div>
            </form>
          </Card>

          <Card>
            <form onSubmit={salvarSenha} className="flex max-w-xl flex-col gap-3">
              <h2 className="text-base font-semibold">Trocar senha</h2>
              <Campo rotulo="Senha atual"><input type="password" required autoComplete="current-password" value={senha.atual} onChange={(e) => setSenha({ ...senha, atual: e.target.value })} /></Campo>
              <Campo rotulo="Nova senha (mín. 10 caracteres, letras e números)"><input type="password" required autoComplete="new-password" value={senha.nova} onChange={(e) => setSenha({ ...senha, nova: e.target.value })} /></Campo>
              <Campo rotulo="Confirme a nova senha"><input type="password" required autoComplete="new-password" value={senha.confirma} onChange={(e) => setSenha({ ...senha, confirma: e.target.value })} /></Campo>
              <div><Botao type="submit" disabled={ocupado}>Trocar senha</Botao></div>
            </form>
          </Card>
        </div>
      </div>
    </>
  )
}
