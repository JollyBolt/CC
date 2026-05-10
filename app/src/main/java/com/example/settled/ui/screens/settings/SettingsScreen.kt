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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.settled.R
import com.example.settled.ui.theme.LightBackground
import com.example.settled.ui.theme.PrimaryBrand
import androidx.compose.ui.tooling.preview.Preview
import com.example.settled.ui.theme.SettledTheme
import com.example.settled.ui.theme.viewmodel.ThemeEvent
import com.example.settled.ui.theme.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {}
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
                SettingsSection(title = stringResource(R.string.settings_section_account)) {
                    SettingsItem(icon = Icons.Default.Person, label = stringResource(R.string.settings_item_profile), onClick = {})
                    SettingsItem(icon = Icons.Default.CloudSync, label = stringResource(R.string.settings_item_cloud_sync), onClick = {})
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_data)) {
                    SettingsItem(icon = Icons.Default.FileDownload, label = stringResource(R.string.settings_item_export_csv), onClick = {})
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        label = stringResource(R.string.settings_item_delete_account),
                        color = Color.Red,
                        onClick = onNavigateToDeleteAccount
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_general)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        label = "Dark Mode",
                        checked = isDarkMode,
                        onToggle = { themeViewModel.onEvent(ThemeEvent.ToggleDarkMode) }
                    )
                    SettingsItem(icon = Icons.Default.Notifications, label = stringResource(R.string.settings_item_notifications), onClick = {})
                    SettingsItem(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.settings_item_about),
                        onClick = onNavigateToAbout
                    )
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
                Text(stringResource(R.string.settings_pro_title), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.settings_pro_body), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrand),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_pro_upgrade))
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
fun SettingsSwitchItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
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
