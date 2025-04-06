package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных для элемента заказа (товара в заказе).
 * Содержит информацию о товаре, его количестве и цене в рамках конкретного заказа.
 */
@Parcelize
data class OrderItem(
    val id: String = "", // Уникальный идентификатор элемента заказа
    val productId: String = "", // Идентификатор товара
    val name: String = "", // Название товара
    val imageUrl: String = "", // URL изображения товара
    val price: Double = 0.0, // Цена товара на момент заказа (без скидки)
    val discountPrice: Double? = null, // Цена со скидкой на момент заказа (если была)
    val quantity: Int = 1, // Количество товара
    val subtotal: Double = 0.0, // Стоимость с учетом количества (price или discountPrice) * quantity

    // Дополнительная информация о товаре, которая может быть полезна для отображения в заказе
    val categoryId: String = "", // Идентификатор категории товара
    val categoryName: String = "", // Название категории
    val sku: String = "", // Артикул товара
    val attributes: Map<String, String> = mapOf() // Атрибуты товара (например, цвет, размер и т.д.)
) : Parcelable {

    /**
     * Вычисляет стоимость товарной позиции с учетом скидки и количества
     */
    fun calculateSubtotal(): Double {
        val effectivePrice = discountPrice ?: price
        return effectivePrice * quantity
    }

    /**
     * Проверяет, применена ли скидка к товару
     */
    fun hasDiscount(): Boolean {
        return discountPrice != null && discountPrice < price
    }

    /**
     * Вычисляет размер скидки в процентах
     */
    fun getDiscountPercent(): Int {
        return if (hasDiscount() && price > 0) {
            ((price - (discountPrice ?: price)) / price * 100).toInt()
        } else {
            0
        }
    }
}