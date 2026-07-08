# Roadmap V2 — de script local a SaaS

A V1 (script Java que roda no seu computador, em loop, grátis) não vai mais ser usada — o esforço daqui pra frente é todo na V2: um app web com assinatura, onde a pessoa edita um perfil em texto livre, clica em "buscar vagas" sob demanda, e recebe uma lista com nota de aderência (IA) por vaga.

Decisões já tomadas:
- **Stack**: Java + Spring Boot. Reaproveita `GupyJobSource`, `JobFilter`, `Classifier` como estão — só entra uma camada web por cima.
- **Hospedagem**: em aberto, decidida na Fase 3/4. A API é desenhada de forma agnóstica de provedor até lá.
- **Modelo de IA**: Claude Haiku 4.5, pelo custo (~$0,06 por busca de ~40 vagas pré-filtradas).

## Fase 1 — Motor como serviço

Objetivo: provar que o motor da V1 funciona sob demanda (uma chamada HTTP), não só em loop de 6h. Sem tela ainda.

- [x] Adicionar `spring-boot-starter-web` ao `build.gradle`
- [x] Criar `SearchJobsUseCase` (novo, em `application/`) — recebe lista de empresas + `JobFilter`, retorna lista de `ClassifiedJob` sincronamente. Não reutiliza `RunCycleUseCase` diretamente porque esse é do modelo de loop/notificação; o novo caso de uso é "buscar agora e devolver".
- [x] Criar `JobSearchController` em `infrastructure/web/` — `POST /api/search` recebendo `{ empresas: [...], filtro: {...} }`, devolvendo JSON com as vagas encontradas
- [x] Configurar Jackson (vem com Spring Boot) para serializar `Job`/`ClassifiedJob`
- [x] Testes de integração do endpoint com pelo menos uma empresa real (ou mock do `JobSource`)
- [x] Manter `Main.java` (modo CLI/loop) funcionando em paralelo — não quebrar o que já existe, mesmo que não seja mais usado, até termos certeza que a API cobre o caso de uso

**Critério de pronto:** dar `curl -X POST localhost:8080/api/search` com um filtro e uma empresa, e receber vagas de volta em JSON. ✅ Testado manualmente com a Vivo — retornou vaga real de RH.

> Rodar a API: `./gradlew.bat bootRun`. CLI antigo continua com `./gradlew.bat run`.

## Fase 2 — FitScorer (IA) + perfil

Objetivo: dado um perfil em texto livre e uma vaga, obter nota 1-10 + justificativa. Testado isolado, fora do fluxo real.

- [ ] Criar `UserProfile` (domain) — só um texto livre por enquanto, sem persistência ainda
- [ ] Criar port `application/port/FitScorer` — `score(UserProfile profile, ClassifiedJob job) -> FitScore { nota, justificativa }`
- [ ] Implementar `AnthropicFitScorer` em `infrastructure/ai/` usando a API da Claude (Haiku 4.5)
- [ ] Config de API key seguindo o padrão do `telegram.txt` (arquivo local, no `.gitignore`, nunca commitado)
- [ ] Decidir: chamada por vaga ou batch de vagas numa única chamada (batch reduz custo de overhead de prompt, mas complica parsing da resposta)
- [ ] Teste isolado: perfil fixo + 3-4 vagas variadas, validar que a nota faz sentido manualmente antes de confiar no output

**Critério de pronto:** dado um perfil e uma lista de vagas já filtradas, o `FitScorer` retorna nota + justificativa pra cada uma, sem estourar custo.

## Fase 3 — Tela mínima + 1 usuária de teste

Objetivo: sua namorada consegue usar o app de ponta a ponta, sem cobrança.

- [ ] Escolher entre Thymeleaf (server-rendered, mais simples de integrar no Spring Boot) ou uma SPA separada — recomendo Thymeleaf pro MVP de 1 usuária, pra não abrir uma segunda stack de frontend agora
- [ ] Login simples (pode ser usuário único fixo no início — não precisa de Spring Security completo ainda)
- [ ] Persistência de perfil: banco leve (H2 ou SQLite) — trocar por Postgres só quando for multi-tenant de verdade (Fase 4)
- [ ] Tela: editar perfil → botão "buscar vagas" → lista de resultados com nota + link de aplicar
- [ ] Ligar Fase 1 (busca) + Fase 2 (score) nesse fluxo real, ponta a ponta

**Critério de pronto:** sua namorada edita o perfil dela, clica em buscar, e vê uma lista de vagas com nota, sem você rodar nada manualmente.

## Fase 4 — Assinatura

Objetivo: abrir pra mais gente além da usuária de teste, com cobrança.

- [ ] Auth multi-usuário de verdade (Spring Security + cadastro/login)
- [ ] Migrar de H2/SQLite pra Postgres, com isolamento de dados por usuário (multi-tenant)
- [ ] Integração com Stripe (assinatura recorrente)
- [ ] Limite de teste grátis (ex: 3 buscas sem cartão)
- [ ] Decidir hospedagem (Railway/Render/VPS/AWS) e colocar no ar

## Backlog (pós-V2, sem prioridade definida)

- Registro de candidatura (aplicou / chamou / rejeitado) por vaga
- Marcar vagas expiradas automaticamente (link não retorna mais 200)
- **Segunda fonte de vagas: LinkedIn**, seguindo a técnica usada pelo repo [MadsLorentzen/ai-job-search](https://github.com/MadsLorentzen/ai-job-search):
  - Novo `JobSource` (ex: `LinkedInJobSource`), no mesmo padrão do `GupyJobSource` — implementa a interface existente, sem tocar no resto do pipeline
  - Bate no endpoint público não-autenticado `https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search`, com query params: `keywords`, `location`, `f_TPR` (idade da vaga em segundos, formato `r<segundos>`), `f_WT` (tipo: `1`=presencial, `2`=remoto, `3`=híbrido), `start` (paginação, offset de 10 em 10)
  - Resposta é HTML bruto, não JSON — precisa de parsing manual dos cards (`data-entity-urn="urn:li:jobPosting:..."`, classes `base-search-card__title`, `job-search-card__location` etc.), diferente da Gupy que devolve JSON estruturado
  - Precisa de retry com backoff exponencial (o endpoint tem rate limiting agressivo — 429 é esperado com uso frequente)
  - **Risco:** isso não é uma API oficial — é scraping de rota interna do LinkedIn, contra os Termos de Serviço deles. Mais frágil que a Gupy (quebra se o LinkedIn mudar o HTML) e com risco de bloqueio de IP em uso alto. Manter volume baixo se for implementado.
- Resumo diário (digest) além de notificações imediatas — se ainda fizer sentido sem o modelo de notificação da V1

## Ordem de trabalho

As fases são sequenciais por dependência técnica (2 depende de 1 pra ter vagas pra pontuar; 3 depende de 1+2 pra ter algo pra mostrar; 4 depende de 3 funcionando de ponta a ponta com uma usuária real). Não pular fase, mas dentro de cada fase os itens de checklist podem ser feitos fora de ordem quando fizer sentido.
