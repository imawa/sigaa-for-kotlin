package com.winterhazel.sigaaforkotlin.entities

/**
 * Representa uma nota cadastrada em uma disciplina.
 *
 * @param nota Indica o valor da nota. Se for uma nota ainda não inserida (-- no SIGAA), o valor é
 * -1.
 * @param notaMaxima Indica a nota máxima. Se não possuir uma nota máxima definida, o valor é -1.
 * @param peso Indica o peso da nota. Se a nota não possuir um peso definido, o valor é -1.
 */
data class Nota(
    val nota: Float,
    val notaMaxima: Float,
    val peso: Float,
    val abreviacao: String,
    val descricao: String,
    val periodo: String,
    val disciplina: Disciplina
)
