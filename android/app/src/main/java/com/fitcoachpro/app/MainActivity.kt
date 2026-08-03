package com.fitcoachpro.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitcoachpro.app.notifications.NotificationHelper
import com.fitcoachpro.app.ui.CheckInScreen
import com.fitcoachpro.app.ui.SettingsScreen
import com.fitcoachpro.app.ui.theme.FitCoachProTheme

private object Routes {
    const val CHECK_IN = "checkin"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    // Android 13+ requires runtime permission for local notifications - this
    // is what actually shows the reminder WorkManager schedules (see
    // notifications/ReminderWorker.kt). Without this grant the job still
    // runs, it just can't display anything.
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op either way - reminder scheduling doesn't depend on the result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.ensureChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            FitCoachProTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CHECK_IN) {
        composable(Routes.CHECK_IN) {
            CheckInScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
