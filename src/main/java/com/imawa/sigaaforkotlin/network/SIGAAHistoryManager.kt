package com.imawa.sigaaforkotlin.network

import com.imawa.sigaaforkotlin.models.Disciplina
import okhttp3.Response
import timber.log.Timber

class SIGAAHistoryManager(private val parser: SIGAAParser) {
    var lastJavaxViewState: String = "j_id1"

    var currentDisciplina: Disciplina? = null

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
        Timber.d("javax.ViewState mais recente: $lastJavaxViewState")
    }

    fun getLastResponse(): Response = responseHistory[responseHistory.size - 1]

    fun getLastPageBodyString(): String = bodyStringHistory[bodyStringHistory.size - 1]

    fun getLastDisciplinaPageBodyString(): String {
        for (i in maximumHistorySize - 1 downTo 0) {
            if (bodyStringHistory[i].contains("linkNomeTurma")) {
                return bodyStringHistory[i]
            }
        }
        // Se a API chamar esse método, provavelmente não vai chegar a esse segundo return
        return getLastPageBodyString()
    }

    fun clearHistory() {
        lastJavaxViewState = "j_id1"
        responseHistory.clear()
        bodyStringHistory.clear()
    }

    companion object {
        const val maximumHistorySize = 3
    }
}