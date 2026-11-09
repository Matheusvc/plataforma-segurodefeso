import { useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Botao, Campo, Card, Item, Titulo, useAcao, useDados } from '../components/ui.jsx'
import { br } from '../util.js'

const VAZIO = { especie: '', regiao: '', portaria: '', inicio: '', fim: '', prazoRequerimento: '' }

export default function Defesos() {
  const { gestor } = useAuth()
  const { dados, recarregar } = useDados('/defesos')
  const { executar, ocupado } = useAcao()
  const [f, setF] = useState(VAZIO)

  async function salvar(e) {
    e.preventDefault()
    await executar(() => api('/defesos', { metodo: 'POST', corpo: f }), () => { setF(VAZIO); recarregar() })
  }

  async function excluir(id) {
    if (!window.confirm('Excluir este defeso?')) return
    await executar(() => api(`/defesos/${id}`, { metodo: 'DELETE' }), recarregar)
  }

  const campo = (k, rotulo, obrig, dica) => (
    <Campo rotulo={rotulo} obrigatorio={obrig}><input type="text" required={obrig} placeholder={dica} value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} /></Campo>
  )

  return (
    <>
      <Titulo titulo="Defesos" sub="Cadastre as datas de cada portaria. O início e o prazo final para requerer entram automaticamente na tela Prazos." />
      <div className="flex flex-wrap items-start gap-4">
        <Card className="min-w-0 flex-[3_1_520px]">
          <form onSubmit={salvar} className="flex flex-col gap-4">
            <h2 className="text-base font-semibold">Cadastrar defeso (conforme a portaria)</h2>
            <div className="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-3.5">
              {campo('especie', 'Espécie', true)}
              {campo('regiao', 'Municípios / região abrangidos')}
              {campo('portaria', 'Nº da portaria')}
              {campo('inicio', 'Início do defeso', true, 'dd/mm/aaaa')}
              {campo('fim', 'Fim do defeso', false, 'dd/mm/aaaa')}
              {campo('prazoRequerimento', 'Prazo final para requerer', true, 'dd/mm/aaaa')}
            </div>
            <div><Botao type="submit" disabled={ocupado}>Salvar defeso</Botao></div>
          </form>
        </Card>
        <Card className="flex min-w-0 flex-[2_1_320px] flex-col gap-2">
          <h2 className="text-base font-semibold">Cadastrados</h2>
          {(dados || []).map((d) => (
            <Item key={d.id}>
              <div className="min-w-0 flex-1">
                <div className="font-semibold">{d.especie}{d.portaria && ` · Portaria ${d.portaria}`}</div>
                <div className="text-[13px] text-slate-600">Defeso de {br(d.inicio)} a {br(d.fim) || '?'}. Requerer até {br(d.prazoRequerimento)}. {d.regiao || ''}</div>
              </div>
              {gestor && <Botao variante="perigo" pequeno onClick={() => excluir(d.id)}>Excluir</Botao>}
            </Item>
          ))}
          {dados && dados.length === 0 && <p className="text-sm text-slate-500">Nenhum defeso cadastrado.</p>}
        </Card>
      </div>
    </>
  )
}
