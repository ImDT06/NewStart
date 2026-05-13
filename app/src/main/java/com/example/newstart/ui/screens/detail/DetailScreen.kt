package com.example.newstart.ui.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DetailScreen(
    userId: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "User Detail Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Viewing details for User ID: $userId",
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
