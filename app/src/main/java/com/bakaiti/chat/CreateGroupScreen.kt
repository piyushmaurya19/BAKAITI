package com.bakaiti.chat

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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var groupName by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<UserProfile>() }
    val selectedMembers = remember { mutableStateListOf<UserProfile>() }
    var creating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun runSearch() {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return
        firestore.collection("users")
            .whereGreaterThanOrEqualTo("usernameLower", q)
            .whereLessThanOrEqualTo("usernameLower", q + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                searchResults.clear()
                for (doc in snap.documents) {
                    val uid = doc.getString("uid") ?: continue
                    if (uid == auth.currentUser?.uid) continue
                    searchResults.add(
                        UserProfile(
                            uid = uid,
                            username = doc.getString("username") ?: "",
                            displayName = doc.getString("displayName") ?: ""
                        )
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New group", color = NeonBlue, fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (selectedMembers.isNotEmpty()) {
                Text("Selected: ${selectedMembers.joinToString(", ") { it.username }}", color = NeonBlue, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search username to add", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { runSearch() }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonBlue)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            errorMsg?.let {
                Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(searchResults) { user ->
                    val isSelected = selectedMembers.any { it.uid == user.uid }
                    ListItem(
                        headlineContent = { Text(user.username, color = Color.White) },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedMembers.add(user)
                                    else selectedMembers.removeAll { it.uid == user.uid }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = DarkBg)
                    )
                    Divider(color = SurfaceDark)
                }
            }

            Button(
                enabled = groupName.trim().isNotEmpty() && selectedMembers.isNotEmpty() && !creating,
                onClick = {
                    val myUid = auth.currentUser?.uid ?: return@Button
                    creating = true
                    val participants = selectedMembers.map { it.uid } + myUid
                    val data = hashMapOf(
                        "type" to "group",
                        "title" to groupName.trim(),
                        "participants" to participants,
                        "createdBy" to myUid,
                        "lastMessage" to "",
                        "lastMessageTime" to System.currentTimeMillis(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("chats").add(data)
                        .addOnSuccessListener { doc ->
                            creating = false
                            val encodedTitle = URLEncoder.encode(groupName.trim(), "UTF-8")
                            navController.navigate("chat/${doc.id}/group/$encodedTitle") {
                                popUpTo("home")
                            }
                        }
                        .addOnFailureListener { e ->
                            creating = false
                            errorMsg = e.localizedMessage
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text(if (creating) "Creating..." else "Create group", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
