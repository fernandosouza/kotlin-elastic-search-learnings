/**
 * CONCEITO — Documento:
 * A unidade básica do Elasticsearch é o DOCUMENTO: um objeto JSON.
 * Esta data class vira, ao ser indexada, exatamente este JSON:
 *   { "name": "...", "description": "...", "category": "...", "price": 0.0 }
 *
 * Comparando com o que você já conhece:
 *   - Banco relacional:  tabela -> linha   -> coluna
 *   - Elasticsearch:     índice -> documento -> campo
 *
 * Diferente de uma tabela SQL, o documento é aninhável e flexível —
 * mas o TIPO de cada campo é definido no "mapping" (ver ProductRepository.createIndex).
 */
data class Product(
    val id: String,
    val name: String,        // busca full-text (campo "text")
    val description: String, // busca full-text (campo "text")
    val category: String,    // filtro exato (campo "keyword")
    val price: Double,       // filtro por faixa (campo "double")
)

/**
 * CONCEITO — Score de relevância:
 * Toda busca full-text devolve, além do documento, um SCORE (algoritmo BM25):
 * quanto maior, mais relevante o documento é para o texto buscado.
 * É isso que define a ORDEM dos resultados — igual um resultado do Google.
 */
data class ProductHit(
    val score: Double?,
    val product: Product,
)
