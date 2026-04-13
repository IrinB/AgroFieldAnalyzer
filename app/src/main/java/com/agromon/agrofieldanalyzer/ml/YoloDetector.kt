package com.agromon.agrofieldanalyzer.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import android.graphics.RectF
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import org.json.JSONArray
import org.json.JSONObject

class YoloDetector(private val context: Context) {

    companion object {

        private const val IOU_THRESHOLD = 0.3f
        private const val MODEL_FILE = "model/best_float32.tflite"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.6f
    }

    private var interpreter: Interpreter? = null

    data class Detection(
        val boundingBox: RectF,
        val confidence: Float,
        val classId: Int,
        val className: String
    )

    fun initialize(): Boolean {
        return try {
            val modelFile = File(context.filesDir, "best_float32.tflite")

            if (!modelFile.exists()) {
                context.assets.open(MODEL_FILE).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            interpreter = Interpreter(modelFile)

            true
        } catch (e: Exception) {
            false
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val interpreter = this.interpreter ?: run {
            return emptyList()
        }

        try {
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)
            val outputBuffer = Array(1) { Array(5) { FloatArray(8400) } }

            interpreter.run(inputBuffer, outputBuffer)

            return processOutput(outputBuffer[0], bitmap.width, bitmap.height)

        } catch (e: Exception) {
            Log.e("YoloDetector", "Ошибка в detect", e)
            return emptyList()
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    private fun processOutput(output: Array<FloatArray>, originalWidth: Int, originalHeight: Int): List<Detection> {
        val detections = mutableListOf<Detection>()

        val cxArray = output[0]
        val cyArray = output[1]
        val wArray = output[2]
        val hArray = output[3]
        val confArray = output[4]

        val scaleX = originalWidth.toFloat() / INPUT_SIZE
        val scaleY = originalHeight.toFloat() / INPUT_SIZE

        for (i in cxArray.indices) {
            val confidence = sigmoid(confArray[i])

            if (confidence >= CONFIDENCE_THRESHOLD) {
                // Координаты уже в пикселях 640x640, масштабируем до оригинального размера
                val cx = cxArray[i] * scaleX
                val cy = cyArray[i] * scaleY
                val w = wArray[i] * scaleX
                val h = hArray[i] * scaleY

                val left = (cx - w / 2).coerceIn(0f, originalWidth.toFloat())
                val top = (cy - h / 2).coerceIn(0f, originalHeight.toFloat())
                val right = (cx + w / 2).coerceIn(0f, originalWidth.toFloat())
                val bottom = (cy + h / 2).coerceIn(0f, originalHeight.toFloat())

                detections.add(
                    Detection(
                        boundingBox = RectF(left, top, right, bottom),
                        confidence = confidence,
                        classId = 0,
                        className = "soya"
                    )
                )
            }
        }

        return nonMaxSuppression(detections)
    }
    private fun sigmoid(x: Float): Float {
        return (1f / (1f + exp(-x)))
    }

    private fun nonMaxSuppression(detections: List<Detection>): List<Detection> {
        Log.d("YoloDetector", "NMS: вход ${detections.size} детекций")

        if (detections.isEmpty()) return detections

        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val current = iterator.next()
                val iou = calculateIoU(best.boundingBox, current.boundingBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        Log.d("YoloDetector", "NMS: выход ${result.size} детекций")
        return result
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = maxOf(box1.left, box2.left)
        val intersectionTop = maxOf(box1.top, box2.top)
        val intersectionRight = minOf(box1.right, box2.right)
        val intersectionBottom = minOf(box1.bottom, box2.bottom)

        if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) {
            return 0f
        }

        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()

        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    fun countPlants(bitmap: Bitmap): Int {
        val detections = detect(bitmap)
        return detections.count { it.className == "soya" }
    }
    fun close() {
        interpreter?.close()
        interpreter = null
    }

    fun detectionsToJson(detections: List<Detection>): String {
        val jsonArray = JSONArray()
        for (d in detections) {
            val json = JSONObject().apply {
                put("left", d.boundingBox.left)
                put("top", d.boundingBox.top)
                put("right", d.boundingBox.right)
                put("bottom", d.boundingBox.bottom)
                put("confidence", d.confidence)
                put("classId", d.classId)
                put("className", d.className)
            }
            jsonArray.put(json)
        }
        return jsonArray.toString()
    }

    fun jsonToDetections(json: String): List<Detection> {
        val detections = mutableListOf<Detection>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                detections.add(
                    Detection(
                        boundingBox = RectF(
                            obj.getDouble("left").toFloat(),
                            obj.getDouble("top").toFloat(),
                            obj.getDouble("right").toFloat(),
                            obj.getDouble("bottom").toFloat()
                        ),
                        confidence = obj.getDouble("confidence").toFloat(),
                        classId = obj.getInt("classId"),
                        className = obj.getString("className")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("YoloDetector", "Ошибка парсинга JSON", e)
        }
        return detections
    }
}