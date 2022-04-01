package com.imawa.sigaaforkotlin.models

import java.util.*

/**
 * Representa um questionário cadastrado em uma disciplina.
 * Esta classe não possui as questões, somente as informações acessíveis sem abrir o questionário.
 */
data class Questionario(
    val id: Long,
    val titulo: String,
    val dataInicio: Date,
    val dataFim: Date,
    val isEnviado: Boolean,
    val disciplina: Disciplina
)
