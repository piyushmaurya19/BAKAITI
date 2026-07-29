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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun EmailVerificationScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    var message by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verify your email", color = NeonBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We sent a verification link to ${auth.currentUser?.email ?: "your email"}. Tap the link, then come back here.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            message?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = NeonBlue, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                enabled = !checking,
                onClick = {
                    checking = true
                    auth.currentUser?.reload()?.addOnCompleteListener {
                        checking = false
                        if (auth.currentUser?.isEmailVerified == true) {
                            navController.navigate("gate") { popUpTo(0) }
                        } else {
                            message = "Not verified yet. Check your inbox (and spam folder)."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (checking) "Checking..." else "I've verified — Continue", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = {
                auth.currentUser?.sendEmailVerification()
                message = "Verification email resent."
            }) {
                Text("Resend email", color = NeonBlue, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                auth.signOut()
                navController.navigate("auth") { popUpTo(0) }
            }) {
                Text("Logout", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
