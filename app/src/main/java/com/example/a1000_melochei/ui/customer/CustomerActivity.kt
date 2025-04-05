package com.yourstore.app.ui.customer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.auth.FirebaseAuth
import com.yourstore.app.R
import com.yourstore.app.data.repository.CartRepository
import com.yourstore.app.databinding.ActivityCustomerBinding
import org.koin.android.ext.android.inject

/**
 * Главная активность для клиентского интерфейса.
 * Содержит BottomNavigationView для навигации между основными разделами
 * и NavHostFragment для управления фрагментами.
 */
class CustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val cartRepository: CartRepository by inject()
    private val firebaseAuth: FirebaseAuth by inject()

    private var cartBadge: BadgeDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupNavigation()
        setupCartBadge()
        observeCartChanges()
    }

    private fun setupNavigation() {
        // Получаем NavHostFragment и NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Определяем верхнеуровневые пункты назначения без кнопки "Назад"
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_catalog,
                R.id.navigation_cart,
                R.id.navigation_orders,
                R.id.navigation_profile
            )
        )

        // Настраиваем ActionBar и BottomNavigationView
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavView.setupWithNavController(navController)

        // Слушаем изменения пункта назначения для обновления UI
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Показывать/скрывать BottomNavigationView на определенных экранах
            when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_catalog,
                R.id.navigation_cart,
                R.id.navigation_orders,
                R.id.navigation_profile -> showBottomNav()
                else -> hideBottomNav()
            }

            // Настройка заголовка
            when (destination.id) {
                R.id.navigation_home -> supportActionBar?.title = getString(R.string.title_home)
                R.id.navigation_catalog -> supportActionBar?.title = getString(R.string.title_catalog)
                R.id.navigation_cart -> supportActionBar?.title = getString(R.string.title_cart)
                R.id.navigation_orders -> supportActionBar?.title = getString(R.string.title_orders)
                R.id.navigation_profile -> supportActionBar?.title = getString(R.string.title_profile)
            }
        }
    }

    private fun setupCartBadge() {
        cartBadge = binding.bottomNavView.getOrCreateBadge(R.id.navigation_cart)
        cartBadge?.isVisible = false
    }

    private fun observeCartChanges() {
        cartRepository.getCartItemCount().observe(this) { count ->
            if (count > 0) {
                cartBadge?.number = count
                cartBadge?.isVisible = true
            } else {
                cartBadge?.isVisible = false
            }
        }
    }

    private fun showBottomNav() {
        binding.bottomNavView.visibility = View.VISIBLE
    }

    private fun hideBottomNav() {
        binding.bottomNavView.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Метод для программной навигации в корзину из других экранов
     */
    fun navigateToCart() {
        binding.bottomNavView.selectedItemId = R.id.navigation_cart
    }

    /**
     * Метод для обновления данных пользователя, например, после изменения профиля
     */
    fun refreshUserData() {
        // Обновление данных пользователя, если это необходимо
    }
}