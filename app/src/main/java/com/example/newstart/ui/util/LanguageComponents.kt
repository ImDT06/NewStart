package com.example.newstart.ui.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newstart.R

@Composable
fun SmallLanguageSwitcher(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isVietnamese = currentLocale.contains("vi") || currentLocale.isEmpty()

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        shadowElevation = if (backgroundColor == Color.Transparent) 0.dp else 2.dp,
        border = if (backgroundColor == Color.Transparent) BorderStroke(1.dp, tintColor.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Image(
                    painter = painterResource(id = if (isVietnamese) R.drawable.ic_flag_vn else R.drawable.ic_flag_en),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun TransparentLanguageSwitcher(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isVietnamese = currentLocale.contains("vi") || currentLocale.isEmpty()

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Image(
                    painter = painterResource(id = if (isVietnamese) R.drawable.ic_flag_vn else R.drawable.ic_flag_en),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Đảm bảo ảnh luôn lấp đầy hình tròn
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
