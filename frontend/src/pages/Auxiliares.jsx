import { useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Botao, Campo, Card, Carregando, Chip, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'

const VAZIO = { nome: '', telefone: '', oab: '', ativo: true }

export default function Auxiliares() {
  const { gestor } = useAuth()
  const { dados, recarregar } = useDados('/auxiliares')
  const { executar, ocupado } = useAcao()
  const [form, setForm] = useState(VAZIO)

  async function salvar(e) {
    e.preventDefault()
    const { id, ...corpo } = form
    await executar(
      () => api(id ? `/auxiliares/${id}` : '/auxiliares', { metodo: id ? 'PUT' : 'POST', corpo }),
      () => { setForm(VAZIO); recarregar() },
    )
  }

  async function alternar(a) {
    await executar(
      () => api(`/auxiliares/${a.id}`, { metodo: 'PUT', corpo: { nome: a.nome, telefone: a.telefone, oab: a.oab, ativo: !a.ativo } }),
      recarregar,
    )
  }

  return (
    <>
      <Titulo titulo="Auxiliares jurídicos" sub="Cadastre todos os auxiliares do escritório. Na ficha de cada cliente você escolhe o responsável." />
      {gestor && (
        <Card>
          <form onSubmit={salvar} className="flex flex-wrap items-end gap-3">
            <Campo rotulo="Nome" obrigatorio className="flex-[2_1_220px]">
              <input type="text" required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
            </Campo>
            <Campo rotulo="Telefone" className="flex-[1_1_160px]">
              <input type="text" value={form.telefone || ''} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
            </Campo>
            <Campo rotulo="OAB (se houver)" className="flex-[1_1_140px]">
              <input type="text" value={form.oab || ''} onChange={(e) => setForm({ ...form, oab: e.target.value })} />
            </Campo>
            <Botao type="submit" disabled={ocupado}>{form.id ? 'Salvar alterações' : 'Cadastrar auxiliar'}</Botao>
            {form.id && <Botao variante="neutro" onClick={() => setForm(VAZIO)}>Cancelar</Botao>}
          </form>
        </Card>
      )}
      {!dados ? <Carregando /> : (
        <Card semPad className="overflow-x-auto">
          <table className="w-full min-w-[640px] border-collapse">
            <thead><tr><Th>Nome</Th><Th>Telefone</Th><Th>OAB</Th><Th>Situação</Th>{gestor && <Th />}</tr></thead>
            <tbody>
              {dados.map((a) => (
                <tr key={a.id}>
                  <Td><strong>{a.nome}</strong></Td>
                  <Td>{a.telefone || '—'}</Td>
                  <Td>{a.oab || '—'}</Td>
                  <Td><Chip cor={a.ativo ? 'ok' : 'neutro'}>{a.ativo ? 'Ativo' : 'Inativo'}</Chip></Td>
                  {gestor && (
                    <Td>
                      <div className="flex gap-2">
                        <Botao pequeno variante="neutro" onClick={() => setForm(a)}>Editar</Botao>
                        <Botao pequeno variante="neutro" disabled={ocupado} onClick={() => alternar(a)}>{a.ativo ? 'Inativar' : 'Reativar'}</Botao>
                      </div>
                    </Td>
                  )}
                </tr>
              ))}
              {dados.length === 0 && <tr><Td className="py-6 text-slate-500">Nenhum auxiliar cadastrado.</Td></tr>}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
