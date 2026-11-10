import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Inicio from './pages/Inicio.jsx'
import Clientes from './pages/Clientes.jsx'
import ClienteForm from './pages/ClienteForm.jsx'
import Ficha from './pages/Ficha.jsx'
import Triagem from './pages/Triagem.jsx'
import Entrevistas from './pages/Entrevistas.jsx'
import Auxiliares from './pages/Auxiliares.jsx'
import Status from './pages/Status.jsx'
import Requerimentos from './pages/Requerimentos.jsx'
import Dae from './pages/Dae.jsx'
import Prazos from './pages/Prazos.jsx'
import Defesos from './pages/Defesos.jsx'
import Mensagens from './pages/Mensagens.jsx'
import Honorarios from './pages/Honorarios.jsx'
import Relatorios from './pages/Relatorios.jsx'
import Planilhas from './pages/Planilhas.jsx'
import MinhaSenha from './pages/MinhaSenha.jsx'
import Usuarios from './pages/admin/Usuarios.jsx'
import Configuracoes from './pages/admin/Configuracoes.jsx'
import Atividades from './pages/admin/Atividades.jsx'

function SemPermissao() {
  return <p className="text-sm text-slate-600">Você não tem permissão para esta página.</p>
}

export default function App() {
  const { usuario, meta, pronto, gestor, admin } = useAuth()
  const local = useLocation()
  if (!pronto) return <p className="p-8 text-sm text-slate-500">Carregando…</p>
  if (!usuario) return <Login />
  if (!meta) return <p className="p-8 text-sm text-slate-500">Carregando…</p>
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path="/clientes" element={<Clientes />} />
        <Route path="/clientes/novo" element={<ClienteForm />} />
        <Route path="/clientes/:id" element={<Ficha key={local.pathname} />} />
        <Route path="/clientes/:id/editar" element={<ClienteForm />} />
        <Route path="/triagem" element={<Triagem />} />
        <Route path="/entrevistas" element={<Entrevistas />} />
        <Route path="/auxiliares" element={<Auxiliares />} />
        <Route path="/status" element={<Status />} />
        <Route path="/requerimentos" element={<Requerimentos />} />
        <Route path="/dae" element={<Dae />} />
        <Route path="/prazos" element={<Prazos />} />
        <Route path="/defesos" element={<Defesos />} />
        <Route path="/mensagens" element={<Mensagens />} />
        <Route path="/honorarios" element={gestor ? <Honorarios /> : <SemPermissao />} />
        <Route path="/relatorios" element={gestor ? <Relatorios /> : <SemPermissao />} />
        <Route path="/planilhas" element={<Planilhas />} />
        <Route path="/minha-senha" element={<MinhaSenha />} />
        <Route path="/admin/usuarios" element={admin ? <Usuarios /> : <SemPermissao />} />
        <Route path="/admin/configuracoes" element={admin ? <Configuracoes /> : <SemPermissao />} />
        <Route path="/admin/atividades" element={admin ? <Atividades /> : <SemPermissao />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}
