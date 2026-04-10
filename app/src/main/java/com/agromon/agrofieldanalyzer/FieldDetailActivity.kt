package com.agromon.agrofieldanalyzer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class FieldDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_detail)

        val fieldName = intent.getStringExtra("field_name") ?: "Новое поле"
        val fieldArea = intent.getDoubleExtra("field_area", 0.0)

        findViewById<EditText>(R.id.etFieldName).setText(fieldName)
        findViewById<TextView>(R.id.tvArea).text = fieldArea.toString()

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveField()
        }

        findViewById<MaterialButton>(R.id.btnAddPhoto).setOnClickListener {
            // Открыть камеру/галерею
            Toast.makeText(this, "Камера для: ${fieldName}", Toast.LENGTH_SHORT).show()
        }

        // Обработчик кнопки "Назад"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Обработчик кнопки "Сохранить"
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveField()
        }
    }

    private fun saveField() {
        // Сохранение в SQLite
        finish()
    }
}