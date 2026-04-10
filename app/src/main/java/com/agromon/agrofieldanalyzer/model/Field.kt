package com.agromon.agrofieldanalyzer.model

data class Field (
    val id: Long,
    val name: String,
    val area: Double,
    val rowSpacing: Double = 0.0,
    val excludedArea: Double = 0.0,
    val lastCaptureDate: String? = null
)