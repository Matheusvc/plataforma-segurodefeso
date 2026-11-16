import { useState } from 'react'
import { api, urlApi } from '../api.js'
import { useAuth } from '../auth.jsx'
import { br, competencia } from '../util.js'
import { Botao, BotaoLink, Campo, Chip, Item, useAcao } from './ui.jsx'

/** Documentos obrigatórios, envio de arquivos e pasta do cliente (usado na ficha e no assistente de cadastro). */
export default function Documentos({ a, id, docs, dae, recarregar }) {
  const { meta, gestor } = useAuth()
  const { executar, ocupado } = useAcao()
  const [tipo, setTipo] = useState('RG')
  const [comp, setComp] = useState(dae[0]?.competencia || '')
  const [arquivo, setArquivo] = useState(null)
  const recebidos = new Set(a.cliente.documentos)
  const docNome = Object.fromEntries(meta.documentos.map((d) => [d.id, d.nome]))

  async function anexar(e) {
    e.preventDefault()
    if (!arquivo) return
    const form = new FormData()
    form.append('tipo', tipo)
    if (tipo === 'COMPROVANTE_DAE') form.append('competencia', comp)
    form.append('arquivo', arquivo)
    await executar(() => api(`/clientes/${id}/documentos`, { metodo: 'POST', form }), () => {
      e.target.reset()
      setArquivo(null)
      recarregar()
    })
  }

  async function excluir(doc) {
    if (!window.confirm(`Excluir ${doc.nome}?`)) return
    await executar(() => api(`/documentos/${doc.id}`, { metodo: 'DELETE' }), recarregar)
  }

  return (
    <div className="flex flex-wrap items-start gap-4">
      <div className="flex min-w-0 flex-[2_1_380px] flex-col gap-2">
        <h2 className="text-base font-semibold">Documentos obrigatórios</h2>
        {meta.documentos.filter((d) => d.flag).map((d) => (
          <Item key={d.id}>
            <span className="flex-1 font-semibold">{d.nome}</span>
            <Chip cor={recebidos.has(d.id) ? 'ok' : 'erro'}>{recebidos.has(d.id) ? 'Recebido' : 'Pendente'}</Chip>
          </Item>
        ))}
        {a.documentosPendentes.length > 0 && <BotaoLink to={`/mensagens?cliente=${id}&modelo=DOCS`} variante="sec">Pedir pendentes no WhatsApp</BotaoLink>}
      </div>
      <div className="flex min-w-0 flex-[2_1_380px] flex-col gap-3">
        <h2 className="text-base font-semibold">Anexar arquivo</h2>
        <form onSubmit={anexar} className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-slate-50 p-3.5">
          <Campo rotulo="Tipo de documento">
            <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
              {meta.documentos.map((d) => <option key={d.id} value={d.id}>{d.nome}</option>)}
            </select>
          </Campo>
          {tipo === 'COMPROVANTE_DAE' && (
            <Campo rotulo="Competência do DAE pago">
              <select value={comp} onChange={(e) => setComp(e.target.value)}>
                {dae.map((d) => <option key={d.competencia} value={d.competencia}>{d.label}</option>)}
              </select>
            </Campo>
          )}
          <Campo rotulo="Arquivo (PDF ou foto, até 15 MB)">
            <input type="file" required accept=".pdf,.jpg,.jpeg,.png,.webp,.heic" onChange={(e) => setArquivo(e.target.files[0] || null)} />
          </Campo>
          <Botao type="submit" disabled={ocupado || !arquivo}>Anexar</Botao>
        </form>
        <h2 className="mt-1 text-base font-semibold">Arquivos na pasta do cliente</h2>
        {docs.map((d) => (
          <Item key={d.id}>
            <div className="min-w-0 flex-1">
              <a href={urlApi(`/documentos/${d.id}`)} target="_blank" rel="noopener noreferrer" className="font-semibold text-marca-700 underline">{d.nome}</a>
              <div className="text-[13px] text-slate-600">{docNome[d.tipo]}{d.competencia && ` ${competencia(d.competencia)}`} · {br(d.enviadoEm)} · {d.usuario || ''}</div>
            </div>
            {gestor && <Botao variante="perigo" pequeno onClick={() => excluir(d)}>Excluir</Botao>}
          </Item>
        ))}
        {docs.length === 0 && <p className="text-sm text-slate-500">Nenhum arquivo ainda.</p>}
      </div>
    </div>
  )
}
