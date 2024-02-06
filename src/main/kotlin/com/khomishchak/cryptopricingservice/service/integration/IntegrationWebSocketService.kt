package com.khomishchak.cryptopricingservice.service.integration

import com.khomishchak.cryptopricingservice.model.MarkerSubscriptionDetails
import okhttp3.OkHttpClient
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger


interface IntegrationWebSocketService {
    fun getCryptoExchangerType(): CryptoExchanger
    fun connect(client: OkHttpClient)
    fun subscribe(accountId: Long, subscriptionDetails: MarkerSubscriptionDetails)
    fun getLastPrices(accountId: Long, tickers: List<String>): Map<String, Double>
    fun subscribeToAlreadyFollowedTickers(currencies: List<String>)
}