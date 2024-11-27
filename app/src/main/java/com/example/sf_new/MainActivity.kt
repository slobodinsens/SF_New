package com.example.sf_new

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
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
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webView: WebView

    private val CAMERA_REQUEST_CODE = 100
    private val TELEGRAM_BOT_TOKEN = "7236439230:AAE0wtHwL4FYavGXAgMN6TOBy0QBqr72Zd4"
    private val TELEGRAM_CHAT_ID = "809706005"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация
        webView = findViewById(R.id.webView)
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

    @Suppress("DEPRECATION")
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == CAMERA_REQUEST_CODE) && (resultCode == Activity.RESULT_OK)) {
            val photoBitmap = data?.extras?.get("data") as? Bitmap
            if (photoBitmap != null) {
                val photoFile = saveBitmapToFile(photoBitmap)
                if (photoFile != null) {
                    sendPhotoToTelegram(photoFile)
                }
            } else {
                Toast.makeText(this, "Не удалось сохранить фотографию", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val file = File.createTempFile("photo", ".jpg", cacheDir)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения фотографии: ${e.message}")
            null
        }
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
                if (response.isSuccessful) {
                    Log.d(TAG, "Telegram API Response: ${response.body?.string()}")
                } else {
                    Log.e(TAG, "Ошибка отправки фотографии: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отправки фотографии: ${e.message}")
            }
        }.start()
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
