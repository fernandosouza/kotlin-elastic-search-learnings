package org.example.catalog.web

import org.example.catalog.product.Product
import org.example.catalog.product.ProductService
import org.example.catalog.search.ProductHit
import org.example.catalog.search.ProductSearchDocument
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * CONCEITO — Controller declarativo:
 * Mesmas rotas da v1, mas em vez do DSL do Ktor (get("...") { ... }),
 * anotações: @RestController = "toda resposta vira JSON" e cada método é
 * uma rota. Validações que fazíamos na mão (if q.isNullOrBlank -> 400)
 * agora são automáticas: @RequestParam obrigatório ausente = 400 do framework.
 */
@RestController
@RequestMapping("/products")
class ProductController(private val service: ProductService) {

    /** Busca full-text no ES. Ex.: GET /products/search?q=teclado */
    @GetMapping("/search")
    fun search(@RequestParam q: String): List<ProductHit> {
        return service.searchText(q)
    }

    /** Filtro exato por categoria (ES). Ex.: GET /products/category/periferico */
    @GetMapping("/category/{category}")
    fun byCategory(@PathVariable category: String): List<ProductSearchDocument> {
        return service.byCategory(category)
    }


    /** Filtro por faixa de preço (ES). Ex.: GET /products/price?max=100 */
    @GetMapping("/price")
    fun priceUpTo(@RequestParam max: Double): List<ProductSearchDocument> {
        return service.priceUpTo(max)
    }

    /**
     * CONCEITO — A divisão de leitura na prática:
     * Esta rota NÃO toca o Elasticsearch: detalhe por id é papel do banco
     * primário (Mongo). Compare com /search acima — mesma API, storages
     * diferentes por trás. O front nem fica sabendo.
     */
    @GetMapping("/{id}")
    fun byId(@PathVariable id: String): Product {
        return service.findById(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "produto $id não existe")
    }

    /** Cria produto: grava no Mongo e replica no ES (ver ProductService.create). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody product: Product): Product {
        return service.create(product)
    }
}
