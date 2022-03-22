package com.imawa.sigaaforkotlin.util

import com.imawa.sigaaforkotlin.SIGAA.Companion.urlBase
import com.imawa.sigaaforkotlin.sigaa.Disciplina
import com.imawa.sigaaforkotlin.sigaa.Usuario
import okhttp3.Response
import org.jsoup.Jsoup

class Parser {
    fun getSessionId(response: Response): String? =
        response.header("Set-Cookie")?.split("JSESSIONID=")?.get(1)?.split(";")?.get(0)

    fun getLocation(response: Response): String? = response.priorResponse?.header("Location")

    fun getJavaxViewState(body: String): String? {
        val document = Jsoup.parse(body)
        return document.body().getElementById("javax.faces.ViewState")?.attr("value")
    }

    fun getLogado(body: String): Boolean {
        val document = Jsoup.parse(body)
        return document.getElementsByClass("usuario").size > 0 || document.getElementById("painelDadosUsuario") != null
    }

    fun getUsuarioPortalDiscente(body: String, login: String): Usuario {
        val document = Jsoup.parse(body)

        val matricula = Integer.parseInt(
            document.body().getElementById("perfil-docente")!!.child(4).child(1).child(0).child(0)
                .child(1).text()
        )
        val nome = document.body().getElementsByClass("nome")[0].text()
        val email = "" // O portal do discente não mostra o email completo
        val urlAvatar = "${urlBase}${
            document.getElementsByClass("foto")[0].child(0).attr("src").removePrefix("/sigaa")
        }"

        // Disciplinas
        val disciplinasPeriodoAtual = ArrayList<Disciplina>()
        val periodoAtual =
            document.getElementsByClass("periodo-atual")[0].getElementsByTag("strong")[0].text()

        for (element in document.body().getElementsByClass("descricao")) {
            val child = element.child(0).child(1)
            val outerHtmlArgs = child.outerHtml().split("'")

            val id = null
            val nome = child.html()
            val formAcessarTurmaVirtual = outerHtmlArgs[3]
            val formAcessarTurmaVirtualCompleto = outerHtmlArgs[5]
            val frontEndIdTurma = outerHtmlArgs[11]

            disciplinasPeriodoAtual.add(
                Disciplina(
                    id,
                    nome,
                    periodoAtual,
                    formAcessarTurmaVirtual,
                    formAcessarTurmaVirtualCompleto,
                    frontEndIdTurma
                )
            )
        }

        return Usuario(login, matricula, nome, email, urlAvatar, disciplinasPeriodoAtual)
    }
}