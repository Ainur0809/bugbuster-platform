package com.bugbuster.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import io.qameta.allure.Allure;

import java.util.List;

/**
 * Page Object главной страницы магазина: каталог, поиск, фильтры и кнопка корзины в шапке.
 * <p>
 * Каждое действие обёрнуто в {@code Allure.step(...)}, поэтому в отчёте видна
 * пошаговая история: что именно делал тест перед падением.
 */
public class CatalogPage extends BasePage {

    /**
     * Создаёт Page Object каталога.
     *
     * @param page вкладка Playwright
     */
    public CatalogPage(Page page) {
        super(page);
    }

    /**
     * Открывает магазин по адресу и ждёт, пока отрисуется каталог.
     *
     * @param url адрес магазина (GitHub Pages или локальный file://)
     * @return этот же Page Object для цепочки вызовов
     */
    public CatalogPage open(String url) {
        page.navigate(url);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        productCards().first().waitFor();
        return this;
    }

    /**
     * Возвращает локатор всех видимых карточек товаров.
     *
     * @return локатор карточек {@code [data-testid=product-card]}
     */
    public Locator productCards() {
        return byTestId("product-card");
    }

    /**
     * Находит карточку товара по его названию (частичное совпадение).
     *
     * @param productName название товара или его часть, например {@code "Амулет деплоя"}
     * @return локатор карточки
     */
    public Locator productCard(String productName) {
        return productCards().filter(new Locator.FilterOptions().setHasText(productName));
    }

    /**
     * Возвращает названия всех товаров, которые сейчас отображаются в каталоге.
     *
     * @return список названий в порядке отображения
     */
    public List<String> visibleProductNames() {
        return byTestId("product-title").allTextContents();
    }

    /**
     * Возвращает значения атрибута {@code data-category} всех видимых карточек.
     *
     * @return список категорий: potions / artifacts / scrolls
     */
    public List<String> visibleCategories() {
        return productCards().all().stream()
                .map(card -> card.getAttribute("data-category"))
                .toList();
    }

    /**
     * Вводит текст в строку поиска. Каталог фильтруется «на лету».
     *
     * @param query поисковый запрос, например {@code "уточка"}
     * @return этот же Page Object
     */
    public CatalogPage search(String query) {
        Allure.step("Ввести в поиск: «" + query + "»", () -> byTestId("search-input").fill(query));
        return this;
    }

    /**
     * Нажимает кнопку фильтра по категории.
     *
     * @param category значение фильтра: {@code all}, {@code potions}, {@code artifacts} или {@code scrolls}
     * @return этот же Page Object
     */
    public CatalogPage filterBy(String category) {
        Allure.step("Выбрать фильтр категории: " + category,
                () -> byTestId("filter-" + category).click());
        return this;
    }

    /**
     * Добавляет товар в корзину кнопкой «В инвентарь» на его карточке.
     *
     * @param productName название товара (или уникальная часть названия)
     * @return этот же Page Object
     */
    public CatalogPage addToCart(String productName) {
        Allure.step("Добавить в корзину: «" + productName + "»",
                () -> productCard(productName).getByTestId("add-to-cart-btn").click());
        return this;
    }

    /**
     * Возвращает локатор счётчика товаров на иконке корзины в шапке.
     *
     * @return локатор {@code [data-testid=cart-badge]}
     */
    public Locator cartCounter() {
        return byTestId("cart-badge");
    }

    /**
     * Возвращает локатор сообщения «Артефакт не найден» при пустой выдаче.
     *
     * @return локатор {@code [data-testid=empty-catalog]}
     */
    public Locator emptyCatalogMessage() {
        return byTestId("empty-catalog");
    }

    /**
     * Открывает модальное окно корзины кликом по кнопке в шапке.
     *
     * @return Page Object модального окна корзины
     */
    public CartModal openCart() {
        Allure.step("Открыть корзину", () -> byTestId("cart-button").click());
        return new CartModal(page);
    }
}
