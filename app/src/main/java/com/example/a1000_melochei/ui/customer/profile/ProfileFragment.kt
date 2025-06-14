package com.example.a1000_melochei.ui.customer.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Address
import com.example.a1000_melochei.data.model.User
import com.example.a1000_melochei.databinding.FragmentProfileBinding
import com.example.a1000_melochei.ui.auth.LoginActivity
import com.example.a1000_melochei.ui.customer.profile.adapter.AddressAdapter
import com.example.a1000_melochei.ui.customer.profile.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Фрагмент профиля пользователя.
 * Отображает личную информацию, настройки приложения и адреса доставки.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModel()
    private lateinit var addressAdapter: AddressAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Загружаем данные профиля
        viewModel.loadUserProfile()
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(
            onEditClick = { address ->
                navigateToAddressEdit(address)
            },
            onDeleteClick = { address ->
                showDeleteAddressConfirmation(address)
            },
            onDefaultClick = { address ->
                viewModel.setDefaultAddress(address.id)
            }
        )

        binding.rvAddresses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = addressAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за данными профиля
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { user ->
                        updateProfileUI(user)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(true, result.message)
                }
            }
        })

        // Наблюдение за тёмным режимом
        viewModel.isDarkMode.observe(viewLifecycleOwner, Observer { isDarkMode ->
            binding.switchDarkMode.isChecked = isDarkMode
        })

        // Наблюдение за уведомлениями
        viewModel.notificationsEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            binding.switchNotifications.isChecked = enabled
        })

        // Наблюдение за сбором отчетов о сбоях
        viewModel.crashReportingEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            binding.switchCrashReporting.isChecked = enabled
        })

        // Наблюдение за результатом выхода из аккаунта
        viewModel.logoutResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    navigateToLogin()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_logout),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом удаления адреса
        viewModel.deleteAddressResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), R.string.address_deleted, Toast.LENGTH_SHORT).show()
                    // Обновляем профиль
                    viewModel.loadUserProfile()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_deleting_address),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом установки адреса по умолчанию
        viewModel.setDefaultAddressResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), R.string.default_address_set, Toast.LENGTH_SHORT).show()
                    // Обновляем профиль
                    viewModel.loadUserProfile()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_setting_default_address),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })
    }

    private fun setupListeners() {
        // Обновление через свайп вниз
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadUserProfile()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Кнопка редактирования профиля
        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        // Кнопка добавления адреса
        binding.btnAddAddress.setOnClickListener {
            navigateToAddAddress()
        }

        // Кнопка просмотра заказов
        binding.btnViewOrders.setOnClickListener {
            navigateToOrders()
        }

        // Переключатель темной темы
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkMode(isChecked)
        }

        // Переключатель уведомлений
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotificationsEnabled(isChecked)
        }

        // Переключатель отчетов о сбоях
        binding.switchCrashReporting.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setCrashReportingEnabled(isChecked)
        }

        // Кнопка выхода из аккаунта
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Кнопка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadUserProfile()
        }
    }

    private fun updateProfileUI(user: User) {
        // Имя и email пользователя
        binding.tvUserName.text = user.name
        binding.tvUserEmail.text = user.email
        binding.tvUserPhone.text = user.phone

        // Аватар пользователя
        if (user.avatarUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(binding.ivUserAvatar)
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.default_avatar)
        }

        // Адреса доставки
        addressAdapter.submitList(user.addresses)
        binding.tvNoAddresses.isVisible = user.addresses.isEmpty()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.mainContent.isVisible = !isLoading && !binding.errorLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_profile)
        binding.mainContent.isVisible = !isError && !binding.progressBar.isVisible
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteAddressConfirmation(address: Address) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_address_title)
            .setMessage(R.string.delete_address_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAddress(address.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToEditProfile() {
        startActivity(Intent(requireContext(), EditProfileActivity::class.java))
    }

    private fun navigateToAddAddress() {
        val intent = Intent(requireContext(), AddressActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToAddressEdit(address: Address) {
        val intent = Intent(requireContext(), AddressActivity::class.java).apply {
            putExtra("ADDRESS_ID", address.id)
            putExtra("ADDRESS_TITLE", address.title)
            putExtra("ADDRESS_VALUE", address.address)
        }
        startActivity(intent)
    }

    private fun navigateToOrders() {
        // Переключаемся на таб заказов
        requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottom_nav_view
        )?.selectedItemId = R.id.navigation_orders
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        viewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}