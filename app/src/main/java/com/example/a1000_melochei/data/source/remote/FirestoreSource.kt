package com.yourstore.app.data.source.remote

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.yourstore.app.data.common.Resource
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

/**
 * Источник данных для работы с Firebase Firestore.
 * Предоставляет методы для чтения и записи данных в облачную NoSQL базу данных.
 */
class FirestoreSource(private val firestore: FirebaseFirestore) {

    companion object {
        // Коллекции Firestore
        const val USERS_COLLECTION = "users"
        const val PRODUCTS_COLLECTION = "products"
        const val CATEGORIES_COLLECTION = "categories"
        const val ORDERS_COLLECTION = "orders"
        const val CARTS_COLLECTION = "carts"
        const val DELIVERY_ZONES_COLLECTION = "delivery_zones"
        const val PROMOTIONS_COLLECTION = "promotions"
        const val SETTINGS_COLLECTION = "settings"
    }

    // ==================== Общие методы ====================

    /**
     * Получение документа по ID
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @return Resource с DocumentSnapshot
     */
    suspend fun getDocument(collectionPath: String, documentId: String): Resource<com.google.firebase.firestore.DocumentSnapshot> {
        return try {
            val snapshot = firestore.collection(collectionPath).document(documentId).get().await()
            if (snapshot.exists()) {
                Resource.Success(snapshot)
            } else {
                Resource.Error("Документ не найден")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка получения документа: ${e.message}")
        }
    }

    /**
     * Получение всех документов из коллекции
     * @param collectionPath Путь к коллекции
     * @return Resource с QuerySnapshot
     */
    suspend fun getCollection(collectionPath: String): Resource<QuerySnapshot> {
        return try {
            val snapshot = firestore.collection(collectionPath).get().await()
            Resource.Success(snapshot)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка получения коллекции: ${e.message}")
        }
    }

    /**
     * Добавление документа в коллекцию с генерацией ID
     * @param collectionPath Путь к коллекции
     * @param data Данные для добавления
     * @return Resource с ID нового документа
     */
    suspend fun addDocument(collectionPath: String, data: Any): Resource<String> {
        return try {
            val documentReference = firestore.collection(collectionPath).add(data).await()
            Resource.Success(documentReference.id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка добавления документа: ${e.message}")
        }
    }

    /**
     * Установка документа с заданным ID
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @param data Данные для добавления
     * @return Resource с результатом операции
     */
    suspend fun setDocument(collectionPath: String, documentId: String, data: Any): Resource<Unit> {
        return try {
            firestore.collection(collectionPath).document(documentId).set(data).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка установки документа: ${e.message}")
        }
    }

    /**
     * Обновление полей документа
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @param updates Карта обновлений полей
     * @return Resource с результатом операции
     */
    suspend fun updateDocument(collectionPath: String, documentId: String, updates: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(collectionPath).document(documentId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка обновления документа: ${e.message}")
        }
    }

    /**
     * Удаление документа
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @return Resource с результатом операции
     */
    suspend fun deleteDocument(collectionPath: String, documentId: String): Resource<Unit> {
        return try {
            firestore.collection(collectionPath).document(documentId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка удаления документа: ${e.message}")
        }
    }

    /**
     * Получение документов по запросу
     * @param query Запрос Firestore
     * @return Resource с QuerySnapshot
     */
    suspend fun getDocumentsByQuery(query: Query): Resource<QuerySnapshot> {
        return try {
            val snapshot = query.get().await()
            Resource.Success(snapshot)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка выполнения запроса: ${e.message}")
        }
    }

    // ==================== Методы для работы с пользователями ====================

    /**
     * Получение пользователя по ID
     * @param userId ID пользователя
     * @return Resource с DocumentSnapshot
     */
    suspend fun getUser(userId: String): Resource<com.google.firebase.firestore.DocumentSnapshot> {
        return getDocument(USERS_COLLECTION, userId)
    }

    /**
     * Создание/обновление профиля пользователя
     * @param userId ID пользователя
     * @param userData Данные пользователя
     * @return Resource с результатом операции
     */
    suspend fun setUserProfile(userId: String, userData: Any): Resource<Unit> {
        return setDocument(USERS_COLLECTION, userId, userData)
    }

    /**
     * Обновление профиля пользователя
     * @param userId ID пользователя
     * @param updates Обновляемые поля
     * @return Resource с результатом операции
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any?>): Resource<Unit> {
        return updateDocument(USERS_COLLECTION, userId, updates)
    }

    // ==================== Методы для работы с товарами ====================

    /**
     * Получение товара по ID
     * @param productId ID товара
     * @return Resource с DocumentSnapshot
     */
    suspend fun getProduct(productId: String): Resource<com.google.firebase.firestore.DocumentSnapshot> {
        return getDocument(PRODUCTS_COLLECTION, productId)
    }

    /**
     * Получение всех товаров
     * @return Resource с QuerySnapshot
     */
    suspend fun getAllProducts(): Resource<QuerySnapshot> {
        return getCollection(PRODUCTS_COLLECTION)
    }

    /**
     * Получение товаров по категории
     * @param categoryId ID категории
     * @return Resource с QuerySnapshot
     */
    suspend fun getProductsByCategory(categoryId: String): Resource<QuerySnapshot> {
        val query = firestore.collection(PRODUCTS_COLLECTION)
            .whereEqualTo("categoryId", categoryId)
        return getDocumentsByQuery(query)
    }

    /**
     * Получение популярных товаров
     * @param limit Максимальное количество товаров
     * @return Resource с QuerySnapshot
     */
    suspend fun getPopularProducts(limit: Int): Resource<QuerySnapshot> {
        val query = firestore.collection(PRODUCTS_COLLECTION)
            .orderBy("rating", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        return getDocumentsByQuery(query)
    }

    /**
     * Добавление нового товара
     * @param productData Данные товара
     * @return Resource с ID нового товара
     */
    suspend fun addProduct(productData: Any): Resource<String> {
        return addDocument(PRODUCTS_COLLECTION, productData)
    }

    /**
     * Обновление товара
     * @param productId ID товара
     * @param updates Обновляемые поля
     * @return Resource с результатом операции
     */
    suspend fun updateProduct(productId: String, updates: Map<String, Any?>): Resource<Unit> {
        return updateDocument(PRODUCTS_COLLECTION, productId, updates)
    }

    /**
     * Удаление товара
     * @param productId ID товара
     * @return Resource с результатом операции
     */
    suspend fun deleteProduct(productId: String): Resource<Unit> {
        return deleteDocument(PRODUCTS_COLLECTION, productId)
    }

    // ==================== Методы для работы с категориями ====================

    /**
     * Получение категории по ID
     * @param categoryId ID категории
     * @return Resource с DocumentSnapshot
     */
    suspend fun getCategory(categoryId: String): Resource<com.google.firebase.firestore.DocumentSnapshot> {
        return getDocument(CATEGORIES_COLLECTION, categoryId)
    }

    /**
     * Получение всех категорий
     * @return Resource с QuerySnapshot
     */
    suspend fun getAllCategories(): Resource<QuerySnapshot> {
        return getCollection(CATEGORIES_COLLECTION)
    }

    /**
     * Добавление новой категории
     * @param categoryData Данные категории
     * @return Resource с ID новой категории
     */
    suspend fun addCategory(categoryData: Any): Resource<String> {
        return addDocument(CATEGORIES_COLLECTION, categoryData)
    }

    /**
     * Обновление категории
     * @param categoryId ID категории
     * @param updates Обновляемые поля
     * @return Resource с результатом операции
     */
    suspend fun updateCategory(categoryId: String, updates: Map<String, Any?>): Resource<Unit> {
        return updateDocument(CATEGORIES_COLLECTION, categoryId, updates)
    }

    /**
     * Удаление категории
     * @param categoryId ID категории
     * @return Resource с результатом операции
     */
    suspend fun deleteCategory(categoryId: String): Resource<Unit> {
        return deleteDocument(CATEGORIES_COLLECTION, categoryId)
    }

    // ==================== Методы для работы с заказами ====================

    /**
     * Получение заказа по ID
     * @param orderId ID заказа
     * @return Resource с DocumentSnapshot
     */
    suspend fun getOrder(orderId: String): Resource<com.google.firebase.firestore.DocumentSnapshot> {
        return getDocument(ORDERS_COLLECTION, orderId)
    }

    /**
     * Получение заказов пользователя
     * @param userId ID пользователя
     * @return Resource с QuerySnapshot
     */
    suspend fun getUserOrders(userId: String): Resource<QuerySnapshot> {
        val query = firestore.collection(ORDERS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        return getDocumentsByQuery(query)
    }

    /**
     * Получение всех заказов
     * @return Resource с QuerySnapshot
     */
    suspend fun getAllOrders(): Resource<QuerySnapshot> {
        val query = firestore.collection(ORDERS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        return getDocumentsByQuery(query)
    }

    /**
     * Получение последних заказов
     * @param limit Количество заказов
     * @return Resource с QuerySnapshot
     */
    suspend fun getLatestOrders(limit: Int): Resource<QuerySnapshot> {
        val query = firestore.collection(ORDERS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        return getDocumentsByQuery(query)
    }

    /**
     * Добавление нового заказа
     * @param orderData Данные заказа
     * @return Resource с ID нового заказа
     */
    suspend fun addOrder(orderData: Any): Resource<String> {
        return addDocument(ORDERS_COLLECTION, orderData)
    }

    /**
     * Обновление статуса заказа
     * @param orderId ID заказа
     * @param status Новый статус
     * @return Resource с результатом операции
     */
    suspend fun updateOrderStatus(orderId: String, status: String): Resource<Unit> {
        return updateDocument(ORDERS_COLLECTION, orderId, mapOf("status" to status))
    }

    // ==================== Методы для работы с корзиной ====================

    /**
     * Получение корзины по ID
     * @param cartId ID корзины
     * @return Resource с DocumentSnapshot
     */
    suspend fun getCart(cartId: String): Resource<com.google.firebase.firestore.DocumentSnapshot> {
        return getDocument(CARTS_COLLECTION, cartId)
    }

    /**
     * Получение корзины пользователя
     * @param userId ID пользователя
     * @return Resource с QuerySnapshot
     */
    suspend fun getUserCart(userId: String): Resource<QuerySnapshot> {
        val query = firestore.collection(CARTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("active", true)
            .limit(1)
        return getDocumentsByQuery(query)
    }

    /**
     * Создание/обновление корзины
     * @param cartId ID корзины
     * @param cartData Данные корзины
     * @return Resource с результатом операции
     */
    suspend fun setCart(cartId: String, cartData: Any): Resource<Unit> {
        return setDocument(CARTS_COLLECTION, cartId, cartData)
    }

    /**
     * Обновление корзины
     * @param cartId ID корзины
     * @param updates Обновляемые поля
     * @return Resource с результатом операции
     */
    suspend fun updateCart(cartId: String, updates: Map<String, Any?>): Resource<Unit> {
        return updateDocument(CARTS_COLLECTION, cartId, updates)
    }

    /**
     * Создание новой корзины
     * @param cartData Данные корзины
     * @return Resource с ID новой корзины
     */
    suspend fun createCart(cartData: Any): Resource<String> {
        return addDocument(CARTS_COLLECTION, cartData)
    }

    // ==================== Методы для работы с промо-акциями ====================

    /**
     * Получение всех активных промо-акций
     * @return Resource с QuerySnapshot
     */
    suspend fun getActivePromotions(): Resource<QuerySnapshot> {
        val currentTime = System.currentTimeMillis()
        val query = firestore.collection(PROMOTIONS_COLLECTION)
            .whereGreaterThan("endDate", currentTime)
            .whereLessThan("startDate", currentTime)
        return getDocumentsByQuery(query)
    }

    // ==================== Методы для работы с зонами доставки ====================

    /**
     * Получение всех зон доставки
     * @return Resource с QuerySnapshot
     */
    suspend fun getDeliveryZones(): Resource<QuerySnapshot> {
        return getCollection(DELIVERY_ZONES_COLLECTION)
    }

    // ==================== Методы транзакций ====================

    /**
     * Выполнение транзакции Firestore
     * @param transaction Функция транзакции
     * @return Resource с результатом транзакции типа T
     */
    suspend fun <T> runTransaction(transaction: suspend (com.google.firebase.firestore.Transaction) -> T): Resource<T> {
        return try {
            val result = firestore.runTransaction { transaction ->
                transaction(transaction)
            }.await()
            Resource.Success(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка транзакции: ${e.message}")
        }
    }

    /**
     * Получение ссылки на документ
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @return Ссылка на документ
     */
    fun getDocumentReference(collectionPath: String, documentId: String): DocumentReference {
        return firestore.collection(collectionPath).document(documentId)
    }
}