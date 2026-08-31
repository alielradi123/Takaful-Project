package com.example.takaful

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.takaful.ui.navigation.TakafulNavGraph
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider

import com.example.takaful.ui.theme.TakafulTheme
import com.example.takaful.utils.SharedPrefsHelper
import com.example.takaful.utils.NotificationHelper
import android.os.Build
import androidx.core.app.ActivityCompat
import android.Manifest

class MainActivity : FragmentActivity() {
    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    // State to force recomposition when theme toggles
    var isDarkModeEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefsHelper = SharedPrefsHelper(this)
        isDarkModeEnabled = sharedPrefsHelper.isDarkMode

        NotificationHelper.createNotificationChannel(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
        
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(currentUser.uid)
                    .update("fcmToken", token)
            }
        }

        enableEdgeToEdge()

        setContent {
            TakafulTheme(darkTheme = isDarkModeEnabled) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TakafulNavGraph()
                }
            }
        }
    }

    // Call this from ProfileScreen to toggle dark mode dynamically
    fun toggleDarkMode(enabled: Boolean) {
        sharedPrefsHelper.isDarkMode = enabled
        isDarkModeEnabled = enabled
    }
}
