<!-- MARKDOWN LINKS & BADGES -->
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-1.7.5-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material_3-1.3.1-757575?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Hilt](https://img.shields.io/badge/Hilt-2.60.1-1E8CBE?style=for-the-badge&logo=dagger&logoColor=white)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/Room-2.8.4-003545?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/room)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.11-48B984?style=for-the-badge&logo=square&logoColor=white)](https://square.github.io/retrofit/)
[![CI](https://github.com/Zama47/asthma_helper/actions/workflows/ci.yml/badge.svg)](https://github.com/Zama47/asthma_helper/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

<br>
<div align="center">
  <a href="https://github.com/Zama47/asthma_helper">
    <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Logo" width="100" height="100">
  </a>

  <h1 align="center">Asthma Helper</h1>
  <p align="center">
    <strong>Ваш персональный помощник в управлении астмой.</strong>
    <br />
    Отслеживание пикфлоуметрии, контроль приема лекарств и мониторинг качества воздуха.
    <br />
    <br />
    <a href="https://github.com/Zama47/asthma_helper/releases"><strong>Скачать APK (Pre-release)</strong></a>
    ·
    <a href="https://github.com/Zama47/asthma_helper/issues"><strong>Сообщить об ошибке</strong></a>
  </p>
</div>
<br />

<details>
  <summary>📖 <strong>Оглавление</strong></summary>
  <ol>
    <li><a href="#-о-проекте">О проекте</a></li>
    <li><a href="#-основные-функции">Основные функции</a></li>
    <li><a href="#-технологический-стек">Технологический стек</a></li>
    <li><a href="#-архитектура-и-структура-проекта">Архитектура и структура проекта</a></li>
    <li><a href="#-внешние-api">Внешние API</a></li>
    <li><a href="#-демонстрация-работы">Демонстрация работы</a></li>
    <li><a href="#-планы-по-развитию">Планы по развитию</a></li>
    <li><a href="#-автор">Автор</a></li>
  </ol>
</details>

---

## 📱 О проекте

**Asthma Helper** — это Android-приложение, созданное, чтобы помочь людям с астмой вести ежедневный контроль над своим состоянием. Оно объединяет четыре ключевых аспекта управления заболеванием:

- **Дыхание**: Трекинг пикфлоуметрии с визуализацией на графике и индикацией зон нормы.
- **Лекарства**: Составление расписания приема с умными напоминаниями.
- **Календарь**: Дневник приступов с тепловой картой, сериями без обострений и SOS-кнопкой.
- **Среда**: Мониторинг погоды, качества воздуха и уровня пыльцы, которые напрямую влияют на самочувствие.

---

## ✨ Основные функции

### 🏠 Главный экран
- **Быстрое добавление** замера пикфлоуметрии (ПФМ).
- **Виджеты** уровня аллергенности и качества воздуха (реальные данные из Open-Meteo).
- **Чек-лист** приема лекарств на сегодня с независимыми отметками о выполнении.
- **Сводка**: последний замер + норма, история записей с цветными индикаторами.

### 🌬️ Дыхание (Трекер ПФМ)
- **График динамики** пикфлоуметрии (собственный рендеринг на Canvas в стиле Material 3).
- **Три зоны нормы** (классическая пикфлоуметрия): зеленая (80–120%), желтая (50–80%), красная (< 50%).
- **Добавление замера** на **произвольную дату и время** (TimePicker).
- **Фильтр** по периоду: день / неделя / месяц / год.
- **Настройка нормы**: вручную или автоматический расчет по формуле (возраст + рост).
- **Карточка суточной вариабельности**: `(вечернее − утреннее) / максимум × 100%` с оценкой контроля астмы.

### 💊 Лекарства
- **Расписание приема**: с возможностью указать время, дозировку и **дни недели**.
- **Готовая база** из **16 предзаполненных популярных препаратов** (Сальбутамол, Будесонид, Серетид и др.).
- **Создание собственных** лекарств с индивидуальными настройками.
- **Умные уведомления-напоминания** через `WorkManager` (проверка расписаний каждые 15 минут).

### 📅 Календарь приступов
- **Тепловая карта месяца**: приступы цветом по тяжести, пропущенные приёмы лекарств — отдельной иконкой.
- **Серия без обострений (стрик)**: счётчик дней с аптеки до последнего приступа.
- **Учёт обострений**: дата/время, тяжесть, триггеры, комментарий — с редактированием и возвратом к нужной дате.
- **SOS-кнопка**: быстрый лог приступа и план первой помощи по рекомендациям GINA.
- **Статистика**: приступы за месяц, в том числе ночные.

### 🌤️ Погода и качество воздуха
- **Текущая погода**: температура, ощущаемая температура, влажность, давление, ветер.
- **Качество воздуха**: европейский индекс AQI (0–100+), концентрация PM2.5, PM10, O₃, NO₂.
- **Реальная пыльца** из Open-Meteo: береза, ольха, злаки, полынь, амброзия.
- **Персональные рекомендации** на основе AQI, уровня пыльцы и силы ветра.
- **Автоматическое определение местоположения** (FusedLocationProvider) + обратное геокодирование для получения названия города.
- **Выбор города вручную**: поиск через геокодинг Open-Meteo, выбор сохраняется между запусками (при недоступности GPS — подсказка и город по умолчанию).

---

## 🛠️ Технологический стек

Проект построен на **Kotlin** с использованием всей мощи современной экосистемы Android.

| Слой | Технологии |
| :--- | :--- |
| **Язык** | [Kotlin 2.2.0](https://kotlinlang.org/) |
| **UI** | [Jetpack Compose](https://developer.android.com/jetpack/compose), [Material 3](https://m3.material.io/) |
| **Архитектура** | **MVVM** + **Clean Architecture** (слои `data` / `domain` / `ui`) |
| **DI** | [Hilt 2.60.1](https://dagger.dev/hilt/) |
| **БД** | [Room 2.8.4](https://developer.android.com/jetpack/androidx/releases/room) (6 сущностей, `Flow`-реактивность) |
| **Сеть** | [Retrofit 2.11](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) |
| **Графики** | Компоновка на Canvas (Jetpack Compose) |
| **Фоновые задачи** | [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) + `HiltWorker` |
| **Геолокация** | [Google Play Services Location](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary) |
| **Сборка** | [AGP 9.1.1](https://developer.android.com/studio/releases/gradle-plugin), [KSP](https://kotlinlang.org/docs/ksp-overview.html), `compileSdk 37`, `minSdk 24` |

---

## 🧩 Архитектура и структура проекта

Проект организован по принципам **Clean Architecture**, что обеспечивает тестируемость, гибкость и независимость от фреймворков.

```mermaid
graph TD
    subgraph Presentation
        UI[Compose Screens] --> VM[ViewModels]
    end

    subgraph Domain
        VM --> UC[Use Cases]
        UC --> RI[Repository Interfaces]
    end

    subgraph Data
        RI --> RImp[Repository Implementations]
        RImp --> Local[Local Data Sources<br/>Room]
        RImp --> Remote[Remote Data Sources<br/>Retrofit]
    end

    style Presentation fill:#f9f,stroke:#333,stroke-width:2px
    style Domain fill:#ccf,stroke:#333,stroke-width:2px
    style Data fill:#cfc,stroke:#333,stroke-width:2px
```

---

## 🎬 Демонстрация работы

| Главный экран | Добавление записи ПФМ | График пикфлоуметрии |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/266d104a-d1d0-476c-a0cd-423802cd076a" width="250" /> | <img src="https://github.com/user-attachments/assets/bca4eac4-c32c-4959-908f-2107e6e443d3" width="250" /> | <img src="https://github.com/user-attachments/assets/cedaf7e0-01c3-476a-9035-0ba5e3702262" width="250" /> |

| Добавление лекарства | Окно лекарств | Погода |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/3f2c1299-02a9-43e6-b5f8-b20e4f9fd7b4" width="250" /> | <img src="https://github.com/user-attachments/assets/79fd58af-a277-452f-8d71-6de247344765" width="250" /> | <img src="https://github.com/user-attachments/assets/cf037f96-0dde-4af9-964b-211872ee8c0f" width="250" /> |

---

## 🗺️ Планы по улучшению

- [ ] **Публикация**: release-подпись, политика конфиденциальности, выход в RuStore
- [ ] **Расширенные уведомления**: настраиваемые интервалы напоминаний, предупреждение о снижении ПФМ
- [ ] **Тестирование**: Unit-тесты для расчётов (стрик, вариабельность) и ViewModel, интеграционные тесты для Room
- [ ] **Расширенные графики**: Сравнение с нормами, тренды, экспорт данных в CSV
- [ ] **Дневник самочувствия**: Привязка записей к замерам ПФМ, теги симптомов
- [ ] **Оффлайн-кэш погоды**: последние данные без интернета
- [ ] **Адаптивный дизайн**: Поддержка планшетов и складных устройств
- [ ] **Мультиязычность**: Поддержка английского и других языков
- [ ] **Виджеты на рабочий стол**: Быстрый доступ к ключевым функциям без открытия приложения

---

## 👤 Автор

**Замышляев Антон Денисович**  

📧 [zama_47@outlook.com](mailto:zama_47@outlook.com)  
📱 [Telegram @Zamaa47](https://t.me/Zamaa47)  
🐙 [GitHub Zama47](https://github.com/Zama47) 
