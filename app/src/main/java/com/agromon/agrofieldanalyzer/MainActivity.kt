package com.agromon.agrofieldanalyzer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agromon.agrofieldanalyzer.adapters.FieldAdapter
import com.agromon.agrofieldanalyzer.database.DatabaseHelper
import com.agromon.agrofieldanalyzer.model.Field
import com.agromon.agrofieldanalyzer.utils.CameraHelper
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FieldAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var cameraHelper: CameraHelper
    private var currentFieldForPhoto: Field? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setLogo(R.drawable.logo)
        supportActionBar?.setDisplayUseLogoEnabled(true)

        dbHelper = DatabaseHelper(this)
        cameraHelper = CameraHelper(this)

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
                currentFieldForPhoto = field
                cameraHelper.openCamera()
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

    private fun openCameraForField() {
        cameraHelper.showImageSourceDialog(
            onCamera = {
                cameraHelper.openCamera()
            },
            onGallery = {
                cameraHelper.openGallery()
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CameraHelper.REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraForField()
            } else {
                Toast.makeText(this, "Разрешение на камеру необходимо для съемки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        val field = currentFieldForPhoto ?: return

        when (requestCode) {
            CameraHelper.REQUEST_CODE_TAKE_PHOTO -> {
                cameraHelper.currentPhotoUri?.let { uri ->
                    savePhotoForField(field.id, uri)
                }
            }
            CameraHelper.REQUEST_CODE_PICK_IMAGE -> {
                data?.data?.let { uri ->
                    savePhotoForField(field.id, uri)
                }
            }
        }
        currentFieldForPhoto = null
    }

    private fun savePhotoForField(fieldId: Long, photoUri: Uri) {
        val photoTable = dbHelper.getPhotoTable()
        photoTable.insert(fieldId, photoUri.toString())

        dbHelper.getFieldTable().updateLastCaptureDate(fieldId)

        Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
        loadFieldsFromDatabase()
    }

}