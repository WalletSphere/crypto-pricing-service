package com.khomishchak.cryptopricingservice.model.auth

import org.springframework.http.HttpStatusCode

data class AuthenticationResultResp(val statusCode: HttpStatusCode, val authenticationStatus: AuthenticationStatus,
                                    val errorMessage: String = "")
