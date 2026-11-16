const CAMINHOS = {
  inicio: 'M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10',
  cadastro: 'M12 12a4 4 0 100-8 4 4 0 000 8zM4 21a8 8 0 0116 0',
  dae: 'M7 3h8l4 4v14H7zM14 3v5h5M10 13h6M10 17h6',
  prazos: 'M5 5h14v15H5zM5 10h14M9 3v4M15 3v4',
  entrevistas: 'M12 21a9 9 0 100-18 9 9 0 000 18zM8 12l3 3 5-6',
  requerimentos: 'M9 4h6v3H9zM7 5H5v16h14V5h-2M9 12h6M9 16h6',
  mensagens: 'M4 5h16v11H9l-5 4z',
  honorarios: 'M12 3v18M16 7.5c0-1.5-1.8-2.5-4-2.5s-4 1-4 2.5 1.8 2.3 4 2.5 4 1 4 2.5-1.8 2.5-4 2.5-4-1-4-2.5',
  relatorios: 'M4 20V10M10 20V4M16 20v-8M22 20H2',
  planilhas: 'M3 5h18v14H3zM3 10h18M3 15h18M9 5v14',
  auxiliares: 'M4 8h16v11H4zM9 8V5h6v3M4 13h16',
  defesos: 'M3 9c3-3 6 3 9 0s6 3 9 0M3 15c3-3 6 3 9 0s6 3 9 0',
  perfil: 'M12 12a4 4 0 100-8 4 4 0 000 8zM4 21a8 8 0 0116 0',
  usuarios: 'M9 11a3.5 3.5 0 100-7 3.5 3.5 0 000 7zM2 20a7 7 0 0114 0M16 4.5a3.5 3.5 0 010 6.5M18 14a7 7 0 013 6',
  parametros: 'M12 15a3 3 0 100-6 3 3 0 000 6zM12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1',
  atividades: 'M12 21a9 9 0 100-18 9 9 0 000 18zM12 7v5l3 2',
  menu: 'M4 7h16M4 12h16M4 17h16',
  fechar: 'M6 6l12 12M18 6L6 18',
}

/** Ícone de traço simples (24x24). Decorativo: o texto ao lado já descreve o item. */
export default function Icone({ nome, tamanho = 18, className = '' }) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className={`shrink-0 ${className}`}>
      <path d={CAMINHOS[nome]} />
    </svg>
  )
}
