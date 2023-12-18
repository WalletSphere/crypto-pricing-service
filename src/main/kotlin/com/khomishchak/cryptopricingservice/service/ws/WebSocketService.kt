package com.khomishchak.cryptopricingservice.service.ws

import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger

fun interface WebSocketService {
    fun subscribe(accountId: Long, exchanger: CryptoExchanger, tickers: List<String>)
}