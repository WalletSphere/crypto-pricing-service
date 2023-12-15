package com.khomishchak.cryptopricingservice.model.integration

data class WhiteBitWSMarketResp(val method: String, val params: List<Any>, val id: Any?)

data class TradingMetrics(val open: String, val close: String, val high: String, val low: String, val volume: String,
        val deal: String, val last: String, val period: Int)