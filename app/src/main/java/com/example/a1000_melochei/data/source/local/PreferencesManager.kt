package com.yourstore.app.data.source.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.appcompat.app.AppCompatDelegate

/**
 * Менеджер для работы с локальными настройками приложения через SharedPreferences.
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "melochei_preferences"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CART_ID = "cart_id"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_FCM_TOKEN = "fcm_token" // Новый ключ для FCM токена
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Установка статуса администратора
     */
    fun setAdmin(isAdmin: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_IS_ADMIN, isAdmin)
        }
    }

    /**
     * Проверка, является ли пользователь администратором
     */
    fun isAdmin(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_ADMIN, false)
    }

    /**
     * Установка темного/светлого режима
     */
    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_DARK_MODE, enabled)
        }

        val mode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Получение состояния темного режима
     */
    fun getDarkMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    /**
     * Включение/выключение уведомлений
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_NOTIFICATIONS, enabled)
        }
    }

    /**
     * Получение состояния уведомлений
     */
    fun getNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true)
    }

    /**
     * Включение/выключение сбора отчетов о сбоях
     */
    fun setCrashReportingEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_CRASH_REPORTING, enabled)
        }
    }

    /**
     * Получение состояния сбора отчетов о сбоях
     */
    fun getCrashReportingEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_CRASH_REPORTING, true)
    }

    /**
     * Сохранение ID пользователя
     */
    fun setUserId(userId: String) {
        sharedPreferences.edit {
            putString(KEY_USER_ID, userId)
        }
    }

    /**
     * Получение ID пользователя
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Сохранение ID корзины
     */
    fun setCartId(cartId: String) {
        sharedPreferences.edit {
            putString(KEY_CART_ID, cartId)
        }
    }

    /**
     * Получение ID корзины
     */
    fun getCartId(): String? {
        return sharedPreferences.getString(KEY_CART_ID, null)
    }

    /**
     * Сохранение FCM токена
     */
    fun setFcmToken(token: String) {
        sharedPreferences.edit {
            putString(KEY_FCM_TOKEN, token)
        }
    }

    /**
     * Получение FCM токена
     */
    fun getFcmToken(): String? {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Проверка первого запуска приложения
     */
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Установка флага первого запуска
     */
    fun setFirstLaunch(isFirst: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_FIRST_LAUNCH, isFirst)
        }
    }

    /**
     * Очистка настроек пользователя при выходе из аккаунта
     */
    fun clearUserPreferences() {
        sharedPreferences.edit {
            remove(KEY_IS_ADMIN)
            remove(KEY_USER_ID)
            remove(KEY_CART_ID)
            // Не очищаем настройки темы и уведомлений
        }
    }

    /**
     * Полная очистка всех настроек
     */
    fun clearAllPreferences() {
        sharedPreferences.edit {
            clear()
        }
    }
}