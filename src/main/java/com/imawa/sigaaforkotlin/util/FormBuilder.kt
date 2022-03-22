package com.imawa.sigaaforkotlin.util

import okhttp3.FormBody

class FormBuilder {
    fun buildLoginForm(login: String, senha: String): FormBody =
        FormBody.Builder().add("dispatch", "logOn")
            .add("urlRedirect", "")
            .add("subsistemaRedirect", "")
            .add("acao", "")
            .add("acessibilidade", "")
            .add("user.login", login)
            .add("user.senha", senha)
            .build()
}