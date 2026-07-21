# JobSearch

[![CI](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/ci.yml/badge.svg)](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/codeql.yml/badge.svg)](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/codeql.yml)

Aplicação web em Java/Spring Boot que coleta vagas de páginas de carreira que usam a plataforma Gupy e do LinkedIn, e usa a IA da Claude pra entender seu perfil de busca (área, senioridade, região, remoto ou não, stack) — sob demanda, direto no navegador.

> Projeto de portfólio, pensado pra rodar localmente. Veja o [roadmap](docs/ROADMAP_V2.md) pra entender as fases já entregues e o que vem a seguir.

## Pré-requisitos

- Java 21 ou superior instalado.
- Conexão com internet.
- Um banco Postgres (ex: [Supabase](https://supabase.com/), tem plano grátis) — a URL de conexão vai no `.env`.
- Uma chave de API da [Anthropic](https://console.anthropic.com/) (opcional pra rodar, necessária pra ver as notas de aderência).

## Como rodar

Configure o `.env` primeiro (veja a seção **Configuração** abaixo), depois escolha uma das duas formas:

### Direto com Gradle

```
git clone https://github.com/Vinicius-Braga/JobsSearch.git
cd JobsSearch
.\gradlew.bat bootRun
```

### Com Docker

```
docker compose up --build
```

Sobe a aplicação inteira num container (só a aplicação — o banco é o Postgres externo configurado no `.env`, ex: Supabase). `empresas.txt` é montado como volume, então dá pra editá-lo sem rebuildar a imagem.

Pra rodar 100% offline, sem depender de um Postgres externo, use o profile `local`, que sobe um Postgres junto:

```
docker compose --profile local up --build
```

Nesse caso, aponte o `.env` pro serviço `db` do compose em vez do Supabase:

```
DATABASE_URL=jdbc:postgresql://db:5432/jobsearch
DATABASE_USERNAME=jobsearch
DATABASE_PASSWORD=jobsearch
```

Em ambos os casos, acesse **http://localhost:8080**, clique em **Criar uma conta** pra se cadastrar, e faça login.

## Configuração

Arquivos de texto na raiz do projeto controlam o comportamento. Edite-os e reinicie o `bootRun` pra aplicar.

### `.env` — credenciais

```
DATABASE_URL=jdbc:postgresql://SEU_HOST:5432/postgres?sslmode=require
DATABASE_USERNAME=seu_usuario
DATABASE_PASSWORD=sua_senha
ANTHROPIC_API_KEY=sua_chave_aqui
ANTHROPIC_MODEL=claude-haiku-4-5-20251001
COMPANIES_FILE=empresas.txt
```

- **`DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`**: conexão com o Postgres — é onde ficam as contas de usuário e os perfis de busca. Sem isso o app não sobe.
- **`ANTHROPIC_API_KEY`**: sem ela, a busca ainda roda e mostra quantas vagas bateram no filtro, mas nenhuma é pontuada pela IA (fica visível um aviso na tela).
- **`ANTHROPIC_MODEL`**: opcional — modelo da Claude usado pra extrair critérios do perfil e (quando religada) pontuar vagas. Se omitido, usa `claude-haiku-4-5-20251001`.
- **`COMPANIES_FILE`**: opcional — caminho do arquivo de empresas monitoradas (veja a seção **`empresas.txt`** abaixo). Se omitido, usa `empresas.txt` na raiz do projeto.

**Esse arquivo contém credenciais — nunca commite ele.** Já está no `.gitignore`.

### `empresas.txt` — quais empresas acompanhar

Uma empresa por linha, no formato:

```
subdominio,Nome que aparece na tela
```

O "subdomínio" é a parte antes de `.gupy.io` na URL da página de carreiras da empresa (ex: `https://vivo.gupy.io/` → subdomínio é `vivo`).

Vem com 50 empresas reais que usam Gupy, verificadas uma a uma (cada linha testada com uma chamada HTTP real antes de entrar na lista). Quanto mais empresas, mais tempo a busca leva (a coleta é sequencial, uma empresa por vez) — com as 50 atuais, uma busca leva cerca de 1 minuto.

### Perfil (dentro do app)

O perfil de busca é editado direto na tela, em texto livre, depois de fazer login (ex: *"Desenvolvedor Backend Jr, especialidade Java, quero vagas remotas, junior ou pleno"*). Fica salvo no Postgres, isolado por usuário (cada conta só vê o próprio perfil e histórico).

A cada busca, a IA lê esse texto e decide sozinha os critérios de pré-filtro (área, senioridade, região, remoto ou não) — não existe mais um arquivo de configuração de filtro separado do perfil. Esse pré-filtro reduz o volume de vagas antes da etapa mais cara (pontuar cada uma individualmente com IA).

### Schema do banco (Flyway)

O schema (`account`, `user_profile`) é versionado em `src/main/resources/db/migration/`, aplicado automaticamente pelo Flyway toda vez que o app sobe — não precisa rodar nada manualmente. Pra mudar o schema, adicione um novo arquivo `V3__descricao.sql` (não edite as migrations já aplicadas).

## Como usar

1. Rode `.\gradlew.bat bootRun` (ou `docker compose up --build`) e acesse http://localhost:8080.
2. Clique em **Criar uma conta** (usuário + senha) e depois faça login.
3. Clique em **Editar perfil** e descreva em texto livre o que você procura.
4. Na tela inicial, o botão de busca fica colorido/ativo assim que há um perfil salvo. Clique nele — a busca roda em background (com uma animação de radar), sem recarregar a página: a IA extrai os critérios do seu perfil (área, senioridade, região, remoto ou não) e filtra as vagas das empresas do `empresas.txt`.
5. A lista aparece com as vagas encontradas (título, empresa, local, modalidade, área, senioridade) e link direto pra aplicar.

> A pontuação de aderência por vaga (nota + justificativa via IA) existe no código (`FitScorer`/`AnthropicFitScorer`), mas está desligada do fluxo principal por enquanto — ver o [roadmap](docs/ROADMAP_V2.md).

## Estrutura do código

```
src/main/java/com/jobs/
 ├─ domain/            modelos e regras de negócio puras (Job, Company, Classifier, JobFilter, UserProfile, FitScore)
 ├─ application/       casos de uso (SearchJobsUseCase, SearchAndScoreJobsUseCase) + interfaces em application/port
 └─ infrastructure/
     ├─ web/           controllers, segurança (Spring Security, multi-usuário), persistência (JPA/Postgres) — a aplicação Spring
     ├─ ai/             AnthropicFitScorer + AnthropicSearchCriteriaExtractor (Claude Haiku) + FitScorerPlaygroundApplication (teste manual isolado)
     ├─ gupy/           coleta de vagas na Gupy
     ├─ linkedin/       coleta de vagas no LinkedIn (endpoint público não-oficial, scraping de HTML)
     └─ config/         classes @ConfigurationProperties tipadas (AnthropicProperties)
```

Trocar de fonte de vagas ou de motor de IA significa implementar a interface correspondente em `application/port` — sem tocar nos casos de uso.

## Comandos úteis

| Comando | O que faz |
|---|---|
| `.\gradlew.bat bootRun` | Sobe a aplicação web em http://localhost:8080 |
| `.\gradlew.bat test` | Roda a suíte de testes |
| `.\gradlew.bat fitScorerPlayground` | Testa o `FitScorer` isoladamente contra 4 vagas de exemplo (requer `ANTHROPIC_API_KEY`) |
