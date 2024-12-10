@file:Suppress("DEPRECATION")

package com.example.sf_new

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SelectPictureActivity : AppCompatActivity() {

    private lateinit var selectPictureButton: Button
    private lateinit var sendImageButton: Button
    private lateinit var imagePreview: ImageView
    private val SELECT_PICTURE_REQUEST_CODE = 101
    private val SERVER_URL = "http://10.0.0.43:5000/process"
    private var selectedImageUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_input_controls)

        selectPictureButton = findViewById(R.id.selectPictureButton)
        //sendImageButton = findViewById(R.id.sendImageButton)
        imagePreview = findViewById(R.id.imagePreview)

        imagePreview.visibility = View.GONE // Initially hide the preview
        sendImageButton.visibility = View.GONE // Initially hide the send button

        selectPictureButton.setOnClickListener {
            if (isReadStoragePermissionGranted()) {
                openGallery()
            } else {
                requestReadStoragePermission()
            }
        }

        sendImageButton.setOnClickListener {
            if (selectedImageUri != null) {
                sendImageToServer(selectedImageUri!!)
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, SELECT_PICTURE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                imagePreview.setImageURI(selectedImageUri) // Show the selected image in ImageView
                imagePreview.visibility = View.VISIBLE
                sendImageButton.visibility = View.VISIBLE // Show the send button
            } else {
                Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this, "Server Response: $responseBody", Toast.LENGTH_LONG).show()
                            resetUI()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Server Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Failed to get file path", Toast.LENGTH_SHORT).show()
        }
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

    private fun isReadStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestReadStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            SELECT_PICTURE_REQUEST_CODE
        )
    }

    private fun resetUI() {
        selectedImageUri = null
        sendImageButton.visibility = View.GONE // Hide the send button
        imagePreview.visibility = View.GONE // Hide the preview
        Toast.makeText(this, "Image sent successfully!", Toast.LENGTH_SHORT).show()
    }
}
