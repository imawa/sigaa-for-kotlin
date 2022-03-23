package com.imawa.sigaaforkotlin.network

import com.imawa.sigaaforkotlin.SIGAA.Companion.urlBase
import com.imawa.sigaaforkotlin.models.Avaliacao
import com.imawa.sigaaforkotlin.models.Disciplina
import com.imawa.sigaaforkotlin.models.Disciplina.Companion.PAGINA_AVALIACOES
import com.imawa.sigaaforkotlin.models.Usuario
import okhttp3.Response
import org.jsoup.Jsoup
import java.text.SimpleDateFormat


class SIGAAParser {
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

            val id = element.parent()!!.nextElementSibling()!!.child(0).id().replace("linha_", "")
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

    fun getDisciplinasTodasAsTurmas(body: String): ArrayList<Disciplina> {
        val document = Jsoup.parse(body)
        val bodyTabela = document.getElementsByClass("listagem")[0].getElementsByTag("tbody")[1]

        var periodoAtual = ""
        val disciplinas = ArrayList<Disciplina>()

        for (linha in bodyTabela.children()) {
            when (linha.className()) {
                "linhaPar", "linhaImpar" -> {
                    // Disciplina
                    val nome = linha.child(0).text()
                    val args = linha.getElementsByTag("a")[0].attr("onclick").split("'")
                    val formAcessarTurmaVirtual = args[3]
                    val formAcessarTurmaVirtualCompleto = args[5]
                    val frontEndIdTurma = args[11]
                    disciplinas.add(
                        Disciplina(
                            null,
                            nome,
                            periodoAtual,
                            formAcessarTurmaVirtual,
                            formAcessarTurmaVirtualCompleto,
                            frontEndIdTurma
                        )
                    )
                }
                "destaque no-hover" -> {
                    // Período
                    periodoAtual = linha.text()
                }
            }
        }

        return disciplinas
    }

    fun getArgsBotaoPortalDisciplina(body: String, pagina: Int): ArrayList<ArrayList<String>> {
        val document = Jsoup.parse(body)

        val primeiroPar = ArrayList<String>()
        val segundoPar = ArrayList<String>()

        // Texto do botão em questão
        val textoBotao = when (pagina) {
            PAGINA_AVALIACOES -> "Avaliações"
            else -> "Principal"
        }

        // Encontrar o botão na página
        val itensMenu = document.body().getElementsByClass("itemMenu")

        for (item in itensMenu) {
            if (item.text().equals(textoBotao)) {
                // Primeiro par
                primeiroPar.add(
                    item.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!
                        .parent()!!.parent()!!.id()
                )
                primeiroPar.add(
                    item.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!
                        .parent()!!.id()
                )

                // Segundo par
                for (string in item.parent()!!.outerHtml().split("'")) {
                    if (string.startsWith("formMenu:j_id_jsp_")) {
                        segundoPar.add(string)
                    }
                }
            }
        }

        return arrayListOf(primeiroPar, segundoPar)
    }

    fun getCaminhoBotaoPortalDisciplina(body: String): String =
        Jsoup.parse(body).getElementById("form_nee")!!.attr("action").replace("/sigaa", "")

    fun getAvaliacoesDisciplina(body: String, disciplina: Disciplina): ArrayList<Avaliacao> {
        val avaliacoes = ArrayList<Avaliacao>()

        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            val formatoData = SimpleDateFormat("dd/MM/yyyy HH:mm")

            for (linha in bodyTabela.getElementsByTag("tr")) {
                var id: Long = 0
                var descricao = ""
                var dia = ""
                var hora = ""

                // Pegar valores das colunas
                for (coluna in linha.getElementsByTag("td")) {
                    when (coluna.elementSiblingIndex()) {
                        0 -> dia = coluna.text()
                        1 -> hora = coluna.text()
                        2 -> descricao = coluna.text()
                        3 -> id = coluna.child(0).attr("onclick").split("'")[11].toLong()
                    }
                }

                // O formato do dia é padronizado, mas a hora não é
                // Por causa disso, eu estou definindo a data para
                // a meia noite do dia indicado
                val data = formatoData.parse("$dia 00:00")

                avaliacoes.add(Avaliacao(id, descricao, data, hora, disciplina))
            }
        }

        return avaliacoes
    }
}