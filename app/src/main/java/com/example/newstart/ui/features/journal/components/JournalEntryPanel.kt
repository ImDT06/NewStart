package com.example.newstart.ui.features.journal.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ScaleGestureDetector
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaActionSound
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.concurrent.futures.await
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryPanel(
    onDismiss: () -> Unit,
    onPost: (String, String, Uri?) -> Unit,
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
    
    LaunchedEffect(Unit) {
        mediaActionSound.load(MediaActionSound.SHUTTER_CLICK)
        mediaActionSound.load(MediaActionSound.FOCUS_COMPLETE)
    }
    
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    
    var currentCameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var currentCameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    
    val scope = rememberCoroutineScope()

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
                text = stringResource(R.string.journal_panel_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Camera Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(if (isTextOnlyMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (capturedImageUri != null) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(capturedImageUri)
                        .crossfade(true)
                        .size(coil.size.Size.ORIGINAL) 
                        .build(),
                    contentDescription = "Captured Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isTextOnlyMode) {
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
                    onCameraReady = { info, control ->
                        currentCameraInfo = info
                        currentCameraControl = control
                    },
                    onZoomRatioChanged = { currentZoomRatio = it }
                )
                
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { 
                                flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
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

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ZoomOption(label = "1x", isSelected = currentZoomRatio < 1.5f) { 
                            scope.launch {
                                val anim = Animatable(currentZoomRatio)
                                anim.animateTo(1.0f, tween(400, easing = FastOutSlowInEasing)) {
                                    currentCameraControl?.setZoomRatio(value)
                                }
                            }
                        }
                        ZoomOption(label = "3x", isSelected = currentZoomRatio >= 2.8f && currentZoomRatio <= 3.2f) { 
                            scope.launch {
                                val minZ = currentCameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                                val maxZ = currentCameraInfo?.zoomState?.value?.maxZoomRatio ?: 10f
                                val target = 3.0f.coerceIn(minZ, maxZ)
                                
                                val anim = Animatable(currentZoomRatio)
                                anim.animateTo(target, tween(400, easing = FastOutSlowInEasing)) {
                                    currentCameraControl?.setZoomRatio(value)
                                }
                            }
                        }
                    }
                }
            }

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
                            val placeholder = stringResource(R.string.journal_panel_placeholder)
                            Text(
                                text = text.ifEmpty { placeholder },
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
                                            placeholder, 
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

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val hasContent = capturedImageUri != null || text.isNotBlank()
            val showSendButton = capturedImageUri != null || isTextOnlyMode
            
            if (showSendButton) {
                FilledIconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPost(selectedEmoji, text, capturedImageUri) 
                    },
                    enabled = hasContent && !isUploading,
                    modifier = Modifier.size(88.dp), 
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
                                    try { mediaActionSound.play(MediaActionSound.SHUTTER_CLICK) } catch (e: Exception) {}
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

        Spacer(modifier = Modifier.weight(1f))

        if (capturedImageUri == null && !isTextOnlyMode) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                    Text(stringResource(R.string.journal_panel_gallery), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { isTextOnlyMode = true }) {
                    Text(
                        stringResource(R.string.journal_panel_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

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
    onCameraReady: (CameraInfo, CameraControl) -> Unit = { _, _ -> },
    onZoomRatioChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPreview = LocalInspectionMode.current
    val mediaActionSound = remember { MediaActionSound() }
    
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    
    var currentZoomState by remember { mutableStateOf<ZoomState?>(null) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewUseCase = remember { Preview.Builder().build() }

    LaunchedEffect(cameraProviderFuture) {
        cameraProvider = cameraProviderFuture.await()
    }
    
    LaunchedEffect(cameraProvider, lensFacing) {
        val safeProvider = cameraProvider ?: return@LaunchedEffect
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            safeProvider.unbindAll()
            val camera = safeProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, imageCapture)
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            onCameraReady(camera.cameraInfo, camera.cameraControl)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Binding failed", e)
        }
    }

    DisposableEffect(cameraInfo) {
        val observer = androidx.lifecycle.Observer<ZoomState> { state ->
            currentZoomState = state
            onZoomRatioChanged(state.zoomRatio)
        }
        cameraInfo?.zoomState?.observe(lifecycleOwner, observer)
        onDispose {
            cameraInfo?.zoomState?.removeObserver(observer)
        }
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            kotlinx.coroutines.delay(1000)
            focusTapOffset = null
        }
    }
    
    if (isPreview) {
        Box(modifier = modifier.background(Color.DarkGray), contentAlignment = Alignment.Center) {
            Text("Camera Preview", color = Color.White)
        }
        return
    }

    Box(modifier = modifier) {
        val scaleGestureDetector = remember(context, cameraInfo) {
            ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val currentLinear = cameraInfo?.zoomState?.value?.linearZoom ?: 0f
                    val sensitivity = if (detector.scaleFactor < 1f) 2.5f else 2.0f
                    val delta = (detector.scaleFactor - 1f) * sensitivity
                    cameraControl?.setLinearZoom((currentLinear + delta).coerceIn(0f, 1f))
                    return true
                }
            })
        }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewUseCase.setSurfaceProvider(this.surfaceProvider)
                }
            },
            update = { previewView ->
                previewView.setOnTouchListener { v, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    if (event.action == android.view.MotionEvent.ACTION_UP && event.pointerCount == 1) {
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cameraControl?.startFocusAndMetering(action)
                        focusTapOffset = Offset(event.x, event.y)
                        try { mediaActionSound.play(MediaActionSound.FOCUS_COMPLETE) } catch (e: Exception) {}
                        v.performClick()
                    }
                    true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        focusTapOffset?.let { offset -> FocusCircle(offset) }
        
        val zoomRatio = currentZoomState?.zoomRatio ?: 1.0f
        if (zoomRatio > 1.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) 
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = String.format("%.1fx", zoomRatio),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
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
            .offset {
                IntOffset(
                    (offset.x - 35.dp.toPx()).roundToInt(),
                    (offset.y - 35.dp.toPx()).roundToInt()
                )
            }
            .size(70.dp)
            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            .padding(10.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
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
                // Xử lý cắt ảnh vuông sau khi lưu thành công
                processImageToSquare(photoFile)
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }
        }
    )
}

private fun processImageToSquare(file: File) {
    try {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        
        // Đọc thông tin xoay từ EXIF
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        // Tạo bitmap đã xoay đúng chiều
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        // Cắt vuông (Center Crop)
        val width = rotatedBitmap.width
        val height = rotatedBitmap.height
        val newDimension = if (width < height) width else height
        
        val xOffset = (width - newDimension) / 2
        val yOffset = (height - newDimension) / 2
        
        val squareBitmap = Bitmap.createBitmap(rotatedBitmap, xOffset, yOffset, newDimension, newDimension)
        
        // Lưu đè lại file cũ
        java.io.FileOutputStream(file).use { out ->
            squareBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        // Giải phóng bộ nhớ
        if (bitmap != rotatedBitmap) bitmap.recycle()
        rotatedBitmap.recycle()
        squareBitmap.recycle()
    } catch (e: Exception) {
        Log.e("ImageProcess", "Failed to crop square", e)
    }
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
