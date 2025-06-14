package com.example.a1000_melochei.ui.customer.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.databinding.ActivityAddressBinding
import com.example.a1000_melochei.ui.customer.profile.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale


/**
 * Активность для добавления и редактирования адресов доставки.
 * Позволяет указать название и адрес, а также определить текущее местоположение.
 */
class AddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressBinding
    private val viewModel: ProfileViewModel by viewModel()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var addressId: String? = null
    private var isEditing = false

    // Обработчик результата запроса разрешения на доступ к местоположению
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(
                this,
                R.string.location_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация провайдера местоположения
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Проверяем, это добавление или редактирование
        addressId = intent.getStringExtra("ADDRESS_ID")
        isEditing = addressId != null

        setupToolbar()
        setupObservers()
        setupListeners()
        setupValidation()

        // Если это редактирование, заполняем поля существующими данными
        if (isEditing) {
            val title = intent.getStringExtra("ADDRESS_TITLE") ?: ""
            val address = intent.getStringExtra("ADDRESS_VALUE") ?: ""

            binding.etAddressTitle.setText(title)
            binding.etAddressValue.setText(address)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditing) {
            getString(R.string.edit_address)
        } else {
            getString(R.string.add_address)
        }
    }

    private fun setupObservers() {
        // Наблюдение за валидностью формы
        viewModel.addressFormValid.observe(this) { isValid ->
            binding.btnSaveAddress.isEnabled = isValid
        }

        // Наблюдение за результатом добавления адреса
        viewModel.addAddressResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnSaveAddress.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnSaveAddress.isEnabled = true
                    binding.progressBar.visibility = View.GONE

                    Toast.makeText(
                        this,
                        if (isEditing) R.string.address_updated else R.string.address_added,
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
                is Resource.Error -> {
                    binding.btnSaveAddress.isEnabled = true
                    binding.progressBar.visibility = View.GONE

                    Toast.makeText(
                        this,
                        result.message ?: getString(
                            if (isEditing) R.string.error_updating_address else R.string.error_adding_address
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupListeners() {
        // Кнопка определения текущего местоположения
        binding.btnGetCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        // Кнопка сохранения адреса
        binding.btnSaveAddress.setOnClickListener {
            saveAddress()
        }

        // Кнопка открытия карты
        binding.btnOpenMap.setOnClickListener {
            openMap()
        }
    }

    private fun setupValidation() {
        // Валидация названия адреса
        binding.etAddressTitle.doOnTextChanged { text, _, _, _ ->
            binding.tilAddressTitle.error = if (text.toString().length < 3) {
                getString(R.string.invalid_address_title)
            } else {
                null
            }
            validateForm()
        }

        // Валидация адреса
        binding.etAddressValue.doOnTextChanged { text, _, _, _ ->
            binding.tilAddressValue.error = if (text.toString().length < 5) {
                getString(R.string.invalid_address)
            } else {
                null
            }
            validateForm()
        }
    }

    private fun validateForm() {
        val title = binding.etAddressTitle.text.toString()
        val address = binding.etAddressValue.text.toString()

        viewModel.validateAddressForm(title, address)
    }

    private fun saveAddress() {
        val title = binding.etAddressTitle.text.toString()
        val address = binding.etAddressValue.text.toString()
        val isDefault = binding.cbDefaultAddress.isChecked

        if (isEditing && addressId != null) {
            viewModel.updateAddress(addressId!!, title, address, isDefault)
        } else {
            viewModel.addAddress(title, address, isDefault)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            // Разрешение уже есть, можно получить местоположение
            getCurrentLocation()
        } else {
            // Нужно запросить разрешение
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )) {
                // Показываем объяснение, почему нужно разрешение
                showLocationPermissionRationale()
            } else {
                // Сразу запрашиваем разрешение
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun showLocationPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.location_permission_needed)
            .setMessage(R.string.location_permission_explanation)
            .setPositiveButton(R.string.grant) { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

            binding.locationProgressBar.visibility = View.VISIBLE

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    binding.locationProgressBar.visibility = View.GONE

                    if (location != null) {
                        // Получаем адрес по координатам
                        try {
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1
                            )

                            if (addresses != null && addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val addressText = buildString {
                                    if (!address.thoroughfare.isNullOrEmpty()) {
                                        append(address.thoroughfare)
                                        if (!address.subThoroughfare.isNullOrEmpty()) {
                                            append(", ").append(address.subThoroughfare)
                                        }
                                    } else {
                                        if (!address.subAdminArea.isNullOrEmpty()) {
                                            append(address.subAdminArea).append(", ")
                                        }
                                        if (!address.postalCode.isNullOrEmpty()) {
                                            append(address.postalCode).append(", ")
                                        }
                                    }

                                    if (!address.locality.isNullOrEmpty()) {
                                        if (isNotEmpty()) append(", ")
                                        append(address.locality)
                                    }
                                }

                                if (addressText.isNotEmpty()) {
                                    binding.etAddressValue.setText(addressText)
                                } else {
                                    binding.etAddressValue.setText(
                                        getString(
                                            R.string.coordinates_format,
                                            location.latitude,
                                            location.longitude
                                        )
                                    )
                                }
                            } else {
                                // Если адрес не найден, используем координаты
                                binding.etAddressValue.setText(
                                    getString(
                                        R.string.coordinates_format,
                                        location.latitude,
                                        location.longitude
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // В случае ошибки геокодирования используем координаты
                            binding.etAddressValue.setText(
                                getString(
                                    R.string.coordinates_format,
                                    location.latitude,
                                    location.longitude
                                )
                            )
                        }
                    } else {
                        Toast.makeText(
                            this,
                            R.string.location_not_found,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    binding.locationProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        getString(R.string.location_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun openMap() {
        // Здесь можно открыть карту для выбора адреса
        // В текущей реализации просто показываем сообщение
        Toast.makeText(
            this,
            R.string.map_not_implemented,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}