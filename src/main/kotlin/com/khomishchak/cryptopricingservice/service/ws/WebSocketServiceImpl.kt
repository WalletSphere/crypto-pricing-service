package com.khomishchak.cryptopricingservice.service.ws

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.model.internall.exchanger.UsedToken
import com.khomishchak.cryptopricingservice.model.internall.exchanger.UsedTokens
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService
import okhttp3.OkHttpClient
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@Service
class WebSocketServiceImpl (private val integrationWebSocketConnectors: List<IntegrationWebSocketService>)
    : WebSocketService {

    private val client = OkHttpClient()
    val restTemplate = RestTemplate()

    private val websocketIntegrations: Map<CryptoExchanger, IntegrationWebSocketService> =
            integrationWebSocketConnectors.associateBy { it.getCryptoExchangerType() }

    init {
        subscribeToAlreadyFollowedTickers()
        integrationWebSocketConnectors.forEach { it.connect(client) }
    }

    override fun subscribe(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) =
            websocketIntegrations[exchanger]?.subscribe(accountId, tickers) ?: Unit

    override fun getLastPrices(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) =
            websocketIntegrations[exchanger]!!.getLastPrices(accountId, tickers)

    private fun subscribeToAlreadyFollowedTickers() {
        val response = restTemplate.getForObject<String>("http://localhost:8080/exchangers/used-currencies")
        val type = object : TypeToken<List<UsedToken>>() {}.type
        val data = Gson().fromJson<List<UsedToken>>(response, type)

        UsedTokens(data).records.forEach {
            websocketIntegrations[CryptoExchanger.valueOf(it.code)]
                    ?.subscribeToAlreadyFollowedTickers(it.currencies.split(", ").toList())
        }
    }
}