package org.example.catalog.search

import org.example.catalog.product.Product
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

/**
 * CONCEITO — Mapping por anotações:
 * Este é o documento do ELASTICSEARCH. As anotações @Field substituem o
 * createIndexIfMissing() da v1: na subida da app, o Spring Data lê isto e
 * cria o índice "products" com exatamente o mesmo mapping que fazíamos na mão
 * (Text = tokenizado p/ busca; Keyword = exato p/ filtro; Double = numérico).
 *
 * Por que uma classe SEPARADA de Product (Mongo)?
 * Cada armazenamento tem seu formato ideal: o doc de busca costuma ser um
 * SUBCONJUNTO desnormalizado da entidade (só o que se busca/exibe na lista).
 * Aqui os campos coincidem, mas em projetos reais divergem rápido —
 * acoplar os dois modelos é uma armadilha comum.
 */
@Document(indexName = "products")
data class ProductSearchDocument(
    @Id val id: String,
    @Field(type = FieldType.Text) val name: String,
    @Field(type = FieldType.Text) val description: String,
    @Field(type = FieldType.Keyword) val category: String,
    @Field(type = FieldType.Double) val price: Double,
) {
    companion object {
        /** Tradução entidade (Mongo) -> documento de busca (ES). */
        fun from(product: Product): ProductSearchDocument {
            return ProductSearchDocument(
                id = product.id,
                name = product.name,
                description = product.description,
                category = product.category,
                price = product.price,
            )
        }
    }
}

/**
 * CONCEITO — Score de relevância (BM25), como na v1:
 * DTO de resposta da busca full-text: o documento + a nota que ordenou o resultado.
 */
data class ProductHit(
    val score: Double?,
    val product: ProductSearchDocument,
)
