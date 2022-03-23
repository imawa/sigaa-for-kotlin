package com.imawa.sigaaforkotlin.network

import com.imawa.sigaaforkotlin.models.Disciplina
import okhttp3.FormBody

class SIGAAFormBuilder(private val parser: SIGAAParser) {
    fun buildLoginForm(login: String, senha: String): FormBody =
        FormBody.Builder().add("dispatch", "logOn")
            .add("urlRedirect", "")
            .add("subsistemaRedirect", "")
            .add("acao", "")
            .add("acessibilidade", "")
            .add("user.login", login)
            .add("user.senha", senha)
            .build()

    fun buildOpenPortalDisciplinaPeloPortalDiscenteForm(
        disciplina: Disciplina,
        javaxViewState: String
    ): FormBody = FormBody.Builder()
        .add(disciplina.formAcessarTurmaVirtual, disciplina.formAcessarTurmaVirtual)
        .add("javax.faces.ViewState", javaxViewState)
        .add(disciplina.formAcessarTurmaVirtualCompleto, disciplina.formAcessarTurmaVirtualCompleto)
        .add("frontEndIdTurma", disciplina.frontEndIdTurma)
        .build()

    fun buildOpenPortalDisciplinaPelasTurmasForm(
        disciplina: Disciplina,
        javaxViewState: String
    ): FormBody = FormBody.Builder()
        .add(disciplina.formAcessarTurmaVirtual, disciplina.formAcessarTurmaVirtual)
        .add("javax.faces.ViewState", javaxViewState)
        .add(disciplina.formAcessarTurmaVirtualCompleto, disciplina.formAcessarTurmaVirtualCompleto)
        .add("frontEndIdTurma", disciplina.frontEndIdTurma)
        .add("inciadoPelaBusca", "true")
        .add("paginaListaTurmasOrigem", "/portais/discente/turmas.jsp")
        .build()

    fun buildOpenPaginaPortalDisciplinaForm(
        body: String,
        pagina: Int,
        javaxViewState: String
    ): FormBody {
        val args = parser.getArgsBotaoPortalDisciplina(body, pagina)
        return FormBody.Builder().add("formMenu", "formMenu")
            .add(args[0][0], args[0][1])
            .add("javax.faces.ViewState", javaxViewState)
            .add(args[1][0], args[1][1])
            .build()
    }
}