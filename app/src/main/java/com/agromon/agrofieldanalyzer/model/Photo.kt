package com.agromon.agrofieldanalyzer.model

data class Photo(
    val id: Long,
    val fieldId: Long,
    val photoUri: String,
    val analysisResult: String? = null,
    val photoDate: String
)