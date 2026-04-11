package com.agromon.agrofieldanalyzer.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.agromon.agrofieldanalyzer.model.Field

class FieldTable(db: SQLiteDatabase) : BaseTable(db) {
    companion object {
        const val TABLE_NAME = "fields"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_AREA = "area"
        const val COLUMN_ROW_SPACING = "row_spacing"
        const val COLUMN_EXCLUDED_AREA = "excluded_area"
        const val COLUMN_LAST_CAPTURE = "last_capture"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_DELETED_AT = "deleted_at"

        // SQL для создания таблицы
        val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_AREA REAL DEFAULT 0,
                $COLUMN_ROW_SPACING REAL DEFAULT 0,
                $COLUMN_EXCLUDED_AREA REAL DEFAULT 0,
                $COLUMN_LAST_CAPTURE TIMESTAMP DEFAULT NULL,
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_DELETED_AT TIMESTAMP DEFAULT NULL
            )
        """.trimIndent()
    }

    override fun getTableName(): String = TABLE_NAME
    override fun getIdColumn(): String = COLUMN_ID

    fun insert(name: String, area: Double = 0.0, rowSpacing: Double = 0.0, excludedArea: Double = 0.0): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_AREA, area)
            put(COLUMN_ROW_SPACING, rowSpacing)
            put(COLUMN_EXCLUDED_AREA, excludedArea)
        }
        return insert(values)
    }

    fun update(id: Long, name: String, area: Double, rowSpacing: Double = 0.0, excludedArea: Double = 0.0): Int {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_AREA, area)
            put(COLUMN_ROW_SPACING, rowSpacing)
            put(COLUMN_EXCLUDED_AREA, excludedArea)
        }
        return update(id, values)
    }

    fun updateLastCaptureDate(fieldId: Long): Int {
        val values = ContentValues().apply {
            put(COLUMN_LAST_CAPTURE, "CURRENT_TIMESTAMP")
        }
        return db.update(
            TABLE_NAME,
            values,
            "$COLUMN_ID = ?",
            arrayOf(fieldId.toString())
        )
    }


    fun getAll(): List<Field> {
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_AREA, COLUMN_ROW_SPACING,
                COLUMN_EXCLUDED_AREA, COLUMN_LAST_CAPTURE),
            "$COLUMN_DELETED_AT IS NULL",
            null, null, null,
            "$COLUMN_CREATED_AT DESC"
        )

        val fields = mutableListOf<Field>()
        cursor.use {
            while (it.moveToNext()) {
                val lastCaptureIndex = it.getColumnIndex(COLUMN_LAST_CAPTURE)
                val lastCaptureDate = if (lastCaptureIndex >= 0 && !it.isNull(lastCaptureIndex)) {
                    it.getString(lastCaptureIndex)
                } else {
                    null
                }
                fields.add(
                    Field(
                        id = it.getLongSafe(COLUMN_ID),
                        name = it.getStringSafe(COLUMN_NAME),
                        area = it.getDoubleSafe(COLUMN_AREA),
                        rowSpacing = it.getDoubleSafe(COLUMN_ROW_SPACING),
                        excludedArea = it.getDoubleSafe(COLUMN_EXCLUDED_AREA),
                        lastCaptureDate
                    )
                )
            }
        }
        return fields
    }

    // Получение поля по ID
    fun getById(id: Long): Field? {
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_AREA, COLUMN_ROW_SPACING,
                COLUMN_EXCLUDED_AREA, COLUMN_LAST_CAPTURE),
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                val lastCaptureIndex = it.getColumnIndex(COLUMN_LAST_CAPTURE)
                val lastCaptureDate = if (lastCaptureIndex >= 0 && !it.isNull(lastCaptureIndex)) {
                    it.getString(lastCaptureIndex)
                } else {
                    null
                }

                Field(
                    id = it.getLongSafe(COLUMN_ID),
                    name = it.getStringSafe(COLUMN_NAME),
                    area = it.getDoubleSafe(COLUMN_AREA),
                    rowSpacing = it.getDoubleSafe(COLUMN_ROW_SPACING),
                    excludedArea = it.getDoubleSafe(COLUMN_EXCLUDED_AREA),
                    lastCaptureDate
                )
            } else {
                null
            }
        }
    }

    // Поиск полей по названию
    fun searchByName(query: String): List<Field> {
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_AREA, COLUMN_LAST_CAPTURE),
            "$COLUMN_NAME LIKE ?",
            arrayOf("%$query%"),
            null, null,
            "$COLUMN_CREATED_AT DESC"
        )

        val fields = mutableListOf<Field>()
        cursor.use {
            while (it.moveToNext()) {
                val lastCaptureIndex = it.getColumnIndex(COLUMN_LAST_CAPTURE)
                val lastCaptureDate = if (lastCaptureIndex >= 0 && !it.isNull(lastCaptureIndex)) {
                    it.getString(lastCaptureIndex)
                } else {
                    null
                }
                fields.add(
                    Field(
                        id = it.getLongSafe(COLUMN_ID),
                        name = it.getStringSafe(COLUMN_NAME),
                        area = it.getDoubleSafe(COLUMN_AREA),
                        lastCaptureDate = lastCaptureDate
                    )
                )
            }
        }
        return fields
    }

    fun softDelete(id: Long): Int {
        val values = ContentValues().apply {
            put(COLUMN_DELETED_AT, System.currentTimeMillis().toString())
        }
        return update(id, values)
    }

    fun restore(id: Long): Int {
        val values = ContentValues().apply {
            putNull(COLUMN_DELETED_AT)
        }
        return update(id, values)
    }

    fun hasPhotosOrAnalysis(id: Long): Boolean {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${PhotoTable.TABLE_NAME} WHERE ${PhotoTable.COLUMN_FIELD_ID} = ?",
            arrayOf(id.toString())
        )
        val photoCount = cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }

        // TODO: добавить проверку расчетов, когда будет таблица расчетов

        return photoCount > 0
    }
}