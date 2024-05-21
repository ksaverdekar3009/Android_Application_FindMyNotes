package com.example.find_my_notes.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.example.find_my_notes.R
import com.example.find_my_notes.databinding.ActivitySplashscreenBinding

@SuppressLint("CustomSplashScreen")
class SplashscreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)
        val b = ActivitySplashscreenBinding.inflate(layoutInflater)
        setContentView(b.root)

        val animation = AnimationUtils.loadAnimation(this, R.anim.alpha)

        b.linearSplash.startAnimation(animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                startActivity(Intent(this@SplashscreenActivity, LoginActivity::class.java))
                finish()
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })
    }
}