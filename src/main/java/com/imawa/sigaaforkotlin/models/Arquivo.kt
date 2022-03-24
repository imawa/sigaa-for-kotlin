package com.imawa.sigaaforkotlin.models

data class Arquivo(
    val id: String,
    val titulo: String,
    val descricao: String,
    val topicoDeAula: String,
    val jIdJsp: String,
    val disciplina: Disciplina
)