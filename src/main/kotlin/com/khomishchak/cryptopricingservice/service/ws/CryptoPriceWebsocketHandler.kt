package com.khomishchak.cryptopricingservice.service.ws

import com.google.gson.JsonParser
import com.khomishchak.cryptopricingservice.service.ws.message.WsMessageResolver
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class CryptoPriceWebsocketHandler (private val messageResolvers: List<WsMessageResolver>)
                                   : TextWebSocketHandler() {

    private val websocketIntegrations = messageResolvers.associateBy { it.getMessage() }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val json = JsonParser.parseString(message.payload).asJsonObject
        websocketIntegrations[json["method"].asString]?.process(json, session)
    }
}