package com.example.steptrack

import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import android.content.SharedPreferences
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
    }

    private fun saveUsername(username: String) {
        sharedPreferences.edit().putString("username", username).apply()
    }

    private fun loadUsername() {
        val username = sharedPreferences.getString("username", null)
        if (username != null) {
            usernameEditText.setText(username)
        } else {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_LONG).show()
        }
    }
}
