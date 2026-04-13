package com.agromon.agrofieldanalyzer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FullScreenPhotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_photo)

        val photoUri = intent.getStringExtra("photo_uri") ?: return
        val imageView = findViewById<ImageView>(R.id.ivFullscreenPhoto)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        // Загружаем фото
        val path = photoUri.replace("file://", "")
        val bitmap = BitmapFactory.decodeFile(path)
        imageView.setImageBitmap(bitmap)

        btnClose.setOnClickListener {
            finish()
        }
    }
}