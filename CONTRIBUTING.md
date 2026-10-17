# Как участвовать в разработке

Спасибо за интерес к **Asthma Helper**. Ниже — рабочий процесс проекта.

## Ветки

Используем **GitHub Flow**: `main` всегда в релизо-готовом состоянии, работа идёт в коротких ветках.

| Ветка | Назначение |
| :--- | :--- |
| `main` | Стабильная версия. Прямой push запрещён (branch protection). |
| `feat/<номер-задачи>-краткое-имя` | Новая функциональность |
| `fix/<номер-задачи>-краткое-имя` | Исправление бага |
| `chore/<краткое-имя>` | Инфраструктура, зависимости, документация |

Пример: `feat/12-morning-evening-psv`.

## Коммиты

Формат [Conventional Commits](https://www.conventionalcommits.org/):

```
<тип>(<область>): <краткое описание>
```

Типы: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `perf`.

Примеры:
- `feat(breathing): mark morning/evening PEF measurements`
- `fix(calendar): correct streak calculation on DST day`
- `ci: fail build on ktlint violations`

## Pull Request

1. Создайте ветку от `main` и ведите в ней одну задачу.
2. Перед PR локально прогоните:
   ```bash
   ./gradlew testDebugUnitTest ktlintCheck assembleDebug
   ```
3. Откройте PR, заполните шаблон, свяжите с issue (`Closes #12`).
4. Все проверки CI должны быть зелёными. Слияние — **squash merge**.

## Стиль кода

- Kotlin, стиль проекта задан в `.editorconfig`.
- Проверка и автоисправление:
  ```bash
  ./gradlew ktlintCheck    # проверка
  ./gradlew ktlintFormat   # автоформат
  ```
- Несколько правил: не используйте wildcard-импорты, файлы заканчиваются переводом строки.

## Медицинская логика

Расчёты (стрик, зоны ПСВ, суточная вариабельность, ночные приступы) — ядро продукта.
Любое изменение такой логики сопровождается юнит-тестами в `app/src/test`.

## Релиз

1. Обновите `versionCode`/`versionName` в `app/build.gradle.kts`.
2. Создайте тег `vX.Yb` и запушьте его — GitHub Actions соберёт подписанный APK и приложит к релизу.
3. Опишите изменения в release notes.

## Безопасность

- Никогда не коммитьте `keystore.properties`, `keystore/`, `local.properties`, API-ключи.
- Персональные медицинские данные пользователей не попадают в логи и репозиторий.
