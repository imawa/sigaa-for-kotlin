package com.imawa.sigaaforkotlin.models

import java.util.*

data class Conteudo(
    val id: Int,
    val titulo: String,
    val conteudo: String,
    val data: Date,
    val jIdJsp: String,
    val jIdJspCompleto: String,
    val disciplina: Disciplina
)
