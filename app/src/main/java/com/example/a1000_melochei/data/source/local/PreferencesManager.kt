package com.example.a1000_melochei.data.source.local

import android.content.Context
import android.content.SharedPreferences
import com.example.a1000_melochei.util.Constants

/**
 * Менеджер для работы с SharedPreferences.
 * Обеспечивает централизованное управление настройками приложения.
 */
class PreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    // Данные пользователя

    /**
     * Сохраняет ID пользователя
     */
    fun saveUserId(userId: String) {
        editor.putString(Constants.PREF_USER_ID, userId).apply()
    }

    /**
     * Получает ID пользователя
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(Constants.PREF_USER_ID, null)
    }

    /**
     * Сохраняет имя пользователя
     */
    fun saveUserName(name: String) {
        editor.putString(Constants.PREF_USER_NAME, name).apply()
    }

    /**
     * Получает имя пользователя
     */
    fun getUserName(): String? {
        return sharedPreferences.getString(Constants.PREF_USER_NAME, null)
    }

    /**
     * Сохраняет email пользователя
     */
    fun saveUserEmail(email: String) {
        editor.putString(Constants.PREF_USER_EMAIL, email).apply()
    }

    /**
     * Получает email пользователя
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(Constants.PREF_USER_EMAIL, null)
    }

    /**
     * Сохраняет статус администратора
     */
    fun saveIsAdmin(isAdmin: Boolean) {
        editor.putBoolean(Constants.PREF_IS_ADMIN, isAdmin).apply()
    }

    /**
     * Проверяет, является ли пользователь администратором
     */
    fun isAdmin(): Boolean {
        return sharedPreferences.getBoolean(Constants.PREF_IS_ADMIN, false)
    }

    // Настройки приложения

    /**
     * Сохраняет настройку темного режима
     */
    fun saveDarkMode(isDarkMode: Boolean) {
        editor.putBoolean(Constants.PREF_DARK_MODE, isDarkMode).apply()
    }

    /**
     * Проверяет, включен ли темный режим
     */
    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean(Constants.PREF_DARK_MODE, false)
    }

    /**
     * Сохраняет настройку уведомлений
     */
    fun saveNotificationsEnabled(enabled: Boolean) {
        editor.putBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    /**
     * Проверяет, включены ли уведомления
     */
    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, true)
    }

    /**
     * Сохраняет настройку отчетов о сбоях
     */
    fun saveCrashReportingEnabled(enabled: Boolean) {
        editor.putBoolean(Constants.PREF_CRASH_REPORTING_ENABLED, enabled).apply()
    }

    /**
     * Проверяет, включены ли отчеты о сбоях
     */
    fun isCrashReportingEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.PREF_CRASH_REPORTING_ENABLED, true)
    }

    // FCM токен

    /**
     * Сохраняет FCM токен
     */
    fun saveFcmToken(token: String) {
        editor.putString(Constants.PREF_FCM_TOKEN, token).apply()
    }

    /**
     * Получает FCM токен
     */
    fun getFcmToken(): String? {
        return sharedPreferences.getString(Constants.PREF_FCM_TOKEN, null)
    }

    // Синхронизация данных

    /**
     * Сохраняет время последней синхронизации
     */
    fun saveLastSyncTime(timestamp: Long) {
        editor.putLong(Constants.PREF_LAST_SYNC_TIME, timestamp).apply()
    }

    /**
     * Получает время последней синхронизации
     */
    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(Constants.PREF_LAST_SYNC_TIME, 0)
    }

    // Первый запуск приложения

    /**
     * Проверяет, является ли это первым запуском приложения
     */
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("is_first_launch", true)
    }

    /**
     * Отмечает, что приложение уже запускалось
     */
    fun setFirstLaunchCompleted() {
        editor.putBoolean("is_first_launch", false).apply()
    }

    // Настройки интерфейса

    /**
     * Сохраняет выбранный язык
     */
    fun saveSelectedLanguage(language: String) {
        editor.putString("selected_language", language).apply()
    }

    /**
     * Получает выбранный язык
     */
    fun getSelectedLanguage(): String {
        return sharedPreferences.getString("selected_language", Constants.DEFAULT_LANGUAGE) ?: Constants.DEFAULT_LANGUAGE
    }

    /**
     * Сохраняет размер шрифта
     */
    fun saveFontSize(size: String) {
        editor.putString("font_size", size).apply()
    }

    /**
     * Получает размер шрифта
     */
    fun getFontSize(): String {
        return sharedPreferences.getString("font_size", "medium") ?: "medium"
    }

    // Настройки корзины

    /**
     * Сохраняет время последнего обновления корзины
     */
    fun saveCartLastUpdated(timestamp: Long) {
        editor.putLong("cart_last_updated", timestamp).apply()
    }

    /**
     * Получает время последнего обновления корзины
     */
    fun getCartLastUpdated(): Long {
        return sharedPreferences.getLong("cart_last_updated", 0)
    }

    /**
     * Сохраняет количество товаров в корзине
     */
    fun saveCartItemsCount(count: Int) {
        editor.putInt("cart_items_count", count).apply()
    }

    /**
     * Получает количество товаров в корзине
     */
    fun getCartItemsCount(): Int {
        return sharedPreferences.getInt("cart_items_count", 0)
    }

    // Настройки каталога

    /**
     * Сохраняет режим отображения товаров (список/сетка)
     */
    fun saveProductsViewMode(mode: String) {
        editor.putString("products_view_mode", mode).apply()
    }

    /**
     * Получает режим отображения товаров
     */
    fun getProductsViewMode(): String {
        return sharedPreferences.getString("products_view_mode", "grid") ?: "grid"
    }

    /**
     * Сохраняет выбранную сортировку товаров
     */
    fun saveProductsSortOrder(sortOrder: String) {
        editor.putString("products_sort_order", sortOrder).apply()
    }

    /**
     * Получает выбранную сортировку товаров
     */
    fun getProductsSortOrder(): String {
        return sharedPreferences.getString("products_sort_order", "name_asc") ?: "name_asc"
    }

    // Настройки поиска

    /**
     * Сохраняет последний поисковый запрос
     */
    fun saveLastSearchQuery(query: String) {
        editor.putString("last_search_query", query).apply()
    }

    /**
     * Получает последний поисковый запрос
     */
    fun getLastSearchQuery(): String? {
        return sharedPreferences.getString("last_search_query", null)
    }

    /**
     * Сохраняет историю поиска
     */
    fun saveSearchHistory(history: Set<String>) {
        editor.putStringSet("search_history", history).apply()
    }

    /**
     * Получает историю поиска
     */
    fun getSearchHistory(): Set<String> {
        return sharedPreferences.getStringSet("search_history", emptySet()) ?: emptySet()
    }

    /**
     * Добавляет запрос в историю поиска
     */
    fun addToSearchHistory(query: String) {
        val history = getSearchHistory().toMutableSet()
        history.add(query)

        // Ограничиваем размер истории
        if (history.size > 10) {
            val sortedHistory = history.toList().sorted()
            history.clear()
            history.addAll(sortedHistory.takeLast(10))
        }

        saveSearchHistory(history)
    }

    /**
     * Очищает историю поиска
     */
    fun clearSearchHistory() {
        editor.remove("search_history").apply()
    }

    // Настройки адреса доставки

    /**
     * Сохраняет ID последнего выбранного адреса доставки
     */
    fun saveLastDeliveryAddressId(addressId: String) {
        editor.putString("last_delivery_address_id", addressId).apply()
    }

    /**
     * Получает ID последнего выбранного адреса доставки
     */
    fun getLastDeliveryAddressId(): String? {
        return sharedPreferences.getString("last_delivery_address_id", null)
    }

    /**
     * Сохраняет предпочтительный способ доставки
     */
    fun savePreferredDeliveryMethod(method: String) {
        editor.putString("preferred_delivery_method", method).apply()
    }

    /**
     * Получает предпочтительный способ доставки
     */
    fun getPreferredDeliveryMethod(): String {
        return sharedPreferences.getString("preferred_delivery_method", "delivery") ?: "delivery"
    }

    // Настройки аналитики

    /**
     * Сохраняет время последнего просмотра аналитики
     */
    fun saveLastAnalyticsView(timestamp: Long) {
        editor.putLong("last_analytics_view", timestamp).apply()
    }

    /**
     * Получает время последнего просмотра аналитики
     */
    fun getLastAnalyticsView(): Long {
        return sharedPreferences.getLong("last_analytics_view", 0)
    }

    /**
     * Сохраняет выбранный период для аналитики
     */
    fun saveAnalyticsPeriod(period: String) {
        editor.putString("analytics_period", period).apply()
    }

    /**
     * Получает выбранный период для аналитики
     */
    fun getAnalyticsPeriod(): String {
        return sharedPreferences.getString("analytics_period", "last_30_days") ?: "last_30_days"
    }

    // Общие методы

    /**
     * Сохраняет строковое значение
     */
    fun saveString(key: String, value: String) {
        editor.putString(key, value).apply()
    }

    /**
     * Получает строковое значение
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    /**
     * Сохраняет целочисленное значение
     */
    fun saveInt(key: String, value: Int) {
        editor.putInt(key, value).apply()
    }

    /**
     * Получает целочисленное значение
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    /**
     * Сохраняет булево значение
     */
    fun saveBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    /**
     * Получает булево значение
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Сохраняет длинное целое значение
     */
    fun saveLong(key: String, value: Long) {
        editor.putLong(key, value).apply()
    }

    /**
     * Получает длинное целое значение
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    /**
     * Сохраняет вещественное значение
     */
    fun saveFloat(key: String, value: Float) {
        editor.putFloat(key, value).apply()
    }

    /**
     * Получает вещественное значение
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    /**
     * Проверяет, содержит ли SharedPreferences указанный ключ
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    /**
     * Удаляет значение по ключу
     */
    fun remove(key: String) {
        editor.remove(key).apply()
    }

    /**
     * Очищает все данные пользователя
     */
    fun clearUserData() {
        editor.remove(Constants.PREF_USER_ID)
            .remove(Constants.PREF_USER_NAME)
            .remove(Constants.PREF_USER_EMAIL)
            .remove(Constants.PREF_IS_ADMIN)
            .remove(Constants.PREF_FCM_TOKEN)
            .remove(Constants.PREF_LAST_SYNC_TIME)
            .remove("cart_last_updated")
            .remove("cart_items_count")
            .remove("last_delivery_address_id")
            .apply()
    }

    /**
     * Очищает все настройки
     */
    fun clearAllPreferences() {
        editor.clear().apply()
    }

    /**
     * Экспортирует все настройки в Map
     */
    fun exportPreferences(): Map<String, *> {
        return sharedPreferences.all
    }

    /**
     * Получает размер всех сохраненных данных
     */
    fun getPreferencesSize(): Int {
        return sharedPreferences.all.size
    }
}