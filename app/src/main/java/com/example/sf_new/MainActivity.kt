@file:Suppress("DEPRECATION")

package com.example.sf_new

import ImageAdapter
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val SELECT_PICTURE_REQUEST_CODE = 101
    private val READ_STORAGE_PERMISSION_CODE = 102
    private var photoUri: Uri? = null
    private val SERVER_URL = "http://192.168.1.108:5000/process"

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
        //textInputContainer = findViewById(R.id.textInputContainer)
        buttonsGroup = findViewById(R.id.buttonsGroup)
        receivedImageView = findViewById(R.id.receivedImageView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val settingsButton: Button = findViewById(R.id.settings)
        val recognitionButton: Button = findViewById(R.id.sf_recognition)
        val stolenCarButton: Button = findViewById(R.id.stolen_car)
        val photoButton: Button = findViewById(R.id.photo)
        val selectPictureButton: Button = findViewById(R.id.selectPictureButton)


        // Add click listener for selectPictureButton
        selectPictureButton.setOnClickListener {
            if (isReadStoragePermissionGranted()) {
                showPhotosPopup()
            } else {
                requestReadStoragePermission()
            }
        }
        settingsButton.setOnClickListener {
            // Navigate to EmailPasswordActivity
            val intent = Intent(this, EmailPasswordActivity::class.java)
            startActivity(intent)
        }

        recognitionButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        stolenCarButton.setOnClickListener {
            showStolenCarPage()
        }

        photoButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_REQUEST_CODE
                )
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

        val openTextInputButton = findViewById<Button>(R.id.openTextInputButton)
        val inputControls = findViewById<View>(R.id.inputControls)
        val logicContainer = findViewById<View>(R.id.logicContainer)
        val buttonsGroup = findViewById<View>(R.id.buttonsGroup)

        openTextInputButton.setOnClickListener {
            // Скрываем элементы activity_main
            logicContainer.visibility = View.GONE
            buttonsGroup.visibility = View.GONE

            // Показываем inputControls
            inputControls.visibility = View.VISIBLE
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, SELECT_PICTURE_REQUEST_CODE)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showStolenCarPage() {
        webView.visibility = View.VISIBLE
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.gov.il/apps/police/stolencar/")
        closeResponseButton.visibility = View.VISIBLE
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

    private fun hideResponseView() {
        responseTextView.visibility = View.GONE
        closeResponseButton.visibility = View.GONE
    }

    private fun isReadStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestReadStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                READ_STORAGE_PERMISSION_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun showPhotosPopup() {
        val imageUris = fetchImagesFromStorage(contentResolver)

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No photos found", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_photos_grid, null)
        val gridView: GridView = dialogView.findViewById(R.id.gridViewPhotos)

        val adapter = ImageAdapter(this, imageUris)
        gridView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Select a Photo")
            .setNegativeButton("Close", null)
            .create()

        gridView.setOnItemClickListener { _, _, position, _ ->
            dialog.dismiss()
            val selectedImageUri = imageUris[position]
            displaySelectedPhoto(selectedImageUri)
        }

        dialog.show()
    }

    private fun fetchImagesFromStorage(contentResolver: ContentResolver): List<Uri> {
        val imageUris = mutableListOf<Uri>()

        val projection = arrayOf(MediaStore.Images.Media._ID)
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(queryUri, projection, null, null, sortOrder)

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(queryUri, id.toString())
                imageUris.add(contentUri)
            }
        }

        return imageUris
    }

    private fun displaySelectedPhoto(imageUri: Uri) {
        receivedImageView.setImageURI(imageUri)
        receivedImageView.visibility = View.VISIBLE
    }

    private fun updateResponseText(message: String) {
        runOnUiThread {
            responseTextView.text = message
            responseTextView.visibility = View.VISIBLE
            closeResponseButton.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPhotosPopup()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideImageView() {
        receivedImageView.visibility = View.GONE
        closeResponseButton.visibility = View.GONE
        buttonsGroup.visibility = View.VISIBLE
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
}
