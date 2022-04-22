package com.winterhazel.sigaaforkotlin.entities

/**
 * Representa um arquivo cadastrado na aba "Arquivos" de uma disciplina.
 * Esta classe não possui o arquivo em si! Para obter o conteúdo do arquivo, é necessário utilizar
 * o downloadArquivo().
 */
data class Arquivo(
    val id: String,
    val titulo: String,
    val descricao: String,
    val topicoDeAula: String,
    val jIdJsp: String,
    val disciplina: Disciplina
)
