# Plataforma Seguro-Defeso — Java 21 · Spring Boot · React · Tailwind

Sistema web do escritório para acompanhar os clientes do seguro-defeso: cadastro, triagem de requisitos,
documentos, DAE, pedido e status no Emprega Brasil, prazos, fila do dia, mensagens de WhatsApp, honorários,
relatórios e planilhas.

| Camada | Tecnologia |
|---|---|
| Back-end | Java 21, Spring Boot 3.5 (Web, Data JPA, Security, Validation), Flyway, Apache POI |
| Banco | PostgreSQL em produção · H2 em arquivo para desenvolvimento (sem instalar nada) |
| Front-end | React 19, React Router 7, Tailwind CSS 4, Vite 6 |
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
- **Maven 3.9+** — `mvn -v` (ou use o IntelliJ/Eclipse, que já trazem)
- **Node.js 20+** — `node -v`

## 2. Rodar em desenvolvimento

Dois terminais:

```bash
# Terminal 1 — back-end (porta 8080), com banco H2 em ./data e 10 clientes fictícios para treino
cd backend
# Linux/Mac:
ADMIN_EMAIL=voce@escritorio.com ADMIN_SENHA=SuaSenhaForte1 DADOS_EXEMPLO=true mvn spring-boot:run
# Windows (PowerShell):
$env:ADMIN_EMAIL="voce@escritorio.com"; $env:ADMIN_SENHA="SuaSenhaForte1"; $env:DADOS_EXEMPLO="true"; mvn spring-boot:run

# Terminal 2 — front-end (porta 5173, repassa /api para o 8080)
cd frontend
npm install
npm run dev
```

Abra **http://localhost:5173** e entre com o e-mail e a senha definidos acima.

- O administrador só é criado na **primeira** execução (quando não há usuários). Depois, os demais usuários
  são criados pela tela **Usuários**.
- Para começar do zero, apague a pasta `backend/data`.
- **Nunca** use `DADOS_EXEMPLO=true` com dados reais.

## 3. Gerar a versão de produção (um único arquivo .jar)

```bash
cd frontend && npm install && npm run build     # gera o React dentro de backend/src/main/resources/static
cd ../backend && mvn clean package              # roda os testes e gera target/seguro-defeso-1.0.0.jar
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

```bash
cd backend && mvn test
```

- `RegrasTest` — regras de negócio (CPF, datas, DAE, bloqueios, honorários, prazos, fila, mensagens, triagem).
- `FluxoApiTest` — fluxo pela API com banco H2 em memória (login, cadastro, status, prazos, honorários, permissões, Excel).

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
- **A senha gov.br e o código de SMS dos clientes nunca são guardados.**

## 9. Observações jurídicas

Requisitos com base na Lei 10.779/2003, com a redação da Lei 13.134/2015. O vencimento do DAE é estimado
(dia configurável do mês seguinte): o oficial é o da guia. O prazo recursal não é calculado automaticamente:
conferir na decisão. As datas do defeso dependem da portaria de cada espécie e região.

## 10. Próximas versões

- Robô do DAE (emissão no eSocial com o certificado do escritório).
- Leitura automática de documentos (OCR).
- Envio automático de WhatsApp pela API oficial.
