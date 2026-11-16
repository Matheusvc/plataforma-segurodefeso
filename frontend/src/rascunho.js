// Rascunho do assistente de novo cadastro, guardado só nesta aba do navegador (some ao fechá-la).
// Nunca inclui a senha gov. Depois que o cliente é salvo, o progresso passa a viver no servidor ("Falta finalizar").

const chave = (usuarioId) => `rascunho-novo-cadastro-${usuarioId}`

export function lerRascunho(usuarioId) {
  try {
    const texto = sessionStorage.getItem(chave(usuarioId))
    return texto ? JSON.parse(texto) : null
  } catch {
    return null
  }
}

export function salvarRascunho(usuarioId, dados) {
  try {
    sessionStorage.setItem(chave(usuarioId), JSON.stringify(dados))
  } catch {
    // sem armazenamento disponível: o assistente funciona, só não guarda rascunho
  }
}

export function limparRascunho(usuarioId) {
  try {
    sessionStorage.removeItem(chave(usuarioId))
  } catch {
    // nada a limpar
  }
}
