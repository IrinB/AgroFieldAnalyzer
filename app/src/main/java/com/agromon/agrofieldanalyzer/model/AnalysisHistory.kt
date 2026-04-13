package com.agromon.agrofieldanalyzer.model

data class AnalysisHistory(
    val id: Long,
    val fieldId: Long,
    val analysisDate: String,
    val plantCount: Int,
    val density: Float
)