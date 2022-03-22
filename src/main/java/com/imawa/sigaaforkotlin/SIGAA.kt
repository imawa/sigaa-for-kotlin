package com.imawa.sigaaforkotlin

import com.imawa.sigaaforkotlin.sigaa.Usuario
import com.imawa.sigaaforkotlin.util.FormBuilder
import com.imawa.sigaaforkotlin.util.Parser
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SIGAA {
    private val client = OkHttpClient.Builder().connectTimeout(200, TimeUnit.SECONDS)
        .writeTimeout(200, TimeUnit.SECONDS).writeTimeout(600, TimeUnit.SECONDS).build()

    private val formBuilder = FormBuilder()
    private val parser = Parser()

    var sessionId: String? = null

    var usuario: Usuario? = null

    init {
        Timber.plant(Timber.DebugTree())
    }

    private fun networkGet(caminho: String): Response {
        val requestBuilder = Request.Builder().url(urlBase + caminho)
            .header("Content-Type", "application/x-www-form-urlencoded")

        if (sessionId != null) {
            requestBuilder.header("Cookie", "JSESSIONID=$sessionId")
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    private fun networkPost(caminho: String, formBody: FormBody): Response {
        val requestBuilder = Request.Builder().url("${urlBase}${caminho}")
            .header("Content-Type", "application/x-www-form-urlencoded").post(formBody)

        if (sessionId != null) {
            requestBuilder.addHeader("Cookie", "JSESSIONID=$sessionId")
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    fun login(login: String, senha: String): Boolean {
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
        var responseLogin =
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
            responseLogin = getPortalDiscente()
        }

        // Conferir se logou
        var bodyLogin = responseLogin.body!!.string()
        if (!parser.getLogado(bodyLogin)) {
            // Conferir o portal do discente
            Timber.d("Conferindo o login no portal do discente")

            responseLogin = getPortalDiscente()
            bodyLogin = responseLogin.body!!.string()

            if (!parser.getLogado(bodyLogin)) {
                Timber.d("Não foi possível logar")
                return false
            }
        }

        usuario = parser.getUsuarioPortalDiscente(bodyLogin, login)
        Timber.d("Logado como ${usuario!!.nome}")
        return true
    }

    private fun getPortalDiscente(): Response = networkGet("/verPortalDiscente.do")

    companion object {
        const val urlBase = "https://sig.ifc.edu.br/sigaa"
    }
}