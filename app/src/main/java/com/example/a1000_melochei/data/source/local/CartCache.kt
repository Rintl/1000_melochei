package com.example.a1000_melochei.data.source.local

import android.content.Context
import android.util.Log
import com.example.a1000_melochei.data.model.Cart
import com.example.a1000_melochei.data.model.CartItem
import com.example.a1000_melochei.util.Constants
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * Класс для локального кэширования корзины покупок.
 * Обеспечивает сохранение корзины в файловой системе для оффлайн доступа.
 */
class CartCache(private val context: Context) {

    private val TAG = "CartCache"
    private val gson = Gson()
    private val cacheFile = File(context.filesDir, Constants.CART_CACHE_FILE)

    /**
     * Сохраняет корзину в локальный кэш
     */
    fun saveCart(cart: Cart) {
        try {
            val json = gson.toJson(cart)

            FileWriter(cacheFile).use { writer ->
                writer.write(json)
            }

            Log.d(TAG, "Корзина сохранена в кэш: ${cart.items.size} товаров")
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка сохранения корзины в кэш: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка при сохранении корзины: ${e.message}", e)
        }
    }

    /**
     * Загружает корзину из локального кэша
     */
    fun getCart(): Cart? {
        return try {
            if (!cacheFile.exists()) {
                Log.d(TAG, "Файл кэша корзины не существует")
                return null
            }

            val json = FileReader(cacheFile).use { reader ->
                reader.readText()
            }

            if (json.isBlank()) {
                Log.d(TAG, "Файл кэша корзины пустой")
                return null
            }

            val cart = gson.fromJson(json, Cart::class.java)
            Log.d(TAG, "Корзина загружена из кэша: ${cart?.items?.size ?: 0} товаров")

            cart
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка чтения корзины из кэша: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга корзины из кэша: ${e.message}", e)
            clearCache()
            null
        }
    }

    /**
     * Добавляет товар в кэшированную корзину
     */
    fun addItemToCart(cartItem: CartItem) {
        try {
            val currentCart = getCart() ?: Cart()
            val updatedCart = currentCart.withAddedItem(cartItem)
            saveCart(updatedCart)

            Log.d(TAG, "Товар добавлен в кэшированную корзину: ${cartItem.productName}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка добавления товара в кэшированную корзину: ${e.message}", e)
        }
    }

    /**
     * Удаляет товар из кэшированной корзины
     */
    fun removeItemFromCart(productId: String) {
        try {
            val currentCart = getCart() ?: return
            val updatedCart = currentCart.withRemovedItem(productId)
            saveCart(updatedCart)

            Log.d(TAG, "Товар удален из кэшированной корзины: $productId")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления товара из кэшированной корзины: ${e.message}", e)
        }
    }

    /**
     * Обновляет количество товара в кэшированной корзине
     */
    fun updateItemQuantity(productId: String, newQuantity: Int) {
        try {
            val currentCart = getCart() ?: return
            val updatedCart = currentCart.withUpdatedItemQuantity(productId, newQuantity)
            saveCart(updatedCart)

            Log.d(TAG, "Количество товара обновлено в кэшированной корзине: $productId -> $newQuantity")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления количества товара в кэшированной корзине: ${e.message}", e)
        }
    }

    /**
     * Очищает кэшированную корзину
     */
    fun clearCart() {
        try {
            val emptyCart = Cart()
            saveCart(emptyCart)

            Log.d(TAG, "Кэшированная корзина очищена")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка очистки кэшированной корзины: ${e.message}", e)
        }
    }

    /**
     * Проверяет, есть ли товар в кэшированной корзине
     */
    fun hasItem(productId: String): Boolean {
        return try {
            val cart = getCart()
            cart?.containsProduct(productId) == true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки наличия товара в кэше: ${e.message}", e)
            false
        }
    }

    /**
     * Получает количество определенного товара в кэшированной корзине
     */
    fun getItemQuantity(productId: String): Int {
        return try {
            val cart = getCart()
            cart?.getItemQuantity(productId) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения количества товара из кэша: ${e.message}", e)
            0
        }
    }

    /**
     * Получает общее количество товаров в кэшированной корзине
     */
    fun getTotalItemsCount(): Int {
        return try {
            val cart = getCart()
            cart?.getTotalItemsCount() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения общего количества товаров из кэша: ${e.message}", e)
            0
        }
    }

    /**
     * Получает общую сумму кэшированной корзины
     */
    fun getTotalAmount(): Double {
        return try {
            val cart = getCart()
            cart?.getTotalAmount() ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения общей суммы корзины из кэша: ${e.message}", e)
            0.0
        }
    }

