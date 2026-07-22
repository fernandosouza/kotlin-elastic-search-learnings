import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * CONCEITO — Como uma app conversa com o Elasticsearch:
 * O ES é um servidor independente (subiu no Docker, porta 9200) que só fala
 * HTTP + JSON. Este client é um wrapper tipado: cada método dele vira uma
 * chamada REST por baixo. Ex.:
 *
 *   client.index(...)  ->  PUT  http://localhost:9200/products/_doc/1
 *   client.search(...) ->  POST http://localhost:9200/products/_search
 *
 * Ou seja: é o mesmo papel que o "fetch" faz no front — só que com tipos.
 *
 * O JacksonJsonpMapper diz ao client COMO converter data classes Kotlin <-> JSON
 * (o registerKotlinModule é obrigatório: sem ele o Jackson não sabe instanciar
 * data classes, que não têm construtor vazio).
 */
fun createElasticClient(): ElasticsearchClient {
    val jsonMapper = ObjectMapper().registerKotlinModule()
    return ElasticsearchClient.of { builder ->
        builder
            .host("http://localhost:9200")
            .jsonMapper(JacksonJsonpMapper(jsonMapper))
    }
}
