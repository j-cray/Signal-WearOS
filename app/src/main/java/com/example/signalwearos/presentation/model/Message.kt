package com.example.signalwearos.presentation.model

data class Message(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isIncoming: Boolean
)
