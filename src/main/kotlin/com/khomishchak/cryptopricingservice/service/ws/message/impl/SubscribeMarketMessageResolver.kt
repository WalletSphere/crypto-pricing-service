package com.khomishchak.cryptopricingservice.service.ws.message.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.khomishchak.cryptopricingservice.model.MarkerSubscriptionDetails
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import com.khomishchak.cryptopricingservice.service.ws.WebSocketService
import com.khomishchak.cryptopricingservice.service.ws.message.WsMessageResolver
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession

fun JsonObject.mapToMarkerSubscriptionDetails() =
        MarkerSubscriptionDetails(
                CryptoExchanger.valueOf(this["exchanger"].asString),
                this["initialCurrency"].asString,
                this["tickers"].asJsonArray.map { ticker -> ticker.asString }
        )

@Service
class SubscribeMarketMessageResolver(private val gson: Gson,
                                     private val webSocketService: WebSocketService,
                                     private val sessionMappingService: SessionMappingService) : WsMessageResolver {
    override fun getMessage(): String = "subscribe_market"


    override fun process(messageJson: JsonObject, session: WebSocketSession): Unit =
            messageJson.mapToMarkerSubscriptionDetails().let { handleMarketSubscriptionRequest(session, it) }


    private fun handleMarketSubscriptionRequest(session: WebSocketSession, markerSubscriptionDetails: MarkerSubscriptionDetails) =
        sessionMappingService.getUserId(session).takeIf { it != 0L }
                ?.let {
                    webSocketService.subscribe(it, markerSubscriptionDetails)
                    sendLatestPrices(it, markerSubscriptionDetails.exchanger, markerSubscriptionDetails.allTickers)
                }

    private fun sendLatestPrices(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) =
            gson.toJson(webSocketService.getLastPrices(exchanger, tickers))
                    ?.let { sessionMappingService.sendMessageToSession(accountId, it) }
}