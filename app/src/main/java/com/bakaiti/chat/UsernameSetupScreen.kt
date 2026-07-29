package com.bakaiti.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun UsernameSetupScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var username by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Choose a username", color = NeonBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Others will find you by this name", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                placeholder = { Text("e.g. piyush99") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            errorMsg?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                enabled = username.trim().length >= 3 && !saving,
                onClick = {
                    val uid = auth.currentUser?.uid ?: return@Button
                    val clean = username.trim().lowercase()
                    saving = true
                    errorMsg = null
                    firestore.collection("users")
                        .whereEqualTo("usernameLower", clean)
                        .get()
                        .addOnSuccessListener { snap ->
                            if (!snap.isEmpty) {
                            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                firestore.collection("users").document(uid).update("fcmToken", token)
                            }
                                saving = false
                                errorMsg = "Username already taken, try another"
                            } else {
                                val data = hashMapOf(
                                    "uid" to uid,
                                    "username" to username.trim(),
                                    "usernameLower" to clean,
                                    "displayName" to (auth.currentUser?.displayName ?: ""),
                                    "email" to (auth.currentUser?.email ?: ""),
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                                firestore.collection("users").document(uid).set(data, SetOptions.merge())
                                    .addOnSuccessListener {
                                        saving = false
                                        navController.navigate("home") { popUpTo(0) }
                                    }
                                    .addOnFailureListener { e ->
                                        saving = false
                                        errorMsg = e.localizedMessage
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.localizedMessage
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (saving) "Saving..." else "Save & Continue", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
