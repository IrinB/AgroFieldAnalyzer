package com.agromon.agrofieldanalyzer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.agromon.agrofieldanalyzer.ml.YoloDetector
import com.agromon.agrofieldanalyzer.model.Field
import com.agromon.agrofieldanalyzer.model.Photo
import com.agromon.agrofieldanalyzer.utils.CameraHelper
import java.io.File
import android.Manifest
import android.os.Build
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
    private lateinit var btnCalculateDensity: Button
    private lateinit var tvDensityResult: TextView

    private var hasUnanalyzedPhotos = false
    private var currentFieldArea: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_detail)

        dbHelper = DatabaseHelper(this)
        cameraHelper = CameraHelper(this)

        initViews()
        setupPhotoGrid()

        btnCalculateDensity = findViewById<Button>(R.id.btnCalculateDensity)

        fieldId = intent.getLongExtra("field_id", 0)

        if (fieldId == 0L) {
            tvTitle.text = "Создать поле"
            etFieldName.hint = "Введите название поля"
            etFieldName.setText("")
            etFieldName.requestFocus()

            btnCalculateDensity.isEnabled = false
            btnCalculateDensity.alpha = 0.5f

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
        tvDensityResult = findViewById(R.id.tvDensityResult)
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
            currentFieldArea = it.area
            etFieldName.setText(it.name)
            etArea.setText(it.area.toString())
            etRowSpacing.setText(it.rowSpacing.toString())
            etExcludedArea.setText(it.excludedArea.toString())
        }

        loadLastAnalysisResult()

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

        hasUnanalyzedPhotos = photos.any { it.plantCount == 0 }
        btnCalculateDensity.isEnabled = hasUnanalyzedPhotos && photos.isNotEmpty()
        if (btnCalculateDensity.isEnabled) {
            btnCalculateDensity.alpha = 1.0f
        } else {
            btnCalculateDensity.alpha = 0.5f
        }
    }

    private fun calculateDensityForAllPhotos() {
        if (!checkStoragePermission()) {
            requestStoragePermission()
            return
        }

        val photos = dbHelper.getPhotoTable().getByFieldId(fieldId)
        val unanalyzedPhotos = photos.filter { it.plantCount == 0 }

        if (unanalyzedPhotos.isEmpty()) {
            Toast.makeText(this, "Все фото уже проанализированы", Toast.LENGTH_SHORT).show()
            return
        }

        btnCalculateDensity.isEnabled = false
        btnCalculateDensity.text = "Расчёт..."

        analyzeNextPhoto(unanalyzedPhotos, 0)
    }

    private fun analyzeNextPhoto(photos: List<Photo>, index: Int) {
        if (index >= photos.size) {
            runOnUiThread {
                btnCalculateDensity.text = "Рассчитать"
                btnCalculateDensity.isEnabled = false

                saveAnalysisHistory()
                loadLastAnalysisResult()

                Toast.makeText(this@FieldDetailActivity, "Расчёт завершён!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val photo = photos[index]

        Thread {
            try {
                val path = photo.photoUri.replace("file://", "")
                val bitmap = BitmapFactory.decodeFile(path)

                if (bitmap == null) {
                    runOnUiThread {
                        Toast.makeText(this@FieldDetailActivity, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
                        analyzeNextPhoto(photos, index + 1)
                    }
                    return@Thread
                }

                val detector = YoloDetector(this@FieldDetailActivity)
                val initialized = detector.initialize()

                if (!initialized) {
                    runOnUiThread {
                        Toast.makeText(this@FieldDetailActivity, "Ошибка загрузки модели", Toast.LENGTH_SHORT).show()
                        analyzeNextPhoto(photos, index + 1)
                    }
                    return@Thread
                }

                val plantCount = detector.countPlants(bitmap)
                val density = if (currentFieldArea > 0) plantCount.toFloat() / currentFieldArea.toFloat() else 0f

                val photoTable = dbHelper.getPhotoTable()
                photoTable.updateAnalysisResult(photo.id, plantCount, density)

                detector.close()

                runOnUiThread {
                    analyzeNextPhoto(photos, index + 1)
                }

            } catch (e: Exception) {
                Log.e("FieldDetailActivity", "Ошибка анализа фото", e)
                runOnUiThread {
                    Toast.makeText(this@FieldDetailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    analyzeNextPhoto(photos, index + 1)
                }
            }
        }.start()
    }

    private fun saveAnalysisHistory() {
        val photos = dbHelper.getPhotoTable().getByFieldId(fieldId)
        val analyzedPhotos = photos.filter { it.plantCount > 0 }

        if (analyzedPhotos.isNotEmpty()) {
            val totalPlants = analyzedPhotos.sumOf { it.plantCount }
            val avgDensity = if (currentFieldArea > 0) totalPlants.toFloat() / currentFieldArea.toFloat() else 0f

            dbHelper.getAnalysisHistoryTable().insert(fieldId, totalPlants, avgDensity)
        }
    }

    private fun loadLastAnalysisResult() {
        val lastAnalysis = dbHelper.getAnalysisHistoryTable().getLastByFieldId(fieldId)
        if (lastAnalysis != null) {
            tvDensityResult.text = String.format("Средняя густота: %.1f раст/га", lastAnalysis.density)
            tvDensityResult.visibility = View.VISIBLE
        } else {
            tvDensityResult.visibility = View.GONE
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

        btnCalculateDensity.setOnClickListener {
            calculateDensityForAllPhotos()
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

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            calculateDensityForAllPhotos()
        }
        if (requestCode == CameraHelper.REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Для для съемки необходим доступ к камере", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                1001
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1001
            )
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
        try {
            // Копируем фото во внутреннее хранилище
            val inputStream = contentResolver.openInputStream(photoUri)
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val outputFile = File(filesDir, fileName)

            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Сохраняем путь к файлу в БД
            val photoTable = dbHelper.getPhotoTable()
            photoTable.insert(fieldId, "file://${outputFile.absolutePath}")

            dbHelper.getFieldTable().updateLastCaptureDate(fieldId)

            Toast.makeText(this, "Фото добавлено", Toast.LENGTH_SHORT).show()
            loadPhotos()

            btnCalculateDensity.isEnabled = true

        } catch (e: Exception) {
            Log.e("FieldDetailActivity", "Ошибка сохранения фото", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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