# 🔍 Elastic Search Studies

Projeto didático para aprender **Elasticsearch** integrado a um backend web profissional em **Kotlin + Spring Boot**, com **MongoDB** como banco primário.

## 🌿 As duas versões (branches)

| Branch | O que é | Quando usar |
|---|---|---|
| `v1-raw-ktor` | Versão crua: Ktor + client oficial do ES na mão, sem banco | Entender **o que acontece por baixo** |
| `main` (esta) | Versão "de empresa": Spring Boot + Spring Data + MongoDB | Ver como fica **no código do trabalho** |

As **funcionalidades são idênticas** — compare os dois branches lado a lado: cada query que o Spring "esconde" aqui existe escrita à mão na v1. Esse é o superpoder deste repo.

---

## 🗺️ A arquitetura em 30 segundos

```
                                        ┌──► MongoDB :27017  (fonte da verdade)
Navegador ──► Spring Boot :8080 ────────┤      CRUD, detalhe por id
 (fetch)      valida, orquestra         └──► Elasticsearch :9200  (motor de busca)
                                               full-text, filtros, relevância
```

- **Escrita** (`POST /products`): grava no Mongo **e** replica no ES (dual-write didático — produção usaria outbox/CDC).
- **Leitura por id** (`GET /products/1`): Mongo. Quem quer **o dado** pergunta à fonte da verdade.
- **Busca** (`/search`, `/category`, `/price`): ES. Quem quer **achar** dados pergunta ao motor de busca.
- **O índice do ES é descartável**: apague-o e ele se reconstrói a partir do Mongo na próxima subida (veja `DataSeeder`).

---

## 📁 Mapa do código (e onde está cada conceito)

| Arquivo | Conceito que ensina |
|---|---|
| `CatalogApplication.kt` | Inversão de controle — o Spring monta as peças, não você |
| `product/Product.kt` | Entidade do banco primário (fonte da verdade) |
| `product/ProductMongoRepository.kt` | Mágica nº 1: interface vazia → CRUD implementado pelo Spring |
| `search/ProductSearchDocument.kt` | Mapping por anotações (`@Field` substitui o `createIndex` da v1) |
| `search/ProductSearchRepository.kt` | ⭐ Mágica nº 2: **query pelo nome do método** + `@Query` p/ multi_match |
| `product/ProductService.kt` | Orquestração: a regra "escrita → Mongo+ES, leitura → depende" |
| `web/ProductController.kt` | Rotas por anotação, validação automática (400 de graça) |
| `config/DataSeeder.kt` | Seed na direção certa: Mongo → ES (reindex a cada subida) |
| `application.yml` | Configuração externalizada (com pegadinha do Boot 4 documentada!) |

Cada método continua com seu comentário `CONCEITO — ...`, agora comparando com a v1.

---

## ▶️ Como rodar (3 passos)

```bash
# 1. Abra o Docker Desktop
open -a Docker

# 2. Suba MongoDB + Elasticsearch
docker compose up -d

# 3. Rode a aplicação (ou ▶ no CatalogApplication.kt pelo IntelliJ)
mvn spring-boot:run
```

Depois abra o `requests.http` e vá clicando. 🎯

> **`command not found: docker`?** O CLI está em `~/.docker/bin` mas não no seu PATH.
> Adicione ao `~/.zshrc`: `export PATH="$HOME/.docker/bin:$PATH"` (e abra um terminal novo).

**Espiar o Mongo por dentro:**
```bash
docker exec -it mongo-estudos mongosh catalog
db.products.find()          # a fonte da verdade
```

---

## 🧠 Conceitos-chave

### Do Elasticsearch (iguais nas duas versões)

| Conceito | Em uma frase |
|---|---|
| **Documento / Índice / Mapping** | JSON armazenado / a "tabela" / o "schema" dos campos |
| **`text` vs `keyword`** | analisado p/ busca livre vs exato p/ filtros — ⭐ decisão nº 1 |
| **Índice invertido + analisador** | `token → docs`, alimentado pela tokenização |
| **`match` vs `term` vs `range`** | busca analisada / igualdade exata / faixa numérica |
| **Score (BM25)** | nota de relevância que ordena resultados |
| **Near real-time** | doc aparece na busca ~1s após indexado |

