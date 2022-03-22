package com.imawa.sigaaforkotlin.sigaa

data class Usuario(
    val login: String,
    val matricula: Int,
    val nome: String,
    val email: String,
    val urlAvatar: String,
    val disciplinasPeriodoAtual: ArrayList<Disciplina>
)