package com.yourstore.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
 * Экран-заставка (сплэш-скрин) приложения.
 * Отображается при запуске приложения и обеспечивает плавный переход
 * к основному интерфейсу, выполняя начальную инициализацию.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val preferencesManager: PreferencesManager by inject()
    private val firebaseAuth: FirebaseAuth by inject()

    // Длительность отображения заставки в миллисекундах
    private val SPLASH_DURATION = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Запускаем анимации для элементов
        startAnimations()

        // Определяем, куда перейти после заставки
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DURATION)
    }

    /**
     * Запускает анимации для элементов экрана-заставки
     */
    private fun startAnimations() {
        try {
            // Анимация появления логотипа
            val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            binding.ivLogo.startAnimation(fadeInAnimation)

            // Анимация движения снизу вверх для названия приложения
            val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            binding.tvAppName.startAnimation(slideUpAnimation)
        } catch (e: Exception) {
            // В случае ошибки с анимацией просто продолжаем работу
            e.printStackTrace()
        }
    }

    /**
     * Определяет, куда перейти после заставки:
     * - Если пользователь авторизован как админ, то в панель управления
     * - Если пользователь авторизован как клиент, то в интерфейс клиента
     * - Иначе на экран входа
     */
    private fun navigateToNextScreen() {
        val currentUser = firebaseAuth.currentUser

        val intent = when {
            // Если пользователь авторизован
            currentUser != null -> {
                // Проверяем, является ли пользователь администратором
                if (preferencesManager.isAdmin()) {
                    Intent(this, AdminActivity::class.java)
                } else {
                    Intent(this, CustomerActivity::class.java)
                }
            }
            // Если это первый запуск приложения, можно показать онбординг
            preferencesManager.isFirstLaunch() -> {
                // В будущем здесь можно добавить переход на экран онбординга
                // Intent(this, OnboardingActivity::class.java)
                preferencesManager.setFirstLaunch(false)
                Intent(this, LoginActivity::class.java)
            }
            // В остальных случаях переходим на экран входа
            else -> {
                Intent(this, LoginActivity::class.java)
            }
        }

        // Запускаем переход с анимацией затухания
        startActivity(intent)
        try {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        } catch (e: Exception) {
            // Игнорируем ошибки анимации при переходе
            e.printStackTrace()
        }
        finish()
    }
}