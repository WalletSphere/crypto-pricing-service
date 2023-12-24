package com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit

import com.google.gson.Gson
import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.model.integration.WhiteBitWSMarketResp
import com.khomishchak.cryptopricingservice.model.integration.mapRespToMarketUpdate
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
class WhiteBitWebSocketService(val sessionMappingService: SessionMappingService)
    : IntegrationWebSocketService, WebSocketListener() {

    private val gson = Gson()

    val stablecoins = setOf("UAH", "USD", "PLN")
    val shouldBeMappedTickets = mapOf("USDT" to "USD", "WBT-HOLD" to "WBT")

    private var subscribers = ConcurrentHashMap<String, MutableList<Long>>()
    private var subscribedTickers = mutableListOf<String>()

    private var pricesCache = mutableMapOf <String, Double>()

    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var webSocket: WebSocket

    override fun getCryptoExchangerType(): CryptoExchanger = CryptoExchanger.WHITE_BIT

    override fun connect(client: OkHttpClient) {
        val request = Request.Builder()
                .url(WEBSOCKET_CONNECT_URL)
                .build()

        client.newWebSocket(request, this)
    }

    // TODO: should be refactored
    override fun subscribe(accountId: Long, tickers: List<String>) {
        var shouldReloadWsConnection = false
        tickers.forEach { ticker ->
            if (addNewTickerOrAccountAsSubscriber(ticker, accountId)) shouldReloadWsConnection = true
        }
        if (shouldReloadWsConnection) updateSubscribedTickerForConnection()
    }

    private fun addNewTickerOrAccountAsSubscriber(ticker: String, accountId: Long): Boolean {
        val isNewTicker = subscribers[ticker] == null
        ticker.let {
            subscribers.getOrPut(it) { mutableListOf(accountId) }
            addNewTicker(it, accountId)
        }
        return isNewTicker
    }

    private fun addNewTicker(ticker: String, accoutId: Long) {
        subscribers[ticker] = mutableListOf(accoutId)
        addNewTickerToSubscribedTickers(ticker)
    }

    override fun subscribeToAlreadyFollowedTickers(currencies: List<String>) =
            currencies.forEach { addNewTickerToSubscribedTickers(it) }

    private fun addNewTickerToSubscribedTickers(ticker: String) =
        subscribedTickers.add(shouldBeMappedTickets[ticker]?.let { getUsdtPairForTicker(it) }
                ?: getUsdtPairForTicker(ticker))

    private fun getUsdtPairForTicker(ticker: String) = if (ticker in stablecoins) "USDT_$ticker" else "${ticker}_USDT"

    private fun updateSubscribedTickerForConnection() {
        val subscribeMessage = mapOf(
                "id" to 737457,
                "method" to "market_subscribe",
                "params" to subscribedTickers
        )
        webSocket.send(gson.toJson(subscribeMessage))
        logger.info("WhiteBit WS connection was updated")
    }

    override fun getLastPrices(accountId: Long, tickers: List<String>): Map<String, Double> =
        tickers.mapNotNull { ticker ->
            pricesCache[ticker]?.let { price -> ticker to price }
        }.toMap()

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
            logger.error("got failure for WhiteBit WS connection, error: ${t.message}")

    override fun onMessage(webSocket: WebSocket, text: String) = handleUpdateMessage(text)

    private fun handleUpdateMessage(text: String) =
        gson.mapRespToMarketUpdate<WhiteBitWSMarketResp>(text).takeIf { it.method == "market_update" }
                ?.let {
                    updateLatestPrices(it)
                    notifyUsersWithMarketUpdate(it)
                } ?: logger.info("received unpredicted message from WhiteBit WS: $text")

    private fun updateLatestPrices(update: WhiteBitWSMarketResp) {
        val ticker: String = getTickerFromResp(update)
        pricesCache.put(ticker, getLastPriceFromResp(update))
    }

    private fun getLastPriceFromResp(update: WhiteBitWSMarketResp): Double =
            update.getTradingMetrics()?.last?.toDouble() ?: 0.0

    private fun notifyUsersWithMarketUpdate(update: WhiteBitWSMarketResp) {
        val ticker: String = getTickerFromResp(update)
        notifyAccounts(update, ticker)
    }

    private fun getTickerFromResp(update: WhiteBitWSMarketResp): String =
            update.params.firstOrNull()?.toString()?.substringBefore("_").orEmpty()


    private fun notifyAccounts(update: WhiteBitWSMarketResp, ticker: String) =
            subscribers[ticker]?.let { notifyAccountsForTicker(it, update) }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        logger.info("established connection with WhiteBit WS successfully!")

        updateSubscribedTickerForConnection()
    }

    private fun notifyAccountsForTicker(accountsToBeNotified: MutableList<Long>, update: WhiteBitWSMarketResp) =
            accountsToBeNotified.forEach { sessionMappingService.sendMessageToSession(it, update.toString())}
}