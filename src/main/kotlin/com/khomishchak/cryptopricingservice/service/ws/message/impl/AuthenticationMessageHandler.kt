package com.khomishchak.cryptopricingservice.service.ws.message.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.khomishchak.cryptopricingservice.model.AUTHENTICATE_TOKEN_URL
import com.khomishchak.cryptopricingservice.model.auth.AuthenticationResultResp
import com.khomishchak.cryptopricingservice.model.auth.AuthenticationStatus
import com.khomishchak.cryptopricingservice.model.auth.JwtTokenValidationResult
import com.khomishchak.cryptopricingservice.service.ws.SessionMappingService
import com.khomishchak.cryptopricingservice.service.ws.message.WsMessageResolver
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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
            handleInternalErrorDuringTokenValidation(session)
        }
    }

    private fun handleSuccessfulAuthenticationAttempt(result: JwtTokenValidationResult, session: WebSocketSession) =
            result.takeIf { it.validated }
                    ?.let{ handleSuccessfulTokenValidation(it.userId, session) }
                    ?: handleFailedTokenValidation(session)


    private fun handleSuccessfulTokenValidation(accountId: Long, session: WebSocketSession) {
        sessionMappingService.registerSession(session, accountId)
        AuthenticationResultResp(HttpStatus.OK, AuthenticationStatus.AUTHENTICATED).let {
            sessionMappingService.sendMessageToSession(accountId, gson.toJson(it))
        }
    }

    private fun handleFailedTokenValidation(session: WebSocketSession) {
        AuthenticationResultResp(HttpStatus.UNAUTHORIZED, AuthenticationStatus.UNAUTHENTICATED).let {
            sessionMappingService.sendMessageToSession(session, gson.toJson(it))
        }
    }

    private fun handleInternalErrorDuringTokenValidation(session: WebSocketSession) {
        AuthenticationResultResp(HttpStatus.INTERNAL_SERVER_ERROR, AuthenticationStatus.UNAUTHENTICATED).let {
            sessionMappingService.sendMessageToSession(session, gson.toJson(it))
        }
    }

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