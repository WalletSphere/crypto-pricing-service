package com.khomishchak.cryptopricingservice.service.ws


import com.google.gson.JsonParser
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

class CryptoPriceWebsocketHandler (private val sessionMappingService: SessionMappingService): TextWebSocketHandler() {
    private val sessions = ConcurrentHashMap<String, String>()

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        val json = JsonParser.parseString(payload).asJsonObject

        if (json["type"].asString == "init") {
            val accountId = json["accountId"].asLong
            sessionMappingService.registerSession(session, accountId)
        }
        session.sendMessage(message)
    }
}