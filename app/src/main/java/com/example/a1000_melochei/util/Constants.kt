package com.example.a1000_melochei.util

/**
 * Класс, содержащий константы для всего приложения.
 * Используется для централизованного хранения идентификаторов, ключей, путей и других констант.
 */
object Constants {

    // Общие константы приложения
    const val APP_NAME = "1000 мелочей"
    const val DEFAULT_LANGUAGE = "ru"
    const val DATABASE_VERSION = 1
    const val DATABASE_NAME = "melochei_db"

    // Константы для Firebase
    const val COLLECTION_USERS = "users"
    const val COLLECTION_PRODUCTS = "products"
    const val COLLECTION_CATEGORIES = "categories"
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_PROMOTIONS = "promotions"
    const val COLLECTION_DELIVERY_ZONES = "delivery_zones"
    const val COLLECTION_SETTINGS = "settings"
    const val COLLECTION_CARTS = "carts"

    const val STORAGE_PRODUCTS_FOLDER = "products"
    const val STORAGE_CATEGORIES_FOLDER = "categories"
    const val STORAGE_USER_AVATARS_FOLDER = "user_avatars"

    // Константы для SharedPreferences
    const val PREFERENCES_NAME = "melochei_preferences"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_IS_ADMIN = "is_admin"
    const val PREF_DARK_MODE = "dark_mode"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val PREF_CRASH_REPORTING_ENABLED = "crash_reporting_enabled"
    const val PREF_FCM_TOKEN = "fcm_token"
    const val PREF_LAST_SYNC_TIME = "last_sync_time"

    // Константы для работы с корзиной
    const val CART_CACHE_FILE = "cart_cache.json"
    const val MAX_CART_ITEMS = 99
    const val MIN_ORDER_AMOUNT = 5000.0 // Минимальная сумма заказа в тенге

    // Валидация данных
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_PHONE_LENGTH = 10
    const val PHONE_REGEX = "^\\+?[0-9]{10,15}$"
    const val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"

    // Константы для работы с файлами
    const val TEMP_FILE_PREFIX = "melochei_temp_"
    const val CSV_MIME_TYPE = "text/csv"
    const val EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val PDF_MIME_TYPE = "application/pdf"
    const val IMAGE_QUALITY = 85
    const val MAX_IMAGE_WIDTH = 1200
    const val MAX_IMAGE_HEIGHT = 1200

    // Константы для работы с API
    const val CONNECTION_TIMEOUT = 30000L
    const val READ_TIMEOUT = 30000L
    const val WRITE_TIMEOUT = 30000L
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L

    // Константы для системных настроек
    const val DEFAULT_DELIVERY_COST = 1000.0
    const val FREE_DELIVERY_THRESHOLD = 20000.0

    // Константы для уведомлений
    const val NOTIFICATION_CHANNEL_ORDER_ID = "channel_orders"
    const val NOTIFICATION_CHANNEL_PROMO_ID = "channel_promo"
    const val NOTIFICATION_CHANNEL_SYSTEM_ID = "channel_system"

    // ID уведомлений
    const val NOTIFICATION_NEW_ORDER_ID = 1001
    const val NOTIFICATION_ORDER_STATUS_ID = 1002
    const val NOTIFICATION_PROMO_ID = 2001
    const val NOTIFICATION_SYSTEM_ID = 3001

    // Действия для broadcast receiver
    const val ACTION_UPDATE_DATA = "com.example.a1000_melochei.ACTION_UPDATE_DATA"
    const val ACTION_VIEW_PRODUCT = "com.example.a1000_melochei.ACTION_VIEW_PRODUCT"
    const val ACTION_VIEW_CATEGORY = "com.example.a1000_melochei.ACTION_VIEW_CATEGORY"
    const val ACTION_VIEW_ORDER = "com.example.a1000_melochei.ACTION_VIEW_ORDER"

    // Константы для FileProvider
    const val FILE_PROVIDER_AUTHORITY = "com.example.a1000_melochei.fileprovider"

    // Лимиты и ограничения
    const val MAX_PRODUCT_IMAGES = 5
    const val PRICE_DECIMALS = 2

    // Лимиты для пагинации
    const val DEFAULT_PAGE_SIZE = 20
}