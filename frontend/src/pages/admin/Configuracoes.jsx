import { useEffect, useState } from 'react'
import { api } from '../../api.js'
import { Botao, Campo, Card, Carregando, Titulo, useAcao, useAviso } from '../../components/ui.jsx'
import { mascaras, validadores } from '../../formato.js'

const virgula = (v) => (v === null || v === undefined ? '' : String(v).replace('.', ','))

export default function Configuracoes() {
  const { executar, ocupado } = useAcao()
  const aviso = useAviso()
  const [f, setF] = useState(null)

  useEffect(() => {
    api('/admin/configuracoes').then((c) => setF({
      nomeEscritorio: c.nomeEscritorio, salarioMinimo: virgula(c.salarioMinimo), honorariosPct: virgula(c.honorariosPct),
      aliquotaDae: virgula(c.aliquotaDae), diaVencimentoDae: c.diaVencimentoDae, parcelasPadrao: c.parcelasPadrao,
    }))
  }, [])

  async function salvar(e) {
    e.preventDefault()
    const dia = Number(f.diaVencimentoDae)
    const parcelas = Number(f.parcelasPadrao)
    const problema = !f.salarioMinimo ? 'Informe o salário mínimo vigente.'
      : validadores.dinheiro(f.salarioMinimo) || validadores.percentual(f.honorariosPct) || validadores.percentual(f.aliquotaDae)
      || (!f.honorariosPct || !f.aliquotaDae ? 'Preencha os percentuais de honorários e do DAE.' : '')
      || (!Number.isInteger(dia) || dia < 1 || dia > 28 ? 'O dia de vencimento do DAE deve ser de 1 a 28.' : '')
      || (!Number.isInteger(parcelas) || parcelas < 1 || parcelas > 12 ? 'As parcelas padrão devem ser de 1 a 12.' : '')
    if (problema) {
      aviso.erro(problema)
      return
    }
    await executar(() => api('/admin/configuracoes', { metodo: 'PUT', corpo: { ...f, diaVencimentoDae: dia, parcelasPadrao: parcelas } }))
  }

  if (!f) return <Carregando />
  const campo = (k, rotulo, mascara, extra = {}) => (
    <Campo rotulo={rotulo}>
      <input type={extra.type || 'text'} value={f[k]} onChange={(e) => setF({ ...f, [k]: mascara ? mascara(e.target.value) : e.target.value })} {...extra} />
    </Campo>
  )
  return (
    <>
      <Titulo titulo="Parâmetros do sistema" sub="Valores usados nos cálculos. Atualize o salário mínimo todo início de ano e confira a alíquota do DAE quando houver mudança na lei." />
      <Card className="max-w-3xl">
        <form onSubmit={salvar} noValidate className="flex flex-col gap-4">
          <div className="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-3.5">
            {campo('nomeEscritorio', 'Nome do escritório (aparece nas mensagens)', null, { maxLength: 100 })}
            {campo('salarioMinimo', 'Salário mínimo vigente (R$)', mascaras.dinheiro, { inputMode: 'decimal', maxLength: 13 })}
            {campo('honorariosPct', 'Honorários padrão (% por parcela)', mascaras.percentual, { inputMode: 'decimal', maxLength: 6 })}
            {campo('aliquotaDae', 'Alíquota do DAE do segurado especial (%)', mascaras.percentual, { inputMode: 'decimal', maxLength: 6 })}
            {campo('diaVencimentoDae', 'Dia de vencimento estimado do DAE (1 a 28)', null, { type: 'number', min: 1, max: 28, inputMode: 'numeric' })}
            {campo('parcelasPadrao', 'Parcelas padrão do seguro-defeso (1 a 12)', null, { type: 'number', min: 1, max: 12, inputMode: 'numeric' })}
          </div>
          <p className="text-xs text-slate-500">Referências: salário mínimo de 2026 = R$ 1.621 (Decreto 12.797/2025). DAE do segurado especial = 1,5% da comercialização (1,2% Previdência + 0,1% GILRAT + 0,2% SENAR). O vencimento oficial é o que consta na guia.</p>
          <div><Botao type="submit" disabled={ocupado}>Salvar</Botao></div>
        </form>
      </Card>
    </>
  )
}
