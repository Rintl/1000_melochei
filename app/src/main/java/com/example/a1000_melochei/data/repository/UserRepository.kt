package com.example.a1000_melochei.data.repository

import android.net.Uri
import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Address
import com.example.a1000_melochei.data.model.User
import com.example.a1000_melochei.data.source.remote.FirebaseAuthSource
import com.example.a1000_melochei.data.source.remote.FirestoreSource
import com.example.a1000_melochei.util.Constants
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления пользователями.
 * Обеспечивает взаимодействие с Firebase Auth и Firestore для пользователей.
 */
class UserRepository(
    private val authSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource
) {
    private val TAG = "UserRepository"

    /**
     * Регистрирует нового пользователя
     */
    suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        phone: String
    ): Resource<User> {
        return try {
            // Валидация данных
            if (!User.isValidEmail(email)) {
                return Resource.Error("Некорректный email")
            }
            if (password.length < Constants.MIN_PASSWORD_LENGTH) {
                return Resource.Error("Пароль должен содержать не менее ${Constants.MIN_PASSWORD_LENGTH} символов")
            }
            if (!User.isValidName(name)) {
                return Resource.Error("Некорректное имя")
            }
            if (!User.isValidPhone(phone)) {
                return Resource.Error("Некорректный номер телефона")
            }

            // Регистрируем пользователя в Firebase Auth
            val authResult = authSource.createUser(email, password)
            when (authResult) {
                is Resource.Success -> {
                    val firebaseUser = authResult.data

                    // Создаем профиль пользователя в Firestore
                    val user = User(
                        id = firebaseUser.uid,
                        email = email,
                        name = name,
                        phone = phone,
                        isAdmin = false,
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )

                    val createProfileResult = firestoreSource.createUserProfile(user)
                    when (createProfileResult) {
                        is Resource.Success -> {
                            Log.d(TAG, "Пользователь зарегистрирован: $email")
                            Resource.Success(user)
                        }
                        is Resource.Error -> {
                            // Удаляем пользователя из Auth, если не удалось создать профиль
                            authSource.deleteUser()
                            Resource.Error("Ошибка создания профиля: ${createProfileResult.message}")
                        }
                        is Resource.Loading -> Resource.Loading()
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка регистрации: ${authResult.message}")
                    Resource.Error(authResult.message ?: "Ошибка регистрации")
                }
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при регистрации пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Авторизует пользователя
     */
    suspend fun loginUser(email: String, password: String): Resource<User> {
        return try {
            if (!User.isValidEmail(email)) {
                return Resource.Error("Некорректный email")
            }
            if (password.isBlank()) {
                return Resource.Error("Введите пароль")
            }

            val authResult = authSource.signInUser(email, password)
            when (authResult) {
                is Resource.Success -> {
                    val firebaseUser = authResult.data

                    // Получаем профиль пользователя из Firestore
                    val profileResult = getUserProfile(firebaseUser.uid)
                    when (profileResult) {
                        is Resource.Success -> {
                            // Обновляем время последнего входа
                            val updatedUser = profileResult.data.copy(
                                lastLoginAt = System.currentTimeMillis()
                            )
                            firestoreSource.updateUserProfile(updatedUser)

                            Log.d(TAG, "Пользователь авторизован: $email")
                            Resource.Success(updatedUser)
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Ошибка получения профиля: ${profileResult.message}")
                            Resource.Error("Профиль пользователя не найден")
                        }
                        is Resource.Loading -> Resource.Loading()
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка авторизации: ${authResult.message}")
                    Resource.Error(authResult.message ?: "Ошибка авторизации")
                }
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при авторизации пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Авторизует администратора
     */
    suspend fun loginAdmin(email: String, password: String, adminCode: String): Resource<User> {
        return try {
            // Проверяем код администратора (в реальном приложении код должен храниться в безопасном месте)
            val validAdminCode = "ADMIN2024" // Это должно быть в конфигурации
            if (adminCode != validAdminCode) {
                return Resource.Error("Неверный код администратора")
            }

            val loginResult = loginUser(email, password)
            when (loginResult) {
                is Resource.Success -> {
                    val user = loginResult.data
                    if (!user.isAdmin) {
                        // Можно либо отклонить доступ, либо повысить права
                        // Для демо повышаем права
                        val adminUser = user.copy(isAdmin = true)
                        updateUserProfile(adminUser)
                        Resource.Success(adminUser)
                    } else {
                        Resource.Success(user)
                    }
                }
                is Resource.Error -> loginResult
                is Resource.Loading -> loginResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при авторизации администратора: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Выходит из системы
     */
    suspend fun logout(): Resource<Unit> {
        return try {
            val result = authSource.signOut()
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Пользователь вышел из системы")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка выхода: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выходе: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает профиль пользователя
     */
    suspend fun getUserProfile(userId: String): Resource<User> {
        return try {
            val result = firestoreSource.getUserProfile(userId)
            Log.d(TAG, "Загружен профиль пользователя: ${result.getDataOrNull()?.name}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке профиля: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает текущего пользователя
     */
    suspend fun getCurrentUser(): Resource<User?> {
        return try {
            val currentFirebaseUser = authSource.getCurrentUser()
            if (currentFirebaseUser != null) {
                getUserProfile(currentFirebaseUser.uid)
            } else {
                Resource.Success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении текущего пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет профиль пользователя
     */
    suspend fun updateUserProfile(user: User): Resource<Unit> {
        return try {
            if (!user.isValid()) {
                return Resource.Error("Данные пользователя некорректны")
            }

            val result = firestoreSource.updateUserProfile(user)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Профиль пользователя обновлен: ${user.name}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка обновления профиля: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении профиля: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Добавляет адрес пользователю
     */
    suspend fun addUserAddress(userId: String, address: Address): Resource<Unit> {
        return try {
            val userResult = getUserProfile(userId)
            when (userResult) {
                is Resource.Success -> {
                    val user = userResult.data
                    val newAddress = address.copy(
                        id = if (address.id.isEmpty()) generateAddressId() else address.id
                    )

                    // Если это первый адрес, делаем его основным
                    val isPrimary = user.addresses.isEmpty() || address.isPrimary
                    val finalAddress = newAddress.copy(isPrimary = isPrimary)

                    // Если новый адрес основной, убираем флаг с других адресов
                    val updatedAddresses = if (isPrimary) {
                        user.addresses.map { it.copy(isPrimary = false) } + finalAddress
                    } else {
                        user.addresses + finalAddress
                    }

                    val updatedUser = user.copy(addresses = updatedAddresses)
                    updateUserProfile(updatedUser)
                }
                is Resource.Error -> userResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении адреса: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет адрес пользователя
     */
    suspend fun updateUserAddress(userId: String, address: Address): Resource<Unit> {
        return try {
            val userResult = getUserProfile(userId)
            when (userResult) {
                is Resource.Success -> {
                    val user = userResult.data
                    val updatedAddresses = user.addresses.map { userAddress ->
                        if (userAddress.id == address.id) {
                            // Если делаем адрес основным, убираем флаг с других
                            if (address.isPrimary) {
                                address
                            } else {
                                address
                            }
                        } else {
                            // Убираем флаг основного с других адресов, если новый адрес основной
                            if (address.isPrimary) {
                                userAddress.copy(isPrimary = false)
                            } else {
                                userAddress
                            }
                        }
                    }

                    val updatedUser = user.copy(addresses = updatedAddresses)
                    updateUserProfile(updatedUser)
                }
                is Resource.Error -> userResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении адреса: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Удаляет адрес пользователя
     */
    suspend fun deleteUserAddress(userId: String, addressId: String): Resource<Unit> {
        return try {
            val userResult = getUserProfile(userId)
            when (userResult) {
                is Resource.Success -> {
                    val user = userResult.data
                    val addressToDelete = user.addresses.find { it.id == addressId }

                    if (addressToDelete == null) {
                        return Resource.Error("Адрес не найден")
                    }

                    val updatedAddresses = user.addresses.filter { it.id != addressId }

                    // Если удаляемый адрес был основным, делаем основным первый из оставшихся
                    val finalAddresses = if (addressToDelete.isPrimary && updatedAddresses.isNotEmpty()) {
                        updatedAddresses.mapIndexed { index, address ->
                            if (index == 0) address.copy(isPrimary = true) else address
                        }
                    } else {
                        updatedAddresses
                    }

                    val updatedUser = user.copy(addresses = finalAddresses)
                    updateUserProfile(updatedUser)
                }
                is Resource.Error -> userResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении адреса: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Устанавливает основной адрес
     */
    suspend fun setPrimaryAddress(userId: String, addressId: String): Resource<Unit> {
        return try {
            val userResult = getUserProfile(userId)
            when (userResult) {
                is Resource.Success -> {
                    val user = userResult.data
                    val updatedAddresses = user.addresses.map { address ->
                        address.copy(isPrimary = address.id == addressId)
                    }

                    val updatedUser = user.copy(addresses = updatedAddresses)
                    updateUserProfile(updatedUser)
                }
                is Resource.Error -> userResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при установке основного адреса: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Сбрасывает пароль пользователя
     */
    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            if (!User.isValidEmail(email)) {
                return Resource.Error("Некорректный email")
            }

            val result = authSource.resetPassword(email)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Письмо для сброса пароля отправлено на: $email")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка сброса пароля: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сбросе пароля: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Изменяет пароль пользователя
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Unit> {
        return try {
            if (newPassword.length < Constants.MIN_PASSWORD_LENGTH) {
                return Resource.Error("Новый пароль должен содержать не менее ${Constants.MIN_PASSWORD_LENGTH} символов")
            }

            val result = authSource.changePassword(currentPassword, newPassword)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Пароль изменен")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка изменения пароля: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при изменении пароля: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает аватар пользователя
     */
    suspend fun uploadUserAvatar(userId: String, imageUri: Uri): Resource<String> {
        return try {
            val result = firestoreSource.uploadUserAvatar(userId, imageUri)
            when (result) {
                is Resource.Success -> {
                    // Обновляем профиль пользователя с новым URL аватара
                    val userResult = getUserProfile(userId)
                    if (userResult is Resource.Success) {
                        val updatedUser = userResult.data.copy(avatarUrl = result.data)
                        updateUserProfile(updatedUser)
                    }

                    Log.d(TAG, "Аватар загружен: ${result.data}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка загрузки аватара: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке аватара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    fun isUserLoggedIn(): Boolean {
        return authSource.getCurrentUser() != null
    }

    /**
     * Получает поток состояния авторизации
     */
    fun getAuthStateFlow(): Flow<FirebaseUser?> {
        return authSource.getAuthStateFlow()
    }

    /**
     * Обновляет FCM токен пользователя
     */
    suspend fun updateFcmToken(userId: String, token: String): Resource<Unit> {
        return try {
            val userResult = getUserProfile(userId)
            when (userResult) {
                is Resource.Success -> {
                    val updatedUser = userResult.data.copy(fcmToken = token)
                    updateUserProfile(updatedUser)
                }
                is Resource.Error -> userResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении FCM токена: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Удаляет аккаунт пользователя
     */
    suspend fun deleteUserAccount(userId: String): Resource<Unit> {
        return try {
            // Сначала удаляем данные из Firestore
            val deleteProfileResult = firestoreSource.deleteUserProfile(userId)
            if (deleteProfileResult is Resource.Error) {
                return deleteProfileResult
            }

            // Затем удаляем пользователя из Firebase Auth
            val deleteAuthResult = authSource.deleteUser()
            when (deleteAuthResult) {
                is Resource.Success -> {
                    Log.d(TAG, "Аккаунт пользователя удален: $userId")
                    deleteAuthResult
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка удаления аккаунта: ${deleteAuthResult.message}")
                    deleteAuthResult
                }
                is Resource.Loading -> deleteAuthResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении аккаунта: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Генерирует уникальный ID для адреса
     */
    private fun generateAddressId(): String {
        return "addr_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}