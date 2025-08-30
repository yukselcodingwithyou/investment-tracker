package com.yuksel.investmenttracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.yuksel.investmenttracker.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDisclaimersDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        item {
            Text(
                text = "General",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.AttachMoney,
                title = "Base Currency",
                subtitle = uiState.baseCurrency,
                onClick = { showCurrencyDialog = true }
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Schedule,
                title = "Timezone",
                subtitle = uiState.timezone,
                onClick = { showTimezoneDialog = true }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Download,
                title = "Import from CSV",
                onClick = { settingsViewModel.importData() }
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Upload,
                title = "Export to CSV",
                onClick = { settingsViewModel.exportData() }
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Backup",
                onClick = { settingsViewModel.performBackup() }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                onClick = { showPrivacyDialog = true }
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Description,
                title = "Terms of Service",
                onClick = { showTermsDialog = true }
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Warning,
                title = "Disclaimers",
                onClick = { showDisclaimersDialog = true }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            authState.user?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.ExitToApp,
                title = "Logout",
                onClick = { showLogoutDialog = true },
                titleColor = MaterialTheme.colorScheme.error
            )
        }
    }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.logout()
                        showLogoutDialog = false
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Currency Selection Dialog
    if (showCurrencyDialog) {
        val currencies = listOf("TRY", "USD", "EUR", "GBP", "JPY")
        
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Base Currency") },
            text = {
                LazyColumn {
                    items(currencies.size) { index ->
                        val currency = currencies[index]
                        TextButton(
                            onClick = {
                                settingsViewModel.changeCurrency(currency)
                                showCurrencyDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currency,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Timezone Selection Dialog
    if (showTimezoneDialog) {
        val timezones = listOf(
            "Europe/Istanbul",
            "America/New_York", 
            "Europe/London",
            "Asia/Tokyo",
            "Europe/Berlin"
        )
        
        AlertDialog(
            onDismissRequest = { showTimezoneDialog = false },
            title = { Text("Select Timezone") },
            text = {
                LazyColumn {
                    items(timezones.size) { index ->
                        val timezone = timezones[index]
                        TextButton(
                            onClick = {
                                settingsViewModel.changeTimezone(timezone)
                                showTimezoneDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = timezone,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimezoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Import CSV Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import from CSV") },
            text = { Text("CSV import feature will be implemented in a future update.") },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Export CSV Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export to CSV") },
            text = { Text("CSV export feature will be implemented in a future update.") },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Backup Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup") },
            text = { Text("Backup feature will be implemented in a future update.") },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = { Text("Privacy policy will be available in a future update.") },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service") },
            text = { Text("Terms of service will be available in a future update.") },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Disclaimers Dialog
    if (showDisclaimersDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimersDialog = false },
            title = { Text("Disclaimers") },
            text = { Text("Legal disclaimers will be available in a future update.") },
            confirmButton = {
                TextButton(onClick = { showDisclaimersDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Show messages
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        if (uiState.successMessage != null) {
            // In a real implementation, you would show a Snackbar here
            kotlinx.coroutines.delay(3000)
            settingsViewModel.clearMessages()
        } else if (uiState.errorMessage != null) {
            // In a real implementation, you would show an error Snackbar here
            kotlinx.coroutines.delay(3000)
            settingsViewModel.clearMessages()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor
                )
                
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}