package com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit

import com.google.gson.Gson

import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.model.integration.WhiteBitWSMarketResp
import com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit.mapper.WsResponseMapper
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import okhttp3.Response

const val WEBSOCKET_CONNECT_URL = "wss://api.whitebit.com/ws";
@Service
class WhiteBitWebSocketIntegrationWebSocketService(val wsResponseMapper: WsResponseMapper,
                                                   val sessionMappingService: SessionMappingService)
    : IntegrationWebSocketService, WebSocketListener() {

    private val gson = Gson()

    private lateinit var subscribers: MutableMap<String, MutableList<Long>>// = mutableMapOf<String, MutableList<Long>>()

    init {
        subscribers = mutableMapOf<String, MutableList<Long>>()
    }

    override fun getCryptoExchangerType(): CryptoExchanger = CryptoExchanger.WHITE_BIT

    override fun connect(client: OkHttpClient) {
        val request = Request.Builder()
                .url(WEBSOCKET_CONNECT_URL)
                .build()

        val listener = WhiteBitWebSocketIntegrationWebSocketService(wsResponseMapper, sessionMappingService)
        client.newWebSocket(request, listener)
    }

    override fun subscribe(accoutId: Long, tickers: List<String>) {
        tickers.forEach { ticker ->
            run {
                if (subscribers[ticker] == null) {
                    subscribers[ticker] = mutableListOf(accoutId)
                } else {
                    subscribers[ticker]?.add(accoutId)
                }
            }
        }
        println("on subscribe: ${subscribers["NEAR"]?.size}")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = t.printStackTrace()

    override fun onMessage(webSocket: WebSocket, text: String) {
        notifyUsersWithMarketUpdate(text)
    }

    fun notifyUsersWithMarketUpdate(text: String) {
        println("on message: ${subscribers.size}")
//        wsResponseMapper.mapRespToMarketUpdate(text).also { update ->
//            if (update.method != "market_update") return
//            val ticker: String = update.params[0].toString().split("_")[0];
//            println(ticker)
//            subscribers[ticker]?.let {
//                notifyAccounts(it, update)
//            }
//        }
    }

    private fun notifyAccounts(accountsToBeNotified: MutableList<Long>, update: WhiteBitWSMarketResp) {
        accountsToBeNotified.forEach {accountId -> sessionMappingService.sendMessageToSession(accountId, update.toString())}
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val subscribeMessage = mapOf(
                "id" to 1,
                "method" to "market_subscribe",
                "params" to listOf("NEAR_USDT")
        )
        webSocket.send(gson.toJson(subscribeMessage))
    }
}