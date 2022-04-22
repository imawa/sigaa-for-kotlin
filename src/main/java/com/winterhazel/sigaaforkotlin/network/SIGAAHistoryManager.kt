package com.winterhazel.sigaaforkotlin.network

import com.winterhazel.sigaaforkotlin.entities.Disciplina
import okhttp3.Response
import timber.log.Timber

/**
 * Classe que gerencia as últimas páginas acessadas e guarda o último JSESSIONID.
 *
 * De maneira geral, essa classe possibilita acelerar as consultas ao SIGAA ao remover a necessidade
 * de reabrir páginas já abertas, principalmente as páginas do portal da disciplina.
 *
 * Por exemplo, se você acessar o portal de uma disciplina e, em seguida, acessar uma página do
 * portal do discente, o SIGAA ainda vai acreditar que você está com a última página do portal da
 * disciplina aberto. Assim, mesmo com a última página acessada sendo do portal do discente, é
 * possível fazer uma solicitação com os parâmetros da última página do portal da disciplina para
 * obter diretamente uma outra página do portal da disciplina sem a necessidade de abrir a turma
 * virtual novamente.
 */
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
