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
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

const val WEBSOCKET_CONNECT_URL = "wss://api.whitebit.com/ws";
@Service
class WhiteBitWebSocketIntegrationWebSocketService(val wsResponseMapper: WsResponseMapper,
                                                   val sessionMappingService: SessionMappingService)
    : IntegrationWebSocketService, WebSocketListener() {

    private val gson = Gson()

    private var subscribers = ConcurrentHashMap<String, MutableList<Long>>()
    private var subscribedTickers = mutableListOf<String>()
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var webSocket: WebSocket

    override fun getCryptoExchangerType(): CryptoExchanger = CryptoExchanger.WHITE_BIT

    override fun connect(client: OkHttpClient) {
        val request = Request.Builder()
                .url(WEBSOCKET_CONNECT_URL)
                .build()

        client.newWebSocket(request, this)
    }

    override fun subscribe(accountId: Long, tickers: List<String>) {
        var shouldReloadWsConnection = false
        tickers.forEach { ticker ->
            if (addNewTickerOrAccountAsSubscriber(ticker, accountId)) shouldReloadWsConnection = true
        }
        if (shouldReloadWsConnection) updateSubscribedTickerForConnection()
    }

    private fun addNewTickerOrAccountAsSubscriber(ticker: String, accountId: Long): Boolean {
        val isNewTicker = subscribers[ticker] == null
        subscribers.getOrPut(ticker) { mutableListOf(accountId) }
        return isNewTicker
    }

    private fun addTickerAsNew(ticker: String, accoutId: Long) {
        subscribers[ticker] = mutableListOf(accoutId)
        subscribedTickers.add("${ticker}_USDT")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = t.printStackTrace()

    override fun onMessage(webSocket: WebSocket, text: String) = notifyUsersWithMarketUpdate(text)

    private fun notifyUsersWithMarketUpdate(text: String) {
        wsResponseMapper.mapRespToMarketUpdate(text).also {
            if (it.method != "market_update") return
            val ticker: String = getTickerFromResp(it)
            notifyAccounts(it, ticker)
        }
    }

    private fun updateSubscribedTickerForConnection() {
        val subscribeMessage = mapOf(
                "id" to 737457,
                "method" to "market_subscribe",
                "params" to subscribedTickers
        )
        webSocket.send(gson.toJson(subscribeMessage))
        logger.info("WhiteBit WS connection was updated")
    }

    private fun getTickerFromResp(update: WhiteBitWSMarketResp): String {
        return update.params.firstOrNull()?.toString()?.split("_")?.get(0).orEmpty()
    }

    private fun notifyAccounts(update: WhiteBitWSMarketResp, ticker: String) {
        subscribers[ticker]?.let {
            notifyAccountsForTicker(it, update)
        }
    }

    private fun notifyAccountsForTicker(accountsToBeNotified: MutableList<Long>, update: WhiteBitWSMarketResp) {
        accountsToBeNotified.forEach {
            sessionMappingService.sendMessageToSession(it, update.toString())}
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        logger.info("established connection with WhiteBit WS successfully!")
    }
}