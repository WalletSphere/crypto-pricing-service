package com.khomishchak.cryptopricingservice.model.integration

data class ChangedPriceMessage(val ticker: String, val lastPrice: Double, val exchanger: CryptoExchanger)
