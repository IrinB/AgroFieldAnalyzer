package com.agromon.agrofieldanalyzer.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.agromon.agrofieldanalyzer.model.Photo

class PhotoTable(db: SQLiteDatabase) : BaseTable(db) {

    companion object {
        const val TABLE_NAME = "photos"
        const val COLUMN_ID = "id"
        const val COLUMN_FIELD_ID = "field_id"
        const val COLUMN_PHOTO_URI = "photo_uri"
        const val COLUMN_ANALYSIS_RESULT = "analysis_result"
        const val COLUMN_PHOTO_DATE = "photo_date"
        const val COLUMN_PLANT_COUNT = "plant_count"
        const val COLUMN_DENSITY = "density"
        const val COLUMN_DETECTIONS_FILE = "detections_file"

        val CREATE_TABLE = """
    CREATE TABLE $TABLE_NAME (
        $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COLUMN_FIELD_ID INTEGER NOT NULL,
        $COLUMN_PHOTO_URI TEXT NOT NULL,
        $COLUMN_ANALYSIS_RESULT TEXT,
        $COLUMN_PLANT_COUNT INTEGER DEFAULT 0,
        $COLUMN_DENSITY REAL DEFAULT 0,
        $COLUMN_DETECTIONS_FILE TEXT,
        $COLUMN_PHOTO_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY ($COLUMN_FIELD_ID) REFERENCES ${FieldTable.TABLE_NAME} (${FieldTable.COLUMN_ID}) ON DELETE CASCADE
    )
""".trimIndent()
    }

    override fun getTableName(): String = TABLE_NAME
    override fun getIdColumn(): String = COLUMN_ID

    // Вставка нового фото
    fun insert(fieldId: Long, photoUri: String, analysisResult: String? = null): Long {
        val values = ContentValues().apply {
            put(COLUMN_FIELD_ID, fieldId)
            put(COLUMN_PHOTO_URI, photoUri)
            put(COLUMN_ANALYSIS_RESULT, analysisResult)
        }
        return insert(values)
    }

    fun getByFieldId(fieldId: Long): List<Photo> {
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_FIELD_ID, COLUMN_PHOTO_URI,
                COLUMN_ANALYSIS_RESULT, COLUMN_PHOTO_DATE,
                COLUMN_PLANT_COUNT, COLUMN_DENSITY, COLUMN_DETECTIONS_FILE),
            "$COLUMN_FIELD_ID = ?",
            arrayOf(fieldId.toString()),
            null, null,
            "$COLUMN_PHOTO_DATE DESC"
        )

        val photos = mutableListOf<Photo>()
        cursor.use {
            while (it.moveToNext()) {
                photos.add(
                    Photo(
                        id = it.getLongSafe(COLUMN_ID),
                        fieldId = it.getLongSafe(COLUMN_FIELD_ID),
                        photoUri = it.getStringSafe(COLUMN_PHOTO_URI),
                        analysisResult = it.getStringSafe(COLUMN_ANALYSIS_RESULT),
                        photoDate = it.getStringSafe(COLUMN_PHOTO_DATE),
                        plantCount = it.getIntSafe(COLUMN_PLANT_COUNT),
                        density = it.getFloatSafe(COLUMN_DENSITY),
                        detectionsFile = it.getStringSafe(COLUMN_DETECTIONS_FILE).takeIf { json -> !json.isNullOrEmpty() }
                    )
                )
            }
        }
        return photos
    }

    // Удаление всех фото для поля
    fun deleteByFieldId(fieldId: Long): Int {
        return db.delete(
            TABLE_NAME,
            "$COLUMN_FIELD_ID = ?",
            arrayOf(fieldId.toString())
        )
    }

    fun updateAnalysisResult(photoId: Long, plantCount: Int, density: Float, detectionsFile: String) {
        val values = ContentValues().apply {
            put(COLUMN_PLANT_COUNT, plantCount)
            put(COLUMN_DENSITY, density)
            put(COLUMN_DETECTIONS_FILE, detectionsFile)
        }
        update(photoId, values)
    }

}