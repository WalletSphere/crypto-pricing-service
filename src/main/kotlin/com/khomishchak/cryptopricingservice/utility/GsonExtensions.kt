package com.khomishchak.cryptopricingservice.utility

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.mapJsonResp(json: String): T =
        this.fromJson(json, object : TypeToken<T>() {}.type)