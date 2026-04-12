package com.agromon.agrofieldanalyzer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraHelper(private val activity: Activity) {

    companion object {
        const val REQUEST_CODE_CAMERA_PERMISSION = 100
        const val REQUEST_CODE_TAKE_PHOTO = 101
        const val REQUEST_CODE_PICK_IMAGE = 102

        fun createImageFile(context: Context): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        }
    }

    var currentPhotoUri: Uri? = null
        private set

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CODE_CAMERA_PERMISSION
        )
    }

    fun openCamera(): Uri? {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return null
        }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        return try {
            val photoFile = createImageFile(activity)
            val photoUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile
            )
            currentPhotoUri = photoUri
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            activity.startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO)
            photoUri
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        activity.startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    fun showImageSourceDialog(onCamera: () -> Unit, onGallery: () -> Unit) {
        val options = arrayOf("Камера", "Память устройства")
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Выберите источник фото")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onCamera()
                    1 -> onGallery()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}