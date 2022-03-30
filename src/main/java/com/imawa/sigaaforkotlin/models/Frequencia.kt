package com.imawa.sigaaforkotlin.models

data class Frequencia(
    val presencas: ArrayList<Presenca>,
    val totalFaltas: Int,
    val maximoFaltasPermitido: Int,
    val disciplina: Disciplina
)