package org.example.catalog.product

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * CONCEITO — Fonte da verdade (source of truth):
 * Esta é a entidade do banco PRIMÁRIO (MongoDB, coleção "products").
 * O Mongo também guarda documentos JSON (BSON), mas seu papel aqui é outro:
 * CRUD confiável e durável. Ele NÃO tem índice invertido nem score de
 * relevância — busca textual boa não é o forte dele.
 *
 * Regra da arquitetura: quem quer O DADO pergunta ao Mongo;
 * quem quer ACHAR dados pergunta ao Elasticsearch (ver pacote search/).
 * Se o ES morrer, tudo se reconstrói a partir daqui (ver DataSeeder).
 */
@Document("products")
data class Product(
    @Id val id: String,
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
)
