package com.agromon.agrofieldanalyzer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agromon.agrofieldanalyzer.adapter.FieldAdapter
import com.agromon.agrofieldanalyzer.model.Field

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FieldAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        loadTestData() // Временно, пока нет БД
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
                // Открыть камеру для этого поля
                openCameraForField(field)
            }
        )

        findViewById<RecyclerView>(R.id.rvFields).apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun loadTestData() {
        val testFields = listOf(
            Field(1, "Северное поле", 15.4),
            Field(2, "Южное поле", 22.8),
            Field(3, "Западное поле", 8.2)
        )
        adapter.submitList(testFields)
    }

    private fun openCameraForField(field: Field) {
        // Будет реализовано позже
        Toast.makeText(this, "Камера для: ${field.name}", Toast.LENGTH_SHORT).show()
    }

}