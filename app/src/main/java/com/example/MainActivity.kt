package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ProcessingDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageToPdfScreen
import com.example.ui.screens.MergePdfScreen
import com.example.ui.screens.PdfEditScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PdfViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: PdfViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()

                // Handle incoming PDF intent if opened from file manager
                LaunchedEffect(intent) {
                    if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
                        intent.data?.let { uri ->
                            viewModel.openPdfFromUri(uri, "External Open")
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "ScreenNavigation"
                    ) { screen ->
                        when (screen) {
                            ScreenDestination.HOME -> HomeScreen(viewModel)
                            ScreenDestination.VIEWER -> PdfViewerScreen(viewModel)
                            ScreenDestination.IMAGE_TO_PDF -> ImageToPdfScreen(viewModel)
                            ScreenDestination.MERGE -> MergePdfScreen(viewModel)
                            ScreenDestination.EDIT -> PdfEditScreen(viewModel)
                        }
                    }

                    if (isLoading) {
                        ProcessingDialog(message = statusMessage)
                    }
                }
            }
        }
    }
}