### Novos desta versão (o "mundo Spring")

| Conceito | Em uma frase |
|---|---|
| **Inversão de controle / DI** | você declara as peças; o container instancia e conecta |
| **Spring Data Repository** | interface vazia → implementação gerada em runtime |
| **Derived queries** | `findByPriceLessThanEqual(x)` → o **nome** vira a query `range` |
| **`@Query`** | escotilha p/ escrever a query nativa quando o nome não alcança |
| **Fonte da verdade vs réplica de busca** | Mongo é dono do dado; ES é cópia otimizada e **descartável** |
| **Dual-write vs CDC** | replicar na mão é frágil; produção usa log do banco (Debezium) |
| **Config externalizada** | `application.yml` + env vars; e configuração errada **falha em silêncio** |

---

## 📚 Next steps — roadmap de estudo

### Nível 1 — Dominar o que já existe
- [x] Rode os requests e compare: `GET /products/1` (Mongo) vs `/search` (ES) — mesma API, storages diferentes
- [ ] Leia `ProductSearchRepository` e ache na v1 (`git diff v1-raw-ktor main -- src`) a query equivalente escrita à mão
- [x] Apague o índice (`DELETE http://localhost:9200/products`), suba a app e veja o ES se reconstruir do Mongo
- [x] Use o `_analyze` do `requests.http` com textos diferentes e observe os tokens

### Nível 2 — Melhorar a busca (features para implementar)
- [ ] **Tipos de campo do mapping** 📌: aprofundar em como cada tipo funciona e quando usar (text, keyword, numéricos, date, boolean, object/nested, multi-fields) — *tópico marcado para estudo*
- [ ] **Tipos de analyzers** 📌: conhecer os built-in (`standard`, `simple`, `whitespace`, `stop`, `keyword`, `pattern`, `fingerprint` + ~30 de idioma) e a anatomia char filters → tokenizer → token filters; comparar cada um no `_analyze` com a mesma frase
- [ ] **Analyzer `portuguese`**: plural/stemming (`@Field(analyzer = "portuguese")`) — a v1 fazia isso no JSON do mapping
- [ ] **Query `bool`**: full-text + categoria + preço numa busca só — a busca real de e-commerce (use `NativeQuery` ou `CriteriaQuery` do Spring Data)
- [ ] **Fuzziness**: tolerar erro de digitação ("teclaod" → "teclado")
- [ ] **Paginação**: troque `List<>` por `Page<>` + `Pageable` no repositório — o Spring Data pagina sozinho
- [ ] **Highlight**: devolver o trecho que casou (`@Highlight` no Spring Data)
- [ ] **Agregações**: contar produtos por categoria (facets) — o outro superpoder do ES

### Nível 3 — Visão de produção
- [ ] **Kibana**: adicione ao docker-compose e explore o Dev Tools
- [ ] **CDC de verdade**: substitua o dual-write do `ProductService` por Debezium lendo o oplog do Mongo
- [ ] **Testcontainers**: testes de integração subindo Mongo + ES descartáveis
- [ ] **Resiliência**: o que acontece se o ES estiver fora? (try/catch no dual-write, fila de retry)
- [ ] **Segurança**: reative `xpack.security` e conecte com API key
- [ ] **Shards e réplicas**: por que o cluster health é `yellow` com 1 nó

### 📖 Referências
- [Elasticsearch Guide (oficial)](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Spring Data Elasticsearch (oficial)](https://docs.spring.io/spring-data/elasticsearch/reference/)
- [Query methods / derived queries](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/repositories/elasticsearch-repository-queries.html)

---

## 🔧 Stack

Kotlin 2.3 · Maven · Spring Boot 4.1 (WebMVC + Data MongoDB + Data Elasticsearch) · MongoDB 8 (Docker) · Elasticsearch 9.4.4 (Docker) · Java 17+
