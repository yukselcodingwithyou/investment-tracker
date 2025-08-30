package com.yuksel.investmenttracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    var showAddAcquisitionMessage by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No acquisitions yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tap 'Add Acquisition' to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { showAddAcquisitionMessage = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Acquisition")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { 
                        // TODO: Navigate to portfolio screen when implemented
                        // navController.navigate("portfolio")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Portfolio")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { 
                        // TODO: Navigate to history/import screen  
                        // This is already handled by the bottom navigation
                        navController.navigate("history")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Data")
                }
            }
        }
    }
    
    // Add Acquisition Dialog
    if (showAddAcquisitionMessage) {
        AlertDialog(
            onDismissRequest = { showAddAcquisitionMessage = false },
            title = { Text("Add Acquisition") },
            text = { 
                Column {
                    Text("Add acquisition functionality will be available soon.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Features coming soon:\n• Asset search and selection\n• Acquisition date picker\n• Price and quantity input\n• Fee calculation\n• Portfolio integration",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddAcquisitionMessage = false }) {
                    Text("OK")
                }
            }
        )
    }
}