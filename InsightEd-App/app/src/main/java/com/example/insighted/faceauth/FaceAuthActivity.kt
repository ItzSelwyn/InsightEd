package com.example.insighted.faceauth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

@OptIn(ExperimentalGetImage::class)
@Composable
fun FaceAuthScreen(
    onFaceAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var infoText by remember { mutableStateOf("Waiting for face...") }
    var isProcessing by remember { mutableStateOf(false) }
    var continueEnabled by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        cameraPermissionGranted = granted
        if (!granted) {
            infoText = "Camera permission denied"
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
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
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Face Authentication",
                fontSize = 28.sp,
                color = Color(0xFF232323)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Face Icon

            Image(
                painter = painterResource(id = R.drawable.face_id),
                contentDescription = "Face Icon",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = infoText,
                fontSize = 18.sp,
                color = Color(0xFF232323)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (cameraPermissionGranted) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            this.scaleType = PreviewView.ScaleType.FILL_CENTER
                            startCamera(ctx, lifecycleOwner, onFaceDetected = { faceCount ->
                                if (!isProcessing && faceCount > 0) {
                                    isProcessing = true
                                    infoText = "Processing..."
                                    continueEnabled = false

                                    // delay 3 seconds, then mark verified and enable button
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        infoText = "Verified"
                                        continueEnabled = true
                                    }, 3000)
                                } else if (!isProcessing) {
                                    infoText = "Waiting for face..."
                                    continueEnabled = false
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (continueEnabled) {
                        onFaceAuthSuccess()
                    }
                },
                enabled = continueEnabled,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Text(text = "Continue", color = Color(0xFF232323))
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun PreviewView.startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onFaceDetected: (faceCount: Int) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

        val detector = FaceDetection.getClient(options)

        val imageAnalyzer = ImageAnalysis.Builder().build()

        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            processImageProxy(detector, imageProxy, onFaceDetected)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
        } catch (e: Exception) {
            Log.e("FaceAuthScreen", "Use case binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    detector: FaceDetector,
    imageProxy: ImageProxy,
    onFaceDetected: (faceCount: Int) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { faces ->
                onFaceDetected(faces.size)
            }
            .addOnFailureListener {
                Log.e("FaceAuthScreen", "Face detection failed", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
