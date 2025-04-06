package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных категории товаров
 * @property id Уникальный идентификатор категории
 * @property name Название категории
 * @property description Описание категории
 * @property imageUrl URL изображения категории
 * @property parentId Идентификатор родительской категории (для подкатегорий)
 * @property productCount Количество товаров в категории
 * @property sortOrder Порядок сортировки категории
 * @property isActive Флаг, указывающий активна ли категория
 */
@Parcelize
data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val parentId: String = "",
    val productCount: Int = 0,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
) : Parcelable