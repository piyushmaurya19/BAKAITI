package com.bakaiti.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            username = doc.getString("username") ?: ""
            displayName = doc.getString("displayName") ?: ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier.size(90.dp).background(SurfaceDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "?",
                color = NeonBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(displayName.ifEmpty { "..." }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("@$username", color = NeonBlue, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(auth.currentUser?.email ?: "", color = Color.Gray, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                auth.signOut()
                navController.navigate("auth") { popUpTo(0) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
        }
    }
}
