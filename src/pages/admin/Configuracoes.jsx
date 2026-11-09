import { useEffect, useState } from 'react'
import { api } from '../../api.js'
import { Botao, Campo, Card, Carregando, Titulo, useAcao } from '../../components/ui.jsx'

const virgula = (v) => (v === null || v === undefined ? '' : String(v).replace('.', ','))

export default function Configuracoes() {
  const { executar, ocupado } = useAcao()
  const [f, setF] = useState(null)

  useEffect(() => {
    api('/admin/configuracoes').then((c) => setF({
      nomeEscritorio: c.nomeEscritorio, salarioMinimo: virgula(c.salarioMinimo), honorariosPct: virgula(c.honorariosPct),
      aliquotaDae: virgula(c.aliquotaDae), diaVencimentoDae: c.diaVencimentoDae, parcelasPadrao: c.parcelasPadrao,
    }))
  }, [])

  async function salvar(e) {
    e.preventDefault()
    await executar(() => api('/admin/configuracoes', { metodo: 'PUT', corpo: { ...f, diaVencimentoDae: Number(f.diaVencimentoDae), parcelasPadrao: Number(f.parcelasPadrao) } }))
  }

  if (!f) return <Carregando />
  const campo = (k, rotulo, tipo = 'text') => (
    <Campo rotulo={rotulo}><input type={tipo} value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} /></Campo>
  )
  return (
    <>
      <Titulo titulo="Configurações" sub="Valores usados nos cálculos. Atualize o salário mínimo todo início de ano e confira a alíquota do DAE quando houver mudança na lei." />
      <Card className="max-w-3xl">
        <form onSubmit={salvar} className="flex flex-col gap-4">
          <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5">
            {campo('nomeEscritorio', 'Nome do escritório (aparece nas mensagens)')}
            {campo('salarioMinimo', 'Salário mínimo vigente (R$)')}
            {campo('honorariosPct', 'Honorários padrão (% por parcela)')}
            {campo('aliquotaDae', 'Alíquota do DAE do segurado especial (%)')}
            {campo('diaVencimentoDae', 'Dia de vencimento estimado do DAE', 'number')}
            {campo('parcelasPadrao', 'Parcelas padrão do seguro-defeso', 'number')}
          </div>
          <p className="text-xs text-slate-500">Referências: salário mínimo de 2026 = R$ 1.621 (Decreto 12.797/2025). DAE do segurado especial = 1,5% da comercialização (1,2% Previdência + 0,1% GILRAT + 0,2% SENAR). O vencimento oficial é o que consta na guia.</p>
          <div><Botao type="submit" disabled={ocupado}>Salvar</Botao></div>
        </form>
      </Card>
    </>
  )
}
