package com.imawa.sigaaforkotlin.sigaa

data class Disciplina(
    val id: String?, // Valor que só as disciplinas do https://sig.ifc.edu.br/sigaa/portais/discente/turmas.jsf possuem
    val nome: String,
    val periodo: String,
    val formAcessarTurmaVirtual: String,
    val formAcessarTurmaVirtualCompleto: String,
    val frontEndIdTurma: String
)