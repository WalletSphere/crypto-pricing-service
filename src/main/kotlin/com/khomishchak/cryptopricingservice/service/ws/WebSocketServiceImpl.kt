package com.khomishchak.cryptopricingservice.service.ws

import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import okhttp3.OkHttpClient
import org.springframework.stereotype.Service
import com.khomishchak.cryptopricingservice.service.integration.IntegrationWebSocketService

@Service
class WebSocketServiceImpl (private val integrationWebSocketConnectors: List<IntegrationWebSocketService>)
    : WebSocketService {

    private val client = OkHttpClient()

    init {
        integrationWebSocketConnectors.forEach { connector -> connector.connect(client) }
    }

    override fun subscribe(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>) {
        integrationWebSocketConnectors
                .filter { connector -> exchanger == connector.getCryptoExchangerType() }[0].subscribe(accountId, tickers)
    }

}