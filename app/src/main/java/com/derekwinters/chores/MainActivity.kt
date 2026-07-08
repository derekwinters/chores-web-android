package com.derekwinters.chores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.derekwinters.chores.ui.ChoresApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * App entry point.
 *
 * Screens stay plain composables reading state from Hilt ViewModels.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChoresApp()
        }
    }
}
