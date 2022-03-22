package com.imawa.sigaaforkotlin.network

class SIGAAException(message: String?) : Exception(message) {
    companion object {
        const val INTERNET_INDISPONIVEL = "Internet indisponível"

        const val RESPOSTA_INVALIDA = "SIGAA em manutenção / resposta inválida"

        const val SESSAO_EXPIRADA = "Sessão expirada"
    }
}