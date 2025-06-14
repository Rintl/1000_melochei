package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных пользователя
 * @property id Уникальный идентификатор пользователя
 * @property email Email пользователя
 * @property name Имя пользователя
 * @property phone Номер телефона
 * @property isAdmin Флаг администратора
 * @property addresses Список адресов пользователя
 * @property avatarUrl URL аватара пользователя
 * @property isActive Активен ли аккаунт пользователя
 * @property fcmToken Токен для push-уведомлений
 * @property createdAt Время создания аккаунта
 * @property lastLoginAt Время последнего входа
 */
@Parcelize
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val isAdmin: Boolean = false,
    val addresses: List<Address> = emptyList(),
    val avatarUrl: String = "",
    val isActive: Boolean = true,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Возвращает основной адрес пользователя
     */
    fun getPrimaryAddress(): Address? {
        return addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    }

    /**
     * Проверяет, заполнен ли профиль пользователя
     */
    fun isProfileComplete(): Boolean {
        return name.isNotBlank() &&
                phone.isNotBlank() &&
                email.isNotBlank() &&
                addresses.isNotEmpty()
    }

    /**
     * Возвращает отображаемое имя пользователя
     */
    fun getDisplayName(): String {
        return when {
            name.isNotBlank() -> name
            email.isNotBlank() -> email.substringBefore("@")
            phone.isNotBlank() -> phone
            else -> "Пользователь"
        }
    }

    /**
     * Проверяет валидность данных пользователя
     */
    fun isValid(): Boolean {
        return email.isNotBlank() &&
                email.contains("@") &&
                name.isNotBlank() &&
                name.length >= 2
    }

    companion object {
        /**
         * Создает пустого пользователя
         */
        fun empty(): User {
            return User()
        }

        /**
         * Проверяет валидность email
         */
        fun isValidEmail(email: String): Boolean {
            return email.isNotBlank() &&
                    android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        /**
         * Проверяет валидность номера телефона
         */
        fun isValidPhone(phone: String): Boolean {
            val cleanPhone = phone.replace(Regex("[^+\\d]"), "")
            return cleanPhone.length >= 10 && cleanPhone.length <= 15
        }

        /**
         * Проверяет валидность имени
         */
        fun isValidName(name: String): Boolean {
            return name.isNotBlank() &&
                    name.trim().length >= 2 &&
                    name.trim().length <= 50
        }
    }
}