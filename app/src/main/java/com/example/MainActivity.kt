package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ai.ReceiptScannerService
import com.example.ai.configureAppCheck
import com.example.data.local.PantryDatabase
import com.example.data.local.UserPreferences
import com.example.data.repository.PantryRepository
import com.example.ui.PantryFlowApp
import com.example.viewmodel.PantryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent before UI/ViewModels
        configureAppCheck(this, intent)
        com.example.data.util.NotificationHelper.ensureNotificationChannel(this)

        enableEdgeToEdge()

        val database = PantryDatabase.getInstance(applicationContext)
        val preferences = UserPreferences(applicationContext)
        val repository = PantryRepository(database.pantryDao(), preferences)
        val receiptScannerService = ReceiptScannerService()
        val recipeSuggestionService = com.example.ai.RecipeSuggestionService()
        val viewModel = ViewModelProvider(
            this,
            PantryViewModel.Factory(repository, receiptScannerService, recipeSuggestionService)
        )[PantryViewModel::class.java]

        setContent {
            PantryFlowApp(viewModel = viewModel)
        }
    }
}
