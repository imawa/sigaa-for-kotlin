package com.imawa.sigaaforkotlin.entities

import java.util.*

/**
 * Representa uma data da aba "Frequência" de uma disciplina, isto é, uma linha da lista de
 * frequência.
 */
data class Presenca(
    val data: Date,
    val faltas: Int,
    val isRegistrada: Boolean
)
