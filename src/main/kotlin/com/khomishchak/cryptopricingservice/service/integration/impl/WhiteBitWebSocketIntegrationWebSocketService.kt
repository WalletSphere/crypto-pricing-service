package com.khomishchak.cryptopricingservice.service.integration.impl

import com.google.gson.Gson
import okhttp3.*
import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService
import com.khomishchak.cryptopricingservice.service.model.integration.IntegrationType

@Service
class WhiteBitWebSocketIntegrationWebSocketService: IntegrationWebSocketService, WebSocketListener() {

    private val gson = Gson()

    override fun getIntegrationType(): IntegrationType = IntegrationType.WHITE_BIT

    override fun connect(client: OkHttpClient) {
        val request = Request.Builder()
                .url("wss://api.whitebit.com/ws")
                .build()

        val listener = WhiteBitWebSocketIntegrationWebSocketService()
        client.newWebSocket(request, listener)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        t.printStackTrace()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println("Received $text")

    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val subscribeMessage = mapOf(
                "id" to 1,
                "method" to "market_subscribe",
                "params" to listOf("BTC_USDT")
        )
        webSocket.send(gson.toJson(subscribeMessage))
    }
}