package com.khomishchak.cryptopricingservice.service.ws.message.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import com.khomishchak.cryptopricingservice.service.ws.WebSocketService
import com.khomishchak.cryptopricingservice.service.ws.message.WsMessageResolver
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession

@Service
class SubscribeMarketMessageResolver(private val gson: Gson,
                                     private val webSocketService: WebSocketService,
                                     private val sessionMappingService: SessionMappingService) : WsMessageResolver {
    override fun getMessage(): String = "subscribe_market"

    override fun process(messageJson: JsonObject, session: WebSocketSession) {
        val exchanger = CryptoExchanger.valueOf(messageJson["exchanger"].asString)
        val tickersJsonArray = messageJson["tickers"].asJsonArray
        val tickers: List<String> = tickersJsonArray.map { it.asString }

        sessionMappingService.getUserId(session).takeIf { it != 0L }
                ?.let {
                    webSocketService.subscribe(it, exchanger, tickers)
                    sendLatestPrices(it, exchanger, tickers)
                }
    }

    private fun sendLatestPrices(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) =
            gson.toJson(webSocketService.getLastPrices(accountId, exchanger, tickers))
                    ?.let { sessionMappingService.sendMessageToSession(accountId, it) }
}