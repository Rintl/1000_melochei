package com.yourstore.app.data.source.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourstore.app.data.model.Cart
import com.yourstore.app.data.model.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Класс для локального кэширования корзины пользователя.
 * Использует DataStore для хранения данных.
 */
class CartCache(private val context: Context) {

    companion object {
        // Правильное определение delegate для DataStore
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cart_cache")
        private val CART_ITEMS_KEY = stringPreferencesKey("cart_items")
        private val gson = Gson()
    }

    /**
     * Получение корзины из кэша
     * @return Объект корзины
     */
    fun getCart(): Cart {
        // Синхронное получение данных для внутреннего использования
        val cartItemsJson = context.getSharedPreferences("cart_cache", Context.MODE_PRIVATE)
            .getString(CART_ITEMS_KEY.name, "[]") ?: "[]"
        val type = object : TypeToken<List<CartItem>>() {}.type
        val items: List<CartItem> = gson.fromJson(cartItemsJson, type)

        return Cart(
            id = "",
            items = items,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Сохранение корзины в кэш
     * @param cart Объект корзины для сохранения
     */
    suspend fun saveCart(cart: Cart) {
        val cartItemsJson = gson.toJson(cart.items)
        context.dataStore.edit { preferences ->
            preferences[CART_ITEMS_KEY] = cartItemsJson
        }

        // Дублируем в SharedPreferences для синхронного доступа
        context.getSharedPreferences("cart_cache", Context.MODE_PRIVATE)
            .edit()
            .putString(CART_ITEMS_KEY.name, cartItemsJson)
            .apply()
    }

    /**
     * Получение элементов корзины из кэша
     * @return Flow со списком элементов корзины
     */
    fun getCartItems(): Flow<List<CartItem>> {
        return context.dataStore.data.map { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<List<CartItem>>() {}.type
            gson.fromJson(cartItemsJson, type)
        }
    }

    /**
     * Добавление элемента в корзину
     * @param cartItem Элемент корзины
     */
    suspend fun addCartItem(cartItem: CartItem) {
        context.dataStore.edit { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            val cartItems: MutableList<CartItem> = gson.fromJson(cartItemsJson, type)

            // Проверяем, есть ли уже такой товар в корзине
            val existingItemIndex = cartItems.indexOfFirst { it.productId == cartItem.productId }
            if (existingItemIndex != -1) {
                // Обновляем количество, если товар уже есть
                val existingItem = cartItems[existingItemIndex]
                val newQuantity = existingItem.quantity + cartItem.quantity
                val updatedItem = existingItem.copy(quantity = newQuantity)
                cartItems[existingItemIndex] = updatedItem
            } else {
                // Добавляем новый товар
                cartItems.add(cartItem)
            }

            preferences[CART_ITEMS_KEY] = gson.toJson(cartItems)

            // Дублируем в SharedPreferences для синхронного доступа
            context.getSharedPreferences("cart_cache", Context.MODE_PRIVATE)
                .edit()
                .putString(CART_ITEMS_KEY.name, gson.toJson(cartItems))
                .apply()
        }
    }

    /**
     * Обновление количества товара в корзине
     * @param cartItemId ID элемента корзины
     * @param quantity Новое количество
     */
    suspend fun updateCartItemQuantity(cartItemId: String, quantity: Int) {
        context.dataStore.edit { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            val cartItems: MutableList<CartItem> = gson.fromJson(cartItemsJson, type)

            val itemIndex = cartItems.indexOfFirst { it.id == cartItemId }
            if (itemIndex != -1) {
                val item = cartItems[itemIndex]
                cartItems[itemIndex] = item.copy(quantity = quantity)
            }

            preferences[CART_ITEMS_KEY] = gson.toJson(cartItems)

            // Дублируем в SharedPreferences для синхронного доступа
            context.getSharedPreferences("cart_cache", Context.MODE_PRIVATE)
                .edit()
                .putString(CART_ITEMS_KEY.name, gson.toJson(cartItems))
                .apply()
        }
    }

    /**
     * Удаление элемента из корзины
     * @param cartItemId ID элемента корзины
     */
    suspend fun removeCartItem(cartItemId: String) {
        context.dataStore.edit { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            val cartItems: MutableList<CartItem> = gson.fromJson(cartItemsJson, type)

            cartItems.removeIf { it.id == cartItemId }

            preferences[CART_ITEMS_KEY] = gson.toJson(cartItems)

            // Дублируем в SharedPreferences для синхронного доступа
            context.getSharedPreferences("cart_cache", Context.MODE_PRIVATE)
                .edit()
                .putString(CART_ITEMS_KEY.name, gson.toJson(cartItems))
                .apply()
        }
    }

    /**
     * Полная очистка корзины
     */
    suspend fun clearCart() {
        context.dataStore.edit { preferences ->
            preferences[CART_ITEMS_KEY] = "[]"

            // Дублируем в SharedPreferences для синхронного доступа
            context.getSharedPreferences("cart_cache", Context.MODE_PRIVATE)
                .edit()
                .putString(CART_ITEMS_KEY.name, "[]")
                .apply()
        }
    }

    /**
     * Получение общего количества товаров в корзине
     * @return Flow с количеством товаров
     */
    fun getCartItemCount(): Flow<Int> {
        return getCartItems().map { items ->
            items.sumOf { it.quantity }
        }
    }

    /**
     * Получение количества товаров как LiveData
     * @return LiveData с количеством товаров
     */
    fun getCartItemCountLiveData(): LiveData<Int> {
        return getCartItemCount().asLiveData()
    }

    /**
     * Проверка, есть ли товар в корзине
     * @param productId ID товара
     * @return Flow с результатом проверки (true - товар в корзине)
     */
    fun isProductInCart(productId: String): Flow<Boolean> {
        return getCartItems().map { items ->
            items.any { it.productId == productId }
        }
    }
}