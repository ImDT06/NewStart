package com.example.newstart.ui.features.habits.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newstart.R
import com.example.newstart.domain.model.Habit


@Composable
fun HabitItem(
    habit: Habit,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {

    val color = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF1D5FE2)
        }
    }


    val interactionSource = remember {
        MutableInteractionSource()
    }


    val isPressed by interactionSource.collectIsPressedAsState()


    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "scale"
    )


    Surface(

        modifier = Modifier
            .fillMaxWidth()

            .graphicsLayer {

                scaleX = scale
                scaleY = scale

                alpha =
                    if(habit.isCompleted)
                        0.75f
                    else
                        1f
            }

            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onEdit
            ),


        shape = RoundedCornerShape(16.dp),

        shadowElevation = 4.dp,

        color = if (habit.isCompleted) {
            color.copy(alpha = 0.25f).compositeOver(MaterialTheme.colorScheme.surface)
        } else {
            color.copy(alpha = 0.15f).compositeOver(MaterialTheme.colorScheme.surface)
        }

    ) {


        Row(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ICON
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(habit.icon, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))





            Column(
                modifier = Modifier.weight(1f)
            ) {


                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {


                    Text(

                        text = habit.name,


                        color =
                            if(habit.isCompleted)

                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                            else

                                MaterialTheme.colorScheme.onSurface,


                        fontSize = 14.sp,


                        fontWeight = FontWeight.Bold,


                        textDecoration =

                            if(habit.isCompleted)

                                TextDecoration.LineThrough

                            else

                                TextDecoration.None

                    )




                    if (habit.streak > 0) {


                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )



                        Icon(

                            Icons.Default.LocalFireDepartment,

                            contentDescription = null,

                            tint = Color(0xFFFFA500),

                            modifier =
                                Modifier.size(12.dp)

                        )



                        Text(

                            "${habit.streak} " +
                                    stringResource(
                                        R.string.habits_streak_day
                                    ),

                            color =
                                if(habit.isCompleted)

                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                                else

                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,


                            fontSize = 9.sp,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }





                Text(

                    text =

                        if(habit.reminderTime != null)

                            "${habit.progress}/${habit.goal} • 🔔 ${habit.reminderTime}"

                        else

                            "${habit.progress}/${habit.goal}",



                    color =

                        if(habit.isCompleted)

                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                        else

                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),



                    fontSize = 11.sp

                )


            }





            // CHECK BUTTON
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = if (habit.isCompleted) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .background(if (habit.isCompleted) color else Color.Transparent)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (habit.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

    }

}