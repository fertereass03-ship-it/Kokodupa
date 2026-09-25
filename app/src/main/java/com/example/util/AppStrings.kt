package com.example.util

import com.example.data.settings.AppLanguage
import com.example.data.settings.AppSettingsManager

object AppStrings {
    private val isUa: Boolean
        get() = AppSettingsManager.isUkrainian()

    // Navigation Tabs
    val favoritesTitle: String get() = if (isUa) "Обране" else "Избранное"
    val tabHome: String get() = if (isUa) "Головна" else "Главное"
    val tabCatalog: String get() = if (isUa) "Каталог" else "Каталог"
    val tabFavorites: String get() = if (isUa) "Обране" else "Избранное"
    val tabFriends: String get() = if (isUa) "Друзі" else "Друзья"
    val tabProfile: String get() = if (isUa) "Профіль" else "Профиль"

    // Profile Screen
    val profileTitle: String get() = if (isUa) "Профіль" else "Профиль"
    val settingsTitle: String get() = if (isUa) "Налаштування" else "Настройки"
    val appSettingsTitle: String get() = if (isUa) "Налаштування додатка" else "Настройки приложения"
    val playerSettingsTitle: String get() = if (isUa) "Налаштування плеєра" else "Настройки плеера"
    val tabAppSettings: String get() = if (isUa) "Додаток" else "Приложение"
    val tabPlayerSettings: String get() = if (isUa) "Плеєр" else "Плеер"

    // Language & Theme Settings
    val languageSectionTitle: String get() = if (isUa) "Мова інтерфейсу" else "Язык интерфейса"
    val languageDescription: String get() = if (isUa) "Зміна мови для всього додатка та ШІ-помічника" else "Смена языка для всего приложения и ИИ-помощника"
    val themeSectionTitle: String get() = if (isUa) "Тема оформлення" else "Тема оформления"
    val themeDescription: String get() = if (isUa) "Оберіть візуальний стиль додатка" else "Выберите визуальный стиль приложения"

    val themeOriginal: String get() = if (isUa) "Оригінальна (ANIWERTI)" else "Оригинальная (ANIWERTI)"
    val themeLight: String get() = if (isUa) "Світла тема" else "Светлая тема"
    val themeDark: String get() = if (isUa) "Глибока темна" else "Глубокая тёмная"

    // Player Settings
    val autoSkipOpening: String get() = if (isUa) "Автопропуск опенінгів" else "Автопропуск опенингов"
    val autoSkipDesc: String get() = if (isUa) "Пропускати опенінги та ендінги (+90 сек)" else "Пропускать опенинги и эндинги (+90 сек)"
    val seekStepTitle: String get() = if (isUa) "Крок перемотування" else "Шаг перемотки"
    val defaultQualityTitle: String get() = if (isUa) "Якість відео за замовчуванням" else "Качество видео по умолчанию"
    val playbackSpeedTitle: String get() = if (isUa) "Швидкість відтворення" else "Скорость воспроизведения"
    val clearCache: String get() = if (isUa) "Очистити кеш" else "Очистить кэш"
    val cacheClearedSuccess: String get() = if (isUa) "Кеш успішно очищено" else "Кэш успешно очищен"
    val close: String get() = if (isUa) "Закрити" else "Закрыть"
    val save: String get() = if (isUa) "Зберегти" else "Сохранить"
    val cancel: String get() = if (isUa) "Скасувати" else "Отмена"

    // Profile Details & Stats
    val guestUser: String get() = if (isUa) "Гість" else "Гость"
    val notLoggedIn: String get() = if (isUa) "Вхід не виконано" else "Вход не выполнен"
    val loginOrRegister: String get() = if (isUa) "Увійти або зареєструватися" else "Войти или зарегистрироваться"
    val switchAccount: String get() = if (isUa) "Змінити акаунт" else "Сменить аккаунт"
    val editNickname: String get() = if (isUa) "Змінити нікнейм" else "Изменить никнейм"
    val changeAvatar: String get() = if (isUa) "Змінити аватарку" else "Изменить аватарку"
    val logout: String get() = if (isUa) "Вийти з акаунта" else "Выйти из аккаунта"

    val statsWatched: String get() = if (isUa) "Переглянуто" else "Просмотрено"
    val statsFavorites: String get() = if (isUa) "В обраному" else "В избранном"
    val statsTimeSpent: String get() = if (isUa) "Час перегляду" else "Время просмотра"
    val watchedHistoryTitle: String get() = if (isUa) "Історія переглядів" else "История просмотров"
    val viewAll: String get() = if (isUa) "Дивитися всі" else "Смотреть все"
    val emptyHistory: String get() = if (isUa) "Історія перегляду порожня" else "История просмотров пуста"

    // Home Screen
    val watchNow: String get() = if (isUa) "Дивитися" else "Смотреть"
    val continueWatching: String get() = if (isUa) "Продовжити перегляд" else "Продолжить просмотр"
    val popularAnimes: String get() = if (isUa) "Популярне" else "Популярное"
    val newReleases2026: String get() = if (isUa) "Новинки 2026 року 🔥" else "Новинки 2026 года 🔥"
    val recommendedForYou: String get() = if (isUa) "Рекомендації для вас" else "Рекомендации для вас"
    val animeSchedule: String get() = if (isUa) "Розклад онґоїнґів" else "Расписание онгоингов"

    // Catalog Screen
    val catalogTitle: String get() = if (isUa) "Каталог аніме" else "Каталог аниме"
    val searchPlaceholder: String get() = if (isUa) "Пошук аніме..." else "Поиск аниме..."
    val aiAssistantBtn: String get() = if (isUa) "ШІ-помічник" else "ИИ-помощник"
    val filterButton: String get() = if (isUa) "Фільтри" else "Фильтры"
    val filterTitle: String get() = if (isUa) "Фільтри" else "Фильтры"
    val scheduleButton: String get() = if (isUa) "Розклад" else "Расписание"
    val scheduleTab: String get() = if (isUa) "Розклад" else "Расписание"
    val nothingFound: String get() = if (isUa) "Нічого не знайдено" else "Ничего не найдено"
    val resetFilters: String get() = if (isUa) "Скинути фільтри" else "Сбросить фильтры"
    val episodesShort: String get() = if (isUa) "сер." else "сер."
    val filterAll: String get() = if (isUa) "Всі" else "Все"
    val filterNew: String get() = "Новинки"
    val filterPopular: String get() = if (isUa) "Популярне" else "Популярное"
    val filterRecommendations: String get() = if (isUa) "Рекомендації" else "Рекомендации"
    val filterAnons: String get() = if (isUa) "Анонси" else "Анонсы"
    val filterUnwatched: String get() = if (isUa) "Не переглянуто" else "Не просмотрено"
    val filterWatched: String get() = if (isUa) "Переглянуто" else "Просмотрено"
    val filterWatching: String get() = if (isUa) "Дивлюся" else "Смотрю"
    val markAsWatched: String get() = if (isUa) "Позначити як переглянуто" else "Отметить как просмотрено"
    val markAsUnwatched: String get() = if (isUa) "Зняти позначку" else "Снять отметку"
    val markedWatchedToast: String get() = if (isUa) "Позначено як переглянуто ✓" else "Отмечено как просмотрено ✓"
    val markedUnwatchedToast: String get() = if (isUa) "Позначку знято" else "Отметка снята"

    // AI Assistant
    val aiAssistantName: String get() = if (isUa) "ANIWERTI-помічник" else "ANIWERTI-помощник"
    val aiAssistantTitle: String get() = if (isUa) "ШІ-помічник ANIWERTI" else "ИИ-помощник ANIWERTI"
    val aiAssistantGreeting: String get() = if (isUa) {
        "👋 Привіт! Я — **ANIWERTI-помічник**!\n\nТи можеш:\n• 🔍 **Знайти будь-яке аніме** (наприклад: *«знайди аніме Зошит смерті»*)\n• 🖼️ **Знайти аніме за скріншотом** (натисни 📎 скріпку поруч з полем вводу та обери кадр)\n• ❓ **Задати будь-яке питання** (*«хто такий Леві?»*, *«порадь щось схоже на Соло Левелінг»*)\n\nНапиши питання або прикріпи скріншот!"
    } else {
        "👋 Привет! Я — **ANIWERTI-помощник**!\n\nТы можешь:\n• 🔍 **Найти любое аниме** (например: *«найди аниме Тетрадь смерти»*)\n• 🖼️ **Найти аниме по скриншоту** (нажми 📎 скрепку рядом с полем ввода и выбери кадр)\n• ❓ **Задать любой вопрос** (*«кто такой Леви?»*, *«посоветуй похожее на Соло Левелинг»*)\n\nНапиши вопрос или прикрепи скриншот!"
    }
    val aiAnalyzingMessage: String get() = if (isUa) "ШІ-помічник ANIWERTI аналізує запит..." else "ИИ-помощник ANIWERTI анализирует запрос..."
    val aiInputPlaceholder: String get() = if (isUa) "Запитай що завгодно про аніме..." else "Спроси что угодно про аниме..."
    val aiScreenshotAttached: String get() = if (isUa) "Скріншот прикріплено" else "Скриншот прикреплен"
    val aiClearChat: String get() = if (isUa) "Очистити чат" else "Очистить чат"
    val aiApiKeyTitle: String get() = if (isUa) "Ключ Gemini API" else "Ключ Gemini API"
}
