package com.bakaiti.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(currentUser.uid).update("fcmToken", token)
            }
        }
        
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = DarkBg) {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus(isOnline = true)
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus(isOnline = false)
    }

    private fun updateUserStatus(isOnline: Boolean) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.uid)
                .update(mapOf("isOnline" to isOnline, "lastSeen" to System.currentTimeMillis()))
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "gate") {
        composable("gate") {
            AuthGateScreen(navController)
        }
        composable("auth") {
            AuthScreen(onAuthed = {
                navController.navigate("gate") { popUpTo(0) }
            })
        }
        composable("email_verify") {
            EmailVerificationScreen(navController)
        }
        composable("username_setup") {
            UsernameSetupScreen(navController)
        }
        composable("home") {
            MainScreen(navController)
        }
        composable("search_users") {
            SearchUsersScreen(navController)
        }
        composable("create_group") {
            CreateGroupScreen(navController)
        }
        composable("chat/{chatId}/{chatType}/{title}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatType = backStackEntry.arguments?.getString("chatType") ?: "direct"
            val titleEncoded = backStackEntry.arguments?.getString("title") ?: "Chat"
            val title = URLDecoder.decode(titleEncoded, "UTF-8")
            ChatScreen(chatId, chatType, title, navController) { navController.popBackStack() }
        }
        composable("forward/{text}") { backStackEntry ->
            val encodedText = backStackEntry.arguments?.getString("text") ?: ""
            ForwardScreen(encodedText, navController)
        }
    }
}
