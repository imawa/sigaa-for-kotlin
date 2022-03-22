package com.imawa.sigaaforkotlin

import android.content.Context
import com.imawa.sigaaforkotlin.models.Disciplina
import com.imawa.sigaaforkotlin.models.Usuario
import com.imawa.sigaaforkotlin.network.SIGAAFormBuilder
import com.imawa.sigaaforkotlin.network.SIGAAHistoryManager
import com.imawa.sigaaforkotlin.network.SIGAAInterceptor
import com.imawa.sigaaforkotlin.network.SIGAAParser
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SIGAA(context: Context) {
    var sessionId: String? = null

    var usuario: Usuario? = null

    private val formBuilder = SIGAAFormBuilder()
    private val parser = SIGAAParser()

    private val historyManager = SIGAAHistoryManager(parser)

    private val client = OkHttpClient.Builder().addInterceptor(SIGAAInterceptor(context, parser))
        .connectTimeout(200, TimeUnit.SECONDS).writeTimeout(200, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS).build()

    init {
        Timber.plant(Timber.DebugTree())
    }

    private fun networkGet(caminho: String): Response {
        val requestBuilder = Request.Builder().url(urlBase + caminho)
            .header("Content-Type", "application/x-www-form-urlencoded")

        if (sessionId != null) {
            requestBuilder.header("Cookie", "JSESSIONID=$sessionId")
        }

        historyManager.addToHistory(client.newCall(requestBuilder.build()).execute())
        return historyManager.getLastResponse()
    }

    private fun networkPost(caminho: String, formBody: FormBody): Response {
        val requestBuilder = Request.Builder().url("${urlBase}${caminho}")
            .header("Content-Type", "application/x-www-form-urlencoded").post(formBody)

        if (sessionId != null) {
            requestBuilder.addHeader("Cookie", "JSESSIONID=$sessionId")
        }

        historyManager.addToHistory(client.newCall(requestBuilder.build()).execute())
        return historyManager.getLastResponse()
    }

    fun login(login: String, senha: String): Boolean {
        historyManager.clearHistory()
        // Pegar novo sessionId
        sessionId = null

        val responseTelaLogin = networkGet("/verTelaLogin.do")
        sessionId = parser.getSessionId(responseTelaLogin)

        if (sessionId == null) {
            return false
        }
        Timber.d("Novo sessionId: $sessionId")

        // Logar sessionId
        val formLogin = formBuilder.buildLoginForm(login, senha)
        val responseLogin =
            networkPost("/logar.do", formLogin)

        val location = parser.getLocation(responseLogin)

        // Retornar false se for um docente
        if (location?.contains("vinculos.jsf") == true || location?.contains("verMenuPrincipal.do") == true) {
            Timber.d("Usuário logado é um docente")
            return false
        }

        // Pular telas de aviso e de questionários
        if (location?.contains("telaAvisoLogon.jsf") == true || location?.contains("questionarios.jsf") == true) {
            Timber.d("Pulando aviso ou questionário")
            getPortalDiscente()
        }

        // Conferir se logou
        if (!parser.getLogado(historyManager.getLastPageBodyString())) {
            // Conferir o portal do discente
            Timber.d("Conferindo o login no portal do discente")
            getPortalDiscente()

            if (!parser.getLogado(historyManager.getLastPageBodyString())) {
                Timber.d("Não foi possível logar")
                return false
            }
        }

        usuario = parser.getUsuarioPortalDiscente(historyManager.getLastPageBodyString(), login)
        Timber.d("Logado como ${usuario!!.nome}")

        return true
    }

    fun logout() {
        sessionId = null
        usuario = null
        historyManager.clearHistory()
    }

    fun getAllDisciplinas(): ArrayList<Disciplina> {
        networkGet("/portais/discente/turmas.jsf")
        return parser.getDisciplinasTodasAsTurmas(historyManager.getLastPageBodyString())
    }

    private fun getPortalDiscente(): Response {
        val response = networkGet("/verPortalDiscente.do")
        historyManager.addToHistory(response)
        return response
    }

    companion object {
        const val urlBase = "https://sig.ifc.edu.br/sigaa"
    }
}