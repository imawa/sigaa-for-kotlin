package com.imawa.sigaaforkotlin.models

data class Aula(
    val titulo: String,
    val htmlConteudo: String,
    val anexos: ArrayList<Anexo>,
    val disciplina: Disciplina
)