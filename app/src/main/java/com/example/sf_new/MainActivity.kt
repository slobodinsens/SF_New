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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webView: WebView
    private lateinit var responseTextView: TextView
    private lateinit var closeResponseButton: Button
    private lateinit var openTextInputButton: Button
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var inputControls: View
    private lateinit var buttonsGroup: LinearLayout
    private lateinit var receivedImageView: ImageView
    private lateinit var logicContainer: View


    private val CAMERA_REQUEST_CODE = 100
    private val SELECT_PICTURE_REQUEST_CODE = 101
    private val READ_STORAGE_PERMISSION_CODE = 102
    private var photoUri: Uri? = null
    private val SERVER_URL = "http://10.0.0.43:5000/process"

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
        inputControls = findViewById(R.id.inputControls)
        buttonsGroup = findViewById(R.id.buttonsGroup)
        receivedImageView = findViewById(R.id.receivedImageView)
        logicContainer = findViewById(R.id.logicContainer)
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

        openTextInputButton.setOnClickListener {
            logicContainer.visibility = View.GONE
            buttonsGroup.visibility = View.GONE
            inputControls.visibility = View.VISIBLE
        }

        sendButton.setOnClickListener {
            val text = inputEditText.text.toString().trim()
            if (text.isNotBlank()) {
                sendTextToServer(text)
            } else {
                Toast.makeText(this, "Введите текст для отправки", Toast.LENGTH_SHORT).show()
            }
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
                    runOnUiThread {
                        responseTextView.text = "Ответ от сервера: $responseBody"
                        responseTextView.visibility = View.VISIBLE
                        closeResponseButton.visibility = View.VISIBLE
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Ошибка: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка соединения: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedImageUri = data?.data
            if (selectedImageUri != null) {
                sendImageToServer(selectedImageUri)
            } else {
                Toast.makeText(this, "Не удалось выбрать изображение", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (photoUri != null) {
                sendImageToServer(photoUri!!)
            } else {
                Toast.makeText(this, "Не удалось сделать снимок", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendImageToServer(imageUri: Uri) {
        val filePath = getPathFromUri(imageUri)
        if (filePath != null) {
            val client = OkHttpClient()
            val file = File(filePath)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", "android_app")
                .addFormDataPart("image", file.name, file.asRequestBody())
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
                        runOnUiThread {
                            responseTextView.text = "Ответ от сервера: $responseBody"
                            responseTextView.visibility = View.VISIBLE
                            closeResponseButton.visibility = View.VISIBLE
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Ошибка: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Ошибка соединения: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Не удалось получить путь файла", Toast.LENGTH_SHORT).show()
        }
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
    private fun getPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
        return null
    }

    override fun onBackPressed() {
        if (receivedImageView.visibility == View.VISIBLE) {
            receivedImageView.visibility = View.GONE
        } else if (inputControls.visibility == View.VISIBLE) {
            inputControls.visibility = View.GONE
            buttonsGroup.visibility = View.VISIBLE
            logicContainer.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }
}
