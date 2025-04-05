package com.yourstore.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.databinding.ActivityForgotPasswordBinding
import com.yourstore.app.ui.auth.viewmodel.AuthViewModel
import com.yourstore.app.util.ValidationUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Экран восстановления пароля
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        setupListeners()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(R.string.forgot_password_title)
        binding.tvSubtitle.text = getString(R.string.forgot_password_subtitle)
        binding.toolbar.title = getString(R.string.forgot_password)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupObservers() {
        // Наблюдение за результатом сброса пароля
        viewModel.resetPasswordResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showResetSuccess()
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(result.message ?: getString(R.string.password_reset_error))
                }
            }
        })

        // Наблюдение за состоянием валидации email
        viewModel.emailValid.observe(this, Observer { isValid ->
            binding.btnResetPassword.isEnabled = isValid
        })
    }

    private fun setupListeners() {
        // Ввод email
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            val email = text.toString()
            binding.tilEmail.error = if (ValidationUtils.isValidEmail(email)) null
            else getString(R.string.invalid_email)

            viewModel.validateEmail(email)
        }

        // Кнопка сброса пароля
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString()
            viewModel.resetPassword(email)
        }

        // Возврат на экран входа
        binding.tvBackToLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showResetSuccess() {
        binding.layoutEmail.visibility = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE

        // Устанавливаем таймер для автоматического закрытия
        binding.btnBackToLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onBackPressed() {
        if (binding.layoutSuccess.visibility == View.VISIBLE) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}