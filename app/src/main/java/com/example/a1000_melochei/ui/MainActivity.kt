package com.yourstore.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.yourstore.app.R
import com.yourstore.app.data.source.local.PreferencesManager
import com.yourstore.app.ui.admin.AdminActivity
import com.yourstore.app.ui.auth.LoginActivity
import com.yourstore.app.ui.customer.CustomerActivity
import org.koin.android.ext.android.inject

/**
 * Главная активность - точка входа в приложение.
 * Проверяет авторизацию и перенаправляет на соответствующий экран.
 */
class MainActivity : AppCompatActivity() {

    private val preferencesManager: PreferencesManager by inject()
    private val firebaseAuth: FirebaseAuth by inject()

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Для предотвращения повторных переходов
        if (isTaskRoot) {
            // Определяем, куда направить пользователя
            checkAuthAndRedirect()
        } else {
            finish()
        }
    }

    /**
     * Проверяет состояние авторизации и перенаправляет на соответствующую активность
     */
    private fun checkAuthAndRedirect() {
        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            Log.d(TAG, "Пользователь авторизован: ${currentUser.uid}")

            // Проверяем, является ли пользователь администратором
            val isAdmin = preferencesManager.isAdmin()

            if (isAdmin) {
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

        finish()
    }
}