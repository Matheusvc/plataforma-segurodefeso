import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { Botao, BotaoLink, Campo, Card, Chip, Titulo, useAviso, useDados } from '../components/ui.jsx'

export default function Mensagens() {
  const { meta } = useAuth()
  const aviso = useAviso()
  const [params, setParams] = useSearchParams()
  const cliente = params.get('cliente') || ''
  const modelo = params.get('modelo') || 'DAE'
  const { dados: clientes } = useDados('/clientes')
  const { dados: msg } = useDados(cliente ? `/mensagens?cliente=${cliente}&modelo=${modelo}` : null)
  const atual = (clientes || []).find((a) => String(a.cliente.id) === cliente)

  function mudar(k, v) {
    const p = Object.fromEntries(params.entries())
    p[k] = v
    setParams(p)
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
            <select value={cliente} onChange={(e) => mudar('cliente', e.target.value)}>
              <option value="">Escolha o cliente</option>
              {(clientes || []).map((a) => <option key={a.cliente.id} value={a.cliente.id}>{a.cliente.nome}</option>)}
            </select>
          </Campo>
          <div className="text-xs font-semibold text-slate-600">Modelo</div>
          <div className="flex flex-col gap-1.5">
            {meta.modelos.map((m) => (
              <button key={m.id} type="button" onClick={() => mudar('modelo', m.id)}
                className={`min-h-[40px] rounded-lg border px-3 text-left text-sm ${modelo === m.id ? 'border-marca-700 bg-marca-50 font-semibold text-marca-900' : 'border-slate-200 bg-slate-50 text-slate-700'}`}>
                {m.nome}
              </button>
            ))}
          </div>
        </Card>
        <Card className="flex min-w-0 flex-[3_1_420px] flex-col gap-3.5">
          {!cliente || !msg ? <p className="text-sm text-slate-500">Escolha um cliente e um modelo.</p> : (
            <>
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <h2 className="text-base font-semibold">Pré-visualização</h2>
                <span className="text-xs text-slate-600">Para: {atual?.cliente.nome} · {msg.telefone || 'sem telefone'}</span>
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
