package com.khomishchak.cryptopricingservice.service.ws.message.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.khomishchak.cryptopricingservice.model.AUTHENTICATE_TOKEN_URL
import com.khomishchak.cryptopricingservice.model.auth.AuthenticationResp
import com.khomishchak.cryptopricingservice.model.auth.AuthenticationResult
import com.khomishchak.cryptopricingservice.model.auth.JwtTokenValidationResult
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import com.khomishchak.cryptopricingservice.service.ws.message.WsMessageResolver
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.socket.WebSocketSession

@Service
class AuthenticationMessageHandler(@Qualifier("pricingServiceRestTemplate") private val restTemplate: RestTemplate,
                                   private val gson: Gson, private val sessionMappingService: SessionMappingService)
                                    : WsMessageResolver {

    override fun getMessage() = "authenticate"

    override fun process(messageJson: JsonObject, session: WebSocketSession) {
        runCatching {
            validateJwtToken(messageJson["jwt"].asString)
        }.onSuccess {
            handleSuccessfulAuthenticationAttempt(it, session)
        }.onFailure {
            handleFailedTokenValidation(session)
        }
    }

    private fun handleSuccessfulAuthenticationAttempt(result: JwtTokenValidationResult, session: WebSocketSession) =
            result.takeIf { it.validated }
                    ?.let{ handleSuccessfulTokenValidation(it.userId, session) }
                    ?: handleFailedTokenValidation(session)


    private fun handleSuccessfulTokenValidation(accountId: Long, session: WebSocketSession) =
        sessionMappingService.registerSession(session, accountId).let {
            sessionMappingService.sendMessageToSession(accountId, gson.toJson(AuthenticationResp(AuthenticationResult.AUTHENTICATED)))
        }

    private fun handleFailedTokenValidation(session: WebSocketSession) =
        sessionMappingService.sendMessageToSession(session, gson.toJson(AuthenticationResp(AuthenticationResult.UNAUTHENTICATED)))


    private fun validateJwtToken(jwt: String): JwtTokenValidationResult =
        HttpEntity(null, createAuthHeader(jwt)).let {
            restTemplate.postForObject(
                    AUTHENTICATE_TOKEN_URL,
                    it,
                    JwtTokenValidationResult::class.java
            ) ?: JwtTokenValidationResult(0L, false)
        }

    private fun createAuthHeader(jwt: String): HttpHeaders =
        HttpHeaders().apply {
            set(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
            contentType = MediaType.APPLICATION_JSON
        }

}