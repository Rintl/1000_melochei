package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Модель данных товара
 * @property id Уникальный идентификатор товара
 * @property name Название товара
 * @property description Описание товара
 * @property price Цена товара
 * @property discountPrice Цена со скидкой (если есть)
 * @property categoryId Идентификатор категории товара
 * @property images Список URL изображений товара
 * @property availableQuantity Доступное количество товара
 * @property specifications Характеристики товара в виде пар ключ-значение
 * @property rating Рейтинг товара (от 0 до 5)
 * @property reviewCount Количество отзывов
 * @property reviews Список отзывов на товар
 * @property isFavorite Флаг, указывающий добавлен ли товар в избранное
 * @property createdAt Дата добавления товара
 * @property updatedAt Дата последнего обновления товара
 * @property soldCount Количество проданных единиц товара
 * @property sku Артикул товара
 * @property barcode Штрих-код товара
 * @property isActive Флаг, указывающий активен ли товар
 */
@Parcelize
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val discountPrice: Double? = null,
    val categoryId: String = "",
    val images: List<String> = emptyList(),
    val availableQuantity: Int = 0,
    val specifications: Map<String, String> = emptyMap(),
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val reviews: List<Review> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val soldCount: Int = 0,
    val sku: String = "",
    val barcode: String = "",
    val isActive: Boolean = true
) : Parcelable

/**
 * Модель данных отзыва на товар
 * @property id Уникальный идентификатор отзыва
 * @property productId Идентификатор товара
 * @property authorId Идентификатор автора отзыва
 * @property authorName Имя автора отзыва
 * @property rating Рейтинг (от 1 до 5)
 * @property text Текст отзыва
 * @property date Дата создания отзыва
 */
@Parcelize
data class Review(
    val id: String = "",
    val productId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val rating: Float = 0f,
    val text: String = "",
    val date: Long = System.currentTimeMillis()
) : Parcelable