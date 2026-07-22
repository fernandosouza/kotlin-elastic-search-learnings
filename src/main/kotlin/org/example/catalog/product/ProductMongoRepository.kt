package org.example.catalog.product

import org.springframework.data.mongodb.repository.MongoRepository

/**
 * CONCEITO — Spring Data Repository (a mágica nº 1 do dia a dia):
 * Você declara uma INTERFACE vazia e o Spring gera a implementação em runtime.
 * Só por estender MongoRepository, já ganhamos de graça:
 *   save(), findById(), findAll(), deleteById(), count()...
 *
 * Compare com a v1: lá escrevíamos cada operação na mão com o client.
 * Aqui declaramos a intenção e o framework escreve o código chato.
 * (O mesmo padrão vale para JPA/Postgres: seria JpaRepository<Product, Long>.)
 */
interface ProductMongoRepository : MongoRepository<Product, String>
