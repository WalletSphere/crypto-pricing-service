package com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit

import com.google.gson.Gson
import com.khomishchak.cryptopricingservice.model.MarkerSubscriptionDetails
import com.khomishchak.cryptopricingservice.model.WHITEBIT_WEBSOCKET_CONNECT_URL
import com.khomishchak.cryptopricingservice.model.integration.ChangedPriceMessage
import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.model.integration.WhiteBitLastPriceUpdate
import com.khomishchak.cryptopricingservice.service.cache.PriceCacheService
import com.khomishchak.cryptopricingservice.service.cache.SubscriptionCacheService
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import com.khomishchak.cryptopricingservice.utility.mapJsonResp
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import okhttp3.Response

@Service
class WhiteBitWebSocketService(private val sessionMappingService: SessionMappingService,
                               private val priceCacheService: PriceCacheService,
                               private val subscriptionCacheService: SubscriptionCacheService, private val gson: Gson)
    : IntegrationWebSocketService, WebSocketListener() {

    private val logger = KotlinLogging.logger {}

    private lateinit var webSocket: WebSocket

    override fun getCryptoExchangerType(): CryptoExchanger = CryptoExchanger.WHITE_BIT

    override fun connect(client: OkHttpClient) {
        val request = Request.Builder().url(WHITEBIT_WEBSOCKET_CONNECT_URL).build()
        client.newWebSocket(request, this)
    }

    override fun subscribe(accountId: Long, subscriptionDetails: MarkerSubscriptionDetails) {
        var addedNewInitCurrency = subscriptionCacheService.subscribeToInitialCurrency(accountId, subscriptionDetails.initialCurrency, CryptoExchanger.WHITE_BIT)
        var addedNewTicker = subscriptionCacheService.subscribeToTickers(accountId, subscriptionDetails.tickers, CryptoExchanger.WHITE_BIT)

        if (addedNewInitCurrency || addedNewTicker) {
            updateSubscribedTickerForConnection(subscriptionCacheService.getSubscribedTickers(CryptoExchanger.WHITE_BIT))
        }
    }

    override fun subscribeToAlreadyFollowedTickers(currencies: List<String>) =
            currencies.forEach { subscriptionCacheService.subscribeToAlreadyUsedTicker(it, CryptoExchanger.WHITE_BIT) }


    override fun getLastPrices(tickers: List<String>) = priceCacheService.getLastPrices(tickers, CryptoExchanger.WHITE_BIT)

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
            logger.error("got failure for WhiteBit WS connection, error: ${t.message}")

    override fun onMessage(webSocket: WebSocket, text: String) = handleUpdateMessage(text)

    private fun handleUpdateMessage(text: String) {
        gson.mapJsonResp<WhiteBitLastPriceUpdate>(text).takeIf { it.method == "lastprice_update" }
                ?.let { formatAndSendMessage(it) }
                ?: logger.info("received unpredicted message from WhiteBit WS: $text")
    }

    private fun formatAndSendMessage (message: WhiteBitLastPriceUpdate) =
            mapRespToChangedPriceMessage(message).also {
                priceCacheService.updateLastPrice(it)
                notifyAccounts(it)
            }

    private fun getTickerFromResp(tickerWithPrefix: String) = tickerWithPrefix.let {
        it.substringBefore("_").let {
            prefix -> if (prefix != "USDT") prefix else it.substringAfter("_")
        }
    }

    private fun notifyAccounts(update: ChangedPriceMessage) {
        subscriptionCacheService.getSubscriberIdsForTicker(update.ticker, CryptoExchanger.WHITE_BIT)?.let { notifyAccountsForTicker(it, update) }
        notifyAccountAboutInitialCurrencyUpdate(update)
    }

    private fun notifyAccountAboutInitialCurrencyUpdate(update: ChangedPriceMessage) =
        subscriptionCacheService.getSubscriberIdsIntiCurrency(update.ticker, CryptoExchanger.WHITE_BIT)?.let {
            update.ticker = "INITIAL_CURRENCY"
            notifyAccountsForTicker(it, update)
        }


    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        logger.info("established connection with WhiteBit WS successfully!")
        updateSubscribedTickerForConnection(subscriptionCacheService.getSubscribedTickers(CryptoExchanger.WHITE_BIT))
    }

    private fun notifyAccountsForTicker(accountsToBeNotified: MutableList<Long>, update: ChangedPriceMessage) =
            accountsToBeNotified.forEach { sessionMappingService.sendMessageToSession(it, gson.toJson(update))}

    private fun mapRespToChangedPriceMessage(update: WhiteBitLastPriceUpdate) =
            ChangedPriceMessage(getTickerFromResp(update.params[0]), update.params[1].toDouble(), CryptoExchanger.WHITE_BIT)

    private fun updateSubscribedTickerForConnection(subscribedTickers: MutableList<String>) {
        val subscribeMessage = mapOf(
                "id" to 737457,
                "method" to "lastprice_subscribe",
                "params" to subscribedTickers
        )
        webSocket.send(gson.toJson(subscribeMessage))
        logger.info("WhiteBit WS connection was updated")
    }
}