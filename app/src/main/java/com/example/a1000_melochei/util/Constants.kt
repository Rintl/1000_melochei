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

    // Константы для каналов уведомлений
    const val NOTIFICATION_CHANNEL_ORDER_ID = "channel_orders"
    const val NOTIFICATION_CHANNEL_PROMO_ID = "channel_promo"
    const val NOTIFICATION_CHANNEL_SYSTEM_ID = "channel_system"

    // Константы уведомлений
    const val NOTIFICATION_ID_ORDER = 1001
    const val NOTIFICATION_ID_PROMO = 1002
    const val NOTIFICATION_ID_SYSTEM = 1003

    // Константы для работы с корзиной
    const val CART_CACHE_FILE = "cart_cache.json"
    const val MAX_CART_ITEMS = 99
    const val MIN_ORDER_AMOUNT = 5000.0 // Минимальная сумма заказа в тенге

    // Валидация данных
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_PHONE_LENGTH = 10
    const val PHONE_REGEX = "^\\+?[0-9]{10,15}$"
    const val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"

    // Константы для пагинации
    const val PAGE_SIZE = 20
    const val INITIAL_LOAD_SIZE = 40

    // Константы для изображений
    const val MAX_IMAGE_SIZE_MB = 5
    const val IMAGE_QUALITY = 80
    const val THUMBNAIL_SIZE = 300

    // Константы для доставки
    const val FREE_DELIVERY_THRESHOLD = 20000.0 // Бесплатная доставка от суммы в тенге
    const val DELIVERY_COST_PER_KM = 100.0 // Стоимость доставки за км в тенге
    const val MAX_DELIVERY_DISTANCE = 50 // Максимальное расстояние доставки в км

    // Форматирование
    const val CURRENCY_SYMBOL = "₸"
    const val DATE_FORMAT_DISPLAY = "dd.MM.yyyy"
    const val DATE_TIME_FORMAT_DISPLAY = "dd.MM.yyyy HH:mm"
    const val DATE_FORMAT_API = "yyyy-MM-dd"

    // Таймауты сети
    const val NETWORK_TIMEOUT = 30L // секунды
    const val CACHE_TIMEOUT = 300L // 5 минут в секундах

    // Константы для экспорта/импорта
    const val EXPORT_FILE_PREFIX = "melochei_export_"
    const val IMPORT_FILE_EXTENSIONS = arrayOf("csv", "xlsx", "xls")

    // Максимальные значения
    const val MAX_PRODUCT_NAME_LENGTH = 100
    const val MAX_PRODUCT_DESCRIPTION_LENGTH = 1000
    const val MAX_CATEGORY_NAME_LENGTH = 50
    const val MAX_ORDER_ITEMS = 50

    // Статусы заказов
    const val ORDER_STATUS_PENDING = "pending"
    const val ORDER_STATUS_CONFIRMED = "confirmed"
    const val ORDER_STATUS_PROCESSING = "processing"
    const val ORDER_STATUS_READY = "ready"
    const val ORDER_STATUS_DELIVERING = "delivering"
    const val ORDER_STATUS_DELIVERED = "delivered"
    const val ORDER_STATUS_CANCELLED = "cancelled"

    // Методы доставки
    const val DELIVERY_METHOD_PICKUP = "pickup"
    const val DELIVERY_METHOD_DELIVERY = "delivery"

    // Методы оплаты
    const val PAYMENT_METHOD_CASH = "cash"
    const val PAYMENT_METHOD_CARD = "card"
    const val PAYMENT_METHOD_KASPI = "kaspi"

    // Роли пользователей
    const val USER_ROLE_CUSTOMER = "customer"
    const val USER_ROLE_ADMIN = "admin"
    const val USER_ROLE_MODERATOR = "moderator"

    // Типы продуктов
    const val PRODUCT_TYPE_REGULAR = "regular"
    const val PRODUCT_TYPE_FEATURED = "featured"
    const val PRODUCT_TYPE_SALE = "sale"

    // Уровни логирования
    const val LOG_LEVEL_DEBUG = "debug"
    const val LOG_LEVEL_INFO = "info"
    const val LOG_LEVEL_WARNING = "warning"
    const val LOG_LEVEL_ERROR = "error"
}