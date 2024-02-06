package com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit

import com.google.gson.Gson
import com.khomishchak.cryptopricingservice.model.MarkerSubscriptionDetails
import com.khomishchak.cryptopricingservice.model.WHITEBIT_WEBSOCKET_CONNECT_URL
import com.khomishchak.cryptopricingservice.model.integration.ChangedPriceMessage
import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.model.integration.WhiteBitLastPriceUpdate
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import com.khomishchak.cryptopricingservice.utility.mapJsonResp
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

// TODO separate given service class to multiple services to reach single responsibility
@Service
class WhiteBitWebSocketService(val sessionMappingService: SessionMappingService)
    : IntegrationWebSocketService, WebSocketListener() {

    private val gson = Gson()

    // TODO: create a separate cache layer
    private var subscribers = ConcurrentHashMap<String, MutableList<Long>>()
    private var subscribedTickers = mutableListOf<String>()
    private var initialCurrencies = ConcurrentHashMap<String, MutableList<Long>>()

    private var pricesCache = mutableMapOf<String, Double>()

    private val logger = KotlinLogging.logger {}

    private lateinit var webSocket: WebSocket

    override fun getCryptoExchangerType(): CryptoExchanger = CryptoExchanger.WHITE_BIT

    override fun connect(client: OkHttpClient) {
        val request = Request.Builder()
                .url(WHITEBIT_WEBSOCKET_CONNECT_URL)
                .build()

        client.newWebSocket(request, this)
    }

    override fun subscribe(accountId: Long, subscriptionDetails: MarkerSubscriptionDetails) {
        var shouldReloadWsConnection = addInitialCurrencyOrAccountAsSubscriber(subscriptionDetails.initialCurrency, accountId)
        subscriptionDetails.tickers.forEach { ticker ->
            if (addNewTickerOrAccountAsSubscriber(ticker, accountId) && !shouldReloadWsConnection) {
                shouldReloadWsConnection = true
            }
        }
        if (shouldReloadWsConnection) updateSubscribedTickerForConnection()
    }

    private fun addInitialCurrencyOrAccountAsSubscriber(initialCurrency: String, accountId: Long) =
            addNewTickerOrAccountAsSubscriberToMap(initialCurrency, accountId, initialCurrencies)


    private fun addNewTickerOrAccountAsSubscriber(ticker: String, accountId: Long) =
            addNewTickerOrAccountAsSubscriberToMap(ticker, accountId, subscribers)


    private fun addNewTickerOrAccountAsSubscriberToMap(ticker: String, accountId: Long,
                                  subscriptionMap: ConcurrentHashMap<String, MutableList<Long>>) : Boolean{
        val isNew = subscriptionMap[ticker] == null
        ticker.let {
            subscriptionMap.getOrPut(it) { mutableListOf(accountId) }
            addNewTicker(it, accountId, subscriptionMap)
        }
        return isNew
    }

    private fun addNewTicker(ticker: String, accoutId: Long, subscriptionMap: ConcurrentHashMap<String, MutableList<Long>>) {
        subscriptionMap[ticker] = mutableListOf(accoutId)
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
                "method" to "lastprice_subscribe",
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

    private fun handleUpdateMessage(text: String) {
        gson.mapJsonResp<WhiteBitLastPriceUpdate>(text).takeIf { it.method == "lastprice_update" }
                ?.let {
                    formatAndSendMessage(it)
                } ?: logger.info("received unpredicted message from WhiteBit WS: $text")
    }

    private fun formatAndSendMessage (message: WhiteBitLastPriceUpdate) =
            mapRespToChangedPriceMessage(message).also {
                updateLatestPrices(it)
                notifyAccounts(it)
            }

    private fun updateLatestPrices(update: ChangedPriceMessage) = pricesCache.put(update.ticker, update.lastPrice)

    private fun getTickerFromResp(tickerWithPrefix: String) = tickerWithPrefix.let {
        it.substringBefore("_").let {
            prefix -> if (prefix != "USDT") prefix else it.substringAfter("_")
        }
    }

    private fun notifyAccounts(update: ChangedPriceMessage) {
        subscribers[update.ticker]?.let { notifyAccountsForTicker(it, update) }
        notifyAccountAboutInitialCurrencyUpdate(update)
    }

    private fun notifyAccountAboutInitialCurrencyUpdate(update: ChangedPriceMessage) =
        initialCurrencies[update.ticker]?.let {
            update.ticker = "INITIAL_CURRENCY"
            notifyAccountsForTicker(it, update)
        }


    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        logger.info("established connection with WhiteBit WS successfully!")
        updateSubscribedTickerForConnection()
    }

    private fun notifyAccountsForTicker(accountsToBeNotified: MutableList<Long>, update: ChangedPriceMessage) =
            accountsToBeNotified.forEach { sessionMappingService.sendMessageToSession(it, gson.toJson(update))}

    private fun mapRespToChangedPriceMessage(update: WhiteBitLastPriceUpdate) =
            ChangedPriceMessage(getTickerFromResp(update.params[0]), update.params[1].toDouble(), CryptoExchanger.WHITE_BIT)
}