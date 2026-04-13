package com.agromon.agrofieldanalyzer.model

data class Photo(
    val id: Long,
    val fieldId: Long,
    val photoUri: String,
    val analysisResult: String? = null,
    val photoDate: String,
    val plantCount: Int = 0,
    val density: Float = 0f,
    val detectionsFile: String? = null
)