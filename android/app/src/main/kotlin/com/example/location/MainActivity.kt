package com.example.location
import io.flutter.embedding.android.FlutterActivity
import android.os.Bundle
import io.flutter.plugin.common.MethodChannel
import android.content.Intent

class MainActivity: FlutterActivity()
{
    private val CHANNEL = "com.example.location_service"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            call, result ->
            if (call.method == "startLocationUpdates") {
                startService()
            }
            else if (call.method == "stopLocationUpdates") {
                stopService()
                result.success(null)

            }
            else {
                result.notImplemented()
            }
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }
}