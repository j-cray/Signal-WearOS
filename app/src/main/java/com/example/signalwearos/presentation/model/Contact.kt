package com.example.signalwearos.presentation.model

data class Contact(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val avatarUrl: String? = null
)
