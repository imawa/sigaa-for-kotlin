package com.imawa.sigaaforkotlin.models

/**
 * Representa uma nota cadastrada em uma disciplina.
 */
data class Nota(
    val nota: Float,
    val notaMaxima: Float,
    val peso: Float,
    val abreviacao: String,
    val descricao: String,
    val periodo: String,
    val disciplina: Disciplina
)
