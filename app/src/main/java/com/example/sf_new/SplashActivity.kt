package com.example.sf_new

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide // Только для Glide


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Используйте Glide для отображения GIF
        val gifImageView = findViewById<ImageView>(R.id.gifImageView)
        Glide.with(this)
            .load(R.raw.splash) // Укажите ваш GIF
            .into(gifImageView)

        // Задержка перед переходом к главной активности
        gifImageView.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000) // 3000ms = 3 секунды
    }
}
