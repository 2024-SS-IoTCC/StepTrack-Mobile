package com.example.steptrack

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val usernameKey = "username"

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mqttManager: MqttManager

    private lateinit var usernameEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    // executor service to send step data to the edge in regular intervals
    private val executorService = Executors.newSingleThreadScheduledExecutor()

    private var sensorManager: SensorManager? = null

    data class StepEvent(val timestamp: String, val steps: Int)

    data class StepMessage(val username: String, val stepEvents: MutableList<StepEvent>)

    // list to hold all step events during one interval
    private val stepEvents: MutableList<StepEvent> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mqttManager = MqttManager()
        mqttManager.connect()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        usernameEditText = findViewById(R.id.usernameEditText)
        saveButton = findViewById(R.id.saveButton)
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        loadUsername()  //load username at start

        saveButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            if (username.isNotEmpty()) {
                saveUsername(username)
                Toast.makeText(this, "Username saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }

        executorService.scheduleAtFixedRate({
            // TODO: send step data to edge
            val username = sharedPreferences.getString(usernameKey, null)

            var json = ""

            if (username != null && stepEvents.size > 1) {
                val message = StepMessage(username, stepEvents)
                // TODO: coordinate exact key names with the edge team
                json = Gson().toJson(message)
                Log.d("stepData", json)
            } else {
                Log.d("stepData","Message could not be send for username $username and ${stepEvents.size} step events.")
                return@scheduleAtFixedRate
            }

            val stepEventsCount = stepEvents.size
            stepEvents.clear()

            runOnUiThread {
                // send data
                Toast.makeText(this, "Sending step $stepEventsCount events", Toast.LENGTH_SHORT)
                    .show()
                publishMessage(json)
            }
        }, 30, 30, TimeUnit.SECONDS)
    }

    override fun onResume() {
        super.onResume()
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "No step sensor available", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val totalSteps = event!!.values[0]
        val timestamp = System.currentTimeMillis().toString()
        stepEvents.add(StepEvent(timestamp, totalSteps.toInt()))
        val tv = findViewById<TextView>(R.id.tvCurrentSteps)
        tv.text = "Current Steps: ${totalSteps.toInt()}"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun saveUsername(username: String) {
        sharedPreferences.edit().putString(usernameKey, username).apply()
    }

    private fun loadUsername() {
        val username = sharedPreferences.getString(usernameKey, null)
        if (username != null) {
            usernameEditText.setText(username)
        } else {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_LONG).show()
        }
    }

    private fun publishMessage(message: String) {
        val topic = "steptrack"
        mqttManager.publishMessage(topic, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }
}
