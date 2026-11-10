# Plataforma Seguro-Defeso — Java 21 · Spring Boot · React · Tailwind

Sistema web do escritório para acompanhar os clientes do seguro-defeso: cadastro, triagem de requisitos,
documentos, DAE, pedido e status no Emprega Brasil, prazos, fila do dia, mensagens de WhatsApp, honorários,
relatórios e planilhas.

| Camada | Tecnologia |
|---|---|
| Back-end | Java 21, Spring Boot 3.5 (Web, Data JPA, Security, Validation), Flyway, Apache POI |
| Banco | PostgreSQL em produção · H2 em arquivo para desenvolvimento (sem instalar nada) |
| Front-end | React 19, React Router 7, Tailwind CSS 3.4, Vite 6 |
| Segurança | Sessão com cookie HttpOnly, CSRF (cookie XSRF-TOKEN), BCrypt, bloqueio após 5 tentativas, papéis |

```
seguro-defeso/
├── backend/                     Spring Boot (API REST em /api)
│   ├── pom.xml
│   └── src/main/java/br/com/segurodefeso/
│       ├── dominio/             REGRAS DE NEGÓCIO em Java puro (requisitos, prazos, fila do dia, DAE, honorários, mensagens)
│       ├── entidade/            tabelas (JPA)
│       ├── repositorio/         consultas (Spring Data)
│       ├── servico/             casos de uso: clientes, planilhas, configurações, arquivos, login
│       ├── web/                 controllers REST
│       └── config/              segurança, CSRF, servir o React, data de hoje, inicialização
│   └── src/main/resources/
│       ├── application.yml      configuração
│       └── db/migration/        criação das tabelas (Flyway)
└── frontend/                    React + Tailwind
    └── src/
        ├── pages/               uma tela por arquivo
        ├── components/          Layout e componentes visuais
        ├── api.js               chamadas à API (com CSRF)
        └── auth.jsx             login e dados fixos
```

---

## 1. Pré-requisitos

- **Java 21** (JDK) — `java -version`
- **Node.js 20+** — `node -v`
- Maven **não precisa instalar**: o projeto inclui o Maven Wrapper (`mvnw.cmd`).

## 2. Rodar em desenvolvimento

### Opção A — IntelliJ IDEA (recomendado no Windows)

1. Abra a pasta `backend` como projeto no IntelliJ.
2. O IntelliJ detecta o `pom.xml` e baixa as dependências automaticamente.
3. Rode a classe `SeguroDefesoApplication` (clique com botão direito → Run).
4. Em outro terminal, suba o front-end:
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
5. Abra **http://localhost:5173**.

### Opção B — Linha de comando (PowerShell)

```powershell
# Terminal 1 — back-end (porta 8080)
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 2 — front-end (porta 5173, repassa /api para o 8080)
cd frontend
npm install
npm run dev
```

### Login de desenvolvimento

Com os valores padrão do `application.yml`, o sistema já cria um administrador:

| Campo | Valor |
|---|---|
| E-mail | `admin@escritorio.local` |
| Senha | `Defeso2026adm` |

O banco começa vazio. Para treinar a equipe com 10 clientes fictícios, defina `DADOS_EXEMPLO=true`
antes de iniciar (nunca com dados reais).

Para usar credenciais diferentes, defina variáveis de ambiente antes de rodar:

```powershell
$env:ADMIN_EMAIL="voce@escritorio.com"
$env:ADMIN_SENHA="SuaSenhaForte1"
.\mvnw.cmd spring-boot:run
```

- O administrador só é criado na **primeira** execução (quando não há usuários). Depois, os demais usuários
  são criados pela tela **Usuários**.
- Para começar do zero, apague a pasta `backend/data`.
- **Nunca** use `DADOS_EXEMPLO=true` com dados reais.

## 3. Gerar a versão de produção (um único arquivo .jar)

```powershell
cd frontend; npm install; npm run build          # gera o React dentro de backend/src/main/resources/static
cd ..\backend; .\mvnw.cmd clean package          # roda os testes e gera target/seguro-defeso-1.0.0.jar
```

O `.jar` contém tudo (API + telas). Para rodar:

```bash
java -jar seguro-defeso-1.0.0.jar
```

### Variáveis de ambiente de produção

| Variável | Exemplo | Para quê |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/seguro_defeso` | banco PostgreSQL |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `seguro` / `***` | credenciais do banco |
| `UPLOAD_FOLDER` | `/var/seguro_defeso/uploads` | onde ficam os documentos anexados |
| `COOKIE_SEGURO` | `true` | obrigatório atrás de HTTPS |
| `APP_CRIPTO_CHAVE` | frase longa e secreta | criptografa a senha gov dos clientes (obrigatória em produção; não troque depois) |
| `ADMIN_EMAIL` / `ADMIN_SENHA` | — | só na primeira execução; remova depois |
| `PORT` | `8080` | porta do servidor |

### Servidor Linux (exemplo com Ubuntu)

```bash
sudo apt install openjdk-21-jre postgresql nginx
sudo -u postgres createuser -P seguro
sudo -u postgres createdb -O seguro seguro_defeso
```

Rode o `.jar` como serviço do **systemd**, coloque o **nginx** na frente (proxy para `127.0.0.1:8080`,
`client_max_body_size 16M;`) e ative HTTPS com **certbot** (`sudo certbot --nginx`).
As tabelas são criadas automaticamente pelo Flyway na primeira execução.

## 4. Backup

- Banco: `pg_dump -Fc seguro_defeso > seguro_defeso_$(date +%F).dump` (diário, guardado fora do servidor)
- Documentos: copie a pasta `UPLOAD_FOLDER`

## 5. Testes

```powershell
cd backend
.\mvnw.cmd test
```

- `RegrasTest` — regras de negócio (CPF, datas, DAE, bloqueios, honorários, prazos, fila, mensagens, triagem).
- `FluxoApiTest` — fluxo pela API com banco H2 em memória (login, cadastro, status, prazos, honorários, permissões, Excel).
- `EntrevistaDaeApiTest` — cadastro completo, entrevista, trava do DAE, senha gov protegida e auxiliares.

## Fluxo de trabalho

1. **Triagem** (Clientes → Novo cliente): responde os requisitos; só depois abre o cadastro completo.
2. **Entrevistas**: marque cada cliente como Não, Confirmado ou A confirmar.
3. **DAE**: lançamento manual desde maio de 2026, só para clientes com entrevista confirmada (status Declarado ou Pago;
   competência sem lançamento conta como em aberto). O DAE não aparece em Prazos.

## 6. Onde mexer

| Quero mudar… | Arquivo |
|---|---|
| Um requisito da triagem | `backend/.../dominio/Requisito.java` |
| Regras de bloqueio, DAE, honorários | `backend/.../dominio/Analisador.java` |
| Prazos e fila do dia | `dominio/Prazos.java`, `dominio/FilaDoDia.java` |
| Textos das mensagens de WhatsApp | `dominio/Mensagens.java` |
| Salário mínimo, alíquota, % de honorários | tela **Configurações** (não precisa programar) |
| Uma tabela nova | crie `db/migration/V2__descricao.sql` (nunca altere o V1) |
| Uma tela | `frontend/src/pages/` |
| Cores e fonte | `frontend/src/index.css` (`@theme`) |

## 7. Rotina de manutenção

| Quando | O quê |
|---|---|
| Todo janeiro | Atualizar o salário mínimo em Configurações (2026: R$ 1.621) |
| A cada portaria | Cadastrar as datas em Defesos |
| Mudança na lei | Conferir a alíquota do DAE do segurado especial (hoje 1,5%) e os requisitos |
| Sempre | Backup diário; desativar o usuário de quem sair do escritório |

## 8. Segurança e LGPD

- Senhas com BCrypt; mínimo de 10 caracteres com letras e números; bloqueio de 15 min após 5 erros.
- Sessão em cookie HttpOnly; proteção CSRF em todo POST/PUT/DELETE.
- Papéis: Administrador (tudo), Advogado (tudo menos usuários/configurações), Equipe (sem honorários, relatórios e exclusões).
- Documentos fora da pasta pública, só para usuários logados.
- Registro de atividades: quem fez o quê, quando, em cada cliente.
- A senha gov.br do cliente (opcional) é guardada **criptografada** (AES-GCM, chave em `APP_CRIPTO_CHAVE`), nunca aparece
  em listas, planilhas ou logs, e só Administrador/Advogado podem revelá-la; cada consulta fica na linha do tempo.
  Se a chave mudar, as senhas já salvas deixam de abrir: guarde a chave com o mesmo cuidado do backup do banco.
- O código de SMS (verificação em duas etapas) dos clientes nunca é guardado.

## 9. Observações jurídicas

Requisitos com base na Lei 10.779/2003, com a redação da Lei 13.134/2015. O vencimento do DAE é estimado
(dia configurável do mês seguinte): o oficial é o da guia. O prazo recursal não é calculado automaticamente:
conferir na decisão. As datas do defeso dependem da portaria de cada espécie e região.

## 10. Próximas versões

- Robô do DAE (emissão no eSocial com o certificado do escritório).
- Leitura automática de documentos (OCR).
- Envio automático de WhatsApp pela API oficial.
