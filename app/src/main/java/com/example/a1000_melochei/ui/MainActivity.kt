package com.example.a1000_melochei.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.a1000_melochei.data.source.local.PreferencesManager
import com.example.a1000_melochei.ui.admin.AdminActivity
import com.example.a1000_melochei.ui.auth.LoginActivity
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.ui.customer.CustomerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Главная активность приложения.
 * Определяет, какую активность показать пользователю в зависимости от состояния авторизации.
 */
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModel()
    private val preferencesManager: PreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем авторизацию и перенаправляем пользователя
        checkAuthenticationAndRedirect()
    }

    /**
     * Проверяет состояние авторизации и перенаправляет пользователя
     */
    private fun checkAuthenticationAndRedirect() {
        lifecycleScope.launch {
            // Небольшая задержка для плавности перехода
            delay(100)

            try {
                // Проверяем текущего пользователя
                authViewModel.checkCurrentUser()

                // Наблюдаем за состоянием пользователя
                authViewModel.currentUser.observe(this@MainActivity) { user ->
                    when {
                        user == null -> {
                            // Пользователь не авторизован - переходим к экрану авторизации
                            navigateToLogin()
                        }
                        user.isAdmin -> {
                            // Пользователь является администратором
                            navigateToAdmin()
                        }
                        else -> {
                            // Обычный пользователь
                            navigateToCustomer()
                        }
                    }
                }

                // Обрабатываем специальные интенты
                handleSpecialIntent()

            } catch (e: Exception) {
                // При ошибке переходим к экрану авторизации
                navigateToLogin()
            }
        }
    }

    /**
     * Обрабатывает специальные интенты (например, из уведомлений)
     */
    private fun handleSpecialIntent() {
        intent?.let { receivedIntent ->
            when {
                receivedIntent.getBooleanExtra("open_admin", false) -> {
                    // Открыть админ панель
                    if (authViewModel.isUserAdmin()) {
                        val adminIntent = Intent(this, AdminActivity::class.java)

                        // Передаем дополнительные параметры
                        if (receivedIntent.getBooleanExtra("open_orders", false)) {
                            adminIntent.putExtra("open_orders", true)
                        }
                        if (receivedIntent.getBooleanExtra("open_products", false)) {
                            adminIntent.putExtra("open_products", true)
                        }

                        startActivity(adminIntent)
                        finish()
                    }
                }
                receivedIntent.getBooleanExtra("open_cart", false) -> {
                    // Открыть корзину
                    val customerIntent = Intent(this, CustomerActivity::class.java)
                    customerIntent.putExtra("open_cart", true)
                    startActivity(customerIntent)
                    finish()
                }
                receivedIntent.getBooleanExtra("open_orders", false) -> {
                    // Открыть заказы
                    val customerIntent = Intent(this, CustomerActivity::class.java)
                    customerIntent.putExtra("open_orders", true)
                    startActivity(customerIntent)
                    finish()
                }
                receivedIntent.getBooleanExtra("open_order_detail", false) -> {
                    // Открыть детали заказа
                    val orderId = receivedIntent.getStringExtra("order_id")
                    if (!orderId.isNullOrEmpty()) {
                        val customerIntent = Intent(this, CustomerActivity::class.java)
                        customerIntent.putExtra("open_order_detail", true)
                        customerIntent.putExtra("order_id", orderId)
                        startActivity(customerIntent)
                        finish()
                    }
                }
                receivedIntent.getBooleanExtra("open_promotions", false) -> {
                    // Открыть акции
                    val customerIntent = Intent(this, CustomerActivity::class.java)
                    customerIntent.putExtra("open_promotions", true)
                    startActivity(customerIntent)
                    finish()
                }
            }
        }
    }

    /**
     * Переходит к экрану авторизации
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Переходит к клиентской части приложения
     */
    private fun navigateToCustomer() {
        val intent = Intent(this, CustomerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Передаем специальные параметры, если они есть
        this.intent?.let { receivedIntent ->
            when {
                receivedIntent.getBooleanExtra("open_cart", false) -> {
                    intent.putExtra("open_cart", true)
                }
                receivedIntent.getBooleanExtra("open_orders", false) -> {
                    intent.putExtra("open_orders", true)
                }
                receivedIntent.getBooleanExtra("open_order_detail", false) -> {
                    val orderId = receivedIntent.getStringExtra("order_id")
                    if (!orderId.isNullOrEmpty()) {
                        intent.putExtra("open_order_detail", true)
                        intent.putExtra("order_id", orderId)
                    }
                }
                receivedIntent.getBooleanExtra("open_promotions", false) -> {
                    intent.putExtra("open_promotions", true)
                }
            }
        }

        startActivity(intent)
        finish()
    }

    /**
     * Переходит к административной части приложения
     */
    private fun navigateToAdmin() {
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Передаем специальные параметры, если они есть
        this.intent?.let { receivedIntent ->
            when {
                receivedIntent.getBooleanExtra("open_orders", false) -> {
                    intent.putExtra("open_orders", true)
                }
                receivedIntent.getBooleanExtra("open_products", false) -> {
                    intent.putExtra("open_products", true)
                }
                receivedIntent.getBooleanExtra("open_analytics", false) -> {
                    intent.putExtra("open_analytics", true)
                }
                receivedIntent.getStringExtra("order_id") != null -> {
                    intent.putExtra("order_id", receivedIntent.getStringExtra("order_id"))
                    intent.putExtra("open_order_detail", true)
                }
            }
        }

        startActivity(intent)
        finish()
    }

    /**
     * Обрабатывает нажатие кнопки "Назад"
     */
    override fun onBackPressed() {
        // Так как это промежуточная активность, просто закрываем приложение
        finishAffinity()
    }

    /**
     * Обрабатывает новые интенты (например, при получении уведомлений)
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        handleSpecialIntent()
    }

    /**
     * Очищает наблюдателей при уничтожении активности
     */
    override fun onDestroy() {
        super.onDestroy()
        authViewModel.currentUser.removeObservers(this)
    }
}