package com.imawa.sigaaforkotlin.entities

/**
 * Representa o anexo cadastrado em uma aula.
 */
data class Anexo(
    val tipo: Int,
    val titulo: String,
    val descricao: String,
    val url: String?,
    val jIdJspCompleto: String?,
    val idObjetoAssociado: String?,
    val disciplina: Disciplina
) {
    companion object {
        const val ANEXO_DESCONHECIDO = 0
        const val ANEXO_FORUM = 1
        const val ANEXO_CONTEUDO = 2
        const val ANEXO_REFERENCIA = 3
        const val ANEXO_VIDEO = 4
        const val ANEXO_ARQUIVO = 5
        const val ANEXO_TAREFA = 6
        const val ANEXO_QUESTIONARIO = 7
    }
}
