package com.example.newstart.ui.features.habits.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.newstart.domain.model.Habit
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HabitSwipeableItem(
    modifier: Modifier = Modifier,
    habit: Habit,
    onDelete: () -> Unit,
    onToggle: (Habit, Boolean) -> Unit,
    onEdit: (Habit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val anchorWidth = with(density) { 80.dp.toPx() }
    val dismissThreshold = with(density) { 180.dp.toPx() }
    
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF4444))
                .clickable { 
                    scope.launch {
                        offsetX.animateTo(-1500f, spring(stiffness = Spring.StiffnessMedium))
                        onDelete()
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier.width(80.dp).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(habit.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = (offsetX.value + dragAmount).coerceAtMost(0f)
                            scope.launch { offsetX.snapTo(newOffset) }
                            change.consume()
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -dismissThreshold) {
                                    offsetX.animateTo(-1500f, spring(stiffness = Spring.StiffnessMedium))
                                    onDelete()
                                } else if (offsetX.value < -anchorWidth / 2) {
                                    offsetX.animateTo(-anchorWidth, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                }
                            }
                        }
                    )
                }
        ) {
            HabitItem(
                habit = habit, 
                onToggle = { onToggle(habit, !habit.isCompleted) }, 
                onEdit = { onEdit(habit) }
            )
        }
    }
}
