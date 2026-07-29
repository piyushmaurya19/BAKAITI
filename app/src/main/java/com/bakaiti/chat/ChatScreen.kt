package com.bakaiti.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(chatId: String, chatType: String, title: String, navController: androidx.navigation.NavController, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteOptions by remember { mutableStateOf(false) }
    var isUserOnline by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Messages aur read receipts handle karne ke liye
    DisposableEffect(chatId) {
        val subscription = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val deletedBy = doc.get("deletedBy") as? List<String> ?: emptyList()
                        if (!deletedBy.contains(currentUserId)) {
                            val msgId = doc.id
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false

                            if (senderId != currentUserId && !isRead) {
                                firestore.collection("chats")
                                    .document(chatId)
                                    .collection("messages")
                                    .document(msgId)
                                    .update("isRead", true)
                            }

                            messages.add(
                                ChatMessage(
                                    id = msgId,
                                    senderId = senderId,
                                    senderName = doc.getString("senderName") ?: "",
                                    text = doc.getString("text") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    isRead = if (senderId != currentUserId) true else isRead
                                )
                            )
                        }
                    }
                    // Jaise hi naye messages aayein, automatic bottom par scroll ho jaye
                    if (messages.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            }

        var userListener: com.google.firebase.firestore.ListenerRegistration? = null
        if (chatType == "direct") {
            val parts = chatId.split("_")
            val otherUserId = if (parts.size == 2) {
                if (parts[0] == currentUserId) parts[1] else parts[0]
            } else ""

            if (otherUserId.isNotEmpty()) {
                userListener = firestore.collection("users").document(otherUserId)
                    .addSnapshotListener { userDoc, _ ->
                        if (userDoc != null && userDoc.exists()) {
                            isUserOnline = userDoc.getBoolean("isOnline") ?: false
                        }
                    }
            }
        }

        onDispose { 
            subscription.remove()
            userListener?.remove()
        }
    }

    fun sendMessage() {
        val text = messageText.trim()
        if (text.isEmpty()) return
        messageText = ""
        val senderName = FirebaseAuth.getInstance().currentUser?.email ?: "You"
        val timestamp = System.currentTimeMillis()
        
        val messageData = hashMapOf<String, Any>(
            "text" to text,
            "senderId" to currentUserId,
            "senderName" to senderName,
            "timestamp" to timestamp,
            "isRead" to false,
            "deletedBy" to emptyList<String>()
        )
        
        firestore.collection("chats").document(chatId).collection("messages").add(messageData)
        
        firestore.collection("chats").document(chatId).update(
            mapOf(
                "lastMessage" to text,
                "lastMessageTime" to timestamp
            )
        )

        if (chatType == "direct") {
            val parts = chatId.split("_")
            val receiverId = if (parts.size == 2) {
                if (parts[0] == currentUserId) parts[1] else parts[0]
            } else ""

            if (receiverId.isNotEmpty()) {
                val notificationData = hashMapOf(
                    "receiverId" to receiverId,
                    "senderName" to senderName,
                    "message" to text,
                    "timestamp" to timestamp
                )
                firestore.collection("notifications").add(notificationData)
            }
        }

        // Message bhejte hi turant bottom par scroll ho jaye
        coroutineScope.launch {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size)
            }
        }
    }

    fun deleteForEveryone(msg: ChatMessage) {
        firestore.collection("chats").document(chatId).collection("messages").document(msg.id).delete()
    }

    fun deleteForMe(msg: ChatMessage) {
        firestore.collection("chats").document(chatId).collection("messages").document(msg.id)
            .update("deletedBy", FieldValue.arrayUnion(currentUserId))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, color = NeonBlue, fontSize = 18.sp)
                            if (chatType == "direct") {
                                Text(
                                    text = if (isUserOnline) "Online" else "Offline",
                                    color = if (isUserOnline) Color.Green else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (chatType == "direct") {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isUserOnline) Color.Green else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        bottomBar = {
            // imePadding ensure karega ki keyboard khulte hi input box keyboard ke upar aa jaye aur top bar hide na ho
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(8.dp)
                    .background(DarkBg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message", color = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = fieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { sendMessage() },
                    modifier = Modifier.size(50.dp).background(NeonPurple, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        },
        containerColor = DarkBg
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingVertical(vertical = 16.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                val backgroundColor = if (isMine) NeonPurple else SurfaceDark

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                        if (chatType == "group" && !isMine) {
                            Text(msg.senderName, color = NeonBlue, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, bottom = 2.dp))
                        }
                        Column(
                            modifier = Modifier
                                .background(backgroundColor, RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { selectedMessage = msg }
                                )
                                .padding(12.dp)
                        ) {
                            Text(text = msg.text, color = Color.White)
                            
                            if (isMine) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (msg.isRead) "✓✓" else "✓",
                                    color = if (msg.isRead) NeonBlue else Color.LightGray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedMessage?.let { msg ->
        if (!showDeleteOptions) {
            AlertDialog(
                onDismissRequest = { selectedMessage = null },
                containerColor = SurfaceDark,
                title = { Text("Message Options", color = Color.White) },
                text = {
                    Column {
                        TextButton(onClick = {
                            val encoded = URLEncoder.encode(msg.text, "UTF-8")
                            selectedMessage = null
                            navController.navigate("forward/$encoded")
                        }) {
                            Text("Forward", color = NeonBlue)
                        }
                        
                        TextButton(onClick = {
                            showDeleteOptions = true
                        }) {
                            Text("Delete", color = Color(0xFFFF6B6B))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedMessage = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }

    if (showDeleteOptions && selectedMessage != null) {
        val msg = selectedMessage!!
        val isMine = msg.senderId == currentUserId

        AlertDialog(
            onDismissRequest = { 
                showDeleteOptions = false
                selectedMessage = null
            },
            containerColor = SurfaceDark,
            title = { Text("Delete message?", color = Color.White) },
            text = {
                Column {
                    if (isMine) {
                        TextButton(onClick = {
                            deleteForEveryone(msg)
                            showDeleteOptions = false
                            selectedMessage = null
                        }) {
                            Text("Delete for everyone", color = Color(0xFFFF6B6B))
                        }
                    }
                    TextButton(onClick = {
                        deleteForMe(msg)
                        showDeleteOptions = false
                        selectedMessage = null
                    }) {
                        Text("Delete for me", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteOptions = false
                    selectedMessage = null
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// Helper for PaddingValues vertical
@Composable
fun PaddingVertical(vertical: androidx.compose.ui.unit.Dp): PaddingValues {
    return PaddingValues(top = vertical, bottom = vertical)
}
