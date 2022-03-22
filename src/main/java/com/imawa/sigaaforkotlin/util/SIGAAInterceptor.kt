package com.imawa.sigaaforkotlin.util

import android.content.Context
import com.imawa.sigaaforkotlin.util.NetworkUtils.Companion.isConnectionOn
import com.imawa.sigaaforkotlin.util.NetworkUtils.Companion.isInternetAvailable
import com.imawa.sigaaforkotlin.util.NetworkUtils.Companion.isValidResponse
import com.imawa.sigaaforkotlin.util.SIGAAException.Companion.INTERNET_INDISPONIVEL
import com.imawa.sigaaforkotlin.util.SIGAAException.Companion.RESPOSTA_INVALIDA
import com.imawa.sigaaforkotlin.util.SIGAAException.Companion.SESSAO_EXPIRADA
import okhttp3.Interceptor
import okhttp3.Response

class SIGAAInterceptor(private val context: Context, private val parser: Parser) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Conferir internet
        if (!isConnectionOn(context) || !isInternetAvailable()) {
            throw SIGAAException(INTERNET_INDISPONIVEL)
        }

        // Realizar a request
        val response = chain.proceed(request)

        if (isValidResponse(response)) {
            // Conferir se a sessão expirou
            if (parser.getLocation(response)?.contains("expirada.jsp") == true) {
                throw SIGAAException(SESSAO_EXPIRADA)
            }
        } else {
            // SIGAA em manutenção ou resposta inválida
            throw SIGAAException(RESPOSTA_INVALIDA)
        }

        return response
    }
}