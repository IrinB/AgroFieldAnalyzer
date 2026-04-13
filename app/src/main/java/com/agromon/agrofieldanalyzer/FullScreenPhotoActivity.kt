package com.agromon.agrofieldanalyzer

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.agromon.agrofieldanalyzer.ml.YoloDetector
import java.io.File

class FullScreenPhotoActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var tvInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_photo)

        imageView = findViewById(R.id.ivFullscreenPhoto)
        tvInfo = findViewById(R.id.tvInfo)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        val photoUri = intent.getStringExtra("photo_uri") ?: return
        val plantCount = intent.getIntExtra("plant_count", 0)
        val hasAnalysis = intent.getBooleanExtra("has_analysis", false)
        val detectionsJson = intent.getStringExtra("detections_json")

        val path = photoUri.replace("file://", "")
        val originalBitmap = BitmapFactory.decodeFile(path)

        val detectionsFile = intent.getStringExtra("detections_file")
        Log.d("FullScreenPhoto", "detectionsFile: $detectionsFile")

        if (hasAnalysis && plantCount > 0 && !detectionsFile.isNullOrEmpty()) {
            val jsonFile = File(filesDir, detectionsFile)
            if (jsonFile.exists()) {
                val json = jsonFile.readText()
                Log.d("FullScreenPhoto", "Вызов drawBoundingBoxesFromJson...")
                val bitmapWithBoxes = drawBoundingBoxesFromJson(originalBitmap, json)
                Log.d("FullScreenPhoto", "bitmapWithBoxes получен, установка в ImageView")
                imageView.setImageBitmap(bitmapWithBoxes)
                tvInfo.text = "Найдено ростков: $plantCount"
                tvInfo.visibility = android.view.View.VISIBLE
            }
        } else {
            // Просто фото
            imageView.setImageBitmap(originalBitmap)
            tvInfo.visibility = android.view.View.GONE
        }

        btnClose.setOnClickListener {
            finish()
        }

        imageView.setOnClickListener {
            if (hasAnalysis) {
                tvInfo.visibility = if (tvInfo.visibility == android.view.View.VISIBLE) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            }
        }
    }

    private fun drawBoundingBoxesFromJson(bitmap: Bitmap, json: String): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val detector = YoloDetector(this)
        val detections = detector.jsonToDetections(json)

        val paint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        for (detection in detections) {
            // Координаты в JSON нормализованы (0-1), умножаем на размер фото
            val scaledBox = RectF(
                detection.boundingBox.left * bitmap.width,
                detection.boundingBox.top * bitmap.height,
                detection.boundingBox.right * bitmap.width,
                detection.boundingBox.bottom * bitmap.height
            )
            canvas.drawRect(scaledBox, paint)
        }

        return mutableBitmap
    }
}