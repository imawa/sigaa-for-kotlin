package com.imawa.sigaaforkotlin.models

import java.util.*

/**
 * Representa uma notícia cadastrada em uma disciplina.
 */
data class Noticia(
    val id: Int,
    val titulo: String,
    val texto: String,
    val data: Date,
    val jIdJsp: String,
    val jIdJspCompleto: String,
    val disciplina: Disciplina
)
