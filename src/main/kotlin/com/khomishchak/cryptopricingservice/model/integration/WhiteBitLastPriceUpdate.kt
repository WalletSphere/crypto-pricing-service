package com.khomishchak.cryptopricingservice.model.integration

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


inline fun <reified T> Gson.mapRespToLastPriceUpdate(json: String): T =
        this.fromJson(json, object : TypeToken<T>() {}.type)

data class WhiteBitLastPriceUpdate(val method: String, val params: List<String>, val id: Any)
