package com.example.a1000_melochei.data.source.remote

import android.net.Uri
import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.*
import com.example.a1000_melochei.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Источник данных для работы с Firebase Firestore.
 * Обеспечивает прямое взаимодействие с базой данных Firestore.
 */
class FirestoreSource(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "FirestoreSource"
    private val storage = FirebaseStorage.getInstance()

    // CRUD операции для пользователей

    /**
     * Создает профиль пользователя в Firestore
     */
    suspend fun createUserProfile(user: User): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.id)
                .set(user)
                .await()

            Log.d(TAG, "Профиль пользователя создан: ${user.email}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания профиля пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка создания профиля")
        }
    }

    /**
     * Получает профиль пользователя по ID
     */
    suspend fun getUserProfile(userId: String): Resource<User> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    Log.d(TAG, "Профиль пользователя загружен: ${user.email}")
                    Resource.Success(user)
                } else {
                    Resource.Error("Ошибка преобразования данных пользователя")
                }
            } else {
                Resource.Error("Профиль пользователя не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки профиля пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки профиля")
        }
    }

    /**
     * Обновляет профиль пользователя
     */
    suspend fun updateUserProfile(user: User): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.id)
                .set(user)
                .await()

            Log.d(TAG, "Профиль пользователя обновлен: ${user.email}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления профиля пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка обновления профиля")
        }
    }

    /**
     * Удаляет профиль пользователя
     */
    suspend fun deleteUserProfile(userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .delete()
                .await()

            Log.d(TAG, "Профиль пользователя удален: $userId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления профиля пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка удаления профиля")
        }
    }

    /**
     * Загружает аватар пользователя
     */
    suspend fun uploadUserAvatar(userId: String, imageUri: Uri): Resource<String> {
        return try {
            val storageRef = storage.reference
                .child(Constants.STORAGE_USER_AVATARS_FOLDER)
                .child("$userId.jpg")

            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            Log.d(TAG, "Аватар пользователя загружен: $downloadUrl")
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки аватара пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки аватара")
        }
    }

    // CRUD операции для товаров

    /**
     * Получает все товары
     */
    suspend fun getProducts(): Resource<List<Product>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_PRODUCTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val products = querySnapshot.toObjects(Product::class.java)
            Log.d(TAG, "Загружено товаров: ${products.size}")
            Resource.Success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки товаров")
        }
    }

    /**
     * Получает товары по определенному полю
     */
    suspend fun getProductsByField(field: String, value: Any): Resource<List<Product>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo(field, value)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val products = querySnapshot.toObjects(Product::class.java)
            Log.d(TAG, "Загружено товаров по полю $field: ${products.size}")
            Resource.Success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки товаров по полю: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки товаров")
        }
    }

    /**
     * Получает товар по ID
     */
    suspend fun getProductById(productId: String): Resource<Product> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .await()

            if (document.exists()) {
                val product = document.toObject(Product::class.java)
                if (product != null) {
                    Log.d(TAG, "Товар загружен: ${product.name}")
                    Resource.Success(product)
                } else {
                    Resource.Error("Ошибка преобразования данных товара")
                }
            } else {
                Resource.Error("Товар не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки товара")
        }
    }

    /**
     * Добавляет новый товар
     */
    suspend fun addProduct(product: Product): Resource<String> {
        return try {
            val documentRef = firestore.collection(Constants.COLLECTION_PRODUCTS).document()
            val productWithId = product.copy(id = documentRef.id)

            documentRef.set(productWithId).await()

            Log.d(TAG, "Товар добавлен: ${product.name}")
            Resource.Success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка добавления товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка добавления товара")
        }
    }

    /**
     * Обновляет товар
     */
    suspend fun updateProduct(product: Product): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(product.id)
                .set(product)
                .await()

            Log.d(TAG, "Товар обновлен: ${product.name}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка обновления товара")
        }
    }

    /**
     * Удаляет товар
     */
    suspend fun deleteProduct(productId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .delete()
                .await()

            Log.d(TAG, "Товар удален: $productId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка удаления товара")
        }
    }

    /**
     * Поиск товаров по названию
     */
    suspend fun searchProducts(query: String): Resource<List<Product>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_PRODUCTS)
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .await()

            val products = querySnapshot.toObjects(Product::class.java)
            Log.d(TAG, "Найдено товаров по запросу '$query': ${products.size}")
            Resource.Success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка поиска товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка поиска товаров")
        }
    }

    /**
     * Получает поток товаров в реальном времени
     */
    fun getProductsFlow(): Flow<Resource<List<Product>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_PRODUCTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Ошибка получения товаров"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val products = snapshot.toObjects(Product::class.java)
                    trySend(Resource.Success(products))
                }
            }

        awaitClose { listener.remove() }
    }

    // CRUD операции для категорий

    /**
     * Получает все категории
     */
    suspend fun getCategories(): Resource<List<Category>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_CATEGORIES)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get()
                .await()

            val categories = querySnapshot.toObjects(Category::class.java)
            Log.d(TAG, "Загружено категорий: ${categories.size}")
            Resource.Success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки категорий")
        }
    }

    /**
     * Получает категории по определенному полю
     */
    suspend fun getCategoriesByField(field: String, value: Any): Resource<List<Category>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_CATEGORIES)
                .whereEqualTo(field, value)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get()
                .await()

            val categories = querySnapshot.toObjects(Category::class.java)
            Log.d(TAG, "Загружено категорий по полю $field: ${categories.size}")
            Resource.Success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки категорий по полю: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки категорий")
        }
    }

    /**
     * Получает категорию по ID
     */
    suspend fun getCategoryById(categoryId: String): Resource<Category> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_CATEGORIES)
                .document(categoryId)
                .get()
                .await()

            if (document.exists()) {
                val category = document.toObject(Category::class.java)
                if (category != null) {
                    Log.d(TAG, "Категория загружена: ${category.name}")
                    Resource.Success(category)
                } else {
                    Resource.Error("Ошибка преобразования данных категории")
                }
            } else {
                Resource.Error("Категория не найдена")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки категории: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки категории")
        }
    }

    /**
     * Добавляет новую категорию
     */
    suspend fun addCategory(category: Category): Resource<String> {
        return try {
            val documentRef = firestore.collection(Constants.COLLECTION_CATEGORIES).document()
            val categoryWithId = category.copy(id = documentRef.id)

            documentRef.set(categoryWithId).await()

            Log.d(TAG, "Категория добавлена: ${category.name}")
            Resource.Success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка добавления категории: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка добавления категории")
        }
    }

    /**
     * Обновляет категорию
     */
    suspend fun updateCategory(category: Category): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_CATEGORIES)
                .document(category.id)
                .set(category)
                .await()

            Log.d(TAG, "Категория обновлена: ${category.name}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления категории: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка обновления категории")
        }
    }

    /**
     * Удаляет категорию
     */
    suspend fun deleteCategory(categoryId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_CATEGORIES)
                .document(categoryId)
                .delete()
                .await()

            Log.d(TAG, "Категория удалена: $categoryId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления категории: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка удаления категории")
        }
    }

    /**
     * Поиск категорий по названию
     */
    suspend fun searchCategories(query: String): Resource<List<Category>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_CATEGORIES)
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .await()

            val categories = querySnapshot.toObjects(Category::class.java)
            Log.d(TAG, "Найдено категорий по запросу '$query': ${categories.size}")
            Resource.Success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка поиска категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка поиска категорий")
        }
    }

    /**
     * Обновляет счетчик товаров в категории
     */
    suspend fun updateCategoryProductCount(categoryId: String, change: Int) {
        try {
            firestore.runTransaction { transaction ->
                val categoryRef = firestore.collection(Constants.COLLECTION_CATEGORIES).document(categoryId)
                val categorySnapshot = transaction.get(categoryRef)

                if (categorySnapshot.exists()) {
                    val currentCount = categorySnapshot.getLong("productCount")?.toInt() ?: 0
                    val newCount = (currentCount + change).coerceAtLeast(0)
                    transaction.update(categoryRef, "productCount", newCount)
                }
            }.await()

            Log.d(TAG, "Счетчик товаров в категории $categoryId изменен на $change")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления счетчика товаров в категории: ${e.message}", e)
        }
    }

    /**
     * Получает поток категорий в реальном времени
     */
    fun getCategoriesFlow(): Flow<Resource<List<Category>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CATEGORIES)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Ошибка получения категорий"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val categories = snapshot.toObjects(Category::class.java)
                    trySend(Resource.Success(categories))
                }
            }

        awaitClose { listener.remove() }
    }

    // CRUD операции для заказов

    /**
     * Получает все заказы
     */
    suspend fun getOrders(): Resource<List<Order>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_ORDERS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = querySnapshot.toObjects(Order::class.java)
            Log.d(TAG, "Загружено заказов: ${orders.size}")
            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки заказов: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки заказов")
        }
    }

    /**
     * Получает заказы по определенному полю
     */
    suspend fun getOrdersByField(field: String, value: Any): Resource<List<Order>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo(field, value)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = querySnapshot.toObjects(Order::class.java)
            Log.d(TAG, "Загружено заказов по полю $field: ${orders.size}")
            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки заказов по полю: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки заказов")
        }
    }

    /**
     * Получает заказ по ID
     */
    suspend fun getOrderById(orderId: String): Resource<Order> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .get()
                .await()

            if (document.exists()) {
                val order = document.toObject(Order::class.java)
                if (order != null) {
                    Log.d(TAG, "Заказ загружен: ${order.orderNumber}")
                    Resource.Success(order)
                } else {
                    Resource.Error("Ошибка преобразования данных заказа")
                }
            } else {
                Resource.Error("Заказ не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки заказа: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки заказа")
        }
    }

    /**
     * Добавляет новый заказ
     */
    suspend fun addOrder(order: Order): Resource<String> {
        return try {
            val documentRef = firestore.collection(Constants.COLLECTION_ORDERS).document()
            val orderWithId = order.copy(id = documentRef.id)

            documentRef.set(orderWithId).await()

            Log.d(TAG, "Заказ добавлен: ${order.orderNumber}")
            Resource.Success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка добавления заказа: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка добавления заказа")
        }
    }

    /**
     * Обновляет заказ
     */
    suspend fun updateOrder(order: Order): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_ORDERS)
                .document(order.id)
                .set(order)
                .await()

            Log.d(TAG, "Заказ обновлен: ${order.orderNumber}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления заказа: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка обновления заказа")
        }
    }

    /**
     * Получает последние заказы
     */
    suspend fun getRecentOrders(limit: Int): Resource<List<Order>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_ORDERS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val orders = querySnapshot.toObjects(Order::class.java)
            Log.d(TAG, "Загружено последних заказов: ${orders.size}")
            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки последних заказов: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки последних заказов")
        }
    }

    /**
     * Получает заказы за период
     */
    suspend fun getOrdersByDateRange(startDate: Long, endDate: Long): Resource<List<Order>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_ORDERS)
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = querySnapshot.toObjects(Order::class.java)
            Log.d(TAG, "Загружено заказов за период: ${orders.size}")
            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки заказов за период: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки заказов за период")
        }
    }

    /**
     * Поиск заказов
     */
    suspend fun searchOrders(query: String): Resource<List<Order>> {
        return try {
            // Поиск по номеру заказа
            val orderNumberQuery = firestore.collection(Constants.COLLECTION_ORDERS)
                .whereGreaterThanOrEqualTo("orderNumber", query)
                .whereLessThanOrEqualTo("orderNumber", query + "\uf8ff")
                .get()
                .await()

            // Поиск по имени клиента
            val customerNameQuery = firestore.collection(Constants.COLLECTION_ORDERS)
                .whereGreaterThanOrEqualTo("customerInfo.name", query)
                .whereLessThanOrEqualTo("customerInfo.name", query + "\uf8ff")
                .get()
                .await()

            val ordersByNumber = orderNumberQuery.toObjects(Order::class.java)
            val ordersByCustomer = customerNameQuery.toObjects(Order::class.java)

            // Объединяем результаты и убираем дубликаты
            val allOrders = (ordersByNumber + ordersByCustomer).distinctBy { it.id }

            Log.d(TAG, "Найдено заказов по запросу '$query': ${allOrders.size}")
            Resource.Success(allOrders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка поиска заказов: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка поиска заказов")
        }
    }

    /**
     * Получает поток заказов в реальном времени
     */
    fun getOrdersFlow(): Flow<Resource<List<Order>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_ORDERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Ошибка получения заказов"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.toObjects(Order::class.java)
                    trySend(Resource.Success(orders))
                }
            }

        awaitClose { listener.remove() }
    }

    // CRUD операции для корзины

    /**
     * Получает корзину пользователя
     */
    suspend fun getCart(userId: String): Resource<Cart> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_CARTS)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val cart = document.toObject(Cart::class.java)
                if (cart != null) {
                    Log.d(TAG, "Корзина загружена для пользователя: $userId")
                    Resource.Success(cart)
                } else {
                    Resource.Error("Ошибка преобразования данных корзины")
                }
            } else {
                // Создаем пустую корзину
                val emptyCart = Cart(id = userId)
                Resource.Success(emptyCart)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки корзины")
        }
    }

    /**
     * Получает поток корзины в реальном времени
     */
    fun getCartFlow(userId: String): Flow<Resource<Cart>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CARTS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Ошибка получения корзины"))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val cart = snapshot.toObject(Cart::class.java)
                    if (cart != null) {
                        trySend(Resource.Success(cart))
                    } else {
                        trySend(Resource.Success(Cart(id = userId)))
                    }
                } else {
                    trySend(Resource.Success(Cart(id = userId)))
                }
            }

        awaitClose { listener.remove() }
    }
}