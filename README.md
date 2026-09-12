# DiaBAD

Android-приложение для мониторинга глюкозы и звукового оповещения при гипогликемии.

Целевое устройство: **Samsung Galaxy S25 Ultra** (Android 15 / API 35).  
Источник данных: **OtTai** (AAPS Share broadcast).  
Единицы: **ммоль/л**.

## Утверждённая архитектура

| Решение | Выбор |
|--------|--------|
| Источник CGM | OtTai AAPS-broadcast (`Share with AAPS`) |
| Иконка в статус-баре | Bitmap на лету (монохромные цифры + тренд) |
| Структура | Multi-module (`:app`, `:core`, `:data`, `:domain`, `:feature-*`) |
| DI | Hilt |
| UI | Jetpack Compose, One UI / Samsung Health vibe |
| Бренд | DiaBAD (см. `branding/app-icon.png`) |

## Возможности (план)

- Синхронизация сахара из OtTai
- Числовое значение в статус-баре (Foreground Service)
- Фоновый мониторинг, устойчивый к One UI / Doze
- Тревога при гипогликемии (порог по умолчанию 3.9 ммоль/л)
- Кастомные и встроенные звуки тревоги (`USAGE_ALARM`, с обходом DND при разрешении)
- Минималистичный Compose UI: сахар, тренд/график, сигнал, пороги, статус соединения

## Итерации

1. **Core/Data** — модели, репозиторий, приём broadcast OtTai  
2. **Service & Notifications** — FGS, статус-бар Bitmap  
3. **Logic & Alarms** — пороги, USAGE_ALARM, DND  
4. **UI** — Compose screens  

## Локально

Сборка в Android Studio после появления Android-скелета (итерация Core/Data).
