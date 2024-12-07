package com.example.projectvk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private var currentSteps = 0
    private var targetSteps = 0

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: throw RuntimeException("Accelerometer not available")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetSteps = intent?.getIntExtra("targetSteps", 0) ?: 0
        createNotificationChannel()
        startForeground(1, createNotification())
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val acceleration = sqrt((x * x + y * y + z * z).toDouble())
            val movementThreshold = 13.0

            if (acceleration >= movementThreshold && currentSteps < targetSteps) {
                currentSteps++

                // Update notification with currentSteps
                val notification = createNotification()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, notification)

                // Send broadcast to update UI in MainActivity
                val updateUIIntent = Intent("STEP_COUNT_UPDATE")
                updateUIIntent.putExtra("currentSteps", currentSteps)
                sendBroadcast(updateUIIntent)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ...
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "step_counter_channel",
                "Step Counter",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "step_counter_channel")
            .setContentTitle("Step Counter")
            .setContentText("Steps: $currentSteps / $targetSteps")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentIntent(pendingIntent)
            .build()
    }
}