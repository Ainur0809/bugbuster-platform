package com.bugbuster.pages;

import com.bugbuster.utils.PriceParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;

/**
 * Page Object модального окна корзины: позиции, количество, промокод, итоги и кнопки действий.
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
                () -> item(productName).getByTestId("qty-increase").click());
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
                () -> item(productName).getByTestId("qty-decrease").click());
        return this;
    }

    /**
     * Удаляет позицию из корзины кнопкой с корзиной-мусоркой.
     *
     * @param productName название товара в корзине
     * @return этот же Page Object
     */
    public CartModal removeItem(String productName) {
        Allure.step("Удалить из корзины «" + productName + "»",
                () -> item(productName).getByTestId("cart-item-remove").click());
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
            byTestId("promo-apply").click();
        });
        return this;
    }

    /**
     * Нажимает кнопку «Очистить» — удаляет все позиции.
     *
     * @return этот же Page Object
     */
    public CartModal clear() {
        Allure.step("Очистить корзину", () -> byTestId("clear-cart").click());
        return this;
    }

    /**
     * Переходит к форме оформления заказа кнопкой «Оформить заказ».
     *
     * @return Page Object формы оформления заказа
     */
    public CheckoutForm proceedToCheckout() {
        Allure.step("Перейти к оформлению заказа", () -> byTestId("checkout-button").click());
        return new CheckoutForm(page);
    }

    /**
     * Возвращает локатор подытога (сумма без скидки).
     *
     * @return локатор {@code [data-testid=cart-subtotal]}
     */
    public Locator subtotal() {
        return byTestId("cart-subtotal");
    }

    /**
     * Возвращает локатор итоговой суммы к оплате.
     *
     * @return локатор {@code [data-testid=cart-total]}
     */
    public Locator total() {
        return byTestId("cart-total");
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
     * Возвращает локатор сообщения «Корзина пуста».
     *
     * @return локатор {@code [data-testid=cart-empty]}
     */
    public Locator emptyMessage() {
        return byTestId("cart-empty");
    }

    /**
     * Возвращает локатор кнопки «Оформить заказ».
     *
     * @return локатор {@code [data-testid=checkout-button]}
     */
    public Locator checkoutButton() {
        return byTestId("checkout-button");
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
