package com.khomishchak.cryptopricingservice.model.auth

data class JwtTokenValidationResult(val userId: Long, val validated: Boolean)
