package com.khomishchak.cryptopricingservice.model.integration

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


inline fun <reified T> Gson.mapRespToMarketUpdate(json: String): T =
        this.fromJson(json, object : TypeToken<T>() {}.type)

data class WhiteBitWSMarketResp(
        val method: String,
        val params: List<Any>,
        val id: Any?
) {
    // Helper method to extract TradingMetrics if present
    fun getTradingMetrics(): TradingMetrics? {
        return if (params.size >= 2 && params[1] is Map<*, *>) {
            val metricsMap = params[1] as Map<*, *>
            TradingMetrics(
                    open = metricsMap["open"] as? String ?: "",
                    close = metricsMap["close"] as? String ?: "",
                    high = metricsMap["high"] as? String ?: "",
                    low = metricsMap["low"] as? String ?: "",
                    volume = metricsMap["volume"] as? String ?: "",
                    deal = metricsMap["deal"] as? String ?: "",
                    last = metricsMap["last"] as? String ?: "",
                    period = (metricsMap["period"] as? Number)?.toInt() ?: 0
            )
        } else {
            null
        }
    }
}

data class TradingMetrics(
        val open: String,
        val close: String,
        val high: String,
        val low: String,
        val volume: String,
        val deal: String,
        val last: String,
        val period: Int
)
