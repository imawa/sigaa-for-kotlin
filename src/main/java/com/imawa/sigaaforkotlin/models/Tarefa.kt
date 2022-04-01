package com.imawa.sigaaforkotlin.models

import java.util.*

/**
 * Representa uma tarefa cadastrada em uma disciplina.
 */
data class Tarefa(
    val id: String,
    val titulo: String,
    val descricao: String,
    val urlArquivo: String?,
    val dataInicio: Date,
    val dataFim: Date,
    val envios: Int,
    val isEnviavel: Boolean,
    val isEnviada: Boolean,
    val isCorrigida: Boolean,
    val isIndividual: Boolean,
    val jId: String?,
    val jIdEnviar: String?,
    val jIdVisualizar: String?,
    val disciplina: Disciplina
)
