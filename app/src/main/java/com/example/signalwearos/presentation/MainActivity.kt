/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.signalwearos.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.signalwearos.data.UserPreferencesRepository
import com.example.signalwearos.notification.NotificationHelper
import com.example.signalwearos.presentation.model.Contact
import com.example.signalwearos.presentation.theme.SignalWearOSTheme
import com.example.signalwearos.presentation.ui.ChatScreen
import com.example.signalwearos.presentation.ui.ContactListScreen
import com.example.signalwearos.presentation.ui.QrCodeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    SignalWearOSTheme {
        val context = LocalContext.current
        val userPreferencesRepository = UserPreferencesRepository(context)
        val isDeviceLinked by userPreferencesRepository.isDeviceLinked.collectAsState(initial = false)
        val scope = rememberCoroutineScope()
        
        val navController = rememberSwipeDismissableNavController()
        
        // Permission handling for Notifications
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission Granted
            }
        }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            
            // Simulate receiving a notification after 10 seconds for demo purposes
            delay(10000)
            NotificationHelper(context).showNotification("Alice", "Hey, are you there?")
        }
        
        val startDestination = if (isDeviceLinked) "contact_list" else "qr_code"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable("qr_code") {
                    QrCodeScreen(
                        onLinked = {
                            scope.launch {
                                userPreferencesRepository.setDeviceLinked(true)
                            }
                            navController.navigate("contact_list") {
                                popUpTo("qr_code") { inclusive = true }
                            }
                        }
                    )
                }
                composable("contact_list") {
                    // Mock data for demonstration
                    val contacts = listOf(
                        Contact("1", "Alice", "Hey, how are you?", "10:00 AM"),
                        Contact("2", "Bob", "Meeting at 2 PM", "9:30 AM"),
                        Contact("3", "Charlie", "See you later!", "Yesterday")
                    )
                    ContactListScreen(
                        contacts = contacts,
                        onContactClick = { contact ->
                            navController.navigate("chat/${contact.id}")
                        }
                    )
                }
                composable("chat/{contactId}") { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                    ChatScreen(contactId = contactId)
                }
            }
            TimeText()
        }
    }
}
