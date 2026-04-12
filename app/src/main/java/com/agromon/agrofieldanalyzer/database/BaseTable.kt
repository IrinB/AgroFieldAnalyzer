package com.agromon.agrofieldanalyzer.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

abstract class BaseTable(protected val db: SQLiteDatabase) {

    abstract fun getTableName(): String
    abstract fun getIdColumn(): String

    protected fun insert(contentValues: ContentValues): Long {
        return db.insert(getTableName(), null, contentValues)
    }

    protected fun update(id: Long, contentValues: ContentValues): Int {
        return db.update(
            getTableName(),
            contentValues,
            "${getIdColumn()} = ?",
            arrayOf(id.toString())
        )
    }

    fun delete(id: Long): Int {
        return db.delete(
            getTableName(),
            "${getIdColumn()} = ?",
            arrayOf(id.toString())
        )
    }

    fun getCount(): Int {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${getTableName()}", null)
        return cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    fun exists(id: Long): Boolean {
        val cursor = db.query(
            getTableName(),
            arrayOf(getIdColumn()),
            "${getIdColumn()} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use { it.count > 0 }
    }

    protected fun Cursor.getStringSafe(columnName: String): String {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) {
            getString(index)
        } else {
            ""
        }
    }

    protected fun Cursor.getLongSafe(columnName: String): Long {
        return getLong(getColumnIndexOrThrow(columnName))
    }

    protected fun Cursor.getDoubleSafe(columnName: String): Double {
        return getDouble(getColumnIndexOrThrow(columnName))
    }

    protected fun Cursor.getIntSafe(columnName: String): Int {
        return getInt(getColumnIndexOrThrow(columnName))
    }

    protected fun Cursor.getFloatSafe(columnName: String): Float {
        return getFloat(getColumnIndexOrThrow(columnName))
    }
}