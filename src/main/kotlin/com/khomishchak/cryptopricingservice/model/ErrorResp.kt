package com.khomishchak.cryptopricingservice.model

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

data class ErrorResp(
        @JsonProperty("errorType") val errorType: String,
        @JsonProperty("errorMessage") val errorMessage: String,
        @JsonProperty("details") val details: MutableList<ErrorDetail> = mutableListOf()
) {
    data class ErrorDetail(
            var key: String = "",
            var value: Any? = null
    ) {
        @JsonAnySetter
        fun setDetail(key: String, value: Any) {
            this.key = key
            this.value = value
        }
    }
}
