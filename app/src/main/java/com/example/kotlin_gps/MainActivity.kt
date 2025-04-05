package com.example.kotlin_gps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kotlin_gps.ui.theme.KotlingpsTheme

class MainActivity : ComponentActivity() {
    private val permissionState = mutableStateOf(false)
    private val locationState = mutableStateOf<Location?>(null)
    private lateinit var locationManager: LocationManager
    private val TAG = "MainActivity"
    
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "Location update received: ${location.latitude}, ${location.longitude}")
            locationState.value = location
        }
        
        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Provider disabled: $provider")
        }
        
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Provider enabled: $provider")
        }
        
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }
    
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false || 
                                 permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            
            Log.d(TAG, "Permissions result: $locationGranted")
            permissionState.value = locationGranted
            
            if (locationGranted) {
                startLocationUpdates()
            }
        }
        
        enableEdgeToEdge()
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        
        checkLocationPermission()
        
        setContent {
            KotlingpsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (permissionState.value) {
                        LocationDisplay(
                            location = locationState.value,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        PermissionRequestScreen(
                            onRequestPermission = { requestLocationPermission() },
                            onOpenSettings = { openAppSettings() },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
    
    private fun checkLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        permissionState.value = fineLocationGranted || coarseLocationGranted
        
        if (permissionState.value) {
            startLocationUpdates()
        }
    }
    
    private fun requestLocationPermission() {
        Log.d(TAG, "Requesting location permissions")
        
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (shouldShowRationale) {
            Log.d(TAG, "Should show rationale")
            
            
        }
        
        
        requestPermissionLauncher.launch(locationPermissions)
    }
    
    override fun onResume() {
        super.onResume()
        if (permissionState.value) {
            startLocationUpdates()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (permissionState.value) {
            try {
                Log.d(TAG, "Starting location updates")
                
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L, 
                        1f,    
                        locationListener
                    )
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error starting location updates", ex)
            }
        }
    }
    
    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
            Log.d(TAG, "Location updates stopped")
        } catch (ex: Exception) {
            Log.e(TAG, "Error stopping location updates", ex)
        }
    }
    
    private fun openAppSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also {
            startActivity(it)
        }
    }
}

@Composable
fun LocationDisplay(
    location: Location?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (location != null) {
            Text(
                text = "Your current location:",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Latitude: ${location.latitude}",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
            Text(
                text = "Longitude: ${location.longitude}",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
            if (location.hasAltitude()) {
                Text(
                    text = "Altitude: ${location.altitude} meters",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
            if (location.hasAccuracy()) {
                Text(
                    text = "Accuracy: ${location.accuracy} meters",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
        } else {
            Text(
                text = "Waiting for location...",
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Location permission is required for this app to function properly.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Request Permission")
        }
        
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Open Settings")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationDisplayPreview() {
    KotlingpsTheme {
        val mockLocation = Location("mock").apply {
            latitude = 37.7749
            longitude = -122.4194
            altitude = 12.0
            accuracy = 5.0f
        }
        LocationDisplay(location = mockLocation)
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRequestScreenPreview() {
    KotlingpsTheme {
        PermissionRequestScreen(
            onRequestPermission = {},
            onOpenSettings = {}
        )
    }
}