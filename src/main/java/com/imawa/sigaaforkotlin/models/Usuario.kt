package com.imawa.sigaaforkotlin.models

data class Usuario(
    val login: String,
    val matricula: Int?,
    val nome: String,
    val email: String,
    val urlAvatar: String,
    val tipoUsuario: Int,
    val disciplinasPeriodoAtual: ArrayList<Disciplina>
) {
    val isDiscente = tipoUsuario == USUARIO_DISCENTE
    val isDocente = tipoUsuario == USUARIO_DOCENTE

    companion object {
        const val USUARIO_DISCENTE = 1
        const val USUARIO_DOCENTE = 2
    }
}