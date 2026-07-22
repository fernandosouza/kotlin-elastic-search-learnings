# 🔍 Elastic Search Studies

Projeto mínimo e didático para aprender **Elasticsearch** integrado a um backend web em **Kotlin**.

---

## 🗺️ A ideia em 30 segundos

```
Navegador (fetch)          Backend Kotlin (Ktor)          Elasticsearch (Docker)
     :qualquer      ──────►      :8080           ──────►        :9200
   "barra de busca"         valida + monta query          motor de busca (REST/JSON)
```

- **Elasticsearch** = um servidor de busca. Você manda JSON via HTTP, ele devolve JSON. Só isso.
- **O backend** é o intermediário: o front nunca fala com o ES diretamente.
- **Domínio de exemplo**: catálogo de produtos (como a busca de um e-commerce).

---

## 📁 O que foi feito (5 arquivos importam)

| Arquivo | O que é |
|---|---|
| `docker-compose.yml` | Sobe o Elasticsearch local (1 nó, sem senha, só p/ estudo) |
| `src/main/kotlin/Product.kt` | O **documento** (unidade básica do ES) como data class |
| `src/main/kotlin/ElasticClient.kt` | A conexão app → ES (um wrapper HTTP tipado) |
| `src/main/kotlin/ProductRepository.kt` | ⭐ **O coração do estudo**: cada método = 1 conceito do ES |
| `src/main/kotlin/Main.kt` | API web (Ktor) que expõe as buscas em rotas HTTP |
| `requests.http` | Requests clicáveis no IntelliJ (fale com o ES direto e com a API) |

Cada método tem um comentário `CONCEITO — ...` explicando o que acontece por baixo.

---

## ▶️ Como rodar (3 passos)

```bash
# 1. Abra o Docker Desktop (primeira vez: aceite os termos e aguarde ele iniciar)
open -a Docker

# 2. Suba o Elasticsearch (primeira vez baixa a imagem, ~600MB)
docker compose up -d
# confira: curl http://localhost:9200  (ou abra no navegador)

# 3. Rode a aplicação (ou clique em ▶ no Main.kt pelo IntelliJ)
mvn compile exec:java
```

Depois abra o `requests.http` no IntelliJ e vá clicando nos requests. 🎯

> **`command not found: docker`?** O CLI está em `~/.docker/bin` mas não no seu PATH.
> Adicione ao `~/.zshrc`: `export PATH="$HOME/.docker/bin:$PATH"` (e abra um terminal novo).

---

## 🧠 Conceitos-chave (o vocabulário mínimo)

| Conceito | Em uma frase | Análogo no mundo que você conhece |
|---|---|---|
| **Documento** | Um objeto JSON armazenado | Uma linha do banco / um objeto JS |
| **Índice** | Coleção de documentos | Uma tabela |
| **Mapping** | Define o tipo de cada campo | Schema da tabela |
| **Índice invertido** | Estrutura `token → [docs]` que torna a busca rápida | Índice remissivo de livro |
| **Analisador** | Quebra texto em tokens ao indexar E ao buscar | `.toLowerCase().split(' ')` turbinado |
| **`text` vs `keyword`** | `text` = analisado (busca livre); `keyword` = exato (filtros) | ⭐ decisão nº 1 do ES |
| **`match` vs `term`** | `match` analisa a busca; `term` compara exato | busca livre vs `===` |
| **Score (BM25)** | Nota de relevância que ordena os resultados | Ranking do Google |
| **Near real-time** | Doc indexado aparece na busca ~1s depois (refresh) | Não é banco transacional! |
| **Bulk** | Várias operações em 1 request | `Promise.all` do bem |

**A regra de bolso mais importante:**
> Texto livre digitado por humano → query `match` em campo `text`.
> Valor exato (categoria, status, id) → query `term` em campo `keyword`.

---

## 📚 Next steps — roadmap de estudo

### Nível 1 — Dominar o que já existe
- [ ] Rode cada request do `requests.http` e leia a resposta JSON inteira (repare em `_score`, `hits.total`)
- [ ] Use o `_analyze` com textos diferentes e observe os tokens
- [ ] Busque `teclados` (plural) e veja que NÃO acha — entenda o porquê (analisador padrão não faz stemming)
- [ ] Delete o índice (`DELETE http://localhost:9200/products`) e reinicie a app para vê-lo ser recriado

### Nível 2 — Melhorar a busca (features para implementar)
- [ ] **Tipos de campo do mapping** 📌: aprofundar em como cada tipo funciona e quando usar (text, keyword, numéricos, date, boolean, object/nested, multi-fields) — *tópico marcado para estudo*
- [ ] **Analyzer `portuguese`**: no mapping, faça plural/stemming funcionar (`"analyzer": "portuguese"`)
- [ ] **Query `bool`**: combine full-text + categoria + preço numa busca só (`must` + `filter`) — é a busca real de e-commerce
- [ ] **Fuzziness**: tolerar erro de digitação (`"fuzziness": "AUTO"` → "teclaod" acha "teclado")
- [ ] **Paginação**: `from`/`size` na busca (e leia sobre o limite de 10.000)
- [ ] **Highlight**: devolver o trecho que casou com `<em>` (como o Google faz em negrito)
- [ ] **Agregações**: contar produtos por categoria (facets: "periferico (4)") — o outro superpoder do ES
- [ ] **Autocomplete**: campo `search_as_you_type` para sugestões enquanto digita

### Nível 3 — Visão de arquitetura (como é em produção)
- [ ] **Kibana**: adicione ao docker-compose e explore o Dev Tools (console interativo de queries)
- [ ] **ES não é fonte de verdade**: estude o padrão *dual-write / CDC* — Postgres é o dono dos dados, ES recebe uma cópia p/ busca
- [ ] **Shards e réplicas**: por que o cluster health fica `yellow` com 1 nó
- [ ] **Testcontainers**: testes de integração subindo um ES descartável
- [ ] **Segurança**: reative `xpack.security` e conecte com API key (como seria em produção)

### 📖 Referências
- [Elasticsearch Guide (oficial)](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Client Java (oficial)](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Full text queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/full-text-queries.html)

---

## 🔧 Stack

Kotlin 2.3 · Maven · Ktor 3.5 (servidor web) · `elasticsearch-java` 9.4.4 (client oficial) · Elasticsearch 9.4.4 (Docker) · Java 17+
