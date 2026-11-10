import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Alerta, Botao, Card, Chip, Item, Titulo, useAcao } from '../components/ui.jsx'

export default function Triagem() {
  const { meta } = useAuth()
  const navegar = useNavigate()
  const { executar, ocupado } = useAcao()
  const [resp, setResp] = useState({})
  const [resultado, setResultado] = useState(null)

  async function avaliar() {
    const r = await executar(() => api('/triagem/avaliar', { metodo: 'POST', corpo: { respostas: resp } }))
    if (r) setResultado(r)
  }

  return (
    <>
      <Titulo titulo="Triagem" sub="Primeiro passo do atendimento: responda os requisitos e veja se o pescador tem direito. Depois siga para o cadastro." />
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
              <Botao onClick={() => navegar('/clientes/novo', { state: { respostas: resp, triagem: resultado.titulo } })}>
                Continuar para o cadastro ›
              </Botao>
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
