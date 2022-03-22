package com.imawa.sigaaforkotlin.util

import okhttp3.Response

class HistoryManager(private val parser: Parser) {
    var lastJavaxViewState: String? = null

    val responseHistory = ArrayList<Response>()
    val bodyStringHistory = ArrayList<String>()

    fun addToHistory(response: Response) {
        responseHistory.add(response)
        bodyStringHistory.add(response.body!!.string())

        // Remover o mais antigo ao passar do tamanho maximo
        if (responseHistory.size > maximumHistorySize) {
            responseHistory.removeAt(0)
            bodyStringHistory.removeAt(0)
        }

        // Atualizar ultimo javaxViewState
        val javaxViewState = parser.getJavaxViewState(getLastPageBodyString())
        javaxViewState?.let { lastJavaxViewState = it }
    }

    fun getLastResponse(): Response = responseHistory[responseHistory.size - 1]

    fun getLastPageBodyString(): String = bodyStringHistory[bodyStringHistory.size - 1]

    companion object {
        const val maximumHistorySize = 3
    }
}