package com.khomishchak.cryptopricingservice.service.integration

import okhttp3.OkHttpClient
import com.khomishchak.cryptopricingservice.service.model.integration.IntegrationType


interface IntegrationWebSocketService {
    fun getIntegrationType(): IntegrationType
    fun connect(client: OkHttpClient)
}