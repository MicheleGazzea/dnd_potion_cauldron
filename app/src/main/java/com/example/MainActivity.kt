package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.PotionDatabase
import com.example.data.PotionRepository
import com.example.ui.CauldronApp
import com.example.ui.PotionViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge full screen drawing support
        enableEdgeToEdge()

        // Room Database & Repository initialization
        val database = PotionDatabase.getDatabase(applicationContext)
        val repository = PotionRepository(database)

        // ViewModel Factory pattern instanced directly
        val factory = PotionViewModel.Factory(repository)
        val viewModel = ViewModelProvider(this, factory)[PotionViewModel::class.java]

        setContent {
            MyApplicationTheme {
                CauldronApp(viewModel = viewModel)
            }
        }
    }
}
