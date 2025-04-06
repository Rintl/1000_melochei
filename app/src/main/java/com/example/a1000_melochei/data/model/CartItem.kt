package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных товара в корзине
 * @property id Уникальный идентификатор элемента корзины
 * @property productId Идентификатор товара
 * @property name Название товара
 * @property price Цена за единицу товара
 * @property discountPrice Цена со скидкой (если есть)
 * @property quantity Количество товара
 * @property imageUrl URL изображения товара
 * @property availableQuantity Доступное количество товара (для проверки лимитов)
 * @property addedAt Дата добавления товара в корзину
 */
@Parcelize
data class CartItem(
    val id: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val discountPrice: Double? = null,
    val quantity: Int = 1,
    val imageUrl: String = "",
    val availableQuantity: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable