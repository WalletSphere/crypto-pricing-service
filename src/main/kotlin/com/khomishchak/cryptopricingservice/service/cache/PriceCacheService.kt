package com.khomishchak.cryptopricingservice.service.cache

import com.khomishchak.cryptopricingservice.model.integration.ChangedPriceMessage
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import org.springframework.stereotype.Service

data class LastPriceDTO(val ticker: String, var lastPrice: Double)

@Service
class PriceCacheService {

    private var pricesCache = mutableMapOf<CryptoExchanger, MutableList<LastPriceDTO>>()

    fun getLastPrices(tickers: List<String>, exchanger: CryptoExchanger): Map<String, Double> =
            pricesCache[exchanger]?.filter { tickers.contains(it.ticker) }
                    ?.associate { it.ticker to it.lastPrice } ?: emptyMap()


    fun updateLastPrice(update: ChangedPriceMessage) {
        val existingPrices = pricesCache.getOrPut(update.exchanger) { mutableListOf() }
        val tickerIndex = existingPrices.indexOfFirst { it.ticker == update.ticker }

        if (tickerIndex != -1) {
            existingPrices[tickerIndex].lastPrice = update.lastPrice
        } else {
            existingPrices.add(LastPriceDTO(update.ticker, update.lastPrice))
        }
    }

}