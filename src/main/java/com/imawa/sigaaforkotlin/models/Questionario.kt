package com.imawa.sigaaforkotlin.models

import java.util.*

data class Questionario(
    val id: Long,
    val titulo: String,
    val dataInicio: Date,
    val dataFim: Date,
    val isEnviado: Boolean,
    val disciplina: Disciplina
)
