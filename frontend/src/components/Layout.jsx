import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import Avatar from './Avatar.jsx'
import Icone from './Icone.jsx'
import { AvisoProvider } from './ui.jsx'
import { br } from '../util.js'

function Grupo({ children }) {
  return <div className="px-3 pb-1 pt-5 text-[11px] font-semibold uppercase tracking-widest text-[#8FB5BF]">{children}</div>
}

function Item({ to, icone, children, fim, selo }) {
  return (
    <NavLink to={to} end={fim}
      className={({ isActive }) => `flex min-h-[40px] items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors ${isActive
        ? 'bg-white font-semibold text-marca-900'
        : 'text-[#DCEAEE] hover:bg-marca-800 hover:text-white'}`}>
      <Icone nome={icone} />
      <span className="truncate">{children}</span>
      {selo > 0 && (
        <span className="ml-auto rounded-full bg-amber-400 px-2 py-0.5 text-[11px] font-bold text-marca-950"
          title={`${selo} cadastro(s) para finalizar`} aria-label={`${selo} cadastros para finalizar`}>{selo}</span>
      )}
    </NavLink>
  )
}

export default function Layout({ children }) {
  const { usuario, sair, gestor, admin } = useAuth()
  const local = useLocation()
  const [aberto, setAberto] = useState(false)
  const [faltamFinalizar, setFaltamFinalizar] = useState(0)

  useEffect(() => { setAberto(false) }, [local.pathname])
  useEffect(() => {
    const atualizar = () => api('/clientes/andamento').then((r) => setFaltamFinalizar(r.total)).catch(() => {})
    atualizar()
    window.addEventListener('cadastros-mudaram', atualizar)
    return () => window.removeEventListener('cadastros-mudaram', atualizar)
  }, [local.pathname])
  useEffect(() => {
    if (!aberto) return undefined
    const fechar = (e) => { if (e.key === 'Escape') setAberto(false) }
    window.addEventListener('keydown', fechar)
    return () => window.removeEventListener('keydown', fechar)
  }, [aberto])

  return (
    <div className="min-h-screen md:flex">
      <a href="#conteudo"
        className="sr-only z-[60] rounded-lg bg-white px-3 py-2 text-sm font-semibold text-marca-900 focus:not-sr-only focus:fixed focus:left-3 focus:top-3">
        Pular para o conteúdo
      </a>

      <header className="sticky top-0 z-30 flex items-center gap-3 bg-marca-900 px-4 py-3 text-white md:hidden">
        <button type="button" aria-label="Abrir menu" aria-expanded={aberto} aria-controls="menu-principal"
          onClick={() => setAberto(true)} className="rounded-lg p-1.5 hover:bg-marca-800">
          <Icone nome="menu" tamanho={22} />
        </button>
        <span className="font-bold">Seguro-Defeso</span>
        <span className="ml-auto text-xs text-[#B9D3DA]">{br(usuario.hoje)}</span>
      </header>

      {aberto && <div className="fixed inset-0 z-40 bg-black/50 md:hidden" onClick={() => setAberto(false)} aria-hidden="true" />}

      <nav id="menu-principal" aria-label="Menu principal"
        className={`barra-escura fixed inset-y-0 left-0 z-50 flex w-[270px] flex-col gap-0.5 overflow-y-auto bg-marca-900 px-3 py-4 text-white transition-transform duration-200 md:sticky md:top-0 md:z-auto md:h-screen md:w-[250px] md:shrink-0 md:translate-x-0 ${aberto ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="flex items-start justify-between px-3 pb-3">
          <div>
            <div className="text-lg font-bold leading-tight">Seguro-Defeso</div>
            <div className="mt-0.5 text-xs text-[#B9D3DA]">Hoje, {br(usuario.hoje)}</div>
          </div>
          <button type="button" aria-label="Fechar menu" onClick={() => setAberto(false)}
            className="rounded-lg p-1.5 hover:bg-marca-800 md:hidden">
            <Icone nome="fechar" tamanho={20} />
          </button>
        </div>

        <Item to="/" icone="inicio" fim>Início</Item>
        <Item to="/clientes" icone="cadastro" selo={faltamFinalizar}>Cadastro</Item>
        <Item to="/dae" icone="dae">DAE (eSocial)</Item>
        <Item to="/prazos" icone="prazos">Prazos</Item>
        <Item to="/entrevistas" icone="entrevistas">Entrevistas</Item>
        <Item to="/requerimentos" icone="requerimentos">Requerimentos</Item>

        <Grupo>Mais</Grupo>
        <Item to="/mensagens" icone="mensagens">Mensagens</Item>
        {gestor && <Item to="/honorarios" icone="honorarios">Honorários</Item>}
        {gestor && <Item to="/relatorios" icone="relatorios">Relatórios</Item>}
        <Item to="/planilhas" icone="planilhas">Planilhas</Item>
        <Item to="/auxiliares" icone="auxiliares">Auxiliares</Item>
        <Item to="/defesos" icone="defesos">Defesos</Item>

        <Grupo>Configurações</Grupo>
        <Item to="/perfil" icone="perfil">Meu perfil</Item>
        {admin && (
          <>
            <Item to="/admin/usuarios" icone="usuarios">Usuários</Item>
            <Item to="/admin/configuracoes" icone="parametros">Parâmetros do sistema</Item>
            <Item to="/admin/atividades" icone="atividades">Atividades</Item>
          </>
        )}

        <div className="mt-auto flex items-center gap-3 border-t border-marca-800 px-3 pt-4 text-xs text-[#CFE3E8]">
          <Link to="/perfil" aria-label="Abrir meu perfil"><Avatar usuario={usuario} tamanho={36} /></Link>
          <div className="min-w-0">
            <div className="truncate text-sm font-semibold text-white">{usuario.nome}</div>
            <div className="truncate">{usuario.papelNome}</div>
            <button type="button" onClick={sair} className="mt-0.5 font-medium text-white underline underline-offset-2">Sair</button>
          </div>
        </div>
      </nav>

      <main id="conteudo" className="min-w-0 flex-1 px-4 pb-16 pt-6 md:px-10 md:pt-8">
        <AvisoProvider>
          <div className="mx-auto flex max-w-[1240px] flex-col gap-5">{children}</div>
        </AvisoProvider>
      </main>
    </div>
  )
}
