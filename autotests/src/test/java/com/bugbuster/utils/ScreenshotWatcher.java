package com.bugbuster.utils;

import com.bugbuster.base.BaseTest;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * JUnit 5 расширение, которое при падении теста прикладывает к Allure-отчёту
 * скриншот страницы, её HTML и URL.
 * <p>
 * <b>Почему два интерфейса?</b> Методы {@link TestWatcher} вызываются уже ПОСЛЕ {@code @AfterEach},
 * когда {@link BaseTest} закрыл браузерный контекст — делать скриншот в этот момент поздно.
 * Поэтому:
 * <ol>
 *     <li>{@link #afterTestExecution(ExtensionContext)} срабатывает сразу после тела теста
 *         (страница ещё открыта) и, если тест упал, «фотографирует» страницу в хранилище JUnit;</li>
 *     <li>{@link #testFailed(ExtensionContext, Throwable)} из {@link TestWatcher}
 *         достаёт эти данные и прикрепляет их в Allure.</li>
 * </ol>
 * Подключается к тестам через {@code @ExtendWith(ScreenshotWatcher.class)} в {@link BaseTest}.
 */
public class ScreenshotWatcher implements TestWatcher, AfterTestExecutionCallback {

    /** Пространство имён для хранения данных этого расширения внутри JUnit. */
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ScreenshotWatcher.class);

    /** Ключ, под которым сохраняется скриншот (массив байт PNG). */
    private static final String SCREENSHOT_KEY = "screenshot";

    /** Ключ, под которым сохраняется HTML страницы. */
    private static final String HTML_KEY = "html";

    /** Ключ, под которым сохраняется URL страницы в момент падения. */
    private static final String URL_KEY = "url";

    /**
     * Вызывается JUnit сразу после выполнения тела теста, до {@code @AfterEach}.
     * <p>
     * Если тест завершился исключением (упала проверка или произошла ошибка),
     * делает полностраничный скриншот и сохраняет HTML и URL страницы,
     * чтобы позже приложить их к отчёту.
     *
     * @param context контекст текущего теста JUnit (даёт доступ к экземпляру теста и исключению)
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        currentPage(context).ifPresent(page -> {
            ExtensionContext.Store store = context.getStore(NAMESPACE);
            try {
                store.put(SCREENSHOT_KEY, page.screenshot(new Page.ScreenshotOptions().setFullPage(true)));
                store.put(HTML_KEY, page.content());
                store.put(URL_KEY, page.url());
            } catch (RuntimeException e) {
                store.put(URL_KEY, "Не удалось снять скриншот: " + e.getMessage());
            }
        });
    }

    /**
     * Вызывается JUnit, когда тест упал.
     * <p>
     * Прикрепляет к текущему тесту в Allure:
     * скриншот (PNG), HTML-код страницы и URL, на котором тест упал.
     *
     * @param context контекст упавшего теста
     * @param cause   исключение, из-за которого тест упал (например, AssertionError)
     */
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        byte[] screenshot = store.get(SCREENSHOT_KEY, byte[].class);
        if (screenshot != null) {
            Allure.addAttachment("📸 Скриншот в момент падения", "image/png",
                    new ByteArrayInputStream(screenshot), "png");
        }
        String html = store.get(HTML_KEY, String.class);
        if (html != null) {
            Allure.addAttachment("🧾 HTML страницы", "text/html",
                    new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), "html");
        }
        String url = store.get(URL_KEY, String.class);
        if (url != null) {
            Allure.addAttachment("🔗 URL страницы", "text/plain", url);
        }
        System.err.printf("❌ Тест «%s» упал: %s%n", context.getDisplayName(), cause.getMessage());
    }

    /**
     * Вызывается JUnit, когда тест прошёл успешно. Пишет строку в консоль.
     *
     * @param context контекст успешного теста
     */
    @Override
    public void testSuccessful(ExtensionContext context) {
        System.out.printf("✅ Тест «%s» пройден%n", context.getDisplayName());
    }

    /**
     * Достаёт открытую страницу Playwright из экземпляра теста.
     *
     * @param context контекст текущего теста
     * @return страница, если тест унаследован от {@link BaseTest} и страница не закрыта
     */
    private Optional<Page> currentPage(ExtensionContext context) {
        return context.getTestInstance()
                .filter(BaseTest.class::isInstance)
                .map(BaseTest.class::cast)
                .map(BaseTest::getPage)
                .filter(page -> !page.isClosed());
    }
}
