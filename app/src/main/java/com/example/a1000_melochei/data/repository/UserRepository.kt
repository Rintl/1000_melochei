package com.yourstore.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Address
import com.yourstore.app.data.model.User
import com.yourstore.app.data.source.remote.FirebaseAuthSource
import com.yourstore.app.data.source.remote.FirestoreSource
import com.yourstore.app.data.source.remote.StorageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Репозиторий для управления данными пользователей.
 * Обеспечивает доступ к данным пользователей из Firebase Auth и Firestore.
 */
class UserRepository(
    private val firebaseAuthSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource,
    private val storageSource: StorageSource
) {
    private val TAG = "UserRepository"

    // Константы для работы с Firestore
    private val USERS_COLLECTION = "users"

    /**
     * Выполняет вход пользователя
     */
    suspend fun login(email: String, password: String): Resource<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = firebaseAuthSource.login(email, password)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при входе в аккаунт: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при входе в аккаунт")
        }
    }

    // Добавляем в класс UserRepository новый метод для обновления FCM токена

    /**
     * Обновляет FCM токен пользователя в Firestore
     * @param token Новый FCM токен
     * @return Resource с результатом операции
     */
    suspend fun updateFcmToken(token: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = firebaseAuth.currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем текущие данные пользователя
            val userDoc = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)
            val user = userDoc.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Обновляем FCM токен
            firestoreSource.updateField(
                USERS_COLLECTION,
                currentUserId,
                "fcmToken",
                token
            )

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении FCM токена: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении FCM токена")
        }
    }

    /**
     * Регистрирует нового пользователя
     */
    suspend fun register(email: String, password: String): Resource<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = firebaseAuthSource.register(email, password)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при регистрации: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при регистрации")
        }
    }

    /**
     * Создает профиль пользователя в Firestore
     */
    suspend fun createUserProfile(user: User): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = firestoreSource.setDocument(USERS_COLLECTION, user.id, user)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при создании профиля: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при создании профиля")
        }
    }

    /**
     * Получает профиль текущего пользователя
     */
    suspend fun getUserProfile(): Resource<User> = withContext(Dispatchers.IO) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            val userDocResult = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)

            if (userDocResult is Resource.Success) {
                val userDoc = userDocResult.data
                val user = userDoc?.toObject(User::class.java)?.copy(id = currentUserId)
                    ?: return@withContext Resource.Error("Профиль пользователя не найден")

                Resource.Success(user)
            } else {
                Resource.Error((userDocResult as Resource.Error).message ?: "Ошибка при получении данных пользователя")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении профиля: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении профиля")
        }
    }

    /**
     * Получает профиль пользователя как Flow
     */
    fun getUserProfileAsFlow(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        emit(getUserProfile())
    }

    /**
     * Обновляет профиль пользователя
     */
    suspend fun updateUserProfile(
        name: String,
        phone: String,
        avatarFile: File? = null
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем текущие данные пользователя
            val userDocResult = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)

            if (userDocResult !is Resource.Success) {
                return@withContext Resource.Error((userDocResult as Resource.Error).message ?: "Ошибка при получении данных пользователя")
            }

            val userDoc = userDocResult.data
            val currentUser = userDoc?.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Обновляем аватар, если он предоставлен
            var avatarUrl = currentUser.avatarUrl
            if (avatarFile != null) {
                val fileName = "users/$currentUserId/avatar_${System.currentTimeMillis()}.jpg"
                val uploadResult = storageSource.uploadFile(fileName, avatarFile)

                if (uploadResult is Resource.Success) {
                    avatarUrl = uploadResult.data ?: avatarUrl
                }
            }

            // Обновляем данные пользователя
            val updatedUser = currentUser.copy(
                name = name,
                phone = phone,
                avatarUrl = avatarUrl
            )

            firestoreSource.updateDocument(USERS_COLLECTION, currentUserId, updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении профиля: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении профиля")
        }
    }

    /**
     * Изменяет пароль пользователя
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Для изменения пароля сначала нужно повторно аутентифицировать пользователя
            val email = currentUser.email ?: return@withContext Resource.Error("Email пользователя не найден")
            val reauthResult = firebaseAuthSource.reauthenticate(email, currentPassword)

            if (reauthResult is Resource.Error) {
                return@withContext reauthResult
            }

            // Затем изменяем пароль
            val updateResult = firebaseAuthSource.updatePassword(newPassword)
            updateResult
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при смене пароля: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при смене пароля")
        }
    }

    /**
     * Сбрасывает пароль пользователя
     */
    suspend fun resetPassword(email: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firebaseAuthSource.resetPassword(email)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сбросе пароля: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при сбросе пароля")
        }
    }

    /**
     * Выход из аккаунта
     */
    fun logout(): Resource<Unit> {
        return try {
            firebaseAuthSource.logout()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выходе из аккаунта: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при выходе из аккаунта")
        }
    }

    /**
     * Добавляет новый адрес
     */
    suspend fun addAddress(title: String, address: String, isDefault: Boolean): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем текущие данные пользователя
            val userDocResult = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)

            if (userDocResult !is Resource.Success) {
                return@withContext Resource.Error((userDocResult as Resource.Error).message ?: "Ошибка при получении данных пользователя")
            }

            val userDoc = userDocResult.data
            val user = userDoc?.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Создаем новый адрес
            val newAddress = Address(
                id = UUID.randomUUID().toString(),
                title = title,
                address = address,
                isDefault = isDefault
            )

            // Если новый адрес помечен как основной, сбрасываем флаг у остальных адресов
            val updatedAddresses = if (isDefault) {
                user.addresses.map { it.copy(isDefault = false) } + newAddress
            } else {
                // Если у пользователя нет адресов, делаем этот адрес основным
                if (user.addresses.isEmpty()) {
                    listOf(newAddress.copy(isDefault = true))
                } else {
                    user.addresses + newAddress
                }
            }

            // Обновляем пользователя с новым списком адресов
            val updatedUser = user.copy(addresses = updatedAddresses)
            firestoreSource.updateDocument(USERS_COLLECTION, currentUserId, updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении адреса: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при добавлении адреса")
        }
    }

    /**
     * Обновляет существующий адрес
     */
    suspend fun updateAddress(
        addressId: String,
        title: String,
        address: String,
        isDefault: Boolean
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем текущие данные пользователя
            val userDocResult = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)

            if (userDocResult !is Resource.Success) {
                return@withContext Resource.Error((userDocResult as Resource.Error).message ?: "Ошибка при получении данных пользователя")
            }

            val userDoc = userDocResult.data
            val user = userDoc?.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Находим адрес, который нужно обновить
            val addressIndex = user.addresses.indexOfFirst { it.id == addressId }
            if (addressIndex == -1) {
                return@withContext Resource.Error("Адрес не найден")
            }

            // Обновляем адрес
            val updatedAddress = user.addresses[addressIndex].copy(
                title = title,
                address = address,
                isDefault = isDefault
            )

            // Если адрес помечен как основной, сбрасываем флаг у остальных адресов
            val updatedAddresses = if (isDefault) {
                user.addresses.mapIndexed { index, addr ->
                    if (index == addressIndex) updatedAddress else addr.copy(isDefault = false)
                }
            } else {
                val mutableList = user.addresses.toMutableList()
                mutableList[addressIndex] = updatedAddress
                mutableList
            }

            // Обновляем пользователя с новым списком адресов
            val updatedUser = user.copy(addresses = updatedAddresses)
            firestoreSource.updateDocument(USERS_COLLECTION, currentUserId, updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении адреса: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении адреса")
        }
    }

    /**
     * Удаляет адрес
     */
    suspend fun deleteAddress(addressId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем текущие данные пользователя
            val userDocResult = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)

            if (userDocResult !is Resource.Success) {
                return@withContext Resource.Error((userDocResult as Resource.Error).message ?: "Ошибка при получении данных пользователя")
            }

            val userDoc = userDocResult.data
            val user = userDoc?.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Удаляем адрес
            val deletedAddress = user.addresses.find { it.id == addressId }
                ?: return@withContext Resource.Error("Адрес не найден")

            val updatedAddresses = user.addresses.filter { it.id != addressId }

            // Если удаляем основной адрес и есть другие адреса, делаем первый из оставшихся основным
            val finalAddresses = if (deletedAddress.isDefault && updatedAddresses.isNotEmpty()) {
                val mutableList = updatedAddresses.toMutableList()
                mutableList[0] = mutableList[0].copy(isDefault = true)
                mutableList
            } else {
                updatedAddresses
            }

            // Обновляем пользователя с новым списком адресов
            val updatedUser = user.copy(addresses = finalAddresses)
            firestoreSource.updateDocument(USERS_COLLECTION, currentUserId, updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении адреса: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при удалении адреса")
        }
    }

    /**
     * Устанавливает адрес по умолчанию
     */
    suspend fun setDefaultAddress(addressId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем текущие данные пользователя
            val userDocResult = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)

            if (userDocResult !is Resource.Success) {
                return@withContext Resource.Error((userDocResult as Resource.Error).message ?: "Ошибка при получении данных пользователя")
            }

            val userDoc = userDocResult.data
            val user = userDoc?.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Проверяем, существует ли адрес
            if (user.addresses.none { it.id == addressId }) {
                return@withContext Resource.Error("Адрес не найден")
            }

            // Обновляем флаги у всех адресов
            val updatedAddresses = user.addresses.map { address ->
                address.copy(isDefault = address.id == addressId)
            }

            // Обновляем пользователя с новым списком адресов
            val updatedUser = user.copy(addresses = updatedAddresses)
            firestoreSource.updateDocument(USERS_COLLECTION, currentUserId, updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при установке адреса по умолчанию: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при установке адреса по умолчанию")
        }
    }

    /**
     * Устанавливает статус администратора
     */
    suspend fun setAdminStatus(isAdmin: Boolean): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем текущие данные пользователя
            val userDocResult = firestoreSource.getDocument(USERS_COLLECTION, currentUserId)

            if (userDocResult !is Resource.Success) {
                return@withContext Resource.Error((userDocResult as Resource.Error).message ?: "Ошибка при получении данных пользователя")
            }

            val userDoc = userDocResult.data
            val user = userDoc?.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Обновляем статус администратора
            val updatedUser = user.copy(isAdmin = isAdmin)
            firestoreSource.updateDocument(USERS_COLLECTION, currentUserId, updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при установке статуса администратора: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при установке статуса администратора")
        }
    }
}