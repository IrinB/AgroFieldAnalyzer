package com.agromon.agrofieldanalyzer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agromon.agrofieldanalyzer.adapters.PhotoAdapter
import com.agromon.agrofieldanalyzer.database.DatabaseHelper
import com.agromon.agrofieldanalyzer.model.Field
import com.agromon.agrofieldanalyzer.model.Photo
import com.agromon.agrofieldanalyzer.utils.CameraHelper
import com.agromon.agrofieldanalyzer.utils.SquareItemDecoration

class FieldDetailActivity : AppCompatActivity() {

    private var fieldId: Long = 0
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var cameraHelper: CameraHelper
    private lateinit var photoAdapter: PhotoAdapter

    private lateinit var tvTitle: TextView
    private lateinit var etFieldName: EditText
    private lateinit var etArea: EditText
    private lateinit var etRowSpacing: EditText
    private lateinit var etExcludedArea: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_detail)

        dbHelper = DatabaseHelper(this)
        cameraHelper = CameraHelper(this)

        initViews()
        setupPhotoGrid()

        fieldId = intent.getLongExtra("field_id", 0)

        if (fieldId == 0L) {
            tvTitle.text = "Создать поле"
            etFieldName.hint = "Введите название поля"
            etFieldName.setText("")
            etFieldName.requestFocus()

            showKeyboard()

        } else {
            loadFieldData(fieldId)
            tvTitle.text = "Информация о поле"
            loadPhotos()
        }

        setupListeners()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        etFieldName = findViewById(R.id.etFieldName)
        etArea = findViewById(R.id.etArea)
        etRowSpacing = findViewById(R.id.etRowSpacing)
        etExcludedArea = findViewById(R.id.etExcludedArea)
    }

    private fun setupPhotoGrid() {
        photoAdapter = PhotoAdapter(
            onAddClick = {
                if (fieldId == 0L) {
                    saveField()
                }
                openCamera()
            },
            onPhotoClick = { photo ->
                Toast.makeText(this, "Просмотр фото", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { photo ->
                showDeletePhotoDialog(photo)
            }
        )

        val rvPhotos = findViewById<RecyclerView>(R.id.rvPhotos)
        rvPhotos.layoutManager = GridLayoutManager(this, 3)
        rvPhotos.adapter = photoAdapter
        photoAdapter.submitList(emptyList())
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

    private fun loadPhotos() {
        val photos = dbHelper.getPhotoTable().getByFieldId(fieldId)
        photoAdapter.submitList(photos)

        val rvPhotos = findViewById<RecyclerView>(R.id.rvPhotos)
        rvPhotos.post {
            val itemCount = photoAdapter.itemCount
            val rows = (itemCount + 2) / 3
            val itemWidth = rvPhotos.width / 3
            rvPhotos.layoutParams.height = itemWidth * rows
            rvPhotos.requestLayout()
        }
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveField()
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            deleteField()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveField()
        }
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etFieldName, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun saveField() {
        val name = etFieldName.text.toString().trim()
        val areaText = etArea.text.toString().trim()

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

        val rowSpacing = etRowSpacing.text.toString().toDoubleOrNull() ?: 0.0
        val excludedArea = etExcludedArea.text.toString().toDoubleOrNull() ?: 0.0

        val fieldTable = dbHelper.getFieldTable()
        if (fieldId == 0L) {
            fieldTable.insert(name, area, rowSpacing, excludedArea)
        } else {
            fieldTable.update(
                fieldId,
                name,
                area,
                rowSpacing,
                excludedArea
            )
        }

        Toast.makeText(this, "Поле сохранено", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteField() {
        val fieldTable = dbHelper.getFieldTable()

        if (fieldTable.hasPhotosOrAnalysis(fieldId)) {
            fieldTable.softDelete(fieldId)
        } else {
            fieldTable.delete(fieldId)
        }

        Toast.makeText(this, "Поле удалено", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun openCamera() {
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
                openCamera()
            } else {
                Toast.makeText(this, "Для для съемки необходим доступ к камере", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || fieldId == 0L) return

        when (requestCode) {
            CameraHelper.REQUEST_CODE_TAKE_PHOTO -> {
                cameraHelper.currentPhotoUri?.let { uri ->
                    savePhoto(uri)
                }
            }
            CameraHelper.REQUEST_CODE_PICK_IMAGE -> {
                data?.data?.let { uri ->
                    savePhoto(uri)
                }
            }
        }
    }

    private fun savePhoto(photoUri: Uri) {
        val photoTable = dbHelper.getPhotoTable()
        photoTable.insert(fieldId, photoUri.toString())

        dbHelper.getFieldTable().updateLastCaptureDate(fieldId)

        Toast.makeText(this, "Фото добавлено", Toast.LENGTH_SHORT).show()
        loadPhotos()
    }

    private fun showDeletePhotoDialog(photo: Photo) {
        AlertDialog.Builder(this)
            .setTitle("Удаление фото")
            .setMessage("Удалить выбранное фото?")
            .setPositiveButton("Удалить") { _, _ ->
                deletePhoto(photo)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deletePhoto(photo: Photo) {
        val photoTable = dbHelper.getPhotoTable()
        photoTable.delete(photo.id)
        loadPhotos()
    }
}