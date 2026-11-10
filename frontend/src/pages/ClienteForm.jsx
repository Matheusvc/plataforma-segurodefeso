import { useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { Alerta, Botao, BotaoLink, Campo, Card, Carregando, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { br, cpf } from '../util.js'

const SEXOS = ['Masculino', 'Feminino', 'Outro']
const STATUS_CIVIS = ['Solteiro(a)', 'Casado(a)', 'União estável', 'Separado(a)', 'Divorciado(a)', 'Viúvo(a)']
const NIVEIS_GOV = [['bronze', 'Bronze'], ['prata', 'Prata'], ['ouro', 'Ouro']]

// Blocos e campos na ordem e com os nomes definidos pelo escritório.
// Campo: [chave, rótulo, obrigatório, dica, opções ou 'senha']
const SECOES = [
  ['Cadastro', [
    ['nome', 'Nome completo', true],
    ['nascimento', 'Data de nascimento', false, 'dd/mm/aaaa'],
    ['sexo', 'Sexo', false, null, SEXOS],
    ['cpf', 'CPF', true, '000.000.000-00'],
    ['rg', 'RG'],
    ['orgaoEmissor', 'Órgão do emissor', false, 'ex.: SSP'],
    ['dataExpedicao', 'Data da expedição', false, 'dd/mm/aaaa'],
    ['nomePai', 'Nome do pai'],
    ['nomeMae', 'Nome da mãe'],
    ['estadoCivil', 'Status civil', false, null, STATUS_CIVIS],
    ['estadoOrigem', 'Estado de origem', false, 'UF, ex.: MA'],
    ['cidadeOrigem', 'Cidade de origem'],
    ['tituloEleitor', 'Título'],
    ['rgpNumero', 'RGP', true],
    ['senhaGov', 'Senha gov', false, null, 'senha'],
  ]],
  ['Dados do afiliado', [
    ['inscricaoNumero', 'Numeração da inscrição'],
    ['dataAfiliacao', 'Data da afiliação', false, 'dd/mm/aaaa'],
  ]],
  ['Auxiliar jurídico', 'auxiliar'],
  ['Endereço do associado', [
    ['cep', 'CEP', false, '00000-000'],
    ['bairro', 'Bairro'],
    ['numero', 'Número'],
    ['uf', 'Estado', false, 'UF, ex.: MA'],
    ['complemento', 'Complemento'],
    ['municipio', 'Cidade', true],
    ['logradouro', 'Logradouro'],
  ]],
  ['Contato', [
    ['telefone', 'Número do telefone', true],
  ]],
  ['Dados do pedido', [
    ['nit', 'NIT/PIS'],
    ['colonia', 'Colônia'],
    ['rgpDesde', 'RGP registrado desde (mês/ano)', false, 'mm/aaaa'],
    ['rgpValidade', 'Validade do RGP', false, 'dd/mm/aaaa'],
    ['especie', 'Espécie / modalidade de pesca'],
    ['procuracaoValidade', 'Validade da procuração e-CAC', false, 'dd/mm/aaaa'],
    ['govNivel', 'Nível da conta gov.br', false, null, NIVEIS_GOV],
    ['honorariosPct', 'Honorários (% por parcela; vazio = padrão)'],
  ]],
  ['Campo de observação', 'observacao'],
]
const CHAVES = SECOES.flatMap(([, campos]) => (Array.isArray(campos) ? campos.map(([k]) => k) : []))
const DATAS = ['nascimento', 'rgpValidade', 'procuracaoValidade', 'dataExpedicao', 'dataAfiliacao']

function Secao({ titulo, children }) {
  return (
    <fieldset className="flex flex-col gap-3 border-t border-slate-200 pt-4 first:border-0 first:pt-0">
      <legend className="mb-1 text-sm font-bold text-marca-900">{titulo}</legend>
      <div className="grid grid-cols-[repeat(auto-fit,minmax(230px,1fr))] gap-x-4 gap-y-3.5">{children}</div>
    </fieldset>
  )
}

export default function ClienteForm() {
  const { id } = useParams()
  const novo = !id
  const local = useLocation()
  const respostas = local.state?.respostas
  const navegar = useNavigate()
  const { executar, ocupado } = useAcao()
  const [form, setForm] = useState(novo ? {} : null)
  const { dados: auxiliares } = useDados('/auxiliares')

  useEffect(() => {
    if (novo) return
    api(`/clientes/${id}`).then((f) => {
      const c = f.analise.cliente
      const dados = { ...c, ...c.pessoal }
      const v = {}
      CHAVES.forEach((k) => { v[k] = dados[k] ?? '' })
      DATAS.forEach((k) => { v[k] = br(dados[k]) })
      v.cpf = cpf(c.cpf)
      v.honorariosPct = c.honorariosPct != null ? String(c.honorariosPct).replace('.', ',') : ''
      v.observacoes = c.observacoes || ''
      v.auxiliarId = c.pessoal.auxiliarId ?? ''
      v.senhaGov = ''
      v.senhaCadastrada = c.pessoal.senhaGovCadastrada
      if (c.endereco) v.endereco = c.endereco
      setForm(v)
    })
  }, [id, novo])

  function mudar(k, v) {
    setForm((f) => ({ ...f, [k]: v }))
  }

  async function salvar(e) {
    e.preventDefault()
    const { senhaCadastrada, ...resto } = form
    const corpo = { ...resto, auxiliarId: form.auxiliarId ? Number(form.auxiliarId) : null, ...(novo ? { respostas } : {}) }
    await executar(
      () => (novo ? api('/clientes', { metodo: 'POST', corpo }) : api(`/clientes/${id}`, { metodo: 'PUT', corpo })),
      (r) => navegar(novo ? `/clientes/${r.id}?aba=documentos` : `/clientes/${id}`),
    )
  }

  if (novo && !respostas) return <Navigate to="/triagem" replace />
  if (!form) return <Carregando />

  function campo([k, rotulo, obrig, dica, tipo]) {
    let entrada
    if (tipo === 'senha') {
      entrada = (
        <input type="password" autoComplete="new-password" value={form[k] || ''}
          placeholder={form.senhaCadastrada ? 'Já cadastrada: deixe em branco para manter' : 'Guardada criptografada'}
          onChange={(e) => mudar(k, e.target.value)} />
      )
    } else if (tipo) {
      entrada = (
        <select value={form[k] || ''} onChange={(e) => mudar(k, e.target.value)}>
          <option value="">Não informado</option>
          {tipo.map((o) => {
            const [valor, nome] = Array.isArray(o) ? o : [o, o]
            return <option key={valor} value={valor}>{nome}</option>
          })}
        </select>
      )
    } else {
      entrada = <input type="text" value={form[k] || ''} required={obrig} placeholder={dica || undefined} onChange={(e) => mudar(k, e.target.value)} />
    }
    return <Campo key={k} rotulo={rotulo} obrigatorio={obrig}>{entrada}</Campo>
  }

  return (
    <>
      <Titulo titulo={novo ? 'Cadastro' : 'Editar cadastro'} sub="Campos com * são obrigatórios. Datas no formato dd/mm/aaaa." />
      {novo && local.state?.triagem && <Alerta cor="info">Triagem concluída: {local.state.triagem}. Agora complete o cadastro.</Alerta>}
      <Card>
        <form onSubmit={salvar} className="flex flex-col gap-5">
          {SECOES.map(([titulo, conteudo]) => (
            <Secao key={titulo} titulo={titulo}>
              {conteudo === 'auxiliar' && (
                <Campo rotulo="Auxiliar jurídico">
                  <select value={form.auxiliarId ?? ''} onChange={(e) => mudar('auxiliarId', e.target.value)}>
                    <option value="">Não definido</option>
                    {(auxiliares || []).filter((a) => a.ativo || a.id === Number(form.auxiliarId))
                      .map((a) => <option key={a.id} value={a.id}>{a.nome}</option>)}
                  </select>
                </Campo>
              )}
              {conteudo === 'observacao' && (
                <Campo rotulo="Observações" className="col-span-full">
                  <textarea rows={3} value={form.observacoes || ''} onChange={(e) => mudar('observacoes', e.target.value)} />
                </Campo>
              )}
              {Array.isArray(conteudo) && conteudo.map(campo)}
              {titulo === 'Endereço do associado' && form.endereco && (
                <Campo rotulo="Endereço anterior (importado)">
                  <input type="text" value={form.endereco} onChange={(e) => mudar('endereco', e.target.value)} />
                </Campo>
              )}
            </Secao>
          ))}
          <div className="flex flex-wrap gap-2">
            <Botao type="submit" disabled={ocupado}>{novo ? 'Cadastrar' : 'Salvar alterações'}</Botao>
            <BotaoLink to={novo ? '/clientes' : `/clientes/${id}`} variante="neutro">Cancelar</BotaoLink>
          </div>
        </form>
      </Card>
    </>
  )
}
