package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных пользователя
 * @property id Уникальный идентификатор пользователя
 * @property name Имя пользователя
 * @property email Email пользователя
 * @property phone Телефон пользователя
 * @property isAdmin Флаг, указывающий является ли пользователь администратором
 * @property avatarUrl URL изображения аватара пользователя
 * @property addresses Список адресов пользователя
 */
@Parcelize
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val isAdmin: Boolean = false,
    val avatarUrl: String = "",
    val addresses: List<Address> = emptyList()
) : Parcelable