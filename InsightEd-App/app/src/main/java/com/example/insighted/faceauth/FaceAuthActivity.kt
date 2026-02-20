@file:OptIn(ExperimentalGetImage::class)

package com.example.insighted.faceauth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.insighted.R
import com.example.insighted.viewmodels.UserSession
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

private fun l2Normalize(embedding: FloatArray): FloatArray {
    var sum = 0f
    for (value in embedding) {
        sum += value * value
    }
    val norm = kotlin.math.sqrt(sum)
    return embedding.map { it / norm }.toFloatArray()
}
private fun PreviewView.startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onFaceCaptured: (Bitmap) -> Unit
) {

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({

        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalyzer.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy ->
            processImageProxy(detector, imageProxy, onFaceCaptured)
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )

    }, ContextCompat.getMainExecutor(context))
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    detector: FaceDetector,
    imageProxy: ImageProxy,
    onFaceCaptured: (Bitmap) -> Unit
) {

    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )

    detector.process(image)
        .addOnSuccessListener { faces ->

            if (faces.isNotEmpty()) {

                val face = faces[0]

                // ✅ Reject small faces
                if (face.boundingBox.width() < 400 ||
                    face.boundingBox.height() < 400) {
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                // ✅ Require both eyes detected & open
                val leftEye = face.leftEyeOpenProbability
                val rightEye = face.rightEyeOpenProbability

                if (leftEye == null || rightEye == null ||
                    leftEye < 0.6f || rightEye < 0.6f) {
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                // Convert full frame to Bitmap
                val bitmap = imageProxy.toBitmap()

                val box = face.boundingBox

                val left = box.left.coerceAtLeast(0)
                val top = box.top.coerceAtLeast(0)
                val right = box.right.coerceAtMost(bitmap.width)
                val bottom = box.bottom.coerceAtMost(bitmap.height)

                if (right > left && bottom > top) {

                    val cropped = Bitmap.createBitmap(
                        bitmap,
                        left,
                        top,
                        right - left,
                        bottom - top
                    )

                    onFaceCaptured(cropped)
                }
            }
        }
        .addOnFailureListener {
            Log.e("FACE_AUTH", "Face detection failed", it)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private fun ImageProxy.toBitmap(): Bitmap {

    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        width,
        height,
        null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        Rect(0, 0, width, height),
        100,
        out
    )

    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(
        imageBytes,
        0,
        imageBytes.size
    )
}
@Composable
fun FaceAuthScreen(
    onFaceAuthSuccess: () -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var infoText by remember { mutableStateOf("Waiting for face...") }
    var continueEnabled by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var faceHandled by remember { mutableStateOf(false) }

    // IMPORTANT: create FaceNet only once
    val faceNet = remember { FaceNetModel(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) infoText = "Camera permission denied"
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F3EF))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            Spacer(Modifier.height(40.dp))

            Text("Face Authentication", fontSize = 26.sp)

            Spacer(Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.face_id),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(infoText)

            Spacer(Modifier.height(32.dp))

            if (cameraPermissionGranted) {

                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also {
                            previewView = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                // START CAMERA ONLY ONCE
                LaunchedEffect(previewView) {
                    previewView?.let { pv ->
                        pv.startCamera(
                            context,
                            lifecycleOwner
                        ) { bitmap ->

                            if (!isProcessing && !faceHandled) {

                                isProcessing = true
                                faceHandled = true
                                infoText = "Verifying..."

                                val rawEmbedding = faceNet.getEmbedding(bitmap)
                                val embedding = l2Normalize(rawEmbedding)
                                val uuid = UserSession.uuid

                                if (uuid == null) {
                                    infoText = "Session error"
                                    isProcessing = false
                                    faceHandled = false
                                    return@startCamera
                                }

                                val dbRef = FirebaseDatabase
                                    .getInstance()
                                    .reference
                                    .child("users")
                                    .child(uuid)

                                dbRef.child("faceEmbedding")
                                    .get()
                                    .addOnSuccessListener { snapshot ->

                                        if (snapshot.exists()) {

                                            val storedList = snapshot.children.mapNotNull {
                                                it.getValue(Double::class.java)
                                            }

                                            val rawStoredArray = storedList
                                                .map { it.toFloat() }
                                                .toFloatArray()

                                            val storedArray = l2Normalize(rawStoredArray)

                                            val rawEmbedding = faceNet.getEmbedding(bitmap)
                                            val embedding = l2Normalize(rawEmbedding)

                                            val distance = calculateDistance(embedding, storedArray)

                                            Log.d("FACE_AUTH", "Distance: $distance")

                                            if (distance < 1.3f) {
                                                infoText = "Verified"
                                                continueEnabled = true
                                                isProcessing = false
                                            } else {
                                                infoText = "Face does not match"
                                                continueEnabled = false
                                                isProcessing = false
                                                faceHandled = false
                                            }
                                        } else {

                                            // First time registration
                                            val map =
                                                embedding.mapIndexed { index, value ->
                                                    index.toString() to value.toDouble()
                                                }.toMap()

                                            dbRef.child("faceEmbedding")
                                                .setValue(map)

                                            infoText = "Face Registered"
                                            continueEnabled = true
                                        }

                                        isProcessing = false
                                    }
                                    .addOnFailureListener {
                                        infoText = "Database error"
                                        isProcessing = false
                                        faceHandled = false
                                    }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { onFaceAuthSuccess() },
                enabled = continueEnabled
            ) {
                Text("Continue")
            }
        }
    }
}