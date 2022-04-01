package com.imawa.sigaaforkotlin.network

import android.content.Context
import com.imawa.sigaaforkotlin.network.SIGAAException.Companion.INTERNET_INDISPONIVEL
import com.imawa.sigaaforkotlin.network.SIGAAException.Companion.RESPOSTA_INVALIDA
import com.imawa.sigaaforkotlin.network.SIGAAException.Companion.SESSAO_EXPIRADA
import com.imawa.sigaaforkotlin.network.Utils.Companion.isConnectionOn
import com.imawa.sigaaforkotlin.network.Utils.Companion.isInternetAvailable
import com.imawa.sigaaforkotlin.network.Utils.Companion.isValidResponse
import okhttp3.Interceptor
import okhttp3.Response

class SIGAAInterceptor(private val context: Context, private val parser: SIGAAParser) :
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
