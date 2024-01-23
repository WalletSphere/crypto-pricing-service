package com.khomishchak.cryptopricingservice.service.ws.message

import com.google.gson.JsonObject
import org.springframework.web.socket.WebSocketSession

interface WsMessageResolver {

    fun getMessage(): String

    fun process(messageJson: JsonObject, session: WebSocketSession)
}