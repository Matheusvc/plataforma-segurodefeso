import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { Botao, BotaoLink, Campo, Card, Chip, Titulo, useAviso, useDados } from '../components/ui.jsx'
import { cpfMascarado } from '../util.js'

export default function Mensagens() {
  const { meta } = useAuth()
  const aviso = useAviso()
  const [params, setParams] = useSearchParams()
  const cliente = params.get('cliente') || ''
  const modelo = params.get('modelo') || 'DAE'
  const [busca, setBusca] = useState('')
  const [termo, setTermo] = useState('')
  useEffect(() => {
    const t = setTimeout(() => setTermo(busca.trim()), 300)
    return () => clearTimeout(t)
  }, [busca])
  // Busca no servidor: só os primeiros resultados, em vez de carregar todos os clientes.
  const { dados: resultados } = useDados(termo.length >= 2 ? `/clientes?q=${encodeURIComponent(termo)}&tamanho=8` : null)
  const { dados: msg } = useDados(cliente ? `/mensagens?cliente=${cliente}&modelo=${modelo}` : null)

  function mudar(k, v) {
    const p = Object.fromEntries(params.entries())
    p[k] = v
    setParams(p)
  }

  function escolher(id) {
    mudar('cliente', String(id))
    setBusca('')
    setTermo('')
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(msg.texto)
      aviso.ok('Texto copiado.')
    } catch {
      aviso.erro('Não foi possível copiar. Selecione o texto e copie manualmente.')
    }
  }

  return (
    <>
      <Titulo titulo="Mensagens" sub="Modelos prontos, preenchidos com os dados da ficha. O botão abre o WhatsApp com o texto; você só confere e envia." />
      <div className="flex flex-wrap items-start gap-4">
        <Card className="flex min-w-0 flex-[1_1_300px] flex-col gap-3">
          <Campo rotulo="Cliente">
            <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Digite nome, CPF ou município" aria-label="Buscar cliente" />
          </Campo>
          {termo.length >= 2 && resultados && (
            <ul className="flex flex-col gap-1.5" aria-label="Resultados da busca">
              {resultados.itens.map((a) => (
                <li key={a.cliente.id}>
                  <button type="button" onClick={() => escolher(a.cliente.id)}
                    className="flex min-h-[44px] w-full flex-col items-start justify-center rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-left hover:border-marca-700">
                    <span className="text-sm font-semibold">{a.cliente.nome}</span>
                    <span className="text-xs text-slate-500">CPF {cpfMascarado(a.cliente.cpf)} · {a.cliente.municipio}</span>
                  </button>
                </li>
              ))}
              {resultados.itens.length === 0 && <li className="text-sm text-slate-500">Nenhum cliente encontrado.</li>}
              {resultados.total > resultados.itens.length && <li className="text-xs text-slate-500">Mostrando {resultados.itens.length} de {resultados.total}. Digite mais para refinar.</li>}
            </ul>
          )}
          {cliente && msg && <div className="rounded-lg bg-marca-50 px-3 py-2 text-sm">Cliente escolhido: <strong>{msg.nome}</strong></div>}
          <div className="text-xs font-semibold text-slate-600">Modelo</div>
          <div className="flex flex-col gap-1.5">
            {meta.modelos.map((m) => (
              <button key={m.id} type="button" onClick={() => mudar('modelo', m.id)}
                className={`min-h-[44px] rounded-lg border px-3 text-left text-sm ${modelo === m.id ? 'border-marca-700 bg-marca-50 font-semibold text-marca-900' : 'border-slate-200 bg-slate-50 text-slate-700'}`}>
                {m.nome}
              </button>
            ))}
          </div>
        </Card>
        <Card className="flex min-w-0 flex-[3_1_420px] flex-col gap-3.5">
          {!cliente || !msg ? <p className="text-sm text-slate-500">Busque um cliente e escolha um modelo.</p> : (
            <>
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <h2 className="text-base font-semibold">Pré-visualização</h2>
                <span className="text-xs text-slate-600">Para: {msg.nome} · {msg.telefone || 'sem telefone'}</span>
              </div>
              <div className="whitespace-pre-line rounded-xl border border-green-200 bg-green-50 p-4 text-sm leading-relaxed text-green-950">{msg.texto}</div>
              <p className="text-xs text-slate-500">Campos entre colchetes, como [data], precisam ser completados antes de enviar.</p>
              <div className="flex flex-wrap gap-2">
                {msg.link ? <BotaoLink href={msg.link} target="_blank" rel="noopener noreferrer">Abrir no WhatsApp</BotaoLink>
                  : <Chip cor="aviso">Telefone ausente ou inválido no cadastro</Chip>}
                <Botao variante="neutro" onClick={copiar}>Copiar texto</Botao>
              </div>
            </>
          )}
        </Card>
      </div>
    </>
  )
}
