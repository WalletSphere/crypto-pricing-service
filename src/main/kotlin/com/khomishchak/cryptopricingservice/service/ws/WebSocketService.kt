package com.khomishchak.cryptopricingservice.service.ws

import com.khomishchak.cryptopricingservice.model.MarkerSubscriptionDetails
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger

interface WebSocketService {
    fun subscribe(accountId: Long, subscriptionDetails: MarkerSubscriptionDetails)
    fun getLastPrices(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>): Map<String, Double>
}