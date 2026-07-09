# Agregador de Vagas (Gupy)

[![CI](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/ci.yml/badge.svg)](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/codeql.yml/badge.svg)](https://github.com/Vinicius-Braga/JobsSearch/actions/workflows/codeql.yml)

Ferramenta em Java que coleta vagas de páginas de carreira que usam a plataforma Gupy, de várias empresas ao mesmo tempo, e consolida tudo num só lugar — com deduplicação, filtro por área/senioridade/região, visualização em HTML e notificação no Telegram.

## Pré-requisitos

- Java 21 ou superior instalado.
- Conexão com internet.

## Como rodar

```
git clone https://github.com/Vinicius-Braga/JobsSearch.git
cd JobsSearch
.\gradlew.bat run
```

O programa fica rodando em loop, buscando vagas novas a cada 6 horas, até você fechar o terminal (`Ctrl+C`).

A cada ciclo, ele imprime no terminal quantas vagas encontrou por empresa e quantas são novas.

## Configuração

Três arquivos de texto simples, na raiz do projeto, controlam o comportamento. Edite-os e rode `.\gradlew.bat run` de novo pra aplicar.

### `empresas.txt` — quais empresas acompanhar

Uma empresa por linha, no formato:

```
subdominio,Nome que aparece na planilha
```

O "subdomínio" é a parte antes de `.gupy.io` na URL da página de carreiras da empresa (ex: `https://vivo.gupy.io/` → subdomínio é `vivo`).

### `filtro.txt` — o que você quer ver

```
area=RH
senioridade=junior,pleno,senior,auxiliar,assistente,estagio
regiao=RS,Porto Alegre
```

- **area**: RH, TI, Comercial, Financeiro, Marketing, Logistica, Juridico, Atendimento, Engenharia, Outro
- **senioridade**: Estagio, Auxiliar, Assistente, Junior, Pleno, Senior, Nao especificado
- **regiao**: sigla do estado (ex: `RS`) ou nome/parte do nome da cidade (ex: `Porto Alegre`)

Deixe uma linha vazia, apagada ou comentada com `#` pra não filtrar por aquele campo (traz todas as opções). Os valores são combinados com "E" — uma vaga só aparece se bater em todos os campos preenchidos.

### `.env` — credenciais (Telegram e, na V2, a API da Claude)

Se você quiser receber um aviso no Telegram toda vez que surgir uma vaga nova que bate no filtro:

1. No Telegram, fale com o **@BotFather** e mande `/newbot` pra criar um bot. Guarde o token que ele te der.
2. Crie um grupo (ou canal) e adicione o bot como membro (em canal, como administrador).
3. Mande uma mensagem qualquer no grupo/canal.
4. Crie o arquivo `.env` na raiz do projeto:
   ```
   TELEGRAM_TOKEN=SEU_TOKEN_AQUI
   ```
5. Rode o programa uma vez — ele descobre e salva o `TELEGRAM_CHAT_ID` automaticamente no próprio `.env`.

**Esse arquivo contém credenciais — nunca commite ele.** Já está no `.gitignore`.

Se `.env` não existir (ou não tiver `TELEGRAM_TOKEN`), o programa roda normalmente, só sem notificar.

## Arquivos gerados

Nenhum desses é versionado no Git (são dados, não código):

| Arquivo | O que é |
|---|---|
| `vagas.csv` | Histórico completo e acumulado de todas as vagas já vistas, sem filtro. Cresce a cada ciclo, nunca duplica. |
| `vagas_filtradas.csv` | Só as vagas que batem no `filtro.txt` atual, recalculado do zero a cada ciclo. |
| `vagas.html` | A mesma lista filtrada, numa página com busca e ordenação por coluna — abra com duplo clique no navegador. |

## Estrutura do código

```
src/main/java/com/jobs/
 ├─ domain/            modelos e regras de negócio puras (Job, Company, Classifier, JobFilter)
 ├─ application/       caso de uso (RunCycleUseCase) + interfaces (JobSource, JobRepository, JobPublisher, Notifier)
 └─ infrastructure/    implementações concretas: Gupy, CSV, HTML, Telegram
```

Trocar de fonte de vagas, formato de saída ou canal de notificação significa implementar a interface correspondente em `application/port` — sem tocar no `RunCycleUseCase`.
