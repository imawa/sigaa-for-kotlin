package com.imawa.sigaaforkotlin.models

data class Disciplina(
    val id: String?, // Valor que somente as disciplinas atuais possuem
    val nome: String,
    val periodo: String,
    val formAcessarTurmaVirtual: String,
    val formAcessarTurmaVirtualCompleto: String,
    val frontEndIdTurma: String
) {
    companion object {
        const val PAGINA_PARTICIPANTES = 4
        const val PAGINA_ARQUIVOS = 5
        const val PAGINA_AVALIACOES = 1
        const val PAGINA_TAREFAS = 2
        const val PAGINA_QUESTIONARIOS = 3
    }
}