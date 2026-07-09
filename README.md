# Agregador de Vagas (Gupy)

[![CI](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/ci.yml/badge.svg)](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/codeql.yml/badge.svg)](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/codeql.yml)

Aplicação web em Java/Spring Boot que coleta vagas de páginas de carreira que usam a plataforma Gupy, filtra por área/senioridade/região, e usa a IA da Claude pra dar uma nota de aderência de cada vaga contra o seu perfil — sob demanda, direto no navegador.

> Veja o [roadmap](docs/ROADMAP_V2.md) pra entender as fases já entregues e o que vem a seguir.

## Pré-requisitos

- Java 21 ou superior instalado.
- Conexão com internet.
- Uma chave de API da [Anthropic](https://console.anthropic.com/) (opcional pra rodar, necessária pra ver as notas de aderência).

## Como rodar

```
git clone https://github.com/Vinicius-Braga/JobsSearch.git
cd JobsSearch
.\gradlew.bat bootRun
```

Acesse **http://localhost:8080** e faça login com as credenciais configuradas no `.env` (veja abaixo).

## Configuração

Arquivos de texto na raiz do projeto controlam o comportamento. Edite-os e reinicie o `bootRun` pra aplicar.

### `.env` — credenciais

```
APP_USER_USERNAME=admin
APP_USER_PASSWORD=escolha_uma_senha
ANTHROPIC_API_KEY=sua_chave_aqui
```

- **`APP_USER_USERNAME`/`APP_USER_PASSWORD`**: login da aplicação (usuário único por enquanto — multi-usuário vem na Fase 4 do roadmap). Se não definidos, caem no padrão `admin`/`changeme`.
- **`ANTHROPIC_API_KEY`**: sem ela, a busca ainda roda e mostra quantas vagas bateram no filtro, mas nenhuma é pontuada pela IA (fica visível um aviso na tela).

**Esse arquivo contém credenciais — nunca commite ele.** Já está no `.gitignore`.

### `empresas.txt` — quais empresas acompanhar

Uma empresa por linha, no formato:

```
subdominio,Nome que aparece na tela
```

O "subdomínio" é a parte antes de `.gupy.io` na URL da página de carreiras da empresa (ex: `https://vivo.gupy.io/` → subdomínio é `vivo`).

### `filtro.txt` — pré-filtro por palavra-chave

Roda antes da IA, pra reduzir o volume de vagas que precisam ser avaliadas:

```
area=RH
senioridade=junior,pleno,senior,auxiliar,assistente,estagio
regiao=RS,Porto Alegre
```

- **area**: RH, TI, Comercial, Financeiro, Marketing, Logistica, Juridico, Atendimento, Engenharia, Outro
- **senioridade**: Estagio, Auxiliar, Assistente, Junior, Pleno, Senior, Nao especificado
- **regiao**: sigla do estado (ex: `RS`) ou nome/parte do nome da cidade (ex: `Porto Alegre`)

Deixe uma linha vazia, apagada ou comentada com `#` pra não filtrar por aquele campo (traz todas as opções). Os valores são combinados com "E" — uma vaga só passa pro pré-filtro se bater em todos os campos preenchidos.

### Perfil (dentro do app)

O perfil de busca — o que a IA usa pra dar a nota de aderência — é editado direto na tela, em texto livre, depois de fazer login. Fica salvo num banco H2 local (`data/jobsearch.mv.db`, também no `.gitignore`).

## Como usar

1. Rode `.\gradlew.bat bootRun` e acesse http://localhost:8080.
2. Faça login com as credenciais do `.env`.
3. Clique em **Editar perfil** e descreva o que você procura (área, senioridade, região, tipo de empresa).
4. Clique em **Buscar vagas** — a busca roda nas empresas do `empresas.txt`, aplica o `filtro.txt`, e pontua cada vaga pré-filtrada com a IA (até 40 por busca, por custo).
5. A lista aparece ordenada por nota, com link direto pra aplicar.

## Estrutura do código

```
src/main/java/com/jobs/
 ├─ domain/            modelos e regras de negócio puras (Job, Company, Classifier, JobFilter, UserProfile, FitScore)
 ├─ application/       casos de uso (SearchJobsUseCase, SearchAndScoreJobsUseCase) + interfaces em application/port
 └─ infrastructure/
     ├─ web/           controllers, segurança (Spring Security), persistência de perfil (JPA/H2) — a aplicação Spring
     ├─ ai/             AnthropicFitScorer (Claude Haiku) + FitScorerPlaygroundApplication (teste manual isolado)
     ├─ gupy/           coleta de vagas na Gupy
     └─ config/         classes @ConfigurationProperties tipadas (AnthropicProperties, AppUserProperties)
```

Trocar de fonte de vagas ou de motor de IA significa implementar a interface correspondente em `application/port` — sem tocar nos casos de uso.

## Comandos úteis

| Comando | O que faz |
|---|---|
| `.\gradlew.bat bootRun` | Sobe a aplicação web em http://localhost:8080 |
| `.\gradlew.bat test` | Roda a suíte de testes |
| `.\gradlew.bat fitScorerPlayground` | Testa o `FitScorer` isoladamente contra 4 vagas de exemplo (requer `ANTHROPIC_API_KEY`) |
