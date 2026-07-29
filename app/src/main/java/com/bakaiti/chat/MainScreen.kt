package com.bakaiti.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            BottomNavBar(selectedIndex = selectedTab, onSelect = { selectedTab = it })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            when (selectedTab) {
                0 -> HomeScreen(navController)
                1 -> FriendsScreen(navController)
                2 -> ProfileScreen(navController)
                3 -> CallsScreen()
            }
        }
    }
}
