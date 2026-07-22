import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

/**
 * CONCEITO — Onde o ES entra numa arquitetura web:
 *
 *   Navegador (fetch) --> Backend Kotlin (esta API, porta 8080) --> Elasticsearch (porta 9200)
 *
 * O front NUNCA fala com o ES diretamente: o backend é quem valida, autoriza,
 * monta a query e traduz a resposta. O ES aqui faz o papel de "motor de busca"
 * — em sistemas reais ele convive com um banco primário (Postgres etc.) que é
 * a fonte da verdade; o ES guarda uma CÓPIA otimizada para buscar.
 *
 * Fluxo desta app ao subir:
 *   1. conecta no ES          3. carrega dados de exemplo (seed)
 *   2. cria o índice+mapping  4. expõe rotas HTTP de busca
 */
fun main() {
    val client = createElasticClient()
    val repository = ProductRepository(client)

    repository.createIndexIfMissing()
    repository.seed(sampleProducts())
    println("✅ Índice '${ProductRepository.INDEX}' pronto. API em http://localhost:8080")

    embeddedServer(Netty, port = 8080) {
        // Converte Kotlin <-> JSON automaticamente nas rotas (como res.json no Express)
        install(ContentNegotiation) { jackson() }

        routing {
            /**
             * Busca full-text. Ex.: GET /products/search?q=teclado
             * É a rota que alimentaria a barra de busca do seu front.
             */
            get("/products/search") {
                val query = call.request.queryParameters["q"]
                if (query.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "use ?q=texto"))
                    return@get
                }
                call.respond(repository.searchText(query))
            }

            /**
             * Filtro exato por categoria. Ex.: GET /products/category/periferico
             * É a rota que alimentaria os "filtros laterais" de um e-commerce.
             */
            get("/products/category/{category}") {
                val category = call.parameters["category"]!!
                call.respond(repository.byCategory(category))
            }

            /**
             * Filtro por faixa de preço. Ex.: GET /products/price?max=100
             */
            get("/products/price") {
                val max = call.request.queryParameters["max"]?.toDoubleOrNull()
                if (max == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "use ?max=numero"))
                    return@get
                }
                call.respond(repository.priceUpTo(max))
            }

            /**
             * Indexa um produto novo. Ex.: POST /products  { ...json... }
             * Lembre: ele aparece nas buscas ~1s depois (near real-time).
             */
            post("/products") {
                val product = call.receive<Product>()
                repository.save(product)
                call.respond(HttpStatusCode.Created, product)
            }
        }
    }.start(wait = true)
}

/** Dados de exemplo para você ter o que buscar assim que a app sobe. */
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
