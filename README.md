# ⚗️ BugBuster Platform

Монорепозиторий учебного проекта: интернет-магазин **«BugBuster Alchemy»** (алхимическая лавка для IT-шников)
и автотесты к нему на **Java 21 + Gradle + Playwright + JUnit 5 + Allure**, связанные единым CI/CD.

| Что | Где |
|---|---|
| 🛒 Магазин | `https://<username>.github.io/bugbuster-platform/` |
| 📈 Allure-отчёт | `https://<username>.github.io/bugbuster-platform/allure/` |
| ⚙️ Пайплайн | вкладка **Actions** → «CI/CD — Deploy Shop & Run Autotests» |

## Структура

```text
bugbuster-platform/
├── .github/workflows/ci-cd-pipeline.yml   # деплой магазина → автотесты → Allure → Telegram
├── shop/                                  # Участник 1: HTML + Tailwind (CDN) + Vanilla JS
│   ├── index.html
│   ├── style.css
│   └── app.js                             # const CHAOS_MODE = false;  ← рубильник бага для демо
├── autotests/                             # Участник 2: открыть эту папку в IntelliJ IDEA
│   ├── build.gradle                       # JDK 21 toolchain, Playwright, JUnit 5, Allure, задача playwrightInstall
│   ├── settings.gradle
│   ├── gradlew / gradlew.bat / gradle/wrapper/   # Gradle Wrapper 8.10.2
│   └── src/test/java/com/bugbuster/
│       ├── base/BaseTest.java             # жизненный цикл браузера, @ExtendWith(ScreenshotWatcher)
│       ├── pages/                         # Page Object Model
│       │   ├── BasePage.java
│       │   ├── CatalogPage.java
│       │   ├── CartModal.java
│       │   └── CheckoutForm.java
│       ├── utils/
│       │   ├── TestConfig.java            # baseUrl / headless / slowMo из -D свойств
│       │   ├── PriceParser.java           # "1 200 SP" → 1200, расчёт скидки
│       │   └── ScreenshotWatcher.java     # скриншот + HTML в Allure при падении
│       └── tests/
│           ├── CatalogTest.java           # фильтр, поиск, пустая выдача
│           ├── CartTest.java              # пересчёт суммы, очистка корзины
│           └── CheckoutTest.java          # E2E с промокодом, пустая форма, неверный промокод
└── docs/DEFENSE_SCRIPT.md                 # сценарий защиты вдвоём
```

## Быстрый старт

### Магазин (Участник 1)
Открыть `shop/index.html` в браузере — сборка не нужна.

### Автотесты (Участник 2)
1. IntelliJ IDEA → **Open** → папка `autotests` (как Gradle-проект). Project SDK — **21**.
2. Вкладка **Gradle → Tasks → playwright → playwrightInstall** (один раз скачает Chromium).
3. Запуск всех тестов против локального `../shop/index.html` с видимым браузером:
   ```bash
   ./gradlew clean test
   ```
4. Против задеплоенного сайта, без окна браузера:
   ```bash
   ./gradlew clean test -DbaseUrl=https://<username>.github.io/bugbuster-platform/ -Dheadless=true
   ```
5. Замедленно — для демонстрации зрителям: `-DslowMo=400`.
6. Локальный Allure-отчёт: `./gradlew allureServe`.

## Настройка GitHub (один раз)

1. Создать **публичный** репозиторий `bugbuster-platform`, запушить код в `main`.
2. **Settings → Actions → General → Workflow permissions** → *Read and write permissions*.
3. Дождаться первого запуска пайплайна (появится ветка `gh-pages`), затем
   **Settings → Pages → Deploy from a branch → `gh-pages` / `(root)`**. Перезапустить пайплайн вручную.
4. Telegram:
   - создать бота у [@BotFather](https://t.me/BotFather) → получить токен;
   - создать канал, добавить бота **администратором**;
   - узнать `chat_id` канала (например, переслать сообщение из канала боту `@getidsbot`, вида `-100…`);
   - **Settings → Secrets and variables → Actions**: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`.

   Какие сообщения приходят в канал:

   | Когда | Сообщение | Job |
   |---|---|---|
   | пайплайн стартовал (push, расписание, вручную) | 🚀 «Новая версия в пути!» | `notify-start` |
   | тесты закончились (прошли или упали) | 📊 коллаж из 7 графиков + статистика + ссылки на Allure и лог CI ([allure-notifications](https://github.com/qa-guru/allure-notifications)) | `run-autotests` |
   | упал деплой магазина (тесты не запускались) | 💥 «Деплой магазина упал!» | `notify-deploy-failure` |

## Демо-баг

В `shop/app.js` поменять `const CHAOS_MODE = false;` на `true` и запушить в `main`:
промокод `DEPLOYFRIDAY` начнёт **прибавлять** 20% вместо скидки → упадёт E2E-тест
`CheckoutTest.shouldBuyWithPromoCode` → в Telegram придёт коллаж с упавшим тестом (красный сектор на круговой диаграмме), а в Allure — скриншот падения.
