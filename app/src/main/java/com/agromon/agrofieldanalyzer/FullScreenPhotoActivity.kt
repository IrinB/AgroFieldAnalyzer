package com.agromon.agrofieldanalyzer

import android.graphics.*
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.agromon.agrofieldanalyzer.ml.YoloDetector
import java.io.File
import androidx.core.graphics.toColorInt

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

        val path = photoUri.replace("file://", "")
        val originalBitmap = BitmapFactory.decodeFile(path)

        val detectionsFile = intent.getStringExtra("detections_file")

        if (hasAnalysis && plantCount > 0 && !detectionsFile.isNullOrEmpty()) {
            val jsonFile = File(filesDir, detectionsFile)
            if (jsonFile.exists()) {
                val json = jsonFile.readText()
                val bitmapWithBoxes = drawBoundingBoxesFromJson(originalBitmap, json)
                imageView.setImageBitmap(bitmapWithBoxes)
                imageView.invalidate()

                tvInfo.text = "Найдено ростков: $plantCount"
                tvInfo.visibility = View.VISIBLE
            }
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun drawBoundingBoxesFromJson(bitmap: Bitmap, json: String): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val detector = YoloDetector(this)
        val detections = detector.jsonToDetections(json)

        val paint = Paint().apply {
            color = "#00FF00".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        for (d in detections) {
            canvas.drawRect(d.boundingBox, paint)
        }

        return mutableBitmap
    }
}