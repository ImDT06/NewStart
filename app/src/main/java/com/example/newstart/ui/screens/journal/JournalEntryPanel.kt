package com.example.newstart.ui.screens.journal

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.LanguagePreviews
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryPanel(
    onDismiss: () -> Unit,
    onPost: (String, String, Uri?) -> Unit // Emoji, Text, ImageUri
) {
    var text by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("😊") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val emojis = listOf("😊", "🥰", "😴", "😫", "🚵", "🔥", "✨", "🎉")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nút Close ở góc trên bên trái
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
            }
        }

        // Camera Preview hoặc Ảnh đã chụp - Hình vuông, bo góc cực lớn
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(48.dp)) // Bo góc TikTok/Locket
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (capturedImageUri != null) {
                AsyncImage(
                    model = capturedImageUri,
                    contentDescription = "Captured Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    imageCapture = imageCapture
                )
            }

            // TEXT OVERLAY: Tự động co dãn theo nội dung (Wrap Content)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, end = 32.dp, bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Kỹ thuật "Ghost Text" nâng cao: Đo kích thước nội dung thật
                        Text(
                            text = if (text.isEmpty()) "Bạn đang nghĩ gì?" else text,
                            color = Color.Transparent, 
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 2.dp) // Giữ khoảng trống nhỏ cho cursor
                        )

                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White, 
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.matchParentSize(), // Buộc TextField khớp chính xác với Ghost Text
                            decorationBox = { innerTextField ->
                                if (text.isEmpty()) {
                                    Text(
                                        "Bạn đang nghĩ gì?", 
                                        color = Color.White.copy(alpha = 0.6f), 
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // BOTTOM CONTROLS: [Retake (X)] [Capture/Send] [Spacer]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút Hủy ảnh đã chụp (X)
            if (capturedImageUri != null) {
                IconButton(
                    onClick = { capturedImageUri = null },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Retake", 
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Nút Chụp / Nút Gửi (Hiện icon gửi nếu có ảnh HOẶC có text)
            val isReadyToPost = capturedImageUri != null || text.isNotBlank()
            
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        if (isReadyToPost) {
                            onPost(selectedEmoji, text, capturedImageUri)
                        } else {
                            takePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                executor = executor,
                                onImageCaptured = { capturedImageUri = it },
                                onError = { Log.e("Camera", "Capture failed", it) }
                            )
                        }
                    },
                shape = CircleShape,
                color = if (isReadyToPost) Color.White else Color.Transparent,
                border = if (!isReadyToPost) BorderStroke(6.dp, Color.LightGray) else BorderStroke(1.dp, Color.LightGray)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!isReadyToPost) {
                        // Nút chụp: vòng tròn trong
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                    } else {
                        // Icon gửi
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF1D5FE2),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Emoji Selection
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(emojis) { emoji ->
                val isSelected = selectedEmoji == emoji
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFF0F0F0) else Color.Transparent)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Color(0xFF1D5FE2) else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedEmoji = emoji },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji, 
                        fontSize = if (isSelected) 32.sp else 28.sp // Phóng to nhẹ khi chọn
                    )
                }
            }
        }
        
        // Thêm Spacer ở cuối để đẩy nội dung lên cao
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPreview = LocalInspectionMode.current
    
    if (isPreview) {
        Box(
            modifier = modifier.background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera Preview", color = Color.White)
        }
        return
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ImplementationMode.COMPATIBLE.let { PreviewView.ScaleType.FILL_CENTER }
            }
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
}

private fun takePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val outputDirectory = getOutputDirectory(context)
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }
        }
    )
}

private fun getOutputDirectory(context: android.content.Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}

@LanguagePreviews
@Composable
fun JournalEntryPanelPreview() {
    NewStartTheme {
        Surface(color = Color.White) {
            JournalEntryPanel(
                onDismiss = {},
                onPost = { _, _, _ -> }
            )
        }
    }
}
