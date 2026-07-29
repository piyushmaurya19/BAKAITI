package com.bakaiti.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AuthGateScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user == null) {
            navController.navigate("auth") { popUpTo(0) }
            return@LaunchedEffect
        }
        user.reload().addOnCompleteListener {
            val refreshedUser = auth.currentUser
            if (refreshedUser != null && !refreshedUser.isEmailVerified) {
                navController.navigate("email_verify") { popUpTo(0) }
            } else {
                firestore.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists() && !doc.getString("username").isNullOrEmpty()) {
                            navController.navigate("home") { popUpTo(0) }
                        } else {
                            navController.navigate("username_setup") { popUpTo(0) }
                        }
                    }
                    .addOnFailureListener {
                        navController.navigate("username_setup") { popUpTo(0) }
                    }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NeonBlue)
    }
}
