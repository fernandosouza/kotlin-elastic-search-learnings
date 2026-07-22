import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.Refresh
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation

/**
 * Camada de acesso ao Elasticsearch (padrão Repository).
 * Cada método abaixo demonstra UM conceito fundamental do ES.
 */
class ProductRepository(private val client: ElasticsearchClient) {

    companion object {
        const val INDEX = "products"
    }

    /**
     * CONCEITO — Índice e Mapping:
     * O ÍNDICE é o "lugar" onde documentos ficam (análogo a uma tabela).
     * O MAPPING define o tipo de cada campo — e essa é A decisão mais
     * importante do ES:
     *
     *   - "text"    -> o valor é ANALISADO: quebrado em tokens minúsculos
     *                  ("Teclado Mecânico" vira ["teclado", "mecanico"]) e
     *                  guardado num ÍNDICE INVERTIDO (token -> docs que o contêm,
     *                  como o índice remissivo de um livro). Serve p/ busca livre.
     *   - "keyword" -> o valor é guardado EXATO, sem análise.
     *                  Serve p/ filtros, agregações e ordenação (ex.: categoria).
     *   - "double"  -> numérico; permite comparações de faixa (preço <= X).
     *
     * REST equivalente: PUT /products  { "mappings": { "properties": ... } }
     */
    fun createIndexIfMissing() {
        val exists = client.indices().exists { it.index(INDEX) }.value()
        if (exists) return

        client.indices().create { create ->
            create.index(INDEX).mappings { mapping ->
                mapping
                    .properties("name") { p -> p.text { it } }
                    .properties("description") { p -> p.text { it } }
                    .properties("category") { p -> p.keyword { it } }
                    .properties("price") { p -> p.double_ { it } }
            }
        }
    }

    /**
     * CONCEITO — Indexar um documento:
     * "Indexar" = salvar o JSON no índice E atualizar o índice invertido.
     * Se o id já existe, o documento é substituído (upsert).
     *
     * O ES é "near real-time": o doc só aparece nas BUSCAS ~1s depois
     * (quando ocorre o "refresh" do índice) — não é um banco transacional.
     *
     * REST equivalente: PUT /products/_doc/{id}  { ...json... }
     */
    fun save(product: Product) {
        client.index { it.index(INDEX).id(product.id).document(product) }
    }

    /**
     * CONCEITO — Bulk API:
     * Indexar 1 doc = 1 chamada HTTP. Para carga de dados, o mundo real usa
     * o _bulk: várias operações em UMA request (muito mais rápido).
     * O refresh(True) força o índice a atualizar já — só usamos aqui no seed
     * para os dados aparecerem imediatamente nas buscas.
     *
     * REST equivalente: POST /_bulk
     */
    fun seed(products: List<Product>) {
        val operations = products.map { product ->
            BulkOperation.of { op ->
                op.index<Product> { idx -> idx.index(INDEX).id(product.id).document(product) }
            }
        }
        client.bulk { it.operations(operations).refresh(Refresh.True) }
    }

    /**
     * CONCEITO — Busca full-text (match / multi_match):
     * A query "match" ANALISA o texto buscado com o mesmo analisador do campo
     * e procura os tokens no índice invertido — a busca não é comparação de string!
     * Com o analisador padrão: "TECLADO" acha "teclado" (lowercase) e
     * "mouse teclado" acha docs que tenham QUALQUER um dos tokens (OR),
     * ordenados por relevância. Plural ("teclados") e acentos ("mecanico")
     * ainda NÃO casam — isso pede o analyzer "portuguese" (ver next steps).
     * Cada resultado vem com um SCORE de relevância (BM25) que define a ordem.
     * "multi_match" = o mesmo, mas procurando em vários campos de uma vez.
     *
     * REST equivalente: POST /products/_search
     *   { "query": { "multi_match": { "query": q, "fields": ["name","description"] } } }
     */
    fun searchText(text: String): List<ProductHit> {
        val response = client.search({ search ->
            search.index(INDEX).query { query ->
                query.multiMatch { mm -> mm.query(text).fields("name", "description") }
            }
        }, Product::class.java)

        return response.hits().hits().mapNotNull { hit ->
            hit.source()?.let { ProductHit(hit.score(), it) }
        }
    }

    /**
     * CONCEITO — Busca exata (term):
     * A query "term" NÃO analisa nada: compara o valor byte a byte contra um
     * campo "keyword". É filtro, não busca — "categoria == periferico".
     * Regra de bolso: texto livre -> match em campo text;
     *                 valor exato (status, categoria, id) -> term em campo keyword.
     *
     * REST equivalente: POST /products/_search
     *   { "query": { "term": { "category": valor } } }
     */
    fun byCategory(category: String): List<Product> {
        val response = client.search({ search ->
            search.index(INDEX).query { query ->
                query.term { term -> term.field("category").value(category) }
            }
        }, Product::class.java)

        return response.hits().hits().mapNotNull { it.source() }
    }

    /**
     * CONCEITO — Busca por faixa (range):
     * Compara valores numéricos/datas: gte (>=), lte (<=), gt, lt.
     * É o "WHERE price <= X" do ES.
     *
     * REST equivalente: POST /products/_search
     *   { "query": { "range": { "price": { "lte": max } } } }
     */
    fun priceUpTo(max: Double): List<Product> {
        val response = client.search({ search ->
            search.index(INDEX).query { query ->
                query.range { range ->
                    range.number { n -> n.field("price").lte(max) }
                }
            }
        }, Product::class.java)

        return response.hits().hits().mapNotNull { it.source() }
    }
}
