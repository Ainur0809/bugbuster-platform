package com.bugbuster.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;

/**
 * Page Object формы оформления заказа (второй шаг модального окна корзины)
 * и экрана успешного заказа (третий шаг).
 */
public class CheckoutForm extends BasePage {

    /**
     * Создаёт Page Object формы заказа.
     *
     * @param page вкладка Playwright
     */
    public CheckoutForm(Page page) {
        super(page);
    }

    /**
     * Возвращает локатор самой формы.
     *
     * @return локатор {@code [data-testid=checkout-form]}
     */
    public Locator form() {
        return byTestId("checkout-form");
    }

    /**
     * Заполняет все поля формы.
     *
     * @param name    имя покупателя
     * @param email   e-mail покупателя
     * @param address адрес доставки
     * @return этот же Page Object
     */
    public CheckoutForm fill(String name, String email, String address) {
        Allure.step("Заполнить форму: " + name + ", " + email + ", " + address, () -> {
            byTestId("input-name").fill(name);
            byTestId("input-email").fill(email);
            byTestId("input-address").fill(address);
        });
        return this;
    }

    /**
     * Нажимает кнопку «Подтвердить заказ».
     *
     * @return этот же Page Object
     */
    public CheckoutForm submit() {
        Allure.step("Нажать «Подтвердить заказ»", () -> byTestId("submit-order").click());
        return this;
    }

    /**
     * Возвращает локатор текста ошибки под указанным полем.
     *
     * @param field имя поля: {@code name}, {@code email} или {@code address}
     * @return локатор {@code [data-testid=error-<field>]}
     */
    public Locator error(String field) {
        return byTestId("error-" + field);
    }

    /**
     * Возвращает локатор поля ввода.
     *
     * @param field имя поля: {@code name}, {@code email} или {@code address}
     * @return локатор {@code [data-testid=input-<field>]}
     */
    public Locator input(String field) {
        return byTestId("input-" + field);
    }

    /**
     * Возвращает локатор суммы к оплате над формой.
     *
     * @return локатор {@code [data-testid=checkout-total]}
     */
    public Locator total() {
        return byTestId("checkout-total");
    }

    /**
     * Возвращает локатор экрана «Заказ принят».
     *
     * @return локатор {@code [data-testid=order-success]}
     */
    public Locator successScreen() {
        return byTestId("order-success");
    }

    /**
     * Возвращает локатор номера заказа на экране успеха.
     *
     * @return локатор {@code [data-testid=order-id]}
     */
    public Locator orderId() {
        return byTestId("order-id");
    }

    /**
     * Возвращает локатор списанной суммы на экране успеха.
     *
     * @return локатор {@code [data-testid=order-total]}
     */
    public Locator orderTotal() {
        return byTestId("order-total");
    }
}
