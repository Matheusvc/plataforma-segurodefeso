import { NavLink, Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { AvisoProvider } from './ui.jsx'
import { br } from '../util.js'

function Grupo({ children }) {
  return <div className="px-2.5 pb-1 pt-4 text-[11px] font-semibold uppercase tracking-widest text-[#9FC1CA]">{children}</div>
}

function Item({ to, children, fim }) {
  return (
    <NavLink to={to} end={fim}
      className={({ isActive }) => `flex min-h-[40px] items-center rounded-lg px-2.5 py-2 text-sm ${isActive
        ? 'bg-white font-bold text-marca-900'
        : 'font-medium text-[#E4EFF2] hover:bg-marca-800 hover:text-white'}`}>
      {children}
    </NavLink>
  )
}

export default function Layout({ children }) {
  const { usuario, sair, gestor, admin } = useAuth()
  return (
    <div className="flex min-h-screen flex-wrap">
      <nav aria-label="Menu principal" className="flex w-full flex-col gap-0.5 bg-marca-900 px-3.5 py-5 text-white md:w-[230px] md:shrink-0">
        <div className="px-2.5 pb-3">
          <div className="text-lg font-bold">Seguro-Defeso</div>
          <div className="text-xs text-[#B9D3DA]">Painel do escritório</div>
        </div>
        <Grupo>Operação</Grupo>
        <Item to="/" fim>Início</Item>
        <Item to="/clientes">Clientes</Item>
        <Item to="/triagem">Triagem</Item>
        <Item to="/status">Atualizar status</Item>
        <Grupo>Processos</Grupo>
        <Item to="/requerimentos">Requerimentos</Item>
        <Item to="/dae">DAE (eSocial)</Item>
        <Item to="/prazos">Prazos</Item>
        <Item to="/defesos">Defesos</Item>
        <Grupo>Relacionamento</Grupo>
        <Item to="/mensagens">Mensagens</Item>
        {gestor && <Item to="/honorarios">Honorários</Item>}
        <Grupo>Gestão</Grupo>
        {gestor && <Item to="/relatorios">Relatórios</Item>}
        <Item to="/planilhas">Planilhas</Item>
        {admin && (
          <>
            <Item to="/admin/usuarios">Usuários</Item>
            <Item to="/admin/configuracoes">Configurações</Item>
            <Item to="/admin/atividades">Registro de atividades</Item>
          </>
        )}
        <div className="mt-auto border-t border-marca-800 px-2.5 pt-3 text-xs text-[#CFE3E8]">
          {usuario.nome} · {usuario.papelNome}<br />
          <Link to="/minha-senha" className="text-white underline">Trocar senha</Link>
          {' · '}
          <button type="button" onClick={sair} className="text-white underline">Sair</button>
        </div>
      </nav>
      <main className="flex min-w-0 flex-1 flex-col gap-5 px-4 pb-12 pt-6 md:px-8">
        <div className="flex justify-end">
          <span className="rounded-lg border border-borda bg-white px-3 py-1.5 text-[13px] text-slate-600">
            Hoje: <strong className="text-slate-900">{br(usuario.hoje)}</strong>
          </span>
        </div>
        <AvisoProvider>
          <div className="flex flex-col gap-5">{children}</div>
        </AvisoProvider>
      </main>
    </div>
  )
}
