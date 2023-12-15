package com.khomishchak.cryptopricingservice.service.ws

import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import okhttp3.OkHttpClient
import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService

@Service
class WebSocketServiceImpl (private val integrationWebSocketConnectors: List<IntegrationWebSocketService>)
    : WebSocketService {

    private val client = OkHttpClient()
    private val websocketIntegrations: Map<CryptoExchanger, IntegrationWebSocketService> =
            integrationWebSocketConnectors.associateBy { it.getCryptoExchangerType() }

    init {
        integrationWebSocketConnectors.forEach { connector -> connector.connect(client) }
    }

    override fun subscribe(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) {
        websocketIntegrations[exchanger]?.subscribe(accountId, tickers)
    }

}