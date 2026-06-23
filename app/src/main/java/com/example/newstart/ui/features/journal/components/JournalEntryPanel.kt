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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.res.painterResource
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
    onPost: (String, String, Uri?, String?) -> Unit,
    isUploading: Boolean = false,
    suggestedEmojis: List<String> = emptyList(),
    isSuggesting: Boolean = false,
    onTextChanged: (String) -> Unit = {},
    onCancelUpload: () -> Unit = {},
    onDirtyStateChanged: (Boolean) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("😊") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturedImage by remember { mutableStateOf(false) }
    var isTextOnlyMode by remember { mutableStateOf(false) }
    
    var showDiscardDialog by remember { mutableStateOf(false) }
    var emojiOffset by remember { mutableStateOf(Offset.Zero) }
    val bounceAnim = remember { Animatable(1f) }
    
    // Theo dõi trạng thái "Dirty" (có dữ liệu chưa lưu)
    LaunchedEffect(text, capturedImageUri, isTextOnlyMode) {
        val isDirty = text.isNotBlank() || capturedImageUri != null || isTextOnlyMode
        onDirtyStateChanged(isDirty)
    }
    
    LaunchedEffect(selectedEmoji) {
        bounceAnim.snapTo(0.6f)
        bounceAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val mediaActionSound = remember { MediaActionSound() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    val journalEmojis = listOf("😫", "😔", "😐", "😊", "🥰")
    
    val moodIcons = remember {
        mapOf(
            "😫" to R.drawable.ic_mood_very_bad,
            "😔" to R.drawable.ic_mood_bad,
            "😐" to R.drawable.ic_mood_neutral,
            "😊" to R.drawable.ic_mood_good,
            "🥰" to R.drawable.ic_mood_very_good
        )
    }
    
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
    
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    val uploadTransition = rememberInfiniteTransition(label = "uploading")
    val uploadRotation by uploadTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "uploadRotation"
    )
    val uploadPulseScale by uploadTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "uploadPulseScale"
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedImageUri = uri
            isCapturedImage = false
            isTextOnlyMode = false
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
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
                IconButton(onClick = {
                    if (text.isNotBlank() || capturedImageUri != null) {
                        showDiscardDialog = true
                    } else {
                        onDismiss()
                    }
                }) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = stringResource(R.string.journal_panel_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Camera Preview / Selected Image
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
                        model = capturedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = if (isCapturedImage) ContentScale.Crop else ContentScale.Fit
                    )
                } else if (isTextOnlyMode) {
                    Icon(
                        Icons.Default.EditNote, 
                        null, 
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
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { 
                                    flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape).size(40.dp)
                            ) {
                                Icon(
                                    if (flashMode == ImageCapture.FLASH_MODE_OFF) Icons.Default.FlashOff else Icons.Default.FlashOn,
                                    "Flash", tint = Color.White, modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { 
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK 
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape).size(40.dp)
                            ) {
                                Icon(Icons.Default.FlipCameraAndroid, "Flip", tint = Color.White, modifier = Modifier.size(20.dp))
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
                            ZoomOption(label = "3x", isSelected = currentZoomRatio in 2.8f..3.2f) { 
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

                // 1. Draggable Emoji Stamp
                if (capturedImageUri != null || isTextOnlyMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .offset { IntOffset(emojiOffset.x.roundToInt(), emojiOffset.y.roundToInt()) }
                            .graphicsLayer {
                                scaleX = bounceAnim.value
                                scaleY = bounceAnim.value
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    emojiOffset += dragAmount
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = { emojiOffset = Offset.Zero })
                            }
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = moodIcons[selectedEmoji] ?: R.drawable.ic_mood_neutral),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // 2. Floating Text field & Emoji Selector
                androidx.compose.animation.AnimatedVisibility(
                    visible = capturedImageUri != null || isTextOnlyMode,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            journalEmojis.forEach { emoji ->
                                val isSelected = selectedEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedEmoji = emoji
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = moodIcons[emoji] ?: R.drawable.ic_mood_neutral),
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isSelected) 36.dp else 28.dp)
                                            .graphicsLayer {
                                                if (isSelected) {
                                                    scaleX = 1.15f
                                                    scaleY = 1.15f
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        Surface(
                            color = (if (isTextOnlyMode) MaterialTheme.colorScheme.primary else Color.Black).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .wrapContentWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .wrapContentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val placeholder = stringResource(R.string.journal_panel_placeholder)
                                Text(
                                    text = text.ifEmpty { placeholder },
                                    color = Color.Transparent, 
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                BasicTextField(
                                    value = text,
                                    onValueChange = { 
                                        if (it.length <= 150) {
                                            text = it
                                            onTextChanged(it)
                                        }
                                    },
                                    textStyle = LocalTextStyle.current.copy(
                                        color = Color.White, 
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.matchParentSize(),
                                    decorationBox = { innerTextField ->
                                        if (text.isEmpty()) {
                                            Text(placeholder, color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp, textAlign = TextAlign.Center)
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

            AnimatedVisibility(
                visible = !isKeyboardVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val hasContent = capturedImageUri != null || text.isNotBlank()
                    if (capturedImageUri != null || isTextOnlyMode) {
                        FilledIconButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val source = if (capturedImageUri != null) (if (isCapturedImage) "CAMERA" else "GALLERY") else null
                                onPost(selectedEmoji, text, capturedImageUri, source)
                            },
                            enabled = hasContent && !isUploading,
                            modifier = Modifier.size(88.dp), 
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(36.dp))
                            }
                        }
                    } else {
                        val gradientBrush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                        Box(
                            modifier = Modifier.size(88.dp).clip(CircleShape).border(width = 5.dp, brush = gradientBrush, shape = CircleShape).padding(8.dp).clip(CircleShape).background(Color.White)
                                .pointerInput(Unit) {
                                    detectTapGestures(onPress = {
                                        try { mediaActionSound.play(MediaActionSound.SHUTTER_CLICK) } catch (e: Exception) {}
                                        takePhoto(context, imageCapture, executor, { uri -> capturedImageUri = uri; isCapturedImage = true; isTextOnlyMode = false }, { Log.e("Camera", "Capture failed", it) })
                                    })
                                }
                        )
                    }

                    if (capturedImageUri != null || isTextOnlyMode) {
                        IconButton(
                            onClick = { capturedImageUri = null; isTextOnlyMode = false },
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp).size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(if (capturedImageUri != null) Icons.Default.Refresh else Icons.Default.PhotoCamera, "Camera")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = capturedImageUri == null && !isTextOnlyMode && !isKeyboardVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.journal_panel_gallery), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { isTextOnlyMode = true }) {
                        Text(stringResource(R.string.journal_panel_skip), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
            if (!isKeyboardVisible) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (isUploading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(32.dp)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)).padding(horizontal = 32.dp, vertical = 40.dp)
                ) {
                    Box(modifier = Modifier.size(100.dp).graphicsLayer { scaleX = uploadPulseScale; scaleY = uploadPulseScale }, contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color.Transparent)), CircleShape))
                        CircularProgressIndicator(progress = { 0.35f }, color = MaterialTheme.colorScheme.primary, strokeWidth = 3.5.dp, modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = uploadRotation })
                        Image(painter = painterResource(id = moodIcons[selectedEmoji] ?: R.drawable.ic_mood_neutral), contentDescription = null, modifier = Modifier.size(52.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Đang ghi lại hành trình...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Vui lòng đợi trong giây lát", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onCancelUpload,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                        modifier = Modifier.background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Close, "Cancel", modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hủy gửi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Hủy bỏ bài viết?") },
            text = { Text("Mọi nội dung bạn vừa nhập hoặc ảnh vừa chụp sẽ bị mất. Bạn có chắc chắn muốn thoát?") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showDiscardDialog = false
                        onDismiss() 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hủy bỏ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Tiếp tục viết")
                }
            }
        )
    }
}

@Composable
fun ZoomOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(CircleShape).background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text = label, color = if (isSelected) Color(0xFFFFCC00) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier, imageCapture: ImageCapture, lensFacing: Int = CameraSelector.LENS_FACING_BACK, flashMode: Int = ImageCapture.FLASH_MODE_OFF, onCameraReady: (CameraInfo, CameraControl) -> Unit = { _, _ -> }, onZoomRatioChanged: (Float) -> Unit = {}) {
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
    LaunchedEffect(cameraProviderFuture) { cameraProvider = cameraProviderFuture.await() }
    LaunchedEffect(cameraProvider, lensFacing) {
        val safeProvider = cameraProvider ?: return@LaunchedEffect
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            safeProvider.unbindAll()
            val camera = safeProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, imageCapture)
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            onCameraReady(camera.cameraInfo, camera.cameraControl)
        } catch (e: Exception) { Log.e("CameraPreview", "Binding failed", e) }
    }
    DisposableEffect(cameraInfo) {
        val observer = androidx.lifecycle.Observer<ZoomState> { state -> currentZoomState = state; onZoomRatioChanged(state.zoomRatio) }
        cameraInfo?.zoomState?.observe(lifecycleOwner, observer)
        onDispose { cameraInfo?.zoomState?.removeObserver(observer) }
    }
    LaunchedEffect(flashMode) { imageCapture.flashMode = flashMode }
    LaunchedEffect(focusTapOffset) { if (focusTapOffset != null) { kotlinx.coroutines.delay(1000); focusTapOffset = null } }
    if (isPreview) { Box(modifier = modifier.background(Color.DarkGray), contentAlignment = Alignment.Center) { Text("Camera Preview", color = Color.White) }; return }
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
        AndroidView(factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE; previewUseCase.setSurfaceProvider(this.surfaceProvider) } }, update = { previewView -> previewView.setOnTouchListener { v, event -> scaleGestureDetector.onTouchEvent(event); if (event.action == android.view.MotionEvent.ACTION_UP && event.pointerCount == 1) { val factory = previewView.meteringPointFactory; val point = factory.createPoint(event.x, event.y); val action = FocusMeteringAction.Builder(point).build(); cameraControl?.startFocusAndMetering(action); focusTapOffset = Offset(event.x, event.y); try { mediaActionSound.play(MediaActionSound.FOCUS_COMPLETE) } catch (e: Exception) {}; v.performClick() }; true } }, modifier = Modifier.fillMaxSize())
        focusTapOffset?.let { offset -> FocusCircle(offset) }
        val zoomRatio = currentZoomState?.zoomRatio ?: 1.0f
        if (zoomRatio > 1.01f) { Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(text = String.format("%.1fx", zoomRatio), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
    }
}

@Composable
fun FocusCircle(offset: Offset) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus")
    val scale by infiniteTransition.animateFloat(initialValue = 1.2f, targetValue = 0.8f, animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "scale")
    Box(modifier = Modifier.offset { IntOffset((offset.x - 35.dp.toPx()).roundToInt(), (offset.y - 35.dp.toPx()).roundToInt()) }.size(70.dp).border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape).padding(10.dp).graphicsLayer { scaleX = scale; scaleY = scale }.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape))
}

private fun takePhoto(context: android.content.Context, imageCapture: ImageCapture, executor: ExecutorService, onImageCaptured: (Uri) -> Unit, onError: (ImageCaptureException) -> Unit) {
    val outputDirectory = getOutputDirectory(context)
    val photoFile = File(outputDirectory, SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exception: ImageCaptureException) { onError(exception) }
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) { processImageToSquare(photoFile); val savedUri = Uri.fromFile(photoFile); onImageCaptured(savedUri) }
    })
}

private fun processImageToSquare(file: File) {
    try {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val width = rotatedBitmap.width
        val height = rotatedBitmap.height
        val newDimension = if (width < height) width else height
        val xOffset = (width - newDimension) / 2
        val yOffset = (height - newDimension) / 2
        val squareBitmap = Bitmap.createBitmap(rotatedBitmap, xOffset, yOffset, newDimension, newDimension)
        java.io.FileOutputStream(file).use { out -> squareBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
        if (bitmap != rotatedBitmap) bitmap.recycle()
        rotatedBitmap.recycle(); squareBitmap.recycle()
    } catch (e: Exception) { Log.e("ImageProcess", "Failed to crop square", e) }
}

private fun getOutputDirectory(context: android.content.Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let { File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() } }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}

@AppCombinedPreviews
@Composable
fun JournalEntryPanelPreview() {
    NewStartTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            JournalEntryPanel(onDismiss = {}, onPost = { _, _, _, _ -> })
        }
    }
}
