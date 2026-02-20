package com.example.insighted.faceauth

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import kotlin.math.sqrt

fun calculateDistance(emb1: FloatArray, emb2: FloatArray): Float {
    var sum = 0f
    for (i in emb1.indices) {
        val diff = emb1[i] - emb2[i]
        sum += diff * diff
    }
    return sqrt(sum)
}