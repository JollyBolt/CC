package com.example.settled.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.settled.ui.theme.LightBackground
import com.example.settled.ui.theme.PrimaryBrand
import androidx.compose.ui.tooling.preview.Preview
import com.example.settled.ui.theme.SettledTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProBanner()
            }

            item {
                SettingsSection(title = "Account") {
                    SettingsItem(icon = Icons.Default.Person, label = "Profile", onClick = {})
                    SettingsItem(icon = Icons.Default.CloudSync, label = "Cloud Sync", onClick = {})
                }
            }

            item {
                SettingsSection(title = "Data Management") {
                    SettingsItem(icon = Icons.Default.FileDownload, label = "Export as CSV", onClick = {})
                    SettingsItem(icon = Icons.Default.DeleteForever, label = "Delete Account", color = Color.Red, onClick = {})
                }
            }

            item {
                SettingsSection(title = "General") {
                    SettingsItem(icon = Icons.Default.Notifications, label = "Notifications", onClick = {})
                    SettingsItem(icon = Icons.Default.Info, label = "About Settled", onClick = {})
                }
            }
        }
    }
}

@Composable
fun ProBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        color = Color(0xFF1E1E1E)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Settled Pro", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Unlock unlimited cards, cloud sync and more.", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrand),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Upgrade")
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, label: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (color == Color.Unspecified) Color.Gray else color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (color == Color.Unspecified) Color.Black else color)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettledTheme {
        SettingsScreen()
    }
}
