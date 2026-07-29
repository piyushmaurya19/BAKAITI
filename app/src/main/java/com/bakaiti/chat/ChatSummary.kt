package com.bakaiti.chat

/**
 * Simple data model representing a chat summary used across UI screens.
 *
 * Fields and defaults chosen to match usages in ForwardScreen.kt and HomeScreen.kt:
 *  - chatId: document id of the chat
 *  - type: chat type (e.g., "direct", "group")
 *  - title: display title for the chat
 *  - lastMessage: last message text shown in chat list
 *  - lastMessageTime: epoch millis of last message used for sorting
 *  - participants: list of participant UIDs
 */

data class ChatSummary(
    val chatId: String = "",
    val type: String = "direct",
    val title: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val participants: List<String> = emptyList()
)
