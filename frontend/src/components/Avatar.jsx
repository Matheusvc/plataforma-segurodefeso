/** Foto do usuário logado ou, sem foto, as iniciais do nome. */
export default function Avatar({ usuario, tamanho = 40 }) {
  const iniciais = (usuario.nome || '?').split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('')
  const estilo = { width: tamanho, height: tamanho, fontSize: tamanho * 0.4 }
  if (usuario.foto) {
    return <img src={`/api/auth/foto?v=${usuario.foto}`} alt={`Foto de ${usuario.nome}`} style={estilo}
      className="shrink-0 rounded-full object-cover" />
  }
  return (
    <span aria-hidden="true" style={estilo}
      className="flex shrink-0 items-center justify-center rounded-full bg-marca-700 font-bold text-white">{iniciais}</span>
  )
}
