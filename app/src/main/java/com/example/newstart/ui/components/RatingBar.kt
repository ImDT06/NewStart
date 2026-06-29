package com.example.newstart.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun RatingBar(
    rating: Float,
    maxRating: Int = 5,
    starSize: Dp = 16.dp,
    activeColor: Color = Color(0xFFFFCC00),
    inactiveColor: Color = Color.Gray.copy(alpha = 0.2f),
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        val filledStars = floor(rating).toInt()
        val hasHalfStar = (rating - filledStars) >= 0.1f // Ngưỡng để hiện nửa sao
        
        repeat(maxRating) { index ->
            val starIndex = index + 1
            val icon = when {
                starIndex <= filledStars -> Icons.Default.Star
                starIndex == filledStars + 1 && hasHalfStar -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Default.StarOutline
            }
            
            val color = if (starIndex <= ceil(rating)) activeColor else inactiveColor
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = color
            )
        }
    }
}
