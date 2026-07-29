package com.bakaiti.chat

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardScreen(encodedText: String, navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val text = remember { URLDecoder.decode(encodedText, "UTF-8") }
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUid = auth.currentUser?.uid ?: ""
    val chats = remember { mutableStateListOf<ChatSummary>() }

    DisposableEffect(currentUid) {
        val reg: ListenerRegistration = firestore.collection("chats")
            .whereArrayContains("participants", currentUid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    chats.clear()
                    for (doc in snap.documents) {
                        chats.add(
                            ChatSummary(
                                chatId = doc.id,
                                type = doc.getString("type") ?: "direct",
                                title = doc.getString("title") ?: ""
                            )
                        )
                    }
                }
            }
        onDispose { reg.remove() }
    }

    fun forwardTo(chat: ChatSummary) {
        val senderName = auth.currentUser?.email ?: "You"
        val messageData = hashMapOf<String, Any>(
            "text" to text,
            "senderId" to currentUid,
            "senderName" to senderName,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("chats").document(chat.chatId).collection("messages").add(messageData)
        firestore.collection("chats").document(chat.chatId).update(
            mapOf(
                "lastMessage" to text,
                "lastMessageTime" to System.currentTimeMillis()
            )
        )
        Toast.makeText(context, "Forwarded to ${chat.title}", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forward to...", color = NeonBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(chats) { chat ->
                ListItem(
                    headlineContent = { Text(chat.title, color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = ListItemDefaults.colors(containerColor = DarkBg),
                    modifier = Modifier.clickable { forwardTo(chat) }
                )
                Divider(color = SurfaceDark)
            }
        }
    }
}
