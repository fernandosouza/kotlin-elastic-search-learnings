package org.example.catalog.search

import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
 * CONCEITO — Derived queries ("query methods"):
 * A mágica nº 2 do Spring Data: ele PARSEIA O NOME do método e gera a query.
 * É assim que um projeto profissional evita "chamadas diretas" à API do ES —
 * compare cada método com o equivalente que escrevíamos na mão na v1.
 */
interface ProductSearchRepository : ElasticsearchRepository<ProductSearchDocument, String> {

    /**
     * "findBy" + "Category" -> busca de igualdade no campo category.
     * Como o mapping diz que category é Keyword, o resultado é o mesmo
     * filtro exato da query "term" da v1 (ProductRepository.byCategory).
     */
    fun findByCategory(category: String): List<ProductSearchDocument>

    /**
     * "findBy" + "Price" + "LessThanEqual" -> vira a query "range" { lte: max }
     * da v1 (ProductRepository.priceUpTo). O nome do método É a query.
     */
    fun findByPriceLessThanEqual(max: Double): List<ProductSearchDocument>

    /**
     * CONCEITO — @Query, a escotilha de emergência:
     * Nome de método não expressa multi_match em 2 campos com relevância.
     * Quando a convenção não alcança, escreve-se a query JSON nativa
     * (?0 = 1º parâmetro). É a MESMA query do searchText da v1 — o Spring
     * não elimina o conhecimento de ES, só o esconde até você precisar dele.
     * Retornar SearchHit<T> (em vez de T) preserva o _score de cada resultado.
     */
    @Query("""{ "multi_match": { "query": "?0", "fields": ["name", "description"] } }""")
    fun searchByText(text: String): List<SearchHit<ProductSearchDocument>>
}
