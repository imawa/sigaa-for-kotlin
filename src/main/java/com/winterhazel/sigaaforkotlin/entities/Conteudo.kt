package com.winterhazel.sigaaforkotlin.entities

import java.util.*

/**
 * Representa um conteúdo cadastrado na aba "Conteúdos" de uma disciplina.
 */
data class Conteudo(
    val id: Int,
    val titulo: String,
    val conteudo: String,
    val data: Date,
    val jIdJsp: String,
    val jIdJspCompleto: String,
    val disciplina: Disciplina
)
