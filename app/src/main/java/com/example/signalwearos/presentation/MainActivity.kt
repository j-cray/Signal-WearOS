/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.signalwearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.signalwearos.presentation.model.Contact
import com.example.signalwearos.presentation.theme.SignalWearOSTheme
import com.example.signalwearos.presentation.ui.ChatScreen
import com.example.signalwearos.presentation.ui.ContactListScreen

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
        val navController = rememberSwipeDismissableNavController()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "contact_list"
            ) {
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
