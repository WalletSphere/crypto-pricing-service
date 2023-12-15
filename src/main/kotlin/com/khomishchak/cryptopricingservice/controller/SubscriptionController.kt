package com.khomishchak.cryptopricingservice.controller

import com.khomishchak.cryptopricingservice.model.integration.CryptoExchanger
import com.khomishchak.cryptopricingservice.service.ws.WebSocketService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class SubscriptionController(val webSocketService: WebSocketService) {

    @PostMapping("/subscribe/{accountId}/{token}")
    fun subscribe(@PathVariable accountId: Long, @PathVariable token: String) {
        webSocketService.subscribe(accountId, CryptoExchanger.WHITE_BIT, listOf(token))
    }

}