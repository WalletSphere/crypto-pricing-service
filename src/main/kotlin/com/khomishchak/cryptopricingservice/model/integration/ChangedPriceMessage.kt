package com.khomishchak.cryptopricingservice.model.integration

data class ChangedPriceMessage(var ticker: String, val lastPrice: Double, val exchanger: CryptoExchanger)
