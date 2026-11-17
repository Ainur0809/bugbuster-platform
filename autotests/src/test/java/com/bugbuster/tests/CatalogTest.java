package com.bugbuster.tests;

import com.bugbuster.base.BaseTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты каталога: фильтрация по категориям и поиск товаров.
 */
@Epic("BugBuster Alchemy — интернет-магазин")
@Feature("Каталог товаров")
@DisplayName("Каталог товаров")
class CatalogTest extends BaseTest {

    /**
     * Проверяет фильтрацию каталога по категории «Зелья».
     * <p>
     * Ожидание: после выбора фильтра отображаются ровно 3 товара,
     * и у каждой карточки категория {@code potion}.
     * После возврата к фильтру «Все» снова видны все 8 товаров.
     */
    @Test
    @Story("Фильтрация по категориям")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Фильтр «Зелья» показывает только зелья")
    void shouldFilterProductsByCategory() {
        assertThat(catalog.productCards()).hasCount(8);

        catalog.filterBy("potion");

        Allure.step("Проверить, что видны только 3 зелья", () -> {
            assertThat(catalog.productCards()).hasCount(3);
            List<String> categories = catalog.visibleCategories();
            assertTrue(categories.stream().allMatch("potion"::equals),
                    "В выдаче есть товары не из категории potion: " + categories);
        });

        catalog.filterBy("all");

        Allure.step("Проверить, что фильтр «Все» возвращает полный каталог",
                () -> assertThat(catalog.productCards()).hasCount(8));
    }

    /**
     * Проверяет поиск товара по части названия.
     * <p>
     * Ожидание: по запросу «уточка» находится ровно одна карточка —
     * «Резиновая уточка всезнания» с ценой 250 SP.
     */
    @Test
    @Story("Поиск по каталогу")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Поиск «уточка» находит резиновую уточку")
    void shouldFindProductBySearchQuery() {
        catalog.search("уточка");

        Allure.step("Проверить, что найдена ровно одна уточка за 250 SP", () -> {
            assertThat(catalog.productCards()).hasCount(1);
            assertThat(catalog.productCards().getByTestId("product-name"))
                    .hasText("Резиновая уточка всезнания");
            assertThat(catalog.productCards().getByTestId("product-price")).hasText("250 SP");
        });
    }

    /**
     * Негативная проверка поиска: запрос, которому не соответствует ни один товар.
     * <p>
     * Ожидание: карточек нет, показано сообщение «404: Артефакт не найден».
     */
    @Test
    @Story("Поиск по каталогу")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("Поиск несуществующего товара показывает сообщение 404")
    void shouldShowEmptyStateWhenNothingFound() {
        catalog.search("флешка с продом");

        Allure.step("Проверить пустую выдачу и сообщение 404", () -> {
            assertThat(catalog.productCards()).hasCount(0);
            assertThat(catalog.emptyCatalogMessage()).isVisible();
            assertThat(catalog.emptyCatalogMessage()).containsText("Артефакт не найден");
        });
    }
}
