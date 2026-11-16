import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { br, cpf } from '../util.js'
import { Botao, BotaoLink, Item, useAcao } from './ui.jsx'

/** Conferência manual do CPF na Receita: abre a consulta e registra quem conferiu e quando. */
export default function ConferenciaCpf({ c, por, recarregar }) {
  const { meta } = useAuth()
  const { executar, ocupado } = useAcao()
  const conferido = c.pessoal.cpfConferidoEm
  const receita = meta.sites.find((s) => s.id === 'receitacpf')?.extra

  return (
    <Item>
      <div className="min-w-0 flex-1">
        <div className="font-semibold">Conferência do CPF</div>
        <div className="text-[13px] text-slate-600">
          {conferido
            ? `Conferido na Receita em ${br(conferido)} por ${por || 'usuário removido'}.`
            : `Abra a consulta da Receita e informe o CPF ${cpf(c.cpf)}${c.nascimento ? ` e a data de nascimento ${br(c.nascimento)}` : ''}. Compare o nome com o documento e marque como conferido.`}
        </div>
      </div>
      {!conferido && receita && (
        <BotaoLink href={receita} target="_blank" rel="noopener noreferrer" variante="sec" pequeno>Abrir consulta da Receita ›</BotaoLink>
      )}
      <Botao variante={conferido ? 'neutro' : 'pri'} pequeno disabled={ocupado}
        onClick={() => executar(() => api(`/clientes/${c.id}/cpf-conferido`, { metodo: 'PUT', corpo: { conferido: !conferido } }), recarregar)}>
        {conferido ? 'Desfazer' : 'Marcar como conferido'}
      </Botao>
    </Item>
  )
}
