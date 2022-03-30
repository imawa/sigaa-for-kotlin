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
        const val PAGINA_PRINCIPAL = 0
        const val PAGINA_PARTICIPANTES = 3
        const val PAGINA_NOTICIAS = 6
        const val PAGINA_FREQUENCIAS = 7
        const val PAGINA_NOTAS = 9
        const val PAGINA_CONTEUDOS = 11
        const val PAGINA_REFERENCIAS = 12
        const val PAGINA_ARQUIVOS = 14
        const val PAGINA_AVALIACOES = 15
        const val PAGINA_TAREFAS = 16
        const val PAGINA_QUESTIONARIOS = 17
    }
}