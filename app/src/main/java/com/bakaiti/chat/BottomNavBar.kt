package com.bakaiti.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border

data class NavTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val navTabs = listOf(
    NavTab("Chats", Icons.Default.Chat),
    NavTab("Friends", Icons.Default.People),
    NavTab("Profile", Icons.Default.Person),
    NavTab("Calls", Icons.Default.Call)
)

@Composable
fun BottomNavBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xCC1A1A1A), RoundedCornerShape(32.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navTabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelect(index) }
                        .background(
                            if (isSelected) Color(0x3300E5FF) else Color.Transparent,
                            RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) NeonBlue else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        tab.label,
                        color = if (isSelected) NeonBlue else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
