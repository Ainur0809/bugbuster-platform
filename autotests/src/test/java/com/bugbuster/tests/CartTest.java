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

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты корзины: пересчёт суммы при изменении количества и очистка корзины.
 */
@Epic("BugBuster Alchemy — интернет-магазин")
@Feature("Корзина")
@DisplayName("Корзина")
class CartTest extends BaseTest {

    /** Резиновая уточка всезнания — 250 SP. */
    private static final String DUCK = "Резиновая уточка";

    /** Кофе слёз джуна — 80 SP. */
    private static final String COFFEE = "Кофе слёз джуна";

    /** Плащ-невидимка от дейликов — 400 SP. */
    private static final String CLOAK = "Плащ-невидимка";

    /**
     * Проверяет пересчёт суммы корзины при изменении количества товаров.
     * <p>
     * Сценарий: уточка (250) + кофе (80) = 330 → уточку увеличиваем до 3 шт. = 830 →
     * кофе уменьшаем до 0 (позиция исчезает) = 750. Счётчик в шапке = 3.
     */
    @Test
    @Story("Пересчёт суммы")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Сумма корзины пересчитывается при изменении количества")
    void shouldRecalculateTotalWhenQuantityChanges() {
        catalog.addToCart(DUCK).addToCart(COFFEE);
        assertThat(catalog.cartCounter()).hasText("2");

        catalog.openCart();
        Allure.step("Проверить начальную сумму 250 + 80 = 330 SP",
                () -> assertThat(cart.total()).hasText("330 SP"));

        cart.increaseQty(DUCK).increaseQty(DUCK);
        Allure.step("Проверить: 3 уточки × 250 + 80 = 830 SP", () -> {
            assertThat(cart.item(DUCK).getByTestId("cart-item-qty")).hasText("3");
            assertThat(cart.item(DUCK).getByTestId("cart-item-subtotal")).hasText("750 SP");
            assertThat(cart.total()).hasText("830 SP");
        });

        cart.decreaseQty(COFFEE);
        Allure.step("Проверить: кофе удалён, итого 750 SP, в шапке 3 товара", () -> {
            assertThat(cart.items()).hasCount(1);
            assertThat(cart.total()).hasText("750 SP");
            assertThat(catalog.cartCounter()).hasText("3");
            assertEquals(cart.subtotalValue(), cart.totalValue(),
                    "Без промокода подытог и итог должны совпадать");
        });
    }

    /**
     * Проверяет очистку корзины кнопкой «Очистить инвентарь».
     * <p>
     * Ожидание: позиций 0, показано «Инвентарь пуст», итог 0 SP,
     * счётчик в шапке 0, кнопка «Оформить» неактивна.
     */
    @Test
    @Story("Очистка корзины")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Кнопка «Очистить инвентарь» полностью опустошает корзину")
    void shouldClearCart() {
        catalog.addToCart(DUCK).addToCart(COFFEE).addToCart(CLOAK);
        catalog.openCart();
        assertThat(cart.items()).hasCount(3);

        cart.clear();

        Allure.step("Проверить, что корзина пуста", () -> {
            assertThat(cart.items()).hasCount(0);
            assertThat(cart.emptyMessage()).isVisible();
            assertThat(cart.total()).hasText("0 SP");
            assertThat(catalog.cartCounter()).hasText("0");
            assertThat(cart.checkoutButton()).isDisabled();
        });
    }
}
