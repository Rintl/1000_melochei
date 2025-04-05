package com.yourstore.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.databinding.ActivityLoginBinding
import com.yourstore.app.ui.admin.AdminActivity
import com.yourstore.app.ui.auth.viewmodel.AuthViewModel
import com.yourstore.app.ui.customer.CustomerActivity
import com.yourstore.app.util.ValidationUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Экран входа в приложение для клиентов
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        setupListeners()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(R.string.login_title)
        binding.tvSubtitle.text = getString(R.string.login_subtitle)
    }

    private fun setupObservers() {
        // Наблюдение за результатом входа
        viewModel.loginResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    handleLoginSuccess(result.data)
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(result.message ?: getString(R.string.login_error))
                }
            }
        })

        // Наблюдение за состоянием валидации формы
        viewModel.formValid.observe(this, Observer { isValid ->
            binding.btnLogin.isEnabled = isValid
        })
    }

    private fun setupListeners() {
        // Ввод email
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            binding.tilEmail.error = if (ValidationUtils.isValidEmail(text.toString())) null
            else getString(R.string.invalid_email)

            viewModel.validateForm(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString()
            )
        }

        // Ввод пароля
        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            binding.tilPassword.error = if (ValidationUtils.isValidPassword(text.toString())) null
            else getString(R.string.invalid_password)

            viewModel.validateForm(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString()
            )
        }

        // Кнопка входа
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.login(email, password)
        }

        // Переход на экран регистрации
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Переход на экран восстановления пароля
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Переход на экран входа для администратора
        binding.tvAdminLogin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun handleLoginSuccess(isAdmin: Boolean) {
        if (isAdmin) {
            startActivity(Intent(this, AdminActivity::class.java))
        } else {
            startActivity(Intent(this, CustomerActivity::class.java))
        }
        finish()
    }
}