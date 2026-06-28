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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import com.example.newstart.R
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.model.MovieDetails
import com.example.newstart.domain.model.BookDetails
import com.example.newstart.domain.model.SubjectDetails
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
    onPost: (String, String, Uri?, String?, JournalType, MovieDetails?, BookDetails?, SubjectDetails?) -> Unit,
    isUploading: Boolean = false,
    suggestedEmojis: List<String> = emptyList(),
    isSuggesting: Boolean = false,
    suggestedMovieTitles: List<String> = emptyList(),
    suggestedBookTitles: List<String> = emptyList(),
    suggestedSubjectNames: List<String> = emptyList(),
    suggestedTags: List<String> = emptyList(),
    onTextChanged: (String) -> Unit = {},
    onCancelUpload: () -> Unit = {},
    onDirtyStateChanged: (Boolean) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("😊") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturedImage by remember { mutableStateOf(false) }
    var isTextOnlyMode by remember { mutableStateOf(false) }

    val lastWord = remember(text) { text.split(Regex("\\s+")).lastOrNull() ?: "" }
    val isTypingTag = remember(lastWord) { lastWord.startsWith("#") }
    val tagSuggestions = remember(isTypingTag, lastWord, suggestedTags) {
        if (isTypingTag) {
            suggestedTags.filter { it.startsWith(lastWord, ignoreCase = true) && it != lastWord }
        } else {
            emptyList()
        }
    }

    var selectedType by remember { mutableStateOf(JournalType.NORMAL) }
    var movieTitle by remember { mutableStateOf("") }
    var movieRating by remember { mutableFloatStateOf(0f) }
    var bookTitle by remember { mutableStateOf("") }
    var bookRating by remember { mutableFloatStateOf(0f) }
    var subjectName by remember { mutableStateOf("") }
    var understandingLevel by remember { mutableIntStateOf(3) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var emojiOffset by remember { mutableStateOf(Offset.Zero) }
    val bounceAnim = remember { Animatable(1f) }
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val mediaActionSound = remember { MediaActionSound() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    val journalEmojis = listOf("😫", "😔", "😐", "😊", "🥰")
    val moodIcons = mapOf("😫" to R.drawable.ic_mood_very_bad, "😔" to R.drawable.ic_mood_bad, "😐" to R.drawable.ic_mood_neutral, "😊" to R.drawable.ic_mood_good, "🥰" to R.drawable.ic_mood_very_good)
    
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var currentCameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var currentCameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardVisible = keyboardHeight > 0.dp
    val bottomPadding = if (keyboardHeight < 102.dp) 110.dp - keyboardHeight else 8.dp

    LaunchedEffect(text, capturedImageUri, isTextOnlyMode, movieTitle) {
        onDirtyStateChanged(text.isNotBlank() || capturedImageUri != null || isTextOnlyMode || movieTitle.isNotBlank())
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { capturedImageUri = uri; isCapturedImage = false; isTextOnlyMode = false }
    }

    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }
    LaunchedEffect(Unit) { if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        // LAYER 1: Header & Camera (Cố định, không đẩy)
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (text.isNotBlank() || capturedImageUri != null) showDiscardDialog = true else onDismiss() }) {
                    Icon(Icons.Default.Close, null)
                }
                Text(text = stringResource(R.string.journal_panel_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(48.dp))
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(if (isTextOnlyMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (capturedImageUri != null) {
                    AsyncImage(model = capturedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (isTextOnlyMode) {
                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
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
                        // Flash & Flip Buttons
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

                        // Zoom Controls
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

                            ZoomOption(label = "2x", isSelected = currentZoomRatio in 1.5f..2.8f) {
                                scope.launch {
                                    val minZ = currentCameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                                    val maxZ = currentCameraInfo?.zoomState?.value?.maxZoomRatio ?: 10f
                                    val target = 2.0f.coerceIn(minZ, maxZ)
                                    val anim = Animatable(currentZoomRatio)
                                    anim.animateTo(target, tween(400, easing = FastOutSlowInEasing)) {
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

                // Category metadata overlay (shows directly on the image/preview and is interactive)
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedType != JournalType.NORMAL,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (selectedType) {
                                JournalType.MOVIE -> {
                                    InteractiveRatingBar(
                                        rating = movieRating,
                                        onRatingChanged = { movieRating = it }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("“", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        BasicTextField(
                                            value = movieTitle,
                                            onValueChange = { movieTitle = it },
                                            textStyle = LocalTextStyle.current.copy(
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            ),
                                            cursorBrush = SolidColor(Color.White),
                                            modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = if (movieTitle.isEmpty()) 120.dp else 0.dp),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (movieTitle.isEmpty()) {
                                                        Text(
                                                            text = "Nhập tên phim...",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        Text("”", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (movieTitle.isEmpty() && suggestedMovieTitles.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier.height(26.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            item {
                                                Text(
                                                    text = "Gợi ý:",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            items(suggestedMovieTitles) { title ->
                                                Surface(
                                                    onClick = { movieTitle = title },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                                ) {
                                                    Text(
                                                        text = title,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                JournalType.BOOK -> {
                                    InteractiveRatingBar(
                                        rating = bookRating,
                                        onRatingChanged = { bookRating = it }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("“", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        BasicTextField(
                                            value = bookTitle,
                                            onValueChange = { bookTitle = it },
                                            textStyle = LocalTextStyle.current.copy(
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            ),
                                            cursorBrush = SolidColor(Color.White),
                                            modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = if (bookTitle.isEmpty()) 120.dp else 0.dp),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (bookTitle.isEmpty()) {
                                                        Text(
                                                            text = "Nhập tên sách...",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        Text("”", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (bookTitle.isEmpty() && suggestedBookTitles.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier.height(26.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            item {
                                                Text(
                                                    text = "Gợi ý:",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            items(suggestedBookTitles) { title ->
                                                Surface(
                                                    onClick = { bookTitle = title },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                                ) {
                                                    Text(
                                                        text = title,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                JournalType.SUBJECT -> {
                                    InteractiveRatingBar(
                                        rating = understandingLevel.toFloat(),
                                        onRatingChanged = { understandingLevel = it.roundToInt() }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("“", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        BasicTextField(
                                            value = subjectName,
                                            onValueChange = { subjectName = it },
                                            textStyle = LocalTextStyle.current.copy(
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            ),
                                            cursorBrush = SolidColor(Color.White),
                                            modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = if (subjectName.isEmpty()) 120.dp else 0.dp),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (subjectName.isEmpty()) {
                                                        Text(
                                                            text = "Nhập tên môn...",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        Text("”", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (subjectName.isEmpty() && suggestedSubjectNames.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier.height(26.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            item {
                                                Text(
                                                    text = "Gợi ý:",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            items(suggestedSubjectNames) { name ->
                                                Surface(
                                                    onClick = { subjectName = name },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                                ) {
                                                    Text(
                                                        text = name,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }

                // Nút chụp lại (reset ảnh) góc dưới bên phải preview
                if (capturedImageUri != null || isTextOnlyMode) {
                    IconButton(
                        onClick = { 
                            capturedImageUri = null
                            isTextOnlyMode = false
                            selectedType = JournalType.NORMAL
                            movieTitle = ""
                            movieRating = 0f
                            bookTitle = ""
                            bookRating = 0f
                            subjectName = ""
                            understandingLevel = 3
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Chụp lại",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Đặt cụm nút chụp căn giữa hoàn hảo trong khoảng trống giữa preview và đáy
            if (!isKeyboardVisible && capturedImageUri == null && !isTextOnlyMode) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(56.dp)) { Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary) }
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).border(4.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(6.dp).clip(CircleShape).background(Color.White).clickable {
                            try { mediaActionSound.play(MediaActionSound.SHUTTER_CLICK) } catch (_: Exception) {}
                            takePhoto(context, imageCapture, executor, { capturedImageUri = it; isCapturedImage = true; isTextOnlyMode = false }, {})
                        })
                        IconButton(onClick = { isTextOnlyMode = true }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(56.dp)) { Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.secondary) }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }

        // LAYER 3: Vùng Nhập liệu & Nút Gửi (TỰ ĐẨY LÊN TRÊN BÀN PHÍM - CHỈ HIỆN KHI ĐÃ CÓ ẢNH HOẶC Ở CHẾ ĐỘ CHỈ NHẬP VĂN BẢN)
        if (capturedImageUri != null || isTextOnlyMode) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).imePadding().padding(bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emoji Selector
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        journalEmojis.forEach { emoji ->
                            val isSelected = selectedEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedEmoji = emoji
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(moodIcons[emoji] ?: R.drawable.ic_mood_neutral),
                                    contentDescription = null,
                                    modifier = Modifier.size(if (isSelected) 28.dp else 22.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (tagSuggestions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text(
                                text = "Gợi ý nhãn:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(tagSuggestions) { tag ->
                            Surface(
                                onClick = {
                                    val words = text.split(" ").toMutableList()
                                    if (words.isNotEmpty()) {
                                        words[words.lastIndex] = tag
                                        text = words.joinToString(" ") + " "
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = tag,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Ô nhập văn bản + Nút Gửi (Chỉ hiện nút gửi khi có bàn phím hoặc đã có nội dung)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text, onValueChange = { if (it.length <= 150) { text = it; onTextChanged(it) } },
                            placeholder = { Text(stringResource(R.string.journal_panel_placeholder), fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        
                        if (isKeyboardVisible || text.isNotBlank() || capturedImageUri != null) {
                            IconButton(
                                onClick = { 
                                    val isVietnamese = java.util.Locale.getDefault().language == "vi"
                                    if (selectedType == JournalType.MOVIE && movieTitle.trim().isEmpty()) {
                                        android.widget.Toast.makeText(context, if (isVietnamese) "Vui lòng nhập tên phim" else "Please enter movie title", android.widget.Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    if (selectedType == JournalType.BOOK && bookTitle.trim().isEmpty()) {
                                        android.widget.Toast.makeText(context, if (isVietnamese) "Vui lòng nhập tên sách" else "Please enter book title", android.widget.Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    if (selectedType == JournalType.SUBJECT && subjectName.trim().isEmpty()) {
                                        android.widget.Toast.makeText(context, if (isVietnamese) "Vui lòng nhập tên môn học" else "Please enter subject name", android.widget.Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }

                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPost(selectedEmoji, text, capturedImageUri, if (capturedImageUri != null) (if (isCapturedImage) "CAMERA" else "GALLERY") else null, selectedType, if (selectedType == JournalType.MOVIE) MovieDetails(title = movieTitle.trim(), rating = movieRating) else null, if (selectedType == JournalType.BOOK) BookDetails(title = bookTitle.trim(), rating = bookRating) else null, if (selectedType == JournalType.SUBJECT) SubjectDetails(name = subjectName.trim(), understandingLevel = understandingLevel) else null)
                                },
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                            ) {
                                if (isUploading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        JournalType.NORMAL to ("Nhật ký" to Icons.Default.EditNote),
                        JournalType.MOVIE to ("Phim" to Icons.Default.Movie),
                        JournalType.BOOK to ("Sách" to Icons.AutoMirrored.Filled.MenuBook),
                        JournalType.SUBJECT to ("Môn học" to Icons.Default.School)
                    )
                    items(types) { (type, pair) ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    text = pair.first,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = pair.second,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }
            }
        }
    }

    if (showDiscardDialog) { AlertDialog(onDismissRequest = { showDiscardDialog = false }, title = { Text("Hủy bỏ bài viết?") }, text = { Text("Nội dung sẽ bị mất.") }, confirmButton = { TextButton(onClick = { showDiscardDialog = false; onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Hủy bỏ") } }, dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Tiếp tục") } }) }
}

@Composable
fun InteractiveRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: androidx.compose.ui.unit.Dp = 20.dp
) {
    var rowWidth by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    val halfShape = remember {
        object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density
            ): androidx.compose.ui.graphics.Outline {
                return androidx.compose.ui.graphics.Outline.Rectangle(
                    androidx.compose.ui.geometry.Rect(0f, 0f, size.width * 0.5f, size.height)
                )
            }
        }
    }

    Row(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                rowWidth = coordinates.size.width
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (rowWidth > 0) {
                        val rawRating = (offset.x / rowWidth) * maxStars
                        val roundedRating = ((rawRating * 2).roundToInt() / 2f).coerceIn(0.5f, maxStars.toFloat())
                        if (roundedRating != rating) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRatingChanged(roundedRating)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (rowWidth > 0) {
                            val rawRating = (offset.x / rowWidth) * maxStars
                            val roundedRating = ((rawRating * 2).roundToInt() / 2f).coerceIn(0.5f, maxStars.toFloat())
                            if (roundedRating != rating) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRatingChanged(roundedRating)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (rowWidth > 0) {
                            val rawRating = (change.position.x / rowWidth) * maxStars
                            val roundedRating = ((rawRating * 2).roundToInt() / 2f).coerceIn(0.5f, maxStars.toFloat())
                            if (roundedRating != rating) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRatingChanged(roundedRating)
                            }
                        }
                    }
                )
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..maxStars).forEach { i ->
            Box(modifier = Modifier.size(starSize)) {
                if (rating >= i) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (rating <= i - 1f) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxSize()
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(halfShape)
                    )
                }
            }
        }
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
        } catch (e: Exception) { Log.e("CameraPreview", "Binding failed", e) }
    }
    DisposableEffect(cameraInfo) {
        val observer = androidx.lifecycle.Observer<ZoomState> { state ->
            currentZoomState = state
            onZoomRatioChanged(state.zoomRatio)
        }
        cameraInfo?.zoomState?.observe(lifecycleOwner, observer)
        onDispose { cameraInfo?.zoomState?.removeObserver(observer) }
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
                        try {
                            mediaActionSound.play(MediaActionSound.FOCUS_COMPLETE)
                        } catch (e: Exception) {}
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

private fun takePhoto(context: android.content.Context, imageCapture: ImageCapture, executor: ExecutorService, onImageCaptured: (Uri) -> Unit, onError: (ImageCaptureException) -> Unit) { 
    val outputDirectory = context.filesDir; val photoFile = File(outputDirectory, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg")
    imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(photoFile).build(), executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(e: ImageCaptureException) { onError(e) }
        override fun onImageSaved(r: ImageCapture.OutputFileResults) { processImageToSquare(photoFile); onImageCaptured(Uri.fromFile(photoFile)) }
    })
}

private fun processImageToSquare(file: File) { try { val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return; val exif = ExifInterface(file.absolutePath); val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL); val matrix = Matrix(); when (orientation) { ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f); ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f); ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f) }; val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true); val width = rotatedBitmap.width; val height = rotatedBitmap.height; val newDimension = if (width < height) width else height; val squareBitmap = Bitmap.createBitmap(rotatedBitmap, (width - newDimension) / 2, (height - newDimension) / 2, newDimension, newDimension); java.io.FileOutputStream(file).use { out -> squareBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) } } catch (e: Exception) {} }

@AppCombinedPreviews
@Composable
fun JournalEntryPanelPreview() { NewStartTheme { Surface(color = MaterialTheme.colorScheme.background) { JournalEntryPanel(onDismiss = {}, onPost = { _, _, _, _, _, _, _, _ -> }) } } }
