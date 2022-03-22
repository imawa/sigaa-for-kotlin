package com.imawa.sigaaforkotlin.models

data class Disciplina(
    val id: String?, // Valor que somente as disciplinas atuais possuem
    val nome: String,
    val periodo: String,
    val formAcessarTurmaVirtual: String,
    val formAcessarTurmaVirtualCompleto: String,
    val frontEndIdTurma: String
)