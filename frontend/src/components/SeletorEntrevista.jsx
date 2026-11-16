import { useAuth } from '../auth.jsx'

const ATIVO = {
  ok: 'border-green-700 bg-green-700 text-white',
  erro: 'border-red-700 bg-red-700 text-white',
  aviso: 'border-amber-500 bg-amber-500 text-white',
}

/** Botões Não / Confirmado / A confirmar da entrevista de um cliente. */
export default function SeletorEntrevista({ valor, aoMudar, desabilitado, nome }) {
  const { meta } = useAuth()
  return (
    <div className="flex flex-wrap gap-1.5" role="group" aria-label={`Entrevista de ${nome}`}>
      {meta.entrevistas.map((e) => (
        <button key={e.id} type="button" disabled={desabilitado} aria-pressed={valor === e.id} onClick={() => valor !== e.id && aoMudar(e.id)}
          className={`min-h-[34px] rounded-lg border px-3 text-[13px] font-semibold disabled:opacity-60 ${valor === e.id
            ? ATIVO[e.cor] : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'}`}>
          {e.nome}
        </button>
      ))}
    </div>
  )
}
