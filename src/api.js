// Comunicação com o back-end Spring Boot.
// A sessão fica num cookie; a proteção CSRF usa o cookie XSRF-TOKEN, devolvido no cabeçalho X-XSRF-TOKEN.

const MENSAGENS = {
  401: 'Sua sessão expirou. Entre de novo.',
  403: 'Você não tem permissão para esta ação.',
  404: 'Não encontrado.',
  413: 'Arquivo muito grande.',
  500: 'Erro no servidor. Tente de novo em instantes.',
}

function lerCookie(nome) {
  const item = document.cookie.split('; ').find((c) => c.startsWith(nome + '='))
  return item ? decodeURIComponent(item.split('=')[1]) : null
}

export async function prepararCsrf() {
  await fetch('/api/auth/csrf', { credentials: 'same-origin' })
}

export async function api(caminho, { metodo = 'GET', corpo, form } = {}, tentativa = 0) {
  const headers = { Accept: 'application/json' }
  let body
  if (metodo !== 'GET') {
    const token = lerCookie('XSRF-TOKEN')
    if (token) headers['X-XSRF-TOKEN'] = token
  }
  if (form) {
    body = form
  } else if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(corpo)
  }
  const r = await fetch('/api' + caminho, { method: metodo, headers, body, credentials: 'same-origin' })
  if (r.status === 403 && metodo !== 'GET' && tentativa === 0 && !lerCookie('XSRF-TOKEN')) {
    await prepararCsrf()
    return api(caminho, { metodo, corpo, form }, 1)
  }
  if (r.status === 401 && !caminho.startsWith('/auth/')) {
    window.dispatchEvent(new Event('sessao-expirada'))
  }
  const tipo = r.headers.get('content-type') || ''
  const dados = tipo.includes('application/json') ? await r.json() : null
  if (!r.ok) throw new Error((dados && dados.erro) || MENSAGENS[r.status] || 'Erro inesperado.')
  return dados
}

/** Downloads (planilhas, documentos): o navegador já envia o cookie da sessão. */
export function urlApi(caminho) {
  return '/api' + caminho
}
