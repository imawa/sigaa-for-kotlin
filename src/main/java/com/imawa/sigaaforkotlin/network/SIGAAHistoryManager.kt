package com.imawa.sigaaforkotlin.network

import com.imawa.sigaaforkotlin.models.Disciplina
import okhttp3.Response
import timber.log.Timber

class SIGAAHistoryManager(private val parser: SIGAAParser) {
    var lastResponse: Response? = null
    var lastBody: String? = null
    var lastJavaxViewState: String? = null

    var currentDisciplina: Disciplina? = null
    var lastDisciplinaResponse: Response? = null
    var lastDisciplinaBody: String? = null
    var lastDisciplinaJavaxViewState: String? = null

    fun addToHistory(response: Response) {
        // Última página
        lastResponse = response
        lastBody = response.body!!.string()
        lastJavaxViewState = parser.getJavaxViewState(lastBody!!)
        Timber.d("javax.ViewState mais recente: $lastJavaxViewState")

        // Histórico do portal da disciplina
        if (parser.isPortalDisciplina(lastBody!!)) {
            lastDisciplinaResponse = lastResponse
            lastDisciplinaBody = lastBody
            lastDisciplinaJavaxViewState = lastJavaxViewState
        }
    }

    fun clearHistory() {
        clearDisciplinaAtual()
        lastJavaxViewState = null
        lastResponse = null
        lastBody = null
    }

    fun clearDisciplinaAtual() {
        lastDisciplinaResponse = null
        currentDisciplina = null
        lastDisciplinaBody = null
        lastDisciplinaJavaxViewState = null
    }
}
