package com.bakaiti.chat

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val isOnline: Boolean = false
)
