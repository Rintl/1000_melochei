package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных для адреса.
 * Используется для хранения адресов доставки пользователей.
 */
@Parcelize
data class Address(
    val id: String = "", // Уникальный идентификатор адреса
    val title: String = "", // Название адреса (например, "Дом", "Работа")
    val address: String = "", // Полный адрес

    // Детальная информация об адресе
    val street: String = "", // Улица
    val house: String = "", // Дом
    val apartment: String? = null, // Квартира (может быть null для частных домов)
    val floor: Int? = null, // Этаж (может быть null)
    val entrance: String? = null, // Подъезд (может быть null)
    val city: String = "", // Город
    val region: String = "", // Область/регион
    val postalCode: String = "", // Почтовый индекс

    // Дополнительная информация
    val notes: String = "", // Заметки для курьера
    val coordinates: GeoPoint? = null, // Географические координаты

    // Флаг адреса по умолчанию
    val isDefault: Boolean = false, // Является ли адрес основным для пользователя

    // Зона доставки
    val deliveryZoneId: String? = null // Идентификатор зоны доставки, к которой относится адрес
) : Parcelable {

    /**
     * Возвращает короткую версию адреса (без деталей)
     */
    fun getShortAddress(): String {
        return if (street.isNotEmpty() && house.isNotEmpty()) {
            "$street, $house${apartment?.let { ", кв. $it" } ?: ""}"
        } else {
            address
        }
    }

    /**
     * Возвращает полную версию адреса со всеми деталями
     */
    fun getFullAddress(): String {
        val sb = StringBuilder()

        if (city.isNotEmpty()) sb.append("$city, ")
        if (street.isNotEmpty()) sb.append("$street, ")
        if (house.isNotEmpty()) sb.append("д. $house")

        apartment?.let { sb.append(", кв. $it") }
        entrance?.let { sb.append(", подъезд $it") }
        floor?.let { sb.append(", этаж $it") }

        if (postalCode.isNotEmpty()) sb.append(", $postalCode")

        return if (sb.isNotEmpty()) sb.toString() else address
    }
}

/**
 * Вспомогательный класс для хранения географических координат
 */
@Parcelize
data class GeoPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable