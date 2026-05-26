package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.ui.GameScreen
import com.example.ui.GameViewModel
import com.example.ui.GameViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "math_monster.db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository by lazy {
        GameRepository(database.playerProgressDao())
    }

    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}

