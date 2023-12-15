package com.khomishchak.cryptopricingservice.service

import jakarta.annotation.PostConstruct
import okhttp3.OkHttpClient
import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService

@Service
class WebSocketServiceImpl (private val integrationWebSocketConnectors: List<IntegrationWebSocketService>) {

    private val client = OkHttpClient()

    @PostConstruct
    fun init() {
        integrationWebSocketConnectors.forEach {connector -> connector.connect(client)}
    }
}