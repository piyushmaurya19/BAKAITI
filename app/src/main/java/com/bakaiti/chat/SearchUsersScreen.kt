package com.bakaiti.chat

import androidx.compose.material3.Divider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUsersScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<UserProfile>() }
    var searching by remember { mutableStateOf(false) }

    fun runSearch() {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return
        searching = true
        firestore.collection("users")
            .whereGreaterThanOrEqualTo("usernameLower", q)
            .whereLessThanOrEqualTo("usernameLower", q + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                results.clear()
                for (doc in snap.documents) {
                    val uid = doc.getString("uid") ?: continue
                    if (uid == auth.currentUser?.uid) continue
                    val isOnline = doc.getBoolean("isOnline") ?: false
                    results.add(
                        UserProfile(
                            uid = uid,
                            username = doc.getString("username") ?: "",
                            displayName = doc.getString("displayName") ?: "",
                            isOnline = isOnline
                        )
                    )
                }
                searching = false
            }
            .addOnFailureListener { searching = false }
    }

    fun startChatWith(other: UserProfile) {
        val myUid = auth.currentUser?.uid ?: return
        val chatId = if (myUid < other.uid) "${myUid}_${other.uid}" else "${other.uid}_${myUid}"
        val chatRef = firestore.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val data = hashMapOf(
                    "type" to "direct",
                    "participants" to listOf(myUid, other.uid),
                    "title" to other.username,
                    "lastMessage" to "",
                    "lastMessageTime" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(data)
            }
            val encodedTitle = URLEncoder.encode(other.username, "UTF-8")
            navController.navigate("chat/$chatId/direct/$encodedTitle") {
                popUpTo("home")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New chat", color = NeonBlue, fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search username", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { runSearch() }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonBlue)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (searching) {
                CircularProgressIndicator(color = NeonBlue)
            }
            LazyColumn {
                items(results) { user ->
                    ListItem(
                        headlineContent = { Text(user.username, color = Color.White, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            if (user.displayName.isNotEmpty()) Text(user.displayName, color = Color.Gray)
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (user.isOnline) Color.Green else Color.Gray)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = DarkBg),
                        modifier = Modifier.clickable { startChatWith(user) }
                    )
                    Divider(color = SurfaceDark)
                }
            }
        }
    }
}
