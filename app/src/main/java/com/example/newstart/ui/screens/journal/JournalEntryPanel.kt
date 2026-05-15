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
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.media.MediaActionSound
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.concurrent.futures.await
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryPanel(
    onDismiss: () -> Unit,
    onPost: (String, String, Uri?) -> Unit, // Emoji, Text, ImageUri
    isUploading: Boolean = false
) {
    var text by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("😊") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isTextOnlyMode by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val mediaActionSound = remember { MediaActionSound() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    // Nạp sẵn âm thanh để tránh bị trễ
    LaunchedEffect(Unit) {
        mediaActionSound.load(MediaActionSound.SHUTTER_CLICK)
        mediaActionSound.load(MediaActionSound.FOCUS_COMPLETE)
    }
    
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedImageUri = uri
            isTextOnlyMode = false
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Close", 
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = "Tạo khoảnh khắc",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Camera Preview hoặc Ảnh đã chụp - Tỉ lệ Vuông (1:1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(if (isTextOnlyMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (capturedImageUri != null) {
                AsyncImage(
                    model = capturedImageUri,
                    contentDescription = "Captured Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isTextOnlyMode) {
                // Chế độ chỉ nhập văn bản
                Icon(
                    Icons.Default.EditNote, 
                    contentDescription = null, 
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            } else if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    imageCapture = imageCapture,
                    lensFacing = lensFacing,
                    flashMode = flashMode,
                    zoomRatio = zoomRatio
                )
                
                // Camera Controls Overlays
                Box(modifier = Modifier.fillMaxSize()) {
                    // Flash & Flip Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { 
                                flashMode = when(flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (flashMode == ImageCapture.FLASH_MODE_OFF) Icons.Default.FlashOff else Icons.Default.FlashOn,
                                contentDescription = "Flash",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { 
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                                    CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK 
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Flip",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Zoom Controls
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ZoomOption(label = "1x", isSelected = zoomRatio == 1f) { zoomRatio = 1f }
                        ZoomOption(label = "2x", isSelected = zoomRatio == 2f) { zoomRatio = 2f }
                    }
                }
            }

            // TEXT OVERLAY - Hiện khi ĐÃ chụp xong HOẶC ở TextOnlyMode
            if (capturedImageUri != null || isTextOnlyMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = (if (isTextOnlyMode) MaterialTheme.colorScheme.primary else Color.Black).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text.ifEmpty { "Bạn đang nghĩ gì?" },
                                color = Color.Transparent, 
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
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
                                modifier = Modifier.matchParentSize(),
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
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main Controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Capture Button / Send Button Logic
            val hasContent = capturedImageUri != null || text.isNotBlank()
            val showSendButton = capturedImageUri != null || isTextOnlyMode
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showSendButton) {
                    // Send Button - Enabled only when there's text or an image
                    FilledIconButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPost(selectedEmoji, text, capturedImageUri) 
                        },
                        enabled = hasContent && !isUploading,
                        modifier = Modifier.size(80.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, 
                                contentDescription = "Send",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                } else {
                    // Gradient Capture Button (Only in Camera Mode with no image)
                    val gradientBrush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(width = 5.dp, brush = gradientBrush, shape = CircleShape)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        try {
                                            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
                                        } catch (e: Exception) {
                                            Log.e("Camera", "Shutter sound failed", e)
                                        }
                                        takePhoto(
                                            context = context,
                                            imageCapture = imageCapture,
                                            executor = executor,
                                            onImageCaptured = { 
                                                capturedImageUri = it 
                                                isTextOnlyMode = false
                                            },
                                            onError = { Log.e("Camera", "Capture failed", it) }
                                        )
                                    }
                                )
                            }
                    )
                }

                // Retake / Back to Camera Button
                if (capturedImageUri != null || isTextOnlyMode) {
                    IconButton(
                        onClick = { 
                            capturedImageUri = null
                            isTextOnlyMode = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 48.dp)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (capturedImageUri != null) Icons.Default.Refresh else Icons.Default.PhotoCamera,
                            contentDescription = "Camera"
                        )
                    }
                }
            }

            // Gallery Button
            if (capturedImageUri == null && !isTextOnlyMode) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Chọn từ thư viện", fontWeight = FontWeight.Bold)
                }
            }

            // Skip Photo Text
            if (capturedImageUri == null && !isTextOnlyMode) {
                TextButton(onClick = { isTextOnlyMode = true }) {
                    Text(
                        "Bỏ qua ảnh",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

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
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedEmoji = emoji 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji, 
                        fontSize = if (isSelected) 32.sp else 28.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ZoomOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFFFFCC00) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    zoomRatio: Float = 1f
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPreview = LocalInspectionMode.current
    val mediaActionSound = remember { MediaActionSound() }
    
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    
    // Tự động xóa vòng tròn lấy nét sau 1 giây
    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            kotlinx.coroutines.delay(1000)
            focusTapOffset = null
        }
    }
    
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
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(cameraProviderFuture) {
        cameraProvider = cameraProviderFuture.await()
    }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            update = { previewView ->
                val safeCameraProvider = cameraProvider ?: return@AndroidView
                
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                imageCapture.flashMode = flashMode

                try {
                    safeCameraProvider.unbindAll()
                    val cameraInstance = safeCameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    cameraInstance.cameraControl.setZoomRatio(zoomRatio)

                    // Thiết lập chạm để lấy nét - Cải tiến: Thêm Log để kiểm tra
                    previewView.setOnTouchListener { v, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            
                            cameraInstance.cameraControl.startFocusAndMetering(action)
                            
                            // Hiển thị vòng tròn và phát âm thanh
                            focusTapOffset = Offset(event.x, event.y)
                            try {
                                mediaActionSound.play(MediaActionSound.FOCUS_COMPLETE)
                            } catch (e: Exception) {
                                Log.e("Camera", "Sound play failed", e)
                            }
                            v.performClick()
                            true
                        } else {
                            true // Trả về true để nhận các sự kiện tiếp theo
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Vòng tròn lấy nét Overlay - Đảm bảo vẽ đè lên AndroidView
        focusTapOffset?.let { offset ->
            Box(Modifier.fillMaxSize()) {
                FocusCircle(offset)
            }
        }
    }
}

@Composable
fun FocusCircle(offset: Offset) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .offset(
                x = (offset.x / LocalContext.current.resources.displayMetrics.density).dp - 35.dp,
                y = (offset.y / LocalContext.current.resources.displayMetrics.density).dp - 35.dp
            )
            .size(70.dp)
            .border(2.dp, Color(0xFFFFCC00).copy(alpha = 0.8f), CircleShape)
            .padding(10.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .border(1.dp, Color(0xFFFFCC00).copy(alpha = 0.5f), CircleShape)
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

@AppCombinedPreviews
@Composable
fun JournalEntryPanelPreview() {
    NewStartTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            JournalEntryPanel(
                onDismiss = {},
                onPost = { _, _, _ -> }
            )
        }
    }
}
