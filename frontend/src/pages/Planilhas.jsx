import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, urlApi } from '../api.js'
import { Botao, BotaoLink, Card, Chip, Item, Td, Th, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { cpf } from '../util.js'

const ACAO = { novo: ['ok', 'Novo'], atualizar: ['info', 'Completar'], erro: ['erro', 'Erro'] }

export default function Planilhas() {
  const { dados: listas } = useDados('/planilhas')
  const { executar, ocupado } = useAcao()
  const navegar = useNavigate()
  const [arquivo, setArquivo] = useState(null)
  const [previa, setPrevia] = useState(null)

  async function verPrevia(e) {
    e.preventDefault()
    const form = new FormData()
    form.append('arquivo', arquivo)
    const p = await executar(() => api('/planilhas/importar', { metodo: 'POST', form }))
    if (p) setPrevia(p)
  }

  async function confirmar() {
    await executar(() => api(`/planilhas/importar/${previa.token}/confirmar`, { metodo: 'POST' }), () => {
      setPrevia(null)
      navegar('/clientes')
    })
  }

  return (
    <>
      <Titulo titulo="Planilhas" sub="Exporte qualquer lista (Excel ou CSV) e importe a planilha de clientes que o escritório já usa." />
      {!previa && (
        <>
          <Card className="flex flex-col gap-3.5">
            <h2 className="text-base font-semibold">Exportar</h2>
            <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3">
              {(listas || []).map((l) => (
                <Item key={l.id} className="flex-col items-stretch">
                  <div><div className="font-semibold">{l.titulo}</div><div className="text-[13px] text-slate-600">{l.descricao}</div></div>
                  <div className="flex gap-2">
                    <BotaoLink href={urlApi(`/planilhas/exportar/${l.id}.xlsx`)} variante="sec" pequeno>Excel (.xlsx)</BotaoLink>
                    <BotaoLink href={urlApi(`/planilhas/exportar/${l.id}.csv`)} variante="neutro" pequeno>CSV</BotaoLink>
                  </div>
                </Item>
              ))}
            </div>
          </Card>
          <Card className="flex flex-col gap-3.5">
            <h2 className="text-base font-semibold">Importar planilha de clientes</h2>
            <ol className="list-decimal pl-5 text-sm leading-7">
              <li>Aceita <strong>.xlsx</strong> (Excel) ou <strong>.csv</strong>. Arquivo .xls antigo: abra no Excel e salve como .xlsx.</li>
              <li>Colunas obrigatórias: <strong>Nome</strong> e <strong>CPF</strong>. As demais são aproveitadas se existirem, em qualquer ordem.</li>
              <li>Você verá uma prévia antes de confirmar. CPF já cadastrado: só completa campos vazios, nunca apaga o que a equipe lançou.</li>
            </ol>
            <form onSubmit={verPrevia} className="flex flex-wrap items-center gap-2">
              <input type="file" required accept=".xlsx,.csv,.txt" onChange={(e) => setArquivo(e.target.files[0] || null)} />
              <Botao type="submit" disabled={ocupado || !arquivo}>Ver prévia</Botao>
              <BotaoLink href={urlApi('/planilhas/modelo.xlsx')} variante="neutro">Baixar planilha modelo</BotaoLink>
            </form>
          </Card>
        </>
      )}
      {previa && (
        <>
          <Card className="flex flex-wrap items-center gap-2">
            <strong className="mr-2">{previa.arquivo}</strong>
            <Chip cor="ok">{previa.totais.novo} novo(s)</Chip>
            <Chip cor="info">{previa.totais.atualizar} já cadastrado(s): completar campos vazios</Chip>
            <Chip cor="erro">{previa.totais.erro} com erro (ficam de fora)</Chip>
            <span className="text-xs text-slate-600">Colunas reconhecidas: {previa.colunas.join(', ')}.{previa.naoEncontradas.length > 0 && ` Não encontradas: ${previa.naoEncontradas.join(', ')}.`}</span>
          </Card>
          <Card semPad className="overflow-x-auto">
            <table className="w-full min-w-[820px] border-collapse">
              <thead><tr><Th>Linha</Th><Th>Nome</Th><Th>CPF</Th><Th>Município</Th><Th>RGP</Th><Th>Resultado</Th><Th>Observações</Th></tr></thead>
              <tbody>
                {previa.registros.slice(0, 500).map((r) => (
                  <tr key={r.linha}>
                    <Td>{r.linha}</Td><Td>{r.dados.nome || '—'}</Td><Td>{cpf(r.dados.cpf)}</Td><Td>{r.dados.municipio || '—'}</Td><Td>{r.dados.rgpNumero || '—'}</Td>
                    <Td><Chip cor={ACAO[r.acao][0]}>{ACAO[r.acao][1]}</Chip></Td>
                    <Td className="text-xs text-slate-600">{[...r.erros, ...r.avisos].join('; ')}</Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
          {previa.registros.length > 500 && <p className="text-xs text-slate-500">Mostrando 500 de {previa.registros.length} linhas. Todas serão importadas.</p>}
          <div className="flex gap-2">
            <Botao onClick={confirmar} disabled={ocupado || previa.totais.novo + previa.totais.atualizar === 0}>Confirmar importação</Botao>
            <Botao variante="neutro" onClick={() => setPrevia(null)}>Cancelar</Botao>
          </div>
        </>
      )}
    </>
  )
}