    /**
     * Проверяет, пуста ли кэшированная корзина
     */
    fun isEmpty(): Boolean {
        return try {
            val cart = getCart()
            cart?.isEmpty() != false
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки пустоты корзины в кэше: ${e.message}", e)
            true
        }
    }

    /**
     * Получает список товаров из кэшированной корзины
     */
    fun getCartItems(): List<CartItem> {
        return try {
            val cart = getCart()
            cart?.items ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения списка товаров из кэша: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Синхронизирует кэш с данными из сервера
     */
    fun syncWithServerCart(serverCart: Cart) {
        try {
            val localCart = getCart()

            if (localCart == null) {
                // Если локального кэша нет, просто сохраняем серверную корзину
                saveCart(serverCart)
                Log.d(TAG, "Локальный кэш синхронизирован с сервером")
                return
            }

            // Сравниваем время последнего обновления
            val serverNewer = serverCart.updatedAt > localCart.updatedAt

            if (serverNewer) {
                // Серверная версия новее, обновляем локальный кэш
                saveCart(serverCart)
                Log.d(TAG, "Локальный кэш обновлен серверными данными")
            } else {
                // Локальная версия новее или равна, оставляем как есть
                Log.d(TAG, "Локальный кэш актуален, синхронизация не требуется")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации кэша с сервером: ${e.message}", e)
        }
    }

    /**
     * Создает резервную копию корзины
     */
    fun createBackup(): Boolean {
        return try {
            val cart = getCart()
            if (cart != null) {
                val backupFile = File(context.filesDir, "${Constants.CART_CACHE_FILE}.backup")
                val json = gson.toJson(cart)

                FileWriter(backupFile).use { writer ->
                    writer.write(json)
                }

                Log.d(TAG, "Резервная копия корзины создана")
                true
            } else {
                Log.d(TAG, "Нет данных для создания резервной копии")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания резервной копии корзины: ${e.message}", e)
            false
        }
    }

    /**
     * Восстанавливает корзину из резервной копии
     */
    fun restoreFromBackup(): Boolean {
        return try {
            val backupFile = File(context.filesDir, "${Constants.CART_CACHE_FILE}.backup")

            if (!backupFile.exists()) {
                Log.d(TAG, "Резервная копия корзины не найдена")
                return false
            }

            val json = FileReader(backupFile).use { reader ->
                reader.readText()
            }

            val cart = gson.fromJson(json, Cart::class.java)
            saveCart(cart)

            Log.d(TAG, "Корзина восстановлена из резервной копии")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка восстановления корзины из резервной копии: ${e.message}", e)
            false
        }
    }

    /**
     * Получает информацию о кэше
     */
    fun getCacheInfo(): Map<String, Any> {
        return try {
            val info = mutableMapOf<String, Any>()

            info["exists"] = cacheFile.exists()
            info["size"] = if (cacheFile.exists()) cacheFile.length() else 0L
            info["lastModified"] = if (cacheFile.exists()) cacheFile.lastModified() else 0L

            val cart = getCart()
            if (cart != null) {
                info["itemsCount"] = cart.items.size
                info["totalAmount"] = cart.getTotalAmount()
                info["updatedAt"] = cart.updatedAt
            } else {
                info["itemsCount"] = 0
                info["totalAmount"] = 0.0
                info["updatedAt"] = 0L
            }

            info
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения информации о кэше: ${e.message}", e)
            mapOf("error" to e.message)
        }
    }

    /**
     * Очищает кэш корзины
     */
    fun clearCache() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
                Log.d(TAG, "Кэш корзины очищен")
            }

            // Также удаляем резервную копию
            val backupFile = File(context.filesDir, "${Constants.CART_CACHE_FILE}.backup")
            if (backupFile.exists()) {
                backupFile.delete()
                Log.d(TAG, "Резервная копия корзины удалена")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка очистки кэша корзины: ${e.message}", e)
        }
    }

    /**
     * Проверяет целостность кэша
     */
    fun validateCache(): Boolean {
        return try {
            val cart = getCart()
            if (cart == null) {
                Log.d(TAG, "Кэш корзины не найден")
                return false
            }

            // Проверяем базовую структуру
            var valid = true

            cart.items.forEach { item ->
                if (item.productId.isBlank() ||
                    item.productName.isBlank() ||
                    item.price < 0 ||
                    item.quantity <= 0) {
                    valid = false
                    Log.w(TAG, "Найден некорректный товар в кэше: ${item.productId}")
                }
            }

            if (valid) {
                Log.d(TAG, "Кэш корзины прошел проверку целостности")
            } else {
                Log.w(TAG, "Кэш корзины содержит некорректные данные")
            }

            valid
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки целостности кэша: ${e.message}", e)
            false
        }
    }
}