package com.bakaiti.chat

import androidx.compose.material3.Divider


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUid = auth.currentUser?.uid ?: ""
    val chats = remember { mutableStateListOf<ChatSummary>() }
    var showNewChatMenu by remember { mutableStateOf(false) }

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
                                title = doc.getString("title") ?: "",
                                lastMessage = doc.getString("lastMessage") ?: "",
                                lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                                participants = (doc.get("participants") as? List<String>) ?: emptyList()
                            )
                        )
                    }
                }
            }
        onDispose { reg.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bakaiti", fontWeight = FontWeight.Bold, color = NeonBlue) },
                actions = {
                    TextButton(onClick = {
                        auth.signOut()
                        navController.navigate("auth") { popUpTo(0) }
                    }) {
                        Text("Logout", color = NeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showNewChatMenu) {
                    SmallFloatingActionButton(
                        onClick = {
                            showNewChatMenu = false
                            navController.navigate("create_group")
                        },
                        containerColor = SurfaceDark,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "New group", tint = NeonBlue)
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            showNewChatMenu = false
                            navController.navigate("search_users")
                        },
                        containerColor = SurfaceDark,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "New chat", tint = NeonBlue)
                    }
                }
                FloatingActionButton(
                    onClick = { showNewChatMenu = !showNewChatMenu },
                    containerColor = NeonPurple
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New", tint = Color.White)
                }
            }
        },
        containerColor = DarkBg
    ) { padding ->
        if (chats.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No chats yet. Tap + to start one.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(chats) { chat ->
                    ListItem(
                        headlineContent = { Text(chat.title, color = Color.White, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                chat.lastMessage.ifEmpty { "No messages yet" },
                                color = Color.Gray,
                                maxLines = 1
                            )
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(46.dp).background(SurfaceDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = NeonBlue
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = DarkBg),
                        modifier = Modifier.clickable {
                            val encodedTitle = URLEncoder.encode(chat.title, "UTF-8")
                            navController.navigate("chat/${chat.chatId}/${chat.type}/$encodedTitle")
                        }
                    )
                    Divider(color = SurfaceDark)
                }
            }
        }
    }
}
