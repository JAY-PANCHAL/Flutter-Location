package com.example.location

import android.annotation.TargetApi
import android.app.NotificationChannel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Headers.Companion.toHeaders
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.util.Log
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.location.Location
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import android.content.pm.ServiceInfo
import okhttp3.*
import java.io.IOException

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val client = OkHttpClient()
    val mediaType = "application/json; charset=utf-8".toMediaType()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        startForegroundService()
        startLocationUpdates()
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Tracking location in background")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        val foregroundServiceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            else
                null

        // Start foreground service with type if available
        if (foregroundServiceType != null) {
            startForeground(1, notification, foregroundServiceType)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    updateNotification(location)
                    sendLocationToServer(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToServer(location: Location) {
        val url = "https://666bef2749dbc5d7145bd9d0.mockapi.io/locationapi"
        val json = """
        {
            "latitude": ${location.latitude},
            "longitude": ${location.longitude}
        }
    """

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json.toByteArray()
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        // Add logging before sending the request
        Log.d("LocationSharing", "Sending location to server: $url")
        Log.d("LocationSharing", "Location data: $json")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Log the failure
                Log.e("LocationSharing", "Error sending location: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Log successful response
                    Log.d("LocationSharing", "Location sent successfully: ${response.code}")
                } else {
                    // Log error response
                    Log.e("LocationSharing", "Error sending location: ${response.code}")
                    // Optionally, log the response body for debugging
                    // val responseBody = response.body?.string()
                    // if (responseBody != null) {
                    //     Log.e("LocationSharing", "Response body: $responseBody")
                    // }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun stopService() {
        stopForeground(true)
        stopSelf()
        Log.d("LocationService", "Service stopped")
    }

    private fun updateNotification(location: Location) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationContent = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText(notificationContent)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    companion object {
        private const val CHANNEL_ID = "LocationServiceChannel"
    }
}
