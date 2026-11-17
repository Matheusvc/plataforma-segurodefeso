// Máscaras (formatam e bloqueiam o que não cabe enquanto a pessoa digita) e validadores (mensagem de erro ou '').

const soDigitos = (v, max) => String(v ?? '').replace(/\D/g, '').slice(0, max)
const maiusculo = (v, permitido, max) => String(v ?? '').toUpperCase().replace(permitido, '').slice(0, max)

export const mascaras = {
  /** dd/mm/aaaa */
  data(v) {
    const d = soDigitos(v, 8)
    if (d.length <= 2) return d
    if (d.length <= 4) return `${d.slice(0, 2)}/${d.slice(2)}`
    return `${d.slice(0, 2)}/${d.slice(2, 4)}/${d.slice(4)}`
  },
  /** mm/aaaa */
  mesAno(v) {
    const d = soDigitos(v, 6)
    return d.length <= 2 ? d : `${d.slice(0, 2)}/${d.slice(2)}`
  },
  /** 000.000.000-00 */
  cpf(v) {
    const d = soDigitos(v, 11)
    if (d.length <= 3) return d
    if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`
    if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`
    return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
  },
  /** (00) 00000-0000 ou (00) 0000-0000 */
  telefone(v) {
    const d = soDigitos(v, 11)
    if (d.length === 0) return ''
    if (d.length <= 2) return `(${d}`
    if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`
    if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
    return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
  },
  /** 00000-000 */
  cep(v) {
    const d = soDigitos(v, 8)
    return d.length <= 5 ? d : `${d.slice(0, 5)}-${d.slice(5)}`
  },
  /** 000.00000.00-0 */
  nit(v) {
    const d = soDigitos(v, 11)
    if (d.length <= 3) return d
    if (d.length <= 8) return `${d.slice(0, 3)}.${d.slice(3)}`
    if (d.length <= 10) return `${d.slice(0, 3)}.${d.slice(3, 8)}.${d.slice(8)}`
    return `${d.slice(0, 3)}.${d.slice(3, 8)}.${d.slice(8, 10)}-${d.slice(10)}`
  },
  /** Só números, até 12 (título de eleitor, numeração da inscrição). */
  numeros12: (v) => soDigitos(v, 12),
  /** Nome de pessoa ou cidade: só letras, espaço, apóstrofo, ponto e hífen. */
  nome: (v) => String(v ?? '').replace(/[^A-Za-zÀ-ÖØ-öø-ÿ' .-]/g, '').replace(/^\s+/, '').replace(/\s{2,}/g, ' ').slice(0, 150),
  uf: (v) => String(v ?? '').replace(/[^A-Za-z]/g, '').toUpperCase().slice(0, 2),
  /** Número do endereço: 123, 12A ou S/N. */
  numeroEndereco: (v) => maiusculo(v, /[^0-9A-Z/]/g, 6),
  rg: (v) => maiusculo(v, /[^0-9A-Z.-]/g, 15),
  rgp: (v) => maiusculo(v, /[^0-9A-Z./-]/g, 20),
  orgaoEmissor: (v) => maiusculo(v, /[^0-9A-Z/-]/g, 12),
  oab: (v) => maiusculo(v, /[^0-9A-Z/-]/g, 14),
  /** Valor em reais: só números e uma vírgula com até 2 casas (ex.: 1250,50). */
  dinheiro(v) {
    let s = String(v ?? '').replace(/[^\d,]/g, '')
    const i = s.indexOf(',')
    if (i >= 0) s = s.slice(0, i + 1) + s.slice(i + 1).replace(/,/g, '').slice(0, 2)
    return s.slice(0, 13)
  },
  /** 30 ou 27,5 */
  percentual(v) {
    let s = String(v ?? '').replace(/[^\d,]/g, '')
    const i = s.indexOf(',')
    if (i >= 0) s = s.slice(0, i + 1) + s.slice(i + 1).replace(/,/g, '')
    return s.slice(0, 6)
  },
  texto: (max) => (v) => String(v ?? '').replace(/^\s+/, '').slice(0, max),
}

function lerData(txt) {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(txt || '')
  if (!m) return null
  const [dia, mes, ano] = [Number(m[1]), Number(m[2]), Number(m[3])]
  const dt = new Date(ano, mes - 1, dia)
  return dt.getFullYear() === ano && dt.getMonth() === mes - 1 && dt.getDate() === dia ? dt : null
}

export function cpfValido(txt) {
  const d = String(txt ?? '').replace(/\D/g, '')
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false
  const digito = (n) => {
    let soma = 0
    for (let i = 0; i < n; i++) soma += Number(d[i]) * (n + 1 - i)
    const r = (soma * 10) % 11
    return r === 10 ? 0 : r
  }
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10])
}

export const validadores = {
  /** Data dd/mm/aaaa real. passado: não aceita data futura. */
  data: ({ passado = false, min = 1900, max = 2100 } = {}) => (v) => {
    if (!v) return ''
    const dt = lerData(v)
    if (!dt) return 'Data inválida. Use dd/mm/aaaa, por exemplo 07/02/2008.'
    if (dt.getFullYear() < min || dt.getFullYear() > max) return `Informe um ano entre ${min} e ${max}.`
    if (passado && dt > new Date()) return 'A data não pode ser futura.'
    return ''
  },
  mesAno: (v) => {
    if (!v) return ''
    const m = /^(\d{2})\/(\d{4})$/.exec(v)
    if (!m || Number(m[1]) < 1 || Number(m[1]) > 12) return 'Use mês/ano, por exemplo 05/2019.'
    if (Number(m[2]) < 1950 || Number(m[2]) > new Date().getFullYear()) return 'Ano fora do esperado.'
    return ''
  },
  cpf: (v) => (!v ? '' : cpfValido(v) ? '' : 'CPF inválido: confira os 11 números.'),
  telefone: (v) => {
    if (!v) return ''
    const d = v.replace(/\D/g, '')
    if (d.length < 10 || d.length > 11) return 'Informe o DDD e o número (10 ou 11 números).'
    if (Number(d.slice(0, 2)) < 11) return 'DDD inválido.'
    if (d.length === 11 && d[2] !== '9') return 'Celular com 11 números deve começar com 9 depois do DDD.'
    return ''
  },
  /** Valor em reais, com ou sem separador de milhar: 1250, 1.250,00 ou 486,3. */
  dinheiro: (v) => (!v || /^\d{1,9}(,\d{1,2})?$/.test(String(v).replace(/\./g, '')) ? '' : 'Valor inválido: use só números, por exemplo 1250,00.'),
  cep: (v) => (!v || v.replace(/\D/g, '').length === 8 ? '' : 'O CEP tem 8 números.'),
  nit: (v) => (!v || v.replace(/\D/g, '').length === 11 ? '' : 'O NIT/PIS tem 11 números.'),
  uf: (v) => (!v || /^[A-Z]{2}$/.test(v) ? '' : 'Use a sigla com 2 letras, por exemplo MA.'),
  nomeCompleto: (v) => (!v || v.trim().split(/\s+/).filter((p) => p.length > 1).length >= 2 ? '' : 'Informe nome e sobrenome.'),
  percentual: (v) => {
    if (!v) return ''
    const n = Number(v.replace(',', '.'))
    return Number.isNaN(n) || n < 0 || n > 100 ? 'Use um percentual entre 0 e 100.' : ''
  },
}
