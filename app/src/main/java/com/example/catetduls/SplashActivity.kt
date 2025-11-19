package com.example.catetduls

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragment_splash)


        supportActionBar?.hide()


        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val appNameTextView = findViewById<TextView>(R.id.appNameTextView)



        if (logoImageView != null) {
            val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            logoImageView.startAnimation(fadeInAnim)
        }

        if (appNameTextView != null) {
            val slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            appNameTextView.startAnimation(slideUpAnim)
        }


        Handler(Looper.getMainLooper()).postDelayed({

            val i = Intent(this, MainActivity::class.java)
            startActivity(i)


            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_TIME_OUT)
    }

    companion object {
        private const val SPLASH_TIME_OUT = 1700L
    }
}