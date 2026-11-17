package com.bugbuster.pages;

import com.bugbuster.utils.PriceParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;

/**
 * Page Object выдвижной панели корзины (drawer): позиции, количество, промокод, итоги и кнопки действий.
 */
public class CartModal extends BasePage {

    /**
     * Создаёт Page Object корзины.
     *
     * @param page вкладка Playwright
     */
    public CartModal(Page page) {
        super(page);
    }

    /**
     * Возвращает локатор самого модального окна.
     *
     * @return локатор {@code [data-testid=cart-modal]}
     */
    public Locator modal() {
        return byTestId("cart-modal");
    }

    /**
     * Возвращает локатор всех позиций в корзине.
     *
     * @return локатор строк {@code [data-testid=cart-item]}
     */
    public Locator items() {
        return byTestId("cart-item");
    }

    /**
     * Находит строку корзины по названию товара.
     *
     * @param productName название товара (или его часть)
     * @return локатор строки корзины
     */
    public Locator item(String productName) {
        return items().filter(new Locator.FilterOptions().setHasText(productName));
    }

    /**
     * Увеличивает количество товара на 1 кнопкой «+».
     *
     * @param productName название товара в корзине
     * @return этот же Page Object
     */
    public CartModal increaseQty(String productName) {
        Allure.step("Увеличить количество «" + productName + "» на 1",
                () -> item(productName).getByTestId("increase-qty-btn").click());
        return this;
    }

    /**
     * Уменьшает количество товара на 1 кнопкой «−».
     *
     * @param productName название товара в корзине
     * @return этот же Page Object
     */
    public CartModal decreaseQty(String productName) {
        Allure.step("Уменьшить количество «" + productName + "» на 1",
                () -> item(productName).getByTestId("decrease-qty-btn").click());
        return this;
    }

    /**
     * Удаляет позицию из корзины кнопкой «Удалить».
     *
     * @param productName название товара в корзине
     * @return этот же Page Object
     */
    public CartModal removeItem(String productName) {
        Allure.step("Удалить из корзины «" + productName + "»",
                () -> item(productName).getByTestId("remove-item-btn").click());
        return this;
    }

    /**
     * Вводит промокод и нажимает «Применить».
     *
     * @param code промокод, например {@code "DEPLOYFRIDAY"}
     * @return этот же Page Object
     */
    public CartModal applyPromo(String code) {
        Allure.step("Применить промокод " + code, () -> {
            byTestId("promo-input").fill(code);
            byTestId("apply-promo-btn").click();
        });
        return this;
    }

    /**
     * Нажимает кнопку «Очистить инвентарь» — удаляет все позиции.
     *
     * @return этот же Page Object
     */
    public CartModal clear() {
        Allure.step("Очистить корзину", () -> byTestId("clear-cart-btn").click());
        return this;
    }

    /**
     * Переходит к форме оформления заказа кнопкой «Оформить».
     *
     * @return Page Object формы оформления заказа
     */
    public CheckoutForm proceedToCheckout() {
        Allure.step("Перейти к оформлению заказа", () -> byTestId("checkout-btn").click());
        return new CheckoutForm(page);
    }

    /**
     * Возвращает локатор подытога (сумма без скидки).
     *
     * @return локатор {@code [data-testid=subtotal-price]}
     */
    public Locator subtotal() {
        return byTestId("subtotal-price");
    }

    /**
     * Возвращает локатор итоговой суммы к оплате.
     *
     * @return локатор {@code [data-testid=total-price]}
     */
    public Locator total() {
        return byTestId("total-price");
    }

    /**
     * Возвращает локатор сообщения под полем промокода.
     *
     * @return локатор {@code [data-testid=promo-message]}
     */
    public Locator promoMessage() {
        return byTestId("promo-message");
    }

    /**
     * Возвращает локатор сообщения «Инвентарь пуст».
     *
     * @return локатор {@code [data-testid=cart-empty]}
     */
    public Locator emptyMessage() {
        return byTestId("cart-empty");
    }

    /**
     * Возвращает локатор кнопки «Оформить».
     *
     * @return локатор {@code [data-testid=checkout-btn]}
     */
    public Locator checkoutButton() {
        return byTestId("checkout-btn");
    }

    /**
     * Читает итоговую сумму из UI и превращает её в число.
     *
     * @return сумма к оплате в SP
     */
    public int totalValue() {
        return PriceParser.parse(total().textContent());
    }

    /**
     * Читает подытог из UI и превращает его в число.
     *
     * @return сумма без скидки в SP
     */
    public int subtotalValue() {
        return PriceParser.parse(subtotal().textContent());
    }
}
