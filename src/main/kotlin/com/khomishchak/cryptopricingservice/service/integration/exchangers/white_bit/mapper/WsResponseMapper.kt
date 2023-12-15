package com.khomishchak.cryptopricingservice.service.integration.exchangers.white_bit.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.khomishchak.cryptopricingservice.model.integration.WhiteBitWSMarketResp
import org.springframework.stereotype.Component

@Component
class WsResponseMapper {

    val gson = Gson()

    fun mapRespToMarketUpdate(json: String): WhiteBitWSMarketResp {
        val marketUpdateType = object : TypeToken<WhiteBitWSMarketResp>() {}.type
        return gson.fromJson(json, marketUpdateType)
    }
}