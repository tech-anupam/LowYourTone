package dev.anupam.lowyourtone

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.anupam.lowyourtone.ui.navigation.NavGraph
import dev.anupam.lowyourtone.ui.theme.LowYourToneTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.filter { !it.value }.keys
        if (denied.isNotEmpty()) {
            val messages = denied.map { perm ->
                when (perm) {
                    Manifest.permission.RECORD_AUDIO -> "Microphone denied — wake word detection won't work"
                    Manifest.permission.CAMERA -> "Camera denied — flashlight control won't work"
                    Manifest.permission.READ_CONTACTS -> "Contacts denied — contact picker won't work"
                    Manifest.permission.SEND_SMS -> "SMS denied — sending SMS won't work"
                    Manifest.permission.CALL_PHONE -> "Call denied — making calls won't work"
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> "Location denied — location-based actions won't work"
                    Manifest.permission.READ_PHONE_STATE -> "Phone state denied — call detection won't work"
                    else -> "$perm denied"
                }
            }
            Toast.makeText(this, messages.joinToString("\n"), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val neededPermissions = mutableListOf<String>()
        val checkList = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkList.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        for (perm in checkList) {
            if (checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(perm)
            }
        }
        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions.toTypedArray())
        }

        setContent {
            LowYourToneTheme {
                NavGraph()
            }
        }
    }
}
