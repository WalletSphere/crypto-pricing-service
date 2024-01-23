package com.khomishchak.cryptopricingservice.config

import com.khomishchak.cryptopricingservice.service.ws.CryptoPriceWebsocketHandler
import com.khomishchak.cryptopricingservice.service.ws.message.WsMessageResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@EnableWebSocket
@Configuration
class WebSocketConfig(private val messageResolvers: List<WsMessageResolver>) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(CryptoPriceWebsocketHandler(messageResolvers), "/crypto-pricing")
    }
}