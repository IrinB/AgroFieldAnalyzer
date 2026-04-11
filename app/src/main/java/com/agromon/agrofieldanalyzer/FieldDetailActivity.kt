package com.agromon.agrofieldanalyzer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.agromon.agrofieldanalyzer.database.DatabaseHelper
import com.agromon.agrofieldanalyzer.model.Field
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FieldDetailActivity : AppCompatActivity() {

    private var fieldId: Long = 0
    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tvTitle: TextView
    private lateinit var etFieldName: EditText
    private lateinit var etArea: EditText
    private lateinit var etRowSpacing: EditText
    private lateinit var etExcludedArea: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_detail)

        dbHelper = DatabaseHelper(this)

        tvTitle = findViewById(R.id.tvTitle)
        etFieldName = findViewById(R.id.etFieldName)
        etArea = findViewById(R.id.etArea)
        etRowSpacing = findViewById(R.id.etRowSpacing)
        etExcludedArea = findViewById(R.id.etExcludedArea)

        fieldId = intent.getLongExtra("field_id", 0)

        if (fieldId == 0L) {
            tvTitle.text = "Создать поле"
            etFieldName.hint = "Введите название поля"
            etFieldName.setText("")

            // Показать клавиатуру
            etFieldName.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etFieldName, InputMethodManager.SHOW_IMPLICIT)
        } else {
            loadFieldData(fieldId)
            tvTitle.text = "Информация о поле"
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveField()
        }

        findViewById<MaterialButton>(R.id.btnAddPhoto).setOnClickListener {
            // Открыть камеру/галерею
            Toast.makeText(this, "Камера для: ${etFieldName.text}", Toast.LENGTH_SHORT).show()
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
    private fun loadFieldData(id: Long): Field? {
        val field = dbHelper.getFieldTable().getById(id)
        field?.let {
            title = it.name
            etFieldName.setText(it.name)
            etArea.setText(it.area.toString())
            etRowSpacing.setText(it.rowSpacing.toString())
            etExcludedArea.setText(it.excludedArea.toString())
        }

        return field
    }
    private fun saveField() {
        val name = etFieldName.text.toString().trim()
        val areaText = etArea.text.toString().trim()
        val rowSpacing = etRowSpacing.text.toString().toDoubleOrNull() ?: 0.0
        val excludedArea = etExcludedArea.text.toString().toDoubleOrNull() ?: 0.0

        if (name.isBlank()) {
            etFieldName.error = "Введите название поля"
            etFieldName.requestFocus()
            return
        }

        if (areaText.isBlank()) {
            etArea.error = "Укажите площадь"
            etArea.requestFocus()
            return
        }

        val area = areaText.toDoubleOrNull()
        if (area == null || area <= 0) {
            etArea.error = "Площадь должна быть больше 0"
            etArea.requestFocus()
            return
        }

        val fieldTable = dbHelper.getFieldTable()

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
        val currentDate = dateFormat.format(Date())

        if (fieldId == 0L) {
            fieldTable.insert(name, area, rowSpacing, excludedArea)
        } else {
            fieldTable.update(fieldId, name, area, rowSpacing, excludedArea)
            fieldTable.updateLastCaptureDate(fieldId, currentDate)
        }

        Toast.makeText(this, "Поле сохранено", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteField() {
        val fieldTable = dbHelper.getFieldTable()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        if (fieldTable.hasPhotosOrAnalysis(fieldId)) {
            // Мягкое удаление
            fieldTable.softDelete(fieldId)
            Toast.makeText(this, "Поле помечено как удаленное", Toast.LENGTH_SHORT).show()
        } else {
            // Полное удаление
            fieldTable.delete(fieldId)
            Toast.makeText(this, "Поле удалено", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}