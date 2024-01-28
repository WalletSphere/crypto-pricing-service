package com.khomishchak.cryptopricingservice.model.auth

import com.khomishchak.cryptopricingservice.model.ErrorResp

data class JwtTokenValidationResult(val userId: Long, val validated: Boolean, val errorResp: ErrorResp? = null)
