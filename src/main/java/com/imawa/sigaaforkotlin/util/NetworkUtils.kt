package com.imawa.sigaaforkotlin.util

import okhttp3.Response

class NetworkUtils {
    companion object {
        fun checkValidResponse(response: Response?): Boolean {
            if (response?.isSuccessful == true) {
                return if (response.priorResponse?.isRedirect == true) {
                    response.priorResponse?.headers?.get("Location")?.contains("manutencao.html")
                        ?: false
                } else {
                    true
                }
            }
            return false
        }
    }
}