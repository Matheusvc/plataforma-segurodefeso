import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Alerta, Botao, Campo, Card, Chip, Item, Titulo, useAcao } from '../components/ui.jsx'

export default function Triagem() {
  const { meta } = useAuth()
  const navegar = useNavigate()
  const { executar, ocupado } = useAcao()
  const [resp, setResp] = useState({})
  const [resultado, setResultado] = useState(null)
  const [cad, setCad] = useState({ nome: '', cpf: '', telefone: '' })

  async function avaliar() {
    const r = await executar(() => api('/triagem/avaliar', { metodo: 'POST', corpo: { respostas: resp } }))
    if (r) setResultado(r)
  }

  async function cadastrar(e) {
    e.preventDefault()
    await executar(() => api('/triagem/cadastrar', { metodo: 'POST', corpo: { respostas: resp, ...cad } }),
      (r) => navegar(`/clientes/${r.id}/editar`))
  }

  return (
    <>
      <Titulo titulo="Triagem de elegibilidade" sub="Responda no atendimento e saiba na hora se o pescador tem direito. Se tiver, cadastre com um clique." />
      <div className="flex flex-wrap items-start gap-4">
        <Card className="flex min-w-0 flex-[3_1_520px] flex-col gap-2.5">
          {meta.requisitos.map((r) => (
            <Item key={r.id}>
              <div className="min-w-0 flex-[1_1_300px]">
                <div className="font-semibold">{r.texto} {r.essencial && <Chip>essencial</Chip>}</div>
                <div className="text-[13px] text-slate-600">Como comprovar: {r.prova}</div>
              </div>
              <div className="flex gap-1.5">
                {[[true, 'Sim'], [false, 'Não']].map(([v, n]) => (
                  <button key={n} type="button" onClick={() => setResp((x) => ({ ...x, [r.id]: v }))}
                    className={`min-h-[38px] min-w-[60px] rounded-lg border px-3 text-[13px] font-semibold ${resp[r.id] === v
                      ? (v ? 'border-green-700 bg-green-700 text-white' : 'border-red-700 bg-red-700 text-white')
                      : 'border-slate-300 bg-white text-slate-700'}`}>{n}</button>
                ))}
              </div>
            </Item>
          ))}
          <p className="text-xs text-slate-500">{meta.baseLegal}</p>
          <div><Botao onClick={avaliar} disabled={ocupado}>Ver resultado</Botao></div>
        </Card>
        <div className="flex min-w-0 flex-[2_1_320px] flex-col gap-4">
          {resultado && (
            <>
              <Alerta cor={resultado.cor}>
                <div className="text-lg font-bold">{resultado.titulo}</div>
                <div className="mt-1">{resultado.texto}</div>
                {resultado.itens.length > 0 && <ul className="mt-2 list-disc pl-5">{resultado.itens.map((t) => <li key={t}>{t}</li>)}</ul>}
              </Alerta>
              <Card>
                <form onSubmit={cadastrar} className="flex flex-col gap-3">
                  <h2 className="text-base font-semibold">Cadastrar com este resultado</h2>
                  <Campo rotulo="Nome completo" obrigatorio><input type="text" required value={cad.nome} onChange={(e) => setCad({ ...cad, nome: e.target.value })} /></Campo>
                  <Campo rotulo="CPF" obrigatorio><input type="text" required inputMode="numeric" value={cad.cpf} onChange={(e) => setCad({ ...cad, cpf: e.target.value })} /></Campo>
                  <Campo rotulo="Telefone/WhatsApp"><input type="text" value={cad.telefone} onChange={(e) => setCad({ ...cad, telefone: e.target.value })} /></Campo>
                  <Botao type="submit" disabled={ocupado}>Cadastrar cliente</Botao>
                </form>
              </Card>
            </>
          )}
          <Card>
            <h2 className="mb-2 text-base font-semibold">Documentos para pedir já no atendimento</h2>
            <ul className="list-disc pl-5 text-sm leading-7">{meta.documentos.filter((d) => d.flag).map((d) => <li key={d.id}>{d.nome}</li>)}</ul>
          </Card>
        </div>
      </div>
    </>
  )
}
