package com.imawa.sigaaforkotlin.models

import java.util.*

/**
 * Representa uma avaliação cadastrada na aba "Avaliações" de uma disciplina.
 */
data class Avaliacao(
    val id: Long,
    val descricao: String,
    val dia: Date,
    val hora: String,
    val disciplina: Disciplina
)
