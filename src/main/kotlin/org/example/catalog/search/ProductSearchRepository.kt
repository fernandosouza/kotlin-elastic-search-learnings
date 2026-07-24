package org.example.catalog.search

import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
 * As consultas da aplicação ao Elasticsearch. Cada método corresponde a UMA
 * query de ES (gerada pelo nome do método ou declarada em @Query) — os
 * comentários focam no conceito de busca que cada uma exercita.
 */
interface ProductSearchRepository : ElasticsearchRepository<ProductSearchDocument, String> {

    /**
     * CONCEITO — busca exata em campo keyword:
     * "category" foi mapeada como Keyword: o valor foi indexado INTEIRO, sem
     * análise. A comparação é literal, byte a byte ("periferico" ≠ "Periférico").
     * Isso é FILTRO, não busca textual — um doc casa ou não casa; relevância
     * não entra no jogo. Equivale à query "term" do ES.
     */
    fun findByCategory(category: String): List<ProductSearchDocument>

    /**
     * CONCEITO — busca por faixa (range):
     * Campos numéricos não vão para o índice invertido: o ES os guarda numa
     * estrutura separada, otimizada p/ comparações (<=, >=, entre). Esta busca
     * equivale à query { "range": { "price": { "lte": max } } }.
     */
    fun findByPriceLessThanEqual(max: Double): List<ProductSearchDocument>

    /**
     * CONCEITO — multi_match (busca full-text em vários campos):
     * O texto buscado é ANALISADO (tokenizado, minúsculas) e cada token é
     * procurado no índice invertido de "name" E de "description" ao mesmo tempo.
     * Cada documento que casa recebe um SCORE de relevância (BM25) — quanto mais
     * raro o termo no índice e mais presente no campo, maior a nota — e o
     * resultado vem ORDENADO por esse score. SearchHit = documento + seu _score.
     *
     * Obs.: "?0" não é sintaxe do ES — é placeholder posicional do Spring Data
     * ("0" = primeiro argumento do método, ou seja, "text"). O valor é inserido
     * com escaping seguro antes de a query JSON ser enviada ao Elasticsearch.
     */
    @Query("""{ "multi_match": { "query": "?0", "fields": ["name", "description"] } }""")
    fun searchByText(text: String): List<SearchHit<ProductSearchDocument>>
}
