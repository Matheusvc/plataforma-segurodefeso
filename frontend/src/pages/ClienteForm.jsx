import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { Botao, BotaoLink, Campo, Card, Carregando, Titulo, useAcao } from '../components/ui.jsx'
import { br, cpf } from '../util.js'

const CAMPOS = [
  ['nome', 'Nome completo', true],
  ['cpf', 'CPF', true, '000.000.000-00'],
  ['nit', 'NIT/PIS'],
  ['nascimento', 'Data de nascimento', false, 'dd/mm/aaaa'],
  ['telefone', 'Telefone/WhatsApp', true],
  ['endereco', 'Endereço'],
  ['municipio', 'Município', true],
  ['colonia', 'Colônia'],
  ['rgpNumero', 'Nº do RGP', true],
  ['rgpDesde', 'RGP registrado desde (mês/ano)', false, 'mm/aaaa'],
  ['rgpValidade', 'Validade do RGP', false, 'dd/mm/aaaa'],
  ['especie', 'Espécie / modalidade de pesca'],
  ['procuracaoValidade', 'Validade da procuração e-CAC', false, 'dd/mm/aaaa'],
  ['honorariosPct', 'Honorários (% por parcela; vazio = padrão)'],
]
const DATAS = ['nascimento', 'rgpValidade', 'procuracaoValidade']

export default function ClienteForm() {
  const { id } = useParams()
  const novo = !id
  const navegar = useNavigate()
  const { executar, ocupado } = useAcao()
  const [form, setForm] = useState(novo ? {} : null)

  useEffect(() => {
    if (novo) return
    api(`/clientes/${id}`).then((f) => {
      const c = f.analise.cliente
      const v = {}
      CAMPOS.forEach(([k]) => { v[k] = c[k] ?? '' })
      DATAS.forEach((k) => { v[k] = br(c[k]) })
      v.cpf = cpf(c.cpf)
      v.honorariosPct = c.honorariosPct != null ? String(c.honorariosPct).replace('.', ',') : ''
      v.govNivel = c.govNivel || ''
      v.observacoes = c.observacoes || ''
      setForm(v)
    })
  }, [id, novo])

  function mudar(k, v) {
    setForm((f) => ({ ...f, [k]: v }))
  }

  async function salvar(e) {
    e.preventDefault()
    await executar(
      () => (novo ? api('/clientes', { metodo: 'POST', corpo: form }) : api(`/clientes/${id}`, { metodo: 'PUT', corpo: form })),
      (r) => navegar(novo ? `/clientes/${r.id}?aba=requisitos` : `/clientes/${id}`),
    )
  }

  if (!form) return <Carregando />
  return (
    <>
      <Titulo titulo={novo ? 'Novo cliente' : 'Editar cadastro'}
        sub="Campos com * são obrigatórios. Datas no formato dd/mm/aaaa. A senha gov.br do cliente nunca é registrada aqui." />
      <Card>
        <form onSubmit={salvar} className="flex flex-col gap-5">
          <div className="grid grid-cols-[repeat(auto-fit,minmax(230px,1fr))] gap-x-4 gap-y-3.5">
            {CAMPOS.map(([k, rotulo, obrig, dica]) => (
              <Campo key={k} rotulo={rotulo} obrigatorio={obrig}>
                <input type="text" value={form[k] || ''} required={obrig} placeholder={dica}
                  onChange={(e) => mudar(k, e.target.value)} />
              </Campo>
            ))}
            <Campo rotulo="Nível da conta gov.br">
              <select value={form.govNivel || ''} onChange={(e) => mudar('govNivel', e.target.value)}>
                <option value="">Não informado</option>
                <option value="bronze">Bronze</option>
                <option value="prata">Prata</option>
                <option value="ouro">Ouro</option>
              </select>
            </Campo>
          </div>
          <Campo rotulo="Observações"><textarea rows={3} value={form.observacoes || ''} onChange={(e) => mudar('observacoes', e.target.value)} /></Campo>
          <div className="flex flex-wrap gap-2">
            <Botao type="submit" disabled={ocupado}>{novo ? 'Cadastrar cliente' : 'Salvar alterações'}</Botao>
            <BotaoLink to={novo ? '/clientes' : `/clientes/${id}`} variante="neutro">Cancelar</BotaoLink>
          </div>
        </form>
      </Card>
    </>
  )
}
