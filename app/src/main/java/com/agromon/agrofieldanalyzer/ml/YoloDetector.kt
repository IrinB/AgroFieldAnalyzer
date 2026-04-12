package com.agromon.agrofieldanalyzer.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import android.graphics.RectF
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

class YoloDetector(private val context: Context) {

    companion object {
        private const val MODEL_FILE = "model/best_float32.tflite"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val NUM_CLASSES = 2  // plant и soya
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
            val modelFile = "model/best_float32.tflite"
            val assetFileDescriptor = context.assets.openFd(modelFile)
            val inputStream = assetFileDescriptor.createInputStream()
            val modelBuffer = inputStream.readBytes()

            // Преобразуем ByteArray в ByteBuffer
            val byteBuffer = java.nio.ByteBuffer.allocateDirect(modelBuffer.size)
            byteBuffer.put(modelBuffer)
            byteBuffer.rewind()

            interpreter = Interpreter(byteBuffer)

            inputStream.close()
            assetFileDescriptor.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val interpreter = this.interpreter ?: return emptyList()

        // 1. Изменяем размер bitmap до 640x640
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        // 2. Конвертируем bitmap в ByteBuffer
        val inputBuffer = bitmapToByteBuffer(resizedBitmap)

        // 3. Создаём выходной буфер
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputSize = outputShape.fold(1) { acc, i -> acc * i }
        val outputBuffer = Array(1) { FloatArray(outputSize) }

        // 4. Запускаем модель
        interpreter.run(inputBuffer, outputBuffer)

        // 5. Обрабатываем результаты
        return processOutput(outputBuffer[0], bitmap.width, bitmap.height)
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

    private fun processOutput(output: FloatArray, originalWidth: Int, originalHeight: Int): List<Detection> {
        val detections = mutableListOf<Detection>()

        val numPredictions = 8400
        val outputPerPrediction = 4 + NUM_CLASSES

        val scaleX = originalWidth.toFloat() / INPUT_SIZE
        val scaleY = originalHeight.toFloat() / INPUT_SIZE

        for (i in 0 until numPredictions) {
            val offset = i * outputPerPrediction

            var maxConf = 0f
            var maxClassId = -1

            for (c in 0 until NUM_CLASSES) {
                val conf = sigmoid(output[offset + 4 + c])
                if (conf > maxConf) {
                    maxConf = conf
                    maxClassId = c
                }
            }
            if (maxConf >= CONFIDENCE_THRESHOLD) {
                val cx = output[offset + 0] * scaleX
                val cy = output[offset + 1] * scaleY
                val w = output[offset + 2] * scaleX
                val h = output[offset + 3] * scaleY

                val left = (cx - w / 2).coerceIn(0f, originalWidth.toFloat())
                val top = (cy - h / 2).coerceIn(0f, originalHeight.toFloat())
                val right = (cx + w / 2).coerceIn(0f, originalWidth.toFloat())
                val bottom = (cy + h / 2).coerceIn(0f, originalHeight.toFloat())

                detections.add(
                    Detection(
                        boundingBox = RectF(left, top, right, bottom),
                        confidence = maxConf,
                        classId = maxClassId,
                        className = getClassName(maxClassId)
                    )
                )
            }
        }

        return nonMaxSuppression(detections)
    }

    private fun sigmoid(x: Float): Float {
        return (1f / (1f + exp(-x)))
    }

    private fun getClassName(classId: Int): String {
        return when (classId) {
            0 -> "plant"
            1 -> "soya"
            else -> "unknown"
        }
    }

    private fun nonMaxSuppression(detections: List<Detection>): List<Detection> {
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
                if (iou >= 0.4f) {
                    iterator.remove()
                }
            }
        }
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
        return detect(bitmap).count { it.className == "soya" }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}