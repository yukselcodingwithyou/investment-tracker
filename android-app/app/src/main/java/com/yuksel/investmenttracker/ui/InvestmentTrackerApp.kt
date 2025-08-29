package com.yuksel.investmenttracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yuksel.investmenttracker.ui.auth.AuthScreen
import com.yuksel.investmenttracker.ui.auth.AuthViewModel
import com.yuksel.investmenttracker.ui.navigation.InvestmentTrackerNavigation
import com.yuksel.investmenttracker.ui.navigation.Screen

@Composable
fun InvestmentTrackerApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (authState.isAuthenticated) {
            InvestmentTrackerNavigation()
        } else {
            AuthScreen()
        }
    }
}