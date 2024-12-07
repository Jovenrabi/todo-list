package com.example.projectvk

import android.app.Dialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener {

    // Step counter variables
    private var currentSteps = 0
    private var targetSteps = 0
    private var isStepDetected = false
    private var isCounting = false // Add this flag

    // IMU variables
    private var prevAcceleration = 0f
    private var currentAcceleration = 0f
    private var acceleration = 0f
    private var stepThreshold = 2.5f // Adjust this threshold as needed

    // Sensor variables
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var vibrator: Vibrator

    // UI elements
    private lateinit var addBtn: ImageButton
    private lateinit var startBtn: Button
    private lateinit var resetBtn: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var timeLeftTv: TextView

    // Media Players for finish and notification
    private var finishMediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        addBtn = findViewById(R.id.btnAdd)
        startBtn = findViewById(R.id.btnPlayPause)
        resetBtn = findViewById(R.id.ib_reset)
        progressBar = findViewById(R.id.pbTimer)
        timeLeftTv = findViewById(R.id.tvTimeLeft)

        // Initialize Media Players
        finishMediaPlayer = MediaPlayer.create(this, R.raw.notif)

        // Setup click listeners
        addBtn.setOnClickListener { setTimeFunction() }
        startBtn.setOnClickListener { startCounting() }
        resetBtn.setOnClickListener { resetCounter() }

        // Initialize sensor and vibrator
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: throw RuntimeException("Accelerometer not available")
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrator() {
        val vibrationEffect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    }

    private fun resetCounter() {
        currentSteps = 0
        targetSteps = 0
        progressBar.progress = 0
        timeLeftTv.text = "0"
        startBtn.text = "Start"
        isCounting = false // Reset the flag
    }

    private fun startCounting() {
        if (targetSteps > 0) {
            isCounting = true // Set the flag to true when starting
            startBtn.text = "Counting..."
        } else {
            Toast.makeText(this, "Enter Your Goal Steps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setTimeFunction() {
        val timeDialog = Dialog(this)
        timeDialog.setContentView(R.layout.add_dialog)
        val timeSet = timeDialog.findViewById<EditText>(R.id.etGetTime)
        timeDialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            if (timeSet.text.isEmpty()) {
                Toast.makeText(this, "Enter Target Steps", Toast.LENGTH_SHORT).show()
            } else {
                resetCounter()
                targetSteps = timeSet.text.toString().toInt()
                timeLeftTv.text = targetSteps.toString() // Show target steps initially
                startBtn.text = "Start"
                progressBar.max = targetSteps
            }
            timeDialog.dismiss()
        }
        timeDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        finishMediaPlayer?.release()
        finishMediaPlayer = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER && isCounting) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                prevAcceleration = currentAcceleration
                currentAcceleration = sqrt((x * x + y * y + z * z))
                acceleration = currentAcceleration - prevAcceleration

                if (acceleration > stepThreshold && currentSteps < targetSteps) {
                    currentSteps++
                    timeLeftTv.text = currentSteps.toString()
                    progressBar.progress = currentSteps
                    isStepDetected = true

                    // Check for 5 steps remaining
                    if (targetSteps - currentSteps == 0) {
                    }

                    if (currentSteps >= targetSteps) {
                        // Target steps reached
                        vibrator() // Start vibrating
                        finishMediaPlayer?.start()
                        resetCounter()
                    }
                } else if (acceleration < -stepThreshold) {
                    isStepDetected = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}