package com.jjswigut.eventide

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.jjswigut.eventide.map.MapScreen
import com.jjswigut.eventide.ui.theme.EventideTheme

class MainActivity : ComponentActivity() {
    private var hasLocationPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestLocationPermission()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            hasLocationPermission = isGranted
            setupMapScreen()
        }


    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true
                setupMapScreen()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, ACCESS_COARSE_LOCATION
            ) -> {
                hasLocationPermission = false // todo show rationale
                setupMapScreen()
            }
            else -> {
                requestPermissionLauncher.launch(
                    ACCESS_COARSE_LOCATION
                )
            }
        }
    }

    private fun setupMapScreen() {
        setContent {
            EventideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen(
                        hasLocationPermission = hasLocationPermission,
                    )
                }
            }
        }
    }
}
