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

        // SQL для создания таблицы
        val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_AREA REAL DEFAULT 0,
                $COLUMN_ROW_SPACING REAL DEFAULT 0,
                $COLUMN_EXCLUDED_AREA REAL DEFAULT 0,
                $COLUMN_LAST_CAPTURE TEXT,
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

    fun updateLastCaptureDate(fieldId: Long, date: String): Int {
        val values = ContentValues().apply {
            put(COLUMN_LAST_CAPTURE, date)
        }
        return update(fieldId, values)
    }

    fun getAll(): List<Field> {
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_AREA, COLUMN_ROW_SPACING,
                COLUMN_EXCLUDED_AREA, COLUMN_LAST_CAPTURE),
            null, null, null, null,
            "$COLUMN_CREATED_AT DESC"
        )

        val fields = mutableListOf<Field>()
        cursor.use {
            while (it.moveToNext()) {
                fields.add(
                    Field(
                        id = it.getLongSafe(COLUMN_ID),
                        name = it.getStringSafe(COLUMN_NAME),
                        area = it.getDoubleSafe(COLUMN_AREA),
                        rowSpacing = it.getDoubleSafe(COLUMN_ROW_SPACING),
                        excludedArea = it.getDoubleSafe(COLUMN_EXCLUDED_AREA),
                        lastCaptureDate = it.getStringSafe(COLUMN_LAST_CAPTURE).takeIf { date -> date.isNotEmpty() }
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
                Field(
                    id = it.getLongSafe(COLUMN_ID),
                    name = it.getStringSafe(COLUMN_NAME),
                    area = it.getDoubleSafe(COLUMN_AREA),
                    rowSpacing = it.getDoubleSafe(COLUMN_ROW_SPACING),
                    excludedArea = it.getDoubleSafe(COLUMN_EXCLUDED_AREA),
                    lastCaptureDate = it.getStringSafe(COLUMN_LAST_CAPTURE).takeIf { date -> date.isNotEmpty() }
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
                fields.add(
                    Field(
                        id = it.getLongSafe(COLUMN_ID),
                        name = it.getStringSafe(COLUMN_NAME),
                        area = it.getDoubleSafe(COLUMN_AREA),
                        lastCaptureDate = it.getStringSafe(COLUMN_LAST_CAPTURE).takeIf { date -> date.isNotEmpty() }
                    )
                )
            }
        }
        return fields
    }
}