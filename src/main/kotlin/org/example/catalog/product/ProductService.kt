package org.example.catalog.product

import org.example.catalog.search.ProductHit
import org.example.catalog.search.ProductSearchDocument
import org.example.catalog.search.ProductSearchRepository
import org.springframework.stereotype.Service

/**
 * CONCEITO — Camada de serviço + injeção de dependência:
 * O controller não fala com repositórios; fala com o serviço, que orquestra.
 * Repare: ninguém instancia esta classe nem os repositórios — o Spring vê o
 * construtor e INJETA as implementações (constructor injection). Na v1,
 * o main() fazia essas conexões manualmente.
 *
 * É aqui que mora a regra da arquitetura:
 *   ESCRITA  -> Mongo primeiro (verdade), depois replica no ES
 *   LEITURA  -> por id: Mongo | busca/filtro: ES
 */
@Service
class ProductService(
    private val mongoRepository: ProductMongoRepository,
    private val searchRepository: ProductSearchRepository,
) {

    /**
     * CONCEITO — Dual-write (e sua limitação):
     * Grava no Mongo (fonte da verdade) e replica no ES na sequência.
     * Didático e suficiente aqui, MAS em produção de verdade isso é frágil:
     * se o ES falhar após o Mongo commitar, dessincroniza. O padrão robusto
     * é outbox/CDC (ex.: Debezium lê o oplog do Mongo e alimenta o ES) —
     * está no roadmap do README.
     */
    fun create(product: Product): Product {
        val saved = mongoRepository.save(product)
        searchRepository.save(ProductSearchDocument.from(saved))
        return saved
    }

    /** Leitura por id vai ao MONGO: quem quer O dado pergunta à fonte da verdade. */
    fun findById(id: String): Product? =
        mongoRepository.findById(id).orElse(null)

    /** Busca full-text vai ao ES; converte SearchHit (score + doc) no DTO da API. */
    fun searchText(text: String): List<ProductHit> =
        searchRepository.searchByText(text).map { hit ->
            ProductHit(score = hit.score.toDouble(), product = hit.content)
        }

    /** Filtro exato — ES (derived query sobre campo keyword). */
    fun byCategory(category: String): List<ProductSearchDocument> =
        searchRepository.findByCategory(category)

    /** Filtro por faixa de preço — ES (derived query que vira range lte). */
    fun priceUpTo(max: Double): List<ProductSearchDocument> =
        searchRepository.findByPriceLessThanEqual(max)
}
