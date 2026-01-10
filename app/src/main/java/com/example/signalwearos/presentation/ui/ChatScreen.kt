package com.example.signalwearos.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.signalwearos.presentation.model.Message

@Composable
fun ChatScreen(contactId: String) {
    // Mock messages
    val messages = listOf(
        Message("1", "1", "Hey, how are you?", "10:00 AM", true),
        Message("2", "me", "I'm good, thanks! How about you?", "10:01 AM", false),
        Message("3", "1", "Doing well. Meeting at 2 PM?", "10:02 AM", true),
        Message("4", "me", "Yes, see you then.", "10:03 AM", false)
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        item {
            Text(
                text = "Chat with $contactId",
                style = MaterialTheme.typography.title3,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(messages) { message ->
            MessageBubble(message = message)
        }
        item {
            CompactChip(
                onClick = { /* Handle reply action */ },
                label = { Text("Reply") },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isIncoming) Alignment.CenterStart else Alignment.CenterEnd
    val backgroundColor = if (message.isIncoming) Color.DarkGray else MaterialTheme.colors.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp),
            color = Color.White
        )
    }
}
