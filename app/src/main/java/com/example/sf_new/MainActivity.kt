@file:Suppress("DEPRECATION")

package com.example.sf_new

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webView: WebView
    private lateinit var responseTextView: TextView
    private lateinit var closeResponseButton: Button
    private lateinit var openTextInputButton: Button
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var textInputContainer: LinearLayout
    private lateinit var buttonsGroup: LinearLayout
    private lateinit var receivedImageView: ImageView

    private val CAMERA_REQUEST_CODE = 100
    private var photoUri: Uri? = null

    private val SERVER_URL = "http://10.0.0.43:5000"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        webView = findViewById(R.id.webView)
        responseTextView = findViewById(R.id.responseTextView)
        closeResponseButton = findViewById(R.id.closeResponseButton)
        openTextInputButton = findViewById(R.id.openTextInputButton)
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        textInputContainer = findViewById(R.id.textInputContainer)
        buttonsGroup = findViewById(R.id.buttonsGroup)
        receivedImageView = findViewById(R.id.receivedImageView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val recognitionButton: Button = findViewById(R.id.sf_recognition)
        val stolenCarButton: Button = findViewById(R.id.stolen_car)
        val photoButton: Button = findViewById(R.id.photo)

        recognitionButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        stolenCarButton.setOnClickListener {
            showStolenCarPage()
        }

        photoButton.setOnClickListener {
            if (isCameraPermissionGranted()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        closeResponseButton.setOnClickListener {
            when {
                receivedImageView.visibility == View.VISIBLE -> hideImageView()
                responseTextView.visibility == View.VISIBLE -> hideResponseView()
                webView.visibility == View.VISIBLE -> {
                    webView.visibility = View.GONE
                    closeResponseButton.visibility = View.GONE
                }
            }
        }

        openTextInputButton.setOnClickListener {
            showTextInputContainer()
        }

        sendButton.setOnClickListener {
            val text = inputEditText.text.toString()
            if (text.isNotBlank()) {
                inputEditText.text.clear()
                hideTextInputContainer()
            } else {
                Toast.makeText(this, "Введите текст для отправки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showStolenCarPage() {
        webView.visibility = View.VISIBLE
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.gov.il/apps/police/stolencar/")
        closeResponseButton.visibility = View.VISIBLE
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    private fun openCamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        photoUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showNotification(title: String, message: String) {
        val channelId = "server_notifications"
        val channelName = "Server Notifications"

        // Create Notification Manager
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel (For Android O and above)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create Intent for notification click
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build Notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Use your app's notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show Notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }



    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val file = File(getRealPathFromURI(uri))
                sendPhotoToServer(file)
            } ?: run {
                Toast.makeText(this, "Не удалось сохранить фотографию", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                path = cursor.getString(columnIndex)
            }
        }
        return path
    }

    private fun sendTextToServer(text: String) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id", "android_app")
            .addFormDataPart("text", text)
            .build()

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "No response"
                    updateResponseText(responseBody)
                    showNotification("Server Response", responseBody)
                } else {
                    updateResponseText("Ошибка: ${response.message}")
                }
            } catch (e: Exception) {
                updateResponseText("Ошибка соединения: ${e.message}")
            }
        }.start()
    }
    private fun sendPhotoToServer(file: File) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id", "android_app")
            .addFormDataPart("image", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "Photo received"
                    updateResponseText(responseBody)
                    showNotification("Photo Response", responseBody)
                } else {
                    updateResponseText("Ошибка: ${response.message}")
                }
            } catch (e: Exception) {
                updateResponseText("Ошибка соединения: ${e.message}")
            }
        }.start()
    }


    private fun updateResponseTextWithImage(absolutePath: String) {
        runOnUiThread {
            receivedImageView.setImageURI(Uri.fromFile(File(absolutePath)))
            receivedImageView.visibility = View.VISIBLE
            closeResponseButton.visibility = View.VISIBLE
            // Keep buttons visible
            buttonsGroup.visibility = View.VISIBLE
        }
    }


    private fun hideImageView() {
        receivedImageView.visibility = View.GONE
        closeResponseButton.visibility = View.GONE
        buttonsGroup.visibility = View.VISIBLE
    }



    private fun updateResponseText(message: String) {
        runOnUiThread {
            responseTextView.text = message
            responseTextView.visibility = View.VISIBLE
            closeResponseButton.visibility = View.VISIBLE
        }
    }

    private fun hideResponseView() {
        responseTextView.visibility = View.GONE
        closeResponseButton.visibility = View.GONE
    }

    private fun showTextInputContainer() {
        textInputContainer.visibility = View.VISIBLE
        buttonsGroup.visibility = View.GONE
    }

    private fun hideTextInputContainer() {
        textInputContainer.visibility = View.GONE
        buttonsGroup.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        if (receivedImageView.visibility == View.VISIBLE) {
            hideImageView()
        } else if (textInputContainer.visibility == View.VISIBLE) {
            hideTextInputContainer()
        } else if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
