@file:Suppress("DEPRECATION")

package com.example.sf_new

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

    private val CAMERA_REQUEST_CODE = 100
    private val TELEGRAM_BOT_TOKEN = "7236439230:AAE0wtHwL4FYavGXAgMN6TOBy0QBqr72Zd4"
    private val TELEGRAM_CHAT_ID = "809706005"
    private var photoUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация
        webView = findViewById(R.id.webView)
        responseTextView = findViewById(R.id.responseTextView)
        closeResponseButton = findViewById(R.id.closeResponseButton)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val recognitionButton: Button = findViewById(R.id.sf_recognition)
        val stolenCarButton: Button = findViewById(R.id.stolen_car)
        val photoButton: Button = findViewById(R.id.photo)

        recognitionButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        stolenCarButton.setOnClickListener {
            showElement(webVisible = true) // Показываем WebView
            loadStolenCarPage()
        }

        photoButton.setOnClickListener {
            if (isCameraPermissionGranted()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        closeResponseButton.setOnClickListener {
            hideResponseView()
        }
    }

    private fun loadStolenCarPage() {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.gov.il/apps/police/stolencar/")
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

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                sendPhotoToTelegram(File(getRealPathFromURI(uri)))
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

    private fun sendPhotoToTelegram(file: File) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", TELEGRAM_CHAT_ID)
            .addFormDataPart(
                "photo", file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendPhoto")
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    Log.d(TAG, "Telegram API Response: $responseBody")
                    updateResponseText("Фото отправлено успешно: $responseBody")
                } else {
                    Log.e(TAG, "Ошибка отправки фотографии: ${response.message}")
                    updateResponseText("Ошибка отправки: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отправки фотографии: ${e.message}")
                updateResponseText("Ошибка: ${e.message}")
            }
        }.start()
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

    private fun showElement(webVisible: Boolean) {
        webView.visibility = if (webVisible) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
