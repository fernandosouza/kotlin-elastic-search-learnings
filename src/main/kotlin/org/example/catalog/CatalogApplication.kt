package org.example.catalog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * CONCEITO — Inversão de controle (a "mágica" do Spring):
 * Na v1 (branch v1-raw-ktor), o main() criava cada objeto na mão e na ordem certa:
 * client -> repository -> servidor. No Spring é o CONTRÁRIO: você declara
 * as peças (@Service, @RestController, interfaces de repositório) e o
 * framework as encontra, instancia e conecta — isso é o "container de IoC".
 *
 * @SpringBootApplication liga três coisas:
 *   1. Component scan  — varre este pacote e os abaixo procurando anotações
 *   2. Autoconfiguração — viu mongodb no classpath + uri no yml? cria a conexão
 *   3. Configuração    — permite declarar beans próprios (ver DataSeeder)
 */
@SpringBootApplication
class CatalogApplication

fun main(args: Array<String>) {
    runApplication<CatalogApplication>(*args)
}
