package com.example.a1000_melochei.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.databinding.ActivityRegisterBinding
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.ui.customer.CustomerActivity
import com.example.a1000_melochei.util.showToast
import com.example.a1000_melochei.util.hideKeyboard
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Активность для регистрации новых пользователей.
 * Предоставляет интерфейс для создания нового аккаунта.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupListeners()
    }

    /**
     * Настраивает пользовательский интерфейс
     */
    private fun setupUI() {
        // Настраиваем ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.register_title)
        }

        // Устанавливаем начальное состояние
        updateRegisterButtonState(false)
    }

    /**
     * Настраивает слушатели событий
     */
    private fun setupListeners() {
        // Слушатели изменения текста
        binding.etName.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilName)
        }

        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilEmail)
        }

        binding.etPhone.doOnTextChanged { text, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilPhone)

            // Форматируем номер телефона
            val formattedPhone = authViewModel.formatPhoneNumber(text.toString())
            if (formattedPhone != text.toString()) {
                binding.etPhone.setText(formattedPhone)
                binding.etPhone.setSelection(formattedPhone.length)
            }
        }

        binding.etPassword.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilPassword)
        }

        binding.etConfirmPassword.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilConfirmPassword)
        }

        // Кнопка регистрации
        binding.btnRegister.setOnClickListener {
            hideKeyboard()
            performRegister()
        }

        // Ссылка на авторизацию
        binding.tvLogin.setOnClickListener {
            navigateToLogin()
        }

        // Чекбокс согласия с условиями
        binding.cbTerms.setOnCheckedChangeListener { _, _ ->
            updateFormValidation()
        }
    }

    /**
     * Настраивает наблюдателей ViewModel
     */
    private fun setupObservers() {
        // Наблюдаем за валидацией формы
        authViewModel.registerFormValid.observe(this) { isValid ->
            val termsAccepted = binding.cbTerms.isChecked
            updateRegisterButtonState(isValid && termsAccepted)
        }

        // Наблюдаем за результатом регистрации
        authViewModel.registerResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoadingState(true)
                }
                is Resource.Success -> {
                    showLoadingState(false)
                    showToast(getString(R.string.register_success))

                    // Переходим к клиентской части
                    navigateToCustomer()
                }
                is Resource.Error -> {
                    showLoadingState(false)
                    handleRegisterError(resource.message)
                }
            }
        }
    }

    /**
     * Обновляет валидацию формы
     */
    private fun updateFormValidation() {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        authViewModel.updateRegisterForm(email, password, confirmPassword, name, phone)
    }

    /**
     * Выполняет регистрацию пользователя
     */
    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // Дополнительная валидация
        if (!validateInput(name, email, phone, password, confirmPassword)) {
            return
        }

        // Проверяем согласие с условиями
        if (!binding.cbTerms.isChecked) {
            showToast(getString(R.string.terms_agreement_required))
            return
        }

        // Очищаем предыдущие результаты
        authViewModel.clearResults()

        // Выполняем регистрацию
        authViewModel.registerUser(email, password, confirmPassword, name, phone)
    }

    /**
     * Валидирует введенные данные
     */
    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        // Проверяем имя
        val nameError = authViewModel.getNameValidationError(name)
        if (nameError != null) {
            binding.tilName.error = nameError
            isValid = false
        }

        // Проверяем email
        val emailError = authViewModel.getEmailValidationError(email)
        if (emailError != null) {
            binding.tilEmail.error = emailError
            isValid = false
        }

        // Проверяем телефон
        val phoneError = authViewModel.getPhoneValidationError(phone)
        if (phoneError != null) {
            binding.tilPhone.error = phoneError
            isValid = false
        }

        // Проверяем пароль
        val passwordError = authViewModel.getPasswordValidationError(password)
        if (passwordError != null) {
            binding.tilPassword.error = passwordError
            isValid = false
        }

        // Проверяем подтверждение пароля
        val confirmPasswordError = authViewModel.getConfirmPasswordValidationError(password, confirmPassword)
        if (confirmPasswordError != null) {
            binding.tilConfirmPassword.error = confirmPasswordError
            isValid = false
        }

        return isValid
    }

    /**
     * Обрабатывает ошибки регистрации
     */
    private fun handleRegisterError(errorMessage: String?) {
        val message = errorMessage ?: getString(R.string.register_failed)

        when {
            message.contains("email", ignoreCase = true) -> {
                binding.tilEmail.error = message
            }
            message.contains("password", ignoreCase = true) -> {
                binding.tilPassword.error = message
            }
            message.contains("name", ignoreCase = true) -> {
                binding.tilName.error = message
            }
            message.contains("phone", ignoreCase = true) -> {
                binding.tilPhone.error = message
            }
            else -> {
                showToast(message)
            }
        }
    }

    /**
     * Обновляет состояние кнопки регистрации
     */
    private fun updateRegisterButtonState(isEnabled: Boolean) {
        binding.btnRegister.isEnabled = isEnabled
        binding.btnRegister.alpha = if (isEnabled) 1.0f else 0.5f
    }

    /**
     * Показывает/скрывает состояние загрузки
     */
    private fun showLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading

        // Отключаем все поля ввода
        binding.etName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.cbTerms.isEnabled = !isLoading

        if (isLoading) {
            binding.btnRegister.text = getString(R.string.loading)
        } else {
            binding.btnRegister.text = getString(R.string.register)
        }
    }

    /**
     * Очищает ошибку поля
     */
    private fun clearFieldError(textInputLayout: com.google.android.material.textfield.TextInputLayout) {
        textInputLayout.error = null
    }

    /**
     * Переходит к экрану авторизации
     */
    private fun navigateToLogin() {
        finish() // Просто закрываем текущую активность
    }

    /**
     * Переходит к клиентской части приложения
     */
    private fun navigateToCustomer() {
        val intent = Intent(this, CustomerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Обрабатывает нажатие кнопки "Назад" в ActionBar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Очищает наблюдателей при уничтожении активности
     */
    override fun onDestroy() {
        super.onDestroy()
        authViewModel.registerResult.removeObservers(this)
        authViewModel.registerFormValid.removeObservers(this)
    }
}