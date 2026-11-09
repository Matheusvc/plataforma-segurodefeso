// Formatação no padrão brasileiro.

export function brl(v) {
  const n = Number(v || 0)
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

/** "2026-11-19" ou "2026-11-19T10:30:00" -> "19/11/2026" */
export function br(iso) {
  if (!iso) return ''
  const [a, m, d] = String(iso).slice(0, 10).split('-')
  return d && m && a ? `${d}/${m}/${a}` : ''
}

export function hora(iso) {
  return iso ? String(iso).slice(11, 16) : ''
}

export function cpf(c) {
  const d = String(c || '').replace(/\D/g, '')
  return d.length === 11 ? `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}` : c || ''
}

export function cpfMascarado(c) {
  const d = String(c || '').replace(/\D/g, '')
  return d.length === 11 ? `***.${d.slice(3, 6)}.${d.slice(6, 9)}-**` : c || ''
}

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']
export function competencia(comp) {
  if (!comp) return ''
  const [a, m] = comp.split('-')
  return `${MESES[Number(m) - 1]}/${a.slice(2)}`
}

/** 1234.5 -> "1.234,50" (para preencher campos de valor) */
export function numeroBr(v) {
  if (v === null || v === undefined || v === '') return ''
  return Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
