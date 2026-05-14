package com.example.newstart.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import com.example.newstart.R
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.LanguagePreviews
import com.example.newstart.ui.util.LanguagePickerDialog
import com.example.newstart.ui.util.TransparentLanguageSwitcher

private val WelDeepBlue = Color(0xFF0036D6)
private val WelLightBlue = Color(0xFF1E61FF)
private val WelWaveColor = Color(0xFF154EE8)
private val WelButtonBlue = Color(0xFF2970FF)

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var showLanguagePicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // Abstract Background Shapes (Waves)
        BackgroundGraphics(MaterialTheme.colorScheme.primaryContainer)

        // Language Switcher in Top Right
        TransparentLanguageSwitcher(
            onClick = { showLanguagePicker = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding() // Sử dụng statusBarsPadding đồng bộ với Login/Register
                .padding(top = 8.dp, end = 16.dp)
        )

        if (showLanguagePicker) {
            LanguagePickerDialog(onDismiss = { showLanguagePicker = false })
        }

        // Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section (Logo + Title)
            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.welcome_simple),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = stringResource(id = R.string.welcome_edu),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.offset(y = (-15).dp),
                    letterSpacing = 1.sp
                )
            }

            // Bottom Section (Card)
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.welcome_slogan),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Get Started Button
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.welcome_get_started),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sign Up Button
                    OutlinedButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)) {
                                    append(stringResource(id = R.string.welcome_new_here))
                                }
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)) {
                                    append(stringResource(id = R.string.welcome_sign_up))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundGraphics(accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Top Large Wave
        val path1 = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, height * 0.4f)
            quadraticTo(width * 0.5f, height * 0.6f, 0f, height * 0.4f)
            close()
        }
        drawPath(path1, accentColor.copy(alpha = 0.5f))

        // Middle Overlapping Wave
        val path2 = Path().apply {
            moveTo(0f, height * 0.3f)
            cubicTo(width * 0.3f, height * 0.2f, width * 0.7f, height * 0.5f, width, height * 0.4f)
            lineTo(width, height * 0.7f)
            cubicTo(width * 0.7f, height * 0.8f, width * 0.3f, height * 0.6f, 0f, height * 0.7f)
            close()
        }
        drawPath(path2, accentColor.copy(alpha = 0.3f))

        // Bottom Left Wave
        val path3 = Path().apply {
            moveTo(0f, height * 0.6f)
            quadraticTo(width * 0.4f, height * 0.8f, 0f, height)
            close()
        }
        drawPath(path3, accentColor.copy(alpha = 0.4f))
    }
}

@LanguagePreviews
@Composable
fun WelcomeScreenPreview() {
    NewStartTheme {
        WelcomeScreen(
            onNavigateToLogin = {},
            onNavigateToRegister = {}
        )
    }
}
