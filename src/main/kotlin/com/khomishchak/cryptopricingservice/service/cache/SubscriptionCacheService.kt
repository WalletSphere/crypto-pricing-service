package com.khomishchak.cryptopricingservice.service.cache

import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit.shouldBeMappedTickets
import com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit.stablecoins
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

data class Subscription(val ticker: String, val subscribers: MutableList<Long>)

@Service
class SubscriptionCacheService {

    private var subscribers = ConcurrentHashMap<CryptoExchanger, MutableList<Subscription>>()
    private var subscribedTickers = ConcurrentHashMap<CryptoExchanger, MutableList<String>>()
    private var initialCurrencies = ConcurrentHashMap<CryptoExchanger, MutableList<Subscription>>()

    /**
     * add user's tickers to local cache
     *
     *
     * @return if at least 1 new ticker was added to the cache
     */
    fun subscribeToTickers(accountId: Long, tickers: List<String>, exchanger: CryptoExchanger): Boolean{
        var updatedSubscribedTickers = false

        tickers.forEach { ticker ->
            if (addNewTickerOrAccountAsSubscriber(exchanger, ticker, accountId, subscribers) && !updatedSubscribedTickers) {
                updatedSubscribedTickers = true
            }
        }

        return updatedSubscribedTickers
    }

    /**
     * add user's initial currency to local cache
     *
     *
     * @return if new initial currency was added to cache
     */
    fun subscribeToInitialCurrency(accountId: Long, initialCurrency: String, exchanger: CryptoExchanger) =
            addNewTickerOrAccountAsSubscriber(exchanger, initialCurrency, accountId, initialCurrencies)

    fun subscribeToAlreadyUsedTicker(ticker: String, exchanger: CryptoExchanger) =
            addNewTickerToSubscribedTickers(exchanger, ticker)

    fun getSubscribedTickers(exchanger: CryptoExchanger) = subscribedTickers[exchanger] ?: mutableListOf()

    fun getSubscriberIdsForTicker(ticker: String, exchanger: CryptoExchanger) =
            getSubscriberIds(ticker, exchanger, subscribers)


    fun getSubscriberIdsIntiCurrency(initCurrency: String, exchanger: CryptoExchanger) =
            getSubscriberIds(initCurrency, exchanger, initialCurrencies)

    private fun getSubscriberIds(ticker: String, exchanger: CryptoExchanger,
                                 subscriptionMap: ConcurrentHashMap<CryptoExchanger, MutableList<Subscription>>) =
            subscriptionMap[exchanger]?.find { it.ticker == ticker }?.subscribers

    private fun addNewTickerOrAccountAsSubscriber(exchanger: CryptoExchanger, ticker: String, accountId: Long,
                                                  subscriptionMap: ConcurrentHashMap<CryptoExchanger, MutableList<Subscription>>): Boolean {
        subscriptionMap.getOrPut(exchanger) { mutableListOf() }
                .let {
                    val subscription = it.find { it.ticker == ticker }

                    if (subscription == null) {
                        subscribedNewTicker(subscriptionMap, exchanger, ticker, accountId)
                        return true
                    }

                    if (!subscription.subscribers.contains(accountId)) {
                        subscription.subscribers.add(accountId)
                    }
                    return false
                }
    }

    private fun subscribedNewTicker(subscriptionMap: ConcurrentHashMap<CryptoExchanger, MutableList<Subscription>>,
                                    exchanger: CryptoExchanger, ticker: String, accountId: Long) {
        subscriptionMap[exchanger]?.add(Subscription(ticker, mutableListOf(accountId)))
        addNewTickerToSubscribedTickers(exchanger, ticker)
    }

    private fun addNewTickerToSubscribedTickers(exchanger: CryptoExchanger, ticker: String) =
            getUsdtPairForTicker(ticker).let {
                subscribedTickers.getOrPut(exchanger) { mutableListOf() }.add(it)
            }

    private fun getUsdtPairForTicker(ticker: String): String =
            (shouldBeMappedTickets[ticker] ?: ticker)
                .let { if (it in stablecoins) "USDT_$it" else "${it}_USDT" }
}