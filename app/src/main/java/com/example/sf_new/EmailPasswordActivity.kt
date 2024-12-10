package com.example.sf_new

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*

class EmailPasswordActivity : AppCompatActivity() {

    private val SERVER_URL = "http://10.0.0.43:5000/process"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_password)

        val emailEditText: EditText = findViewById(R.id.email)
        val passwordEditText: EditText = findViewById(R.id.password)
        val submitButton: Button = findViewById(R.id.submit_button)
        val switch1: Switch = findViewById(R.id.switch1)
        val switch2: Switch = findViewById(R.id.switch2)
        val switch3: Switch = findViewById(R.id.switch3)

        submitButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Collect switch states
            val carNumberState = if (switch1.isChecked) "Car Number" else ""
            val liveNotificationState = if (switch2.isChecked) "Live Notification" else ""
            val gpsState = if (switch3.isChecked) "GPS" else ""

            // Validate input
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in both Email and Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Send data to the server
            sendDataToServer(email, password, carNumberState, liveNotificationState, gpsState)
        }
    }

    private fun sendDataToServer(
        email: String,
        password: String,
        carNumber: String,
        liveNotification: String,
        gps: String
    ) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("email", email)
            .addFormDataPart("password", password)
            .apply {
                if (carNumber.isNotEmpty()) addFormDataPart("option", carNumber)
                if (liveNotification.isNotEmpty()) addFormDataPart("option", liveNotification)
                if (gps.isNotEmpty()) addFormDataPart("option", gps)
            }
            .build()

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        // Execute request in a background thread
        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        Toast.makeText(this, "Server Response: $responseBody", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Server Error: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to connect: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
