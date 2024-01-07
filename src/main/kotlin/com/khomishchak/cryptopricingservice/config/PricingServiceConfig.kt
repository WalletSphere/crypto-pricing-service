package com.khomishchak.cryptopricingservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class PricingServiceConfig {

    @Bean("pricingServiceRestTemplate")
    fun getRestTEmplate() = RestTemplate();
}