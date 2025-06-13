package com.example.a1000_melochei.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.source.local.PreferencesManager
import com.example.a1000_melochei.service.NotificationService
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Экран загрузки приложения.
 * Показывается при запуске приложения, выполняет инициализацию и проверку авторизации.
 */
class SplashActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModel()
    private val preferencesManager: PreferencesManager by inject()
    private val notificationService: NotificationService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Скрываем status bar для полноэкранного отображения
        supportActionBar?.hide()

        // Запускаем инициализацию
        initializeApp()
    }

    /**
     * Инициализирует приложение
     */
    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                // Минимальное время показа splash screen
                delay(1500)

                // Выполняем инициализацию параллельно
                initializeServices()

                // Проверяем первый запуск
                if (preferencesManager.isFirstLaunch()) {
                    handleFirstLaunch()
                } else {
                    // Проверяем авторизацию и переходим к основному экрану
                    checkAuthAndProceed()
                }

            } catch (e: Exception) {
                // При ошибке переходим к экрану авторизации
                navigateToLogin()
            }
        }
    }

    /**
     * Инициализирует сервисы приложения
     */
    private suspend fun initializeServices() {
        try {
            // Создаем каналы уведомлений
            notificationService.createNotificationChannels()

            // Проверяем состояние авторизации
            authViewModel.checkCurrentUser()

        } catch (e: Exception) {
            // Логируем ошибки, но не прерываем процесс
            e.printStackTrace()
        }
    }

    /**
     * Обрабатывает первый запуск приложения
     */
    private fun handleFirstLaunch() {
        // Отмечаем, что первый запуск завершен
        preferencesManager.setFirstLaunchCompleted()

        // Устанавливаем настройки по умолчанию
        setDefaultSettings()

        // Переходим к авторизации
        navigateToLogin()
    }

    /**
     * Устанавливает настройки по умолчанию
     */
    private fun setDefaultSettings() {
        // Включаем уведомления по умолчанию
        preferencesManager.saveNotificationsEnabled(true)

        // Включаем отчеты о сбоях
        preferencesManager.saveCrashReportingEnabled(true)

        // Устанавливаем язык по умолчанию
        preferencesManager.saveSelectedLanguage("ru")

        // Устанавливаем режим отображения товаров
        preferencesManager.saveProductsViewMode("grid")

        // Устанавливаем сортировку товаров по умолчанию
        preferencesManager.saveProductsSortOrder("name_asc")
    }

    /**
     * Проверяет авторизацию и переходит к соответствующему экрану
     */
    private fun checkAuthAndProceed() {
        // Наблюдаем за состоянием пользователя
        authViewModel.currentUser.observe(this) { user ->
            when {
                user == null -> {
                    // Пользователь не авторизован
                    navigateToLogin()
                }
                user.isAdmin -> {
                    // Администратор
                    navigateToAdmin()
                }
                else -> {
                    // Обычный пользователь
                    navigateToCustomer()
                }
            }
        }
    }

    /**
     * Переходит к экрану авторизации
     */
    private fun navigateToLogin() {
        val intent = Intent(this, com.example.a1000_melochei.ui.auth.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Переходит к клиентской части
     */
    private fun navigateToCustomer() {
        val intent = Intent(this, com.example.a1000_melochei.ui.customer.CustomerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Переходит к административной части
     */
    private fun navigateToAdmin() {
        val intent = Intent(this, com.example.a1000_melochei.ui.admin.AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Обрабатывает нажатие кнопки "Назад"
     */
    override fun onBackPressed() {
        // Не позволяем пользователю вернуться на splash screen
        // Просто закрываем приложение
        finishAffinity()
    }

    /**
     * Очищаем наблюдателей при уничтожении активности
     */
    override fun onDestroy() {
        super.onDestroy()
        authViewModel.currentUser.removeObservers(this)
    }
}