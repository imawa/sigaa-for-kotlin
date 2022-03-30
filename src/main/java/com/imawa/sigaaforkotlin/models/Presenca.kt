package com.imawa.sigaaforkotlin.models

import java.util.*

data class Presenca(
    val data: Date,
    val faltas: Int,
    val isRegistrada: Boolean
)