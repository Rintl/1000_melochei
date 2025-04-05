package com.yourstore.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.yourstore.app.data.repository.*
import com.yourstore.app.data.source.local.CartCache
import com.yourstore.app.data.source.local.PreferencesManager
import com.yourstore.app.data.source.remote.FirebaseAuthSource
import com.yourstore.app.data.source.remote.FirestoreSource
import com.yourstore.app.data.source.remote.StorageSource
import com.yourstore.app.service.NotificationService
import com.yourstore.app.ui.admin.analytics.viewmodel.AnalyticsViewModel
import com.yourstore.app.ui.admin.categories.viewmodel.CategoryViewModel
import com.yourstore.app.ui.admin.dashboard.viewmodel.DashboardViewModel
import com.yourstore.app.ui.admin.orders.viewmodel.AdminOrderViewModel
import com.yourstore.app.ui.admin.products.viewmodel.AdminProductViewModel
import com.yourstore.app.ui.auth.viewmodel.AuthViewModel
import com.yourstore.app.ui.customer.cart.viewmodel.CartViewModel
import com.yourstore.app.ui.customer.catalog.viewmodel.CatalogViewModel
import com.yourstore.app.ui.customer.catalog.viewmodel.ProductViewModel
import com.yourstore.app.ui.customer.home.viewmodel.HomeViewModel
import com.yourstore.app.ui.customer.orders.viewmodel.OrderViewModel
import com.yourstore.app.ui.customer.profile.viewmodel.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Модуль основных компонентов приложения
 */
val appModule = module {
    // Firebase
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }

    // Локальные компоненты
    single { PreferencesManager(androidContext()) }
    single { CartCache(get()) }
    single { NotificationService(androidContext()) }

    // Сервисы данных
    single { FirebaseAuthSource(get()) }
    single { FirestoreSource(get()) }
    single { StorageSource(get()) }
}

/**
 * Модуль источников данных и репозиториев
 */
val dataModule = module {
    // Репозитории
    single { UserRepository(get(), get()) }
    single { ProductRepository(get(), get()) }
    single { CategoryRepository(get(), get()) }
    single { CartRepository(get(), get()) }
    single { OrderRepository(get(), get(), get()) }
}

/**
 * Модуль ViewModel-ей
 */
val viewModelModule = module {
    // Авторизация
    viewModel { AuthViewModel(get(), get()) }

    // Пользовательские экраны
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { CatalogViewModel(get(), get()) }
    viewModel { ProductViewModel(get(), get()) }
    viewModel { CartViewModel(get(), get()) }
    viewModel { OrderViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }

    // Административные экраны
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { AdminProductViewModel(get(), get()) }
    viewModel { CategoryViewModel(get()) }
    viewModel { AdminOrderViewModel(get()) }
    viewModel { AnalyticsViewModel(get(), get()) }
}