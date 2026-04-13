package com.agromon.agrofieldanalyzer.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.agromon.agrofieldanalyzer.model.AnalysisHistory

class AnalysisHistoryTable(db: SQLiteDatabase) : BaseTable(db) {

    companion object {
        const val TABLE_NAME = "analysis_history"
        const val COLUMN_ID = "_id"
        const val COLUMN_FIELD_ID = "field_id"
        const val COLUMN_ANALYSIS_DATE = "analysis_date"
        const val COLUMN_PLANT_COUNT = "plant_count"
        const val COLUMN_DENSITY = "density"

        val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FIELD_ID INTEGER NOT NULL,
                $COLUMN_ANALYSIS_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_PLANT_COUNT INTEGER DEFAULT 0,
                $COLUMN_DENSITY REAL DEFAULT 0,
                FOREIGN KEY ($COLUMN_FIELD_ID) REFERENCES ${FieldTable.TABLE_NAME} (${FieldTable.COLUMN_ID}) ON DELETE CASCADE
            )
        """.trimIndent()
    }

    override fun getTableName(): String = TABLE_NAME
    override fun getIdColumn(): String = COLUMN_ID

    fun insert(fieldId: Long, plantCount: Int, density: Float): Long {
        val values = ContentValues().apply {
            put(COLUMN_FIELD_ID, fieldId)
            put(COLUMN_PLANT_COUNT, plantCount)
            put(COLUMN_DENSITY, density)
        }
        return insert(values)
    }

    fun getLastByFieldId(fieldId: Long): AnalysisHistory? {
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_FIELD_ID, COLUMN_ANALYSIS_DATE, COLUMN_PLANT_COUNT, COLUMN_DENSITY),
            "$COLUMN_FIELD_ID = ?",
            arrayOf(fieldId.toString()),
            null, null,
            "$COLUMN_ANALYSIS_DATE DESC",
            "1"
        )

        cursor.use {
            return if (it.moveToFirst()) {
                AnalysisHistory(
                    id = it.getLongSafe(COLUMN_ID),
                    fieldId = it.getLongSafe(COLUMN_FIELD_ID),
                    analysisDate = it.getStringSafe(COLUMN_ANALYSIS_DATE),
                    plantCount = it.getIntSafe(COLUMN_PLANT_COUNT),
                    density = it.getFloatSafe(COLUMN_DENSITY)
                )
            } else {
                null
            }
        }
    }

    fun getAllByFieldId(fieldId: Long): List<AnalysisHistory> {
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_FIELD_ID, COLUMN_ANALYSIS_DATE, COLUMN_PLANT_COUNT, COLUMN_DENSITY),
            "$COLUMN_FIELD_ID = ?",
            arrayOf(fieldId.toString()),
            null, null,
            "$COLUMN_ANALYSIS_DATE DESC"
        )

        val history = mutableListOf<AnalysisHistory>()
        cursor.use {
            while (it.moveToNext()) {
                history.add(
                    AnalysisHistory(
                        id = it.getLongSafe(COLUMN_ID),
                        fieldId = it.getLongSafe(COLUMN_FIELD_ID),
                        analysisDate = it.getStringSafe(COLUMN_ANALYSIS_DATE),
                        plantCount = it.getIntSafe(COLUMN_PLANT_COUNT),
                        density = it.getFloatSafe(COLUMN_DENSITY)
                    )
                )
            }
        }
        return history
    }
}