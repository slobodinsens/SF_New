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
import com.bumptech.glide.Glide
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
    private lateinit var gifImageView: ImageView // Added gifImageView initialization

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
        gifImageView = findViewById(R.id.gifImageView) // Initialize gifImageView
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
            hideResponseView()
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

        // Load GIF into gifImageView using Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.your_gif_file) // Replace with your GIF resource
            .into(gifImageView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showStolenCarPage() {
        webView.visibility = View.VISIBLE
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.gov.il/apps/police/stolencar/")
        buttonsGroup.visibility = View.VISIBLE
    }

    // Other existing methods ...

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
        if (textInputContainer.visibility == View.VISIBLE) {
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
