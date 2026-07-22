package org.example.catalog.config

import org.example.catalog.product.Product
import org.example.catalog.product.ProductMongoRepository
import org.example.catalog.search.ProductSearchDocument
import org.example.catalog.search.ProductSearchRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * CONCEITO — Seed na direção certa (Mongo -> ES):
 * Na v1 o seed escrevia direto no ES, porque ele era o único armazenamento.
 * Agora o fluxo é o de produção: popula a FONTE DA VERDADE (Mongo) e o ES
 * é alimentado A PARTIR dela. A linha do reindex demonstra a propriedade
 * mais importante da arquitetura: o índice do ES é DESCARTÁVEL — apague-o
 * e ele se reconstrói inteiro a partir do banco.
 *
 * ApplicationRunner = roda uma vez, depois que a app subiu (e depois que o
 * Spring Data já criou o índice a partir das anotações de ProductSearchDocument).
 */
@Configuration
class DataSeeder {

    @Bean
    fun seedRunner(
        mongoRepository: ProductMongoRepository,
        searchRepository: ProductSearchRepository,
    ) = ApplicationRunner {
        if (mongoRepository.count() == 0L) {
            mongoRepository.saveAll(sampleProducts())
        }
        // Mini-reindex a cada subida: ES = cópia derivada do Mongo, nunca o contrário
        searchRepository.saveAll(mongoRepository.findAll().map(ProductSearchDocument::from))
        println("✅ Mongo (fonte da verdade) e índice ES sincronizados. API em http://localhost:8080")
    }
}

/** Mesmos dados de exemplo da v1. */
fun sampleProducts(): List<Product> = listOf(
    Product("1", "Teclado Mecânico RGB", "Teclado com switches mecânicos e iluminação RGB", "periferico", 89.90),
    Product("2", "Mouse Sem Fio", "Mouse ergonômico sem fio com sensor de alta precisão", "periferico", 45.50),
    Product("3", "Headset Gamer 7.1", "Headset com som surround 7.1 e microfone com cancelamento de ruído", "periferico", 129.99),
    Product("4", "Monitor 27\" 4K", "Monitor IPS 27 polegadas com resolução 4K e 144Hz", "monitor", 399.00),
    Product("5", "Monitor Ultrawide 34\"", "Monitor ultrawide curvo ideal para produtividade", "monitor", 549.00),
    Product("6", "Cadeira Ergonômica", "Cadeira de escritório ergonômica com apoio lombar", "escritorio", 279.90),
    Product("7", "Mesa Ajustável", "Mesa de escritório com altura ajustável elétrica", "escritorio", 459.00),
    Product("8", "Webcam Full HD", "Webcam 1080p com foco automático para videochamadas", "periferico", 69.90),
)
