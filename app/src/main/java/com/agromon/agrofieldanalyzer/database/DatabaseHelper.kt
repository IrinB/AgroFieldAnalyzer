package com.agromon.agrofieldanalyzer.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "agro_fields.db"
        private const val DATABASE_VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(FieldTable.CREATE_TABLE)
        db.execSQL(PhotoTable.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${PhotoTable.TABLE_NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${FieldTable.TABLE_NAME}")
        onCreate(db)
    }

    fun getFieldTable(): FieldTable {
        return FieldTable(writableDatabase)
    }

    fun getPhotoTable(): PhotoTable {
        return PhotoTable(writableDatabase)
    }

    fun runInTransaction(block: () -> Unit) {
        val db = writableDatabase
        db.transaction {
            try {
                block()
            }
            finally {
            }
        }
    }
}