package com.yourstore.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.yourstore.app.R
import com.yourstore.app.data.source.local.PreferencesManager
import com.yourstore.app.databinding.ActivitySplashBinding
import com.yourstore.app.ui.admin.AdminActivity
import com.yourstore.app.ui.auth.LoginActivity
import com.yourstore.app.ui.customer.CustomerActivity
import org.koin.android.ext.android.inject

/**
 * Экран загрузки приложения с отображением логотипа и проверкой авторизации
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val TAG = "SplashActivity"
    private val SPLASH_DELAY = 1500L // 1.5 секунды

    private lateinit var binding: ActivitySplashBinding
    private val firebaseAuth: FirebaseAuth by inject()
    private val preferencesManager: PreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Скрываем системные UI элементы
        setupFullscreen()

        // Анимация появления логотипа
        startLogoAnimation()

        // Проверяем авторизацию после задержки
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, SPLASH_DELAY)
    }

    private fun setupFullscreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun startLogoAnimation() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(slideUp)
    }

    private fun checkAuthAndNavigate() {
        try {
            val currentUser = firebaseAuth.currentUser

            if (currentUser != null) {
                Log.d(TAG, "Пользователь авторизован: ${currentUser.uid}")

                if (preferencesManager.isAdmin()) {
                    Log.d(TAG, "Перенаправление на экран администратора")
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    Log.d(TAG, "Перенаправление на экран клиента")
                    startActivity(Intent(this, CustomerActivity::class.java))
                }
            } else {
                Log.d(TAG, "Пользователь не авторизован, переход на экран входа")
                startActivity(Intent(this, LoginActivity::class.java))
            }

            // Анимация перехода
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке авторизации: ${e.message}")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}