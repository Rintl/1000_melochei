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
    single { CartCache(androidContext()) }
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
    // Репозитории по порядку зависимостей
    single { UserRepository(get<FirebaseAuthSource>(), get<FirestoreSource>()) }
    single { ProductRepository(get<FirestoreSource>(), get<StorageSource>()) }
    single { CategoryRepository(get<FirestoreSource>(), get<StorageSource>()) }
    single { CartRepository(get<FirestoreSource>(), get<CartCache>(), get<FirebaseAuth>()) }
    // OrderRepository зависит от CartRepository, поэтому определяем его последним
    single { OrderRepository(get<FirestoreSource>(), get<CartRepository>(), get<CartCache>()) }
}

/**
 * Модуль ViewModel-ей
 */
val viewModelModule = module {
    // Авторизация
    viewModel { AuthViewModel(get<UserRepository>(), get<PreferencesManager>()) }

    // Пользовательские экраны
    viewModel { HomeViewModel(get<CategoryRepository>(), get<ProductRepository>()) }
    viewModel { CatalogViewModel(get<CategoryRepository>(), get<ProductRepository>()) }
    viewModel { ProductViewModel(get<ProductRepository>(), get<CartRepository>()) }
    viewModel { CartViewModel(get<CartRepository>(), get<UserRepository>()) }
    viewModel { OrderViewModel(get<OrderRepository>()) }
    viewModel { ProfileViewModel(get<UserRepository>(), get<PreferencesManager>()) }

    // Административные экраны
    viewModel { DashboardViewModel(get<OrderRepository>(), get<ProductRepository>(), get<CategoryRepository>()) }
    viewModel { AdminProductViewModel(get<ProductRepository>(), get<CategoryRepository>()) }
    viewModel { CategoryViewModel(get<CategoryRepository>()) }
    viewModel { AdminOrderViewModel(get<OrderRepository>()) }
    viewModel { AnalyticsViewModel(get<OrderRepository>(), get<ProductRepository>()) }
}