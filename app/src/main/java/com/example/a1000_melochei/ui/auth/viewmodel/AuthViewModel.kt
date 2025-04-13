package com.yourstore.app.ui.auth.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Address
import com.yourstore.app.data.model.User
import com.yourstore.app.data.repository.UserRepository
import com.yourstore.app.data.source.local.PreferencesManager
import com.yourstore.app.util.ValidationUtils
import kotlinx.coroutines.launch

/**
 * ViewModel для управления авторизацией и регистрацией пользователей
 */
class AuthViewModel(
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val TAG = "AuthViewModel"

    // Результаты действий
    private val _loginResult = MutableLiveData<Resource<Boolean>>()
    val loginResult: LiveData<Resource<Boolean>> = _loginResult

    private val _adminLoginResult = MutableLiveData<Resource<Unit>>()
    val adminLoginResult: LiveData<Resource<Unit>> = _adminLoginResult

    private val _registerResult = MutableLiveData<Resource<Unit>>()
    val registerResult: LiveData<Resource<Unit>> = _registerResult

    private val _resetPasswordResult = MutableLiveData<Resource<Unit>>()
    val resetPasswordResult: LiveData<Resource<Unit>> = _resetPasswordResult

    // Валидация форм
    private val _formValid = MutableLiveData<Boolean>()
    val formValid: LiveData<Boolean> = _formValid

    private val _adminFormValid = MutableLiveData<Boolean>()
    val adminFormValid: LiveData<Boolean> = _adminFormValid

    private val _registerFormValid = MutableLiveData<Boolean>()
    val registerFormValid: LiveData<Boolean> = _registerFormValid

    private val _emailValid = MutableLiveData<Boolean>()
    val emailValid: LiveData<Boolean> = _emailValid

    /**
     * Авторизация пользователя
     */
    fun login(email: String, password: String) {
        _loginResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.login(email, password)

                if (result.data != null) {
                    // Получаем информацию о пользователе
                    val userInfo = userRepository.getUserProfile()

                    if (userInfo.data != null) {
                        val isAdmin = userInfo.data.isAdmin
                        preferencesManager.setAdmin(isAdmin)
                        _loginResult.value = Resource.Success(isAdmin)
                    } else {
                        _loginResult.value = Resource.Error(
                            userInfo.message ?: "Не удалось получить данные пользователя"
                        )
                    }
                } else {
                    _loginResult.value = Resource.Error(
                        result.message ?: "Ошибка авторизации"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка авторизации: ${e.message}")
                _loginResult.value = Resource.Error(e.message ?: "Неизвестная ошибка авторизации")
            }
        }
    }

    /**
     * Авторизация администратора
     */
    fun loginAsAdmin(email: String, password: String, adminCode: String) {
        _adminLoginResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Сначала проверяем корректность кода администратора
                if (adminCode != Constants.ADMIN_CODE) {
                    _adminLoginResult.value = Resource.Error("Неверный код администратора")
                    return@launch
                }

                val result = userRepository.login(email, password)

                if (result.data != null) {
                    // Проверяем и устанавливаем права администратора
                    val setAdminResult = userRepository.setAdminStatus(true)

                    if (setAdminResult.data != null) {
                        preferencesManager.setAdmin(true)
                        _adminLoginResult.value = Resource.Success(Unit)
                    } else {
                        _adminLoginResult.value = Resource.Error(
                            setAdminResult.message ?: "Не удалось установить статус администратора"
                        )
                    }
                } else {
                    _adminLoginResult.value = Resource.Error(
                        result.message ?: "Ошибка авторизации"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка авторизации администратора: ${e.message}")
                _adminLoginResult.value = Resource.Error(e.message ?: "Неизвестная ошибка авторизации")
            }
        }
    }

    /**
     * Регистрация нового пользователя
     */
    fun register(name: String, phone: String, email: String, password: String, address: String) {
        _registerResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Создаем объект адреса
                val userAddress = Address(
                    id = "",
                    title = "Основной адрес",
                    address = address,
                    isDefault = true
                )

                // Регистрируем пользователя
                val registerResult = userRepository.register(email, password)

                if (registerResult.data != null) {
                    // Создаем профиль пользователя
                    val user = User(
                        id = registerResult.data,
                        name = name,
                        email = email,
                        phone = phone,
                        isAdmin = false,
                        addresses = listOf(userAddress)
                    )

                    val createProfileResult = userRepository.createUserProfile(user)

                    if (createProfileResult.data != null) {
                        preferencesManager.setAdmin(false)
                        _registerResult.value = Resource.Success(Unit)
                    } else {
                        _registerResult.value = Resource.Error(
                            createProfileResult.message ?: "Ошибка создания профиля"
                        )
                    }
                } else {
                    _registerResult.value = Resource.Error(
                        registerResult.message ?: "Ошибка регистрации"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка регистрации: ${e.message}")
                _registerResult.value = Resource.Error(e.message ?: "Неизвестная ошибка регистрации")
            }
        }
    }

    /**
     * Сброс пароля
     */
    fun resetPassword(email: String) {
        _resetPasswordResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.resetPassword(email)

                if (result.data != null) {
                    _resetPasswordResult.value = Resource.Success(Unit)
                } else {
                    _resetPasswordResult.value = Resource.Error(
                        result.message ?: "Ошибка сброса пароля"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сброса пароля: ${e.message}")
                _resetPasswordResult.value = Resource.Error(e.message ?: "Неизвестная ошибка сброса пароля")
            }
        }
    }

    /**
     * Валидация формы входа
     */
    fun validateForm(email: String, password: String) {
        val isEmailValid = ValidationUtils.isValidEmail(email)
        val isPasswordValid = ValidationUtils.isValidPassword(password)

        _formValid.value = isEmailValid && isPasswordValid
    }

    /**
     * Валидация формы входа администратора
     */
    fun validateAdminForm(email: String, password: String, adminCode: String) {
        val isEmailValid = ValidationUtils.isValidEmail(email)
        val isPasswordValid = ValidationUtils.isValidPassword(password)
        val isAdminCodeValid = adminCode.length >= 6

        _adminFormValid.value = isEmailValid && isPasswordValid && isAdminCodeValid
    }

    /**
     * Валидация формы регистрации
     */
    fun validateRegisterForm(
        name: String,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String,
        address: String
    ) {
        val isNameValid = name.length >= 2
        val isPhoneValid = ValidationUtils.isValidPhone(phone)
        val isEmailValid = ValidationUtils.isValidEmail(email)
        val isPasswordValid = ValidationUtils.isValidPassword(password)
        val isConfirmPasswordValid = password == confirmPassword
        val isAddressValid = address.length >= 5

        _registerFormValid.value = isNameValid && isPhoneValid && isEmailValid &&
                isPasswordValid && isConfirmPasswordValid && isAddressValid
    }

    /**
     * Валидация email для сброса пароля
     */
    fun validateEmail(email: String) {
        _emailValid.value = ValidationUtils.isValidEmail(email)
    }
}