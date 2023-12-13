package com.khomishchak.cryptopricingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CryptoPricingServiceApplication

fun main(args: Array<String>) {
	runApplication<CryptoPricingServiceApplication>(*args)
}
