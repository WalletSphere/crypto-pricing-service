package com.khomishchak.cryptopricingservice.service.ws

import com.google.gson.Gson
import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.model.internall.exchanger.UsedToken
import com.khomishchak.cryptopricingservice.model.internall.exchanger.UsedTokens
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService
import com.khomishchak.cryptopricingservice.utility.mapJsonResp
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

const val GET_USED_CURRENCIES_URL = "http://localhost:8080/exchangers/used-currencies"
@Service
class WebSocketServiceImpl (private val integrationWebSocketConnectors: List<IntegrationWebSocketService>,
                            @Qualifier("pricingServiceRestTemplate") private val restTemplate: RestTemplate)
    : WebSocketService {

    private val client = OkHttpClient()

    private val websocketIntegrations: Map<CryptoExchanger, IntegrationWebSocketService> =
            integrationWebSocketConnectors.associateBy { it.getCryptoExchangerType() }

    init {
        subscribeToAlreadyFollowedTickers()
        integrationWebSocketConnectors.forEach { it.connect(client) }
    }

    override fun subscribe(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) =
            websocketIntegrations[exchanger]?.subscribe(accountId, tickers) ?: Unit

    override fun getLastPrices(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) =
            websocketIntegrations[exchanger]?.getLastPrices(accountId, tickers) ?: emptyMap();

    private fun subscribeToAlreadyFollowedTickers() {
        val response = restTemplate.getForObject<String>(GET_USED_CURRENCIES_URL)
        val data = Gson().mapJsonResp<List<UsedToken>>(response)

        UsedTokens(data).records.forEach {
            websocketIntegrations[CryptoExchanger.valueOf(it.code)]
                    ?.subscribeToAlreadyFollowedTickers(it.currencies.split(", ").toList())
        }
    }
}