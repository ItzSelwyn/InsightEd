package com.example.insighted.faceauth

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceNetModel(context: Context) {

    private val interpreter: Interpreter

    init {
        val assetFile = context.assets.open("facenet.tflite")
        val modelBytes = assetFile.readBytes()
        val buffer = ByteBuffer.allocateDirect(modelBytes.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(modelBytes)
        interpreter = Interpreter(buffer)
    }

    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, 160, 160, true)

        val input = ByteBuffer.allocateDirect(1 * 160 * 160 * 3 * 4)
        input.order(ByteOrder.nativeOrder())

        for (y in 0 until 160) {
            for (x in 0 until 160) {
                val pixel = resized.getPixel(x, y)

                input.putFloat(((pixel shr 16 and 0xFF) - 128f) / 128f)
                input.putFloat(((pixel shr 8 and 0xFF) - 128f) / 128f)
                input.putFloat(((pixel and 0xFF) - 128f) / 128f)
            }
        }

        val output = Array(1) { FloatArray(128) }
        interpreter.run(input, output)

        return output[0]
    }
}
