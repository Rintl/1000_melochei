package com.yourstore.app.data.source.remote

import com.google.firebase.auth.FirebaseAuth
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

    /**
     * Получение ID текущего пользователя
     * @return ID пользователя или null, если пользователь не авторизован
     */
    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    // ==================== Общие методы ====================

    /**
     * Получение документа по ID
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @return DocumentSnapshot
     */
    suspend fun getDocument(collectionPath: String, documentId: String): com.google.firebase.firestore.DocumentSnapshot {
        return firestore.collection(collectionPath).document(documentId).get().await()
    }

    /**
     * Получение всех документов из коллекции
     * @param collectionPath Путь к коллекции
     * @return QuerySnapshot
     */
    suspend fun getCollection(collectionPath: String): QuerySnapshot {
        return firestore.collection(collectionPath).get().await()
    }

    /**
     * Получение коллекции с фильтром
     * @param collectionPath Путь к коллекции
     * @param field Поле для фильтрации
     * @param value Значение для фильтрации
     * @return QuerySnapshot
     */
    suspend fun getCollectionWithFilter(
        collectionPath: String,
        field: String,
        value: Any
    ): QuerySnapshot {
        return firestore.collection(collectionPath)
            .whereEqualTo(field, value)
            .get()
            .await()
    }

    /**
     * Получение коллекции с сортировкой
     * @param collectionPath Путь к коллекции
     * @param field Поле для сортировки
     * @param descending Флаг для сортировки по убыванию
     * @param limit Ограничение количества результатов
     * @return QuerySnapshot
     */
    suspend fun getCollectionOrderBy(
        collectionPath: String,
        field: String,
        descending: Boolean = false,
        limit: Int = 0
    ): QuerySnapshot {
        var query = firestore.collection(collectionPath)
            .orderBy(field, if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)

        if (limit > 0) {
            query = query.limit(limit.toLong())
        }

        return query.get().await()
    }

    /**
     * Получение коллекции с фильтром и сортировкой
     * @param collectionPath Путь к коллекции
     * @param field Поле для фильтрации
     * @param value Значение для фильтрации
     * @param orderField Поле для сортировки
     * @param descending Флаг для сортировки по убыванию
     * @return QuerySnapshot
     */
    suspend fun getCollectionWithFilterOrderBy(
        collectionPath: String,
        field: String,
        value: Any,
        orderField: String,
        descending: Boolean = false
    ): QuerySnapshot {
        return firestore.collection(collectionPath)
            .whereEqualTo(field, value)
            .orderBy(orderField, if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
            .get()
            .await()
    }

    /**
     * Добавление документа в коллекцию с указанным ID
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @param data Данные документа
     * @return ID документа
     */
    suspend fun addDocument(collectionPath: String, documentId: String, data: Any): String {
        firestore.collection(collectionPath).document(documentId).set(data).await()
        return documentId
    }

    /**
     * Добавление документа в коллекцию с автоматической генерацией ID
     * @param collectionPath Путь к коллекции
     * @param data Данные документа
     * @return ID нового документа
     */
    suspend fun addDocumentWithAutoId(collectionPath: String, data: Any): String {
        val docRef = firestore.collection(collectionPath).add(data).await()
        return docRef.id
    }

    /**
     * Обновление поля документа
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @param field Поле для обновления
     * @param value Новое значение поля
     */
    suspend fun updateField(
        collectionPath: String,
        documentId: String,
        field: String,
        value: Any?
    ) {
        firestore.collection(collectionPath).document(documentId)
            .update(field, value)
            .await()
    }

    /**
     * Инкрементное увеличение поля
     * @param collectionPath Путь к коллекции
     * @param documentId ID документа
     * @param field Поле для увеличения
     * @param value Значение для увеличения
     */
    suspend fun incrementField(
        collectionPath: String,
        documentId: String,
        field: String,
        value: Long
    ) {
        firestore.collection(collectionPath).document(documentId)
            .update(field, com.google.firebase.firestore.FieldValue.increment(value))
            .await()
    }

    /**
     * Получение коллекции с NULL полем
     * @param collectionPath Путь к коллекции
     * @param field Поле, которое должно быть NULL
     * @return QuerySnapshot
     */
    suspend fun getCollectionWithNullField(
        collectionPath: String,
        field: String
    ): QuerySnapshot {
        return firestore.collection(collectionPath)
            .whereEqualTo(field, null)
            .get()
            .await()
    }

    /**
     * Выполнение транзакции Firestore
     * @param block Блок кода для выполнения в транзакции
     * @return Результат транзакции типа T
     */
    suspend fun <T> runTransaction(block: (com.google.firebase.firestore.Transaction) -> T): T {
        return firestore.runTransaction { transaction ->
            block(transaction)
        }.await()
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