package com.agromon.agrofieldanalyzer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agromon.agrofieldanalyzer.adapter.FieldAdapter
import com.agromon.agrofieldanalyzer.database.DatabaseHelper
import com.agromon.agrofieldanalyzer.model.Field
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FieldAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        setupRecyclerView()
        loadFieldsFromDatabase()

        findViewById<MaterialButton>(R.id.btnCreateField).setOnClickListener {
            val intent = Intent(this, FieldDetailActivity::class.java).apply {
                putExtra("field_id", 0L)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadFieldsFromDatabase()
    }

    private fun setupRecyclerView() {
        adapter = FieldAdapter(
            onFieldClick = { field ->
                // Открыть карточку поля
                val intent = Intent(this, FieldDetailActivity::class.java)
                intent.putExtra("field_id", field.id)
                intent.putExtra("field_name", field.name)
                intent.putExtra("field_area", field.area)
                startActivity(intent)
            },
            onCameraClick = { field ->
                openCameraForField(field)
            }
        )
        val recyclerView = findViewById<RecyclerView>(R.id.rvFields)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadFieldsFromDatabase() {
        val fields = dbHelper.getFieldTable().getAll()
        adapter.submitList(fields)
    }

    private fun openCameraForField(field: Field) {
        // Будет реализовано позже
        Toast.makeText(this, "Камера для: ${field.name}", Toast.LENGTH_SHORT).show()
    }

}