package com.example.sqlitevsroom

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NotasDbHelper(context: Context) :
    SQLiteOpenHelper(context, "notas_sqlite.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notas (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS notas")
        onCreate(db)
    }

    fun insertar(titulo: String) {
        val values = ContentValues().apply { put("titulo", titulo) }
        writableDatabase.insert("notas", null, values)
    }

    fun listar(): List<String> {
        val lista = mutableListOf<String>()
        val cursor = readableDatabase.query("notas", null, null, null, null, null, "id DESC")
        cursor.use {
            while (it.moveToNext()) {
                lista.add(it.getString(it.getColumnIndexOrThrow("titulo")))
            }
        }
        return lista
    }
}