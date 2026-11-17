package com.bugbuster.tests;

import com.bugbuster.base.BaseTest;
import com.bugbuster.utils.PriceParser;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты оформления заказа: сквозная покупка с промокодом, валидация формы, неверный промокод.
 */
@Epic("BugBuster Alchemy — интернет-магазин")
@Feature("Оформление заказа")
@DisplayName("Оформление заказа")
class CheckoutTest extends BaseTest {

    /** Амулет деплоя в пятницу — 500 SP. */
    private static final String AMULET = "Амулет деплоя";

    /** Эликсир анти-выгорания — 150 SP. */
    private static final String ELIXIR = "Эликсир анти-выгорания";

    /** Кофе слёз джуна — 80 SP. */
    private static final String COFFEE = "Кофе слёз джуна";

    /**
     * Сквозной (E2E) сценарий покупки с промокодом DEPLOYFRIDAY.
     * <p>
     * Амулет (500) + Эликсир (150) = 650 SP → промокод −20% (−130) → 520 SP.
     * Затем заполняем форму и проверяем окно «Заказ призван!» с номером заказа и суммой 520 SP.
     * <p>
     * <b>Именно этот тест «ловит» баг CHAOS_MODE</b>: при наценке итог станет 780 SP.
     */
    @Test
    @Story("Покупка с промокодом")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("E2E: покупка с промокодом DEPLOYFRIDAY даёт скидку 20%")
    @Description("Главный бизнес-сценарий: корзина → промокод → форма → успешный заказ")
    void shouldBuyWithPromoCode() {
        catalog.addToCart(AMULET).addToCart(ELIXIR);
        catalog.openCart();

        int subtotal = cart.subtotalValue();
        Allure.step("Проверить подытог 500 + 150 = 650 SP", () -> assertEquals(650, subtotal));

        cart.applyPromo("DEPLOYFRIDAY");
        int expectedTotal = PriceParser.expectedTotalWithPromo(subtotal);

        Allure.step("Проверить, что промокод дал скидку 20%: ожидаем " + expectedTotal + " SP", () -> {
            assertThat(cart.promoMessage()).containsText("Промокод применён");
            assertEquals(expectedTotal, cart.totalValue(),
                    "Промокод DEPLOYFRIDAY должен уменьшать сумму на 20%! Подытог: " + subtotal);
        });

        cart.proceedToCheckout()
                .fill("@ada.lovelace", "ada@bugbuster.dev", "github.com/guild/prod-on-fire")
                .submit();

        Allure.step("Проверить окно «Заказ призван!»", () -> {
            assertThat(checkout.successScreen()).isVisible();
            assertThat(checkout.orderId()).hasText(Pattern.compile("^BB-[A-Z0-9]{6}$"));
            assertThat(checkout.orderTotal()).hasText(expectedTotal + " SP");
            assertThat(catalog.cartCounter()).hasText("0");
        });
    }

    /**
     * Негативный тест: отправка пустой формы заказа.
     * <p>
     * Ожидание: под обоими обязательными полями (имя и email) появляется текст ошибки,
     * поля подсвечены как невалидные, окно успеха НЕ показано.
     */
    @Test
    @Story("Валидация формы")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Негативный: пустая форма заказа не отправляется")
    void shouldNotSubmitEmptyCheckoutForm() {
        catalog.addToCart(COFFEE);
        catalog.openCart().proceedToCheckout();
        assertThat(checkout.form()).isVisible();

        checkout.submit();

        Allure.step("Проверить ошибки валидации под обязательными полями", () -> {
            assertThat(checkout.error("name")).hasText("Назовите своего героя (минимум 2 символа)");
            assertThat(checkout.error("email")).hasText("Нужен корректный email для отправки свитка");
            assertThat(checkout.input("name")).hasAttribute("aria-invalid", "true");
            assertThat(checkout.input("email")).hasAttribute("aria-invalid", "true");
        });

        Allure.step("Проверить, что заказ НЕ оформлен", () -> {
            assertThat(checkout.successScreen()).isHidden();
            assertThat(checkout.form()).isVisible();
            assertThat(catalog.cartCounter()).hasText("1");
        });
    }

    /**
     * Негативный тест: ввод несуществующего промокода.
     * <p>
     * Ожидание: сообщение «✗ Промокод недействителен», итог равен подытогу (скидки нет).
     */
    @Test
    @Story("Покупка с промокодом")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Негативный: неверный промокод не даёт скидку")
    void shouldRejectInvalidPromoCode() {
        catalog.addToCart(AMULET);
        catalog.openCart();

        cart.applyPromo("FREEBUGS");

        Allure.step("Проверить сообщение об ошибке и отсутствие скидки", () -> {
            assertThat(cart.promoMessage()).containsText("Промокод недействителен");
            assertThat(cart.promoMessage()).hasAttribute("data-state", "error");
            assertThat(cart.total()).hasText("500 SP");
        });
    }
}
