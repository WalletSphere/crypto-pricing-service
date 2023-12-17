package com.khomishchak.cryptopricingservice.service.ws

import com.google.gson.JsonParser
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class CryptoPriceWebsocketHandler (private val sessionMappingService: SessionMappingService,
                                   private val webSocketService: WebSocketService): TextWebSocketHandler() {

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val json = JsonParser.parseString(message.payload).asJsonObject
        val method = json["method"].asString

        if (method == "subscribe_market") {
            val accountId = json["accountId"].asLong
            val exchanger = CryptoExchanger.valueOf(json["exchanger"].asString)
            val tickersJsonArray = json["tickers"].asJsonArray
            val tickers: List<String> = tickersJsonArray.map { it.asString }

            sessionMappingService.registerSession(session, accountId)
            webSocketService.subscribe(accountId, exchanger, tickers)
        }
    }


}