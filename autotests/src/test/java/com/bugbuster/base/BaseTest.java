package com.bugbuster.base;

import com.bugbuster.pages.CartModal;
import com.bugbuster.pages.CatalogPage;
import com.bugbuster.pages.CheckoutForm;
import com.bugbuster.utils.ScreenshotWatcher;
import com.bugbuster.utils.TestConfig;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Базовый класс для всех UI-тестов.
 * <p>
 * Жизненный цикл:
 * <ul>
 *     <li>{@code @BeforeAll} — один Playwright и один браузер Chromium на тестовый класс (быстро);</li>
 *     <li>{@code @BeforeEach} — новый изолированный {@link BrowserContext} на каждый тест
 *         (чистые cookies и localStorage — корзина всегда пустая) и открытие магазина;</li>
 *     <li>{@code @AfterEach} / {@code @AfterAll} — закрытие контекста и браузера.</li>
 * </ul>
 * Расширение {@link ScreenshotWatcher} автоматически прикладывает скриншот в Allure при падении.
 */
@ExtendWith(ScreenshotWatcher.class)
public abstract class BaseTest {

    /** Движок Playwright — общий для всех тестов класса. */
    protected static Playwright playwright;

    /** Запущенный браузер Chromium — общий для всех тестов класса. */
    protected static Browser browser;

    /** Изолированный контекст браузера (аналог «инкогнито») — свой для каждого теста. */
    protected BrowserContext context;

    /** Вкладка браузера, с которой работает тест. */
    protected Page page;

    /** Page Object каталога (главная страница магазина). */
    protected CatalogPage catalog;

    /** Page Object модального окна корзины. */
    protected CartModal cart;

    /** Page Object формы оформления заказа. */
    protected CheckoutForm checkout;

    /**
     * Запускает Playwright и браузер Chromium один раз перед всеми тестами класса.
     * <p>
     * Режим headless и замедление берутся из {@link TestConfig}.
     * Также увеличивается таймаут ожидания проверок {@code assertThat(...)} до 10 секунд,
     * чтобы стабильно работать с сайтом на GitHub Pages.
     */
    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(TestConfig.headless())
                .setSlowMo(TestConfig.slowMo()));
        PlaywrightAssertions.setDefaultAssertionTimeout(10_000);
    }

    /**
     * Перед каждым тестом создаёт новый контекст и вкладку,
     * открывает магазин и инициализирует Page Objects.
     * <p>
     * Обёрнуто в {@code Allure.step}, чтобы шаг «Открыть магазин» был виден в отчёте.
     */
    @BeforeEach
    void openShop() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1440, 900)
                .setLocale("ru-RU"));
        page = context.newPage();
        page.setDefaultTimeout(15_000);

        catalog = new CatalogPage(page);
        cart = new CartModal(page);
        checkout = new CheckoutForm(page);

        String url = TestConfig.baseUrl();
        Allure.step("Открыть магазин BugBuster Alchemy: " + url, () -> catalog.open(url));
    }

    /**
     * После каждого теста закрывает контекст браузера (и все его вкладки).
     * Скриншот при падении к этому моменту уже сделан {@link ScreenshotWatcher}.
     */
    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * После всех тестов класса закрывает браузер и останавливает Playwright.
     */
    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Возвращает текущую вкладку браузера.
     * Используется {@link ScreenshotWatcher} для снятия скриншота при падении теста.
     *
     * @return страница Playwright текущего теста (может быть {@code null}, если тест не стартовал)
     */
    public Page getPage() {
        return page;
    }
}
