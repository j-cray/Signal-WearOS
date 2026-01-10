package com.example.signalwearos.presentation.ui

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.example.signalwearos.data.signal.SignalClient
import com.example.signalwearos.presentation.model.Message
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(contactId: String) {
    val scope = rememberCoroutineScope()
    // In a real app, this should be injected or retrieved from a ViewModel
    val signalClient = remember { SignalClient() }

    // Mock messages
    val messages = remember {
        mutableStateListOf(
            Message("1", "1", "Hey, how are you?", "10:00 AM", true),
            Message("2", "me", "I'm good, thanks! How about you?", "10:01 AM", false),
            Message("3", "1", "Doing well. Meeting at 2 PM?", "10:02 AM", true),
            Message("4", "me", "Yes, see you then.", "10:03 AM", false)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val results = RemoteInput.getResultsFromIntent(result.data)
            val text = results?.getCharSequence("reply_text")?.toString()
            if (text != null) {
                // 1. Add to UI immediately
                messages.add(Message("new", "me", text, "Now", false))
                
                // 2. Send via Signal Protocol
                scope.launch {
                    signalClient.sendMessage(contactId, text)
                }
            }
        }
    }

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
                onClick = { 
                    val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    val remoteInputs: List<RemoteInput> = listOf(
                        RemoteInput.Builder("reply_text")
                            .setLabel("Reply")
                            .wearableExtender {
                                setEmojisAllowed(true)
                                setInputActionType(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
                            }
                            .build()
                    )
                    RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                    launcher.launch(intent)
                },
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
