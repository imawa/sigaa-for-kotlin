package com.imawa.sigaaforkotlin.models

/**
 * Representa a lista de frequência de uma disciplina.
 */
data class Frequencia(
    val presencas: ArrayList<Presenca>,
    val totalFaltas: Int,
    val maximoFaltasPermitido: Int,
    val disciplina: Disciplina
)
