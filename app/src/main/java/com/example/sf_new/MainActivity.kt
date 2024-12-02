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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
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

    private val SERVER_URL = "https://sensfusionserver-d68b8e068a0b.herokuapp.com/process"

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
                sendTextToServer(text)
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

    private fun sendPhotoToServer(file: File) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id", "android_app")
            .addFormDataPart(
                "image",
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody != null && response.header("Content-Type")?.contains("image") == true) {
                        val receivedImageFile = File(getExternalFilesDir(null), "received_image.jpg")
                        FileOutputStream(receivedImageFile).use { output ->
                            output.write(responseBody.bytes())
                        }
                        updateResponseTextWithImage(receivedImageFile.absolutePath)
                    } else {
                        responseBody?.string()?.let { updateResponseText(it) }
                    }
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
            buttonsGroup.visibility = View.GONE
        }
    }

    private fun hideImageView() {
        receivedImageView.visibility = View.GONE
        closeResponseButton.visibility = View.GONE
        buttonsGroup.visibility = View.VISIBLE
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
                    val responseBody = response.body?.string()
                    updateResponseText("Ответ от сервера: $responseBody")
                } else {
                    updateResponseText("Ошибка: ${response.message}")
                }
            } catch (e: Exception) {
                updateResponseText("Ошибка соединения: ${e.message}")
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
