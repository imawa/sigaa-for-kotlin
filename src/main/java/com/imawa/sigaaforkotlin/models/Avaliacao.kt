package com.imawa.sigaaforkotlin.models

import java.util.*

data class Avaliacao(
    val id: Long,
    val descricao: String,
    val dia: Date,
    val hora: String,
    val disciplina: Disciplina
)