package com.imawa.sigaaforkotlin.models

/**
 * Representa uma aula cadastrada em uma disciplina.
 */
data class Aula(
    val titulo: String,
    val htmlConteudo: String,
    val anexos: ArrayList<Anexo>,
    val disciplina: Disciplina
)
