package com.khomishchak.cryptopricingservice.config

import com.khomishchak.cryptopricingservice.service.ws.CryptoPriceWebsocketHandler
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@EnableWebSocket
@Configuration
class WebSocketConfig(private val sessionMappingService: SessionMappingService): WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(CryptoPriceWebsocketHandler(sessionMappingService), "/ws")
    }
}