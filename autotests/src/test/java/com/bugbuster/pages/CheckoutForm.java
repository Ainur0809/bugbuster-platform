package com.bugbuster.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;

/**
 * Page Object формы оформления заказа (второй шаг в панели корзины)
 * и модального окна «Заказ призван!».
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
     * Заполняет поля формы.
     *
     * @param name    имя героя / ник в Slack (обязательное поле)
     * @param email   email для отправки свитка (обязательное поле)
     * @param comment адрес репозитория или комментарий (необязательное поле)
     * @return этот же Page Object
     */
    public CheckoutForm fill(String name, String email, String comment) {
        Allure.step("Заполнить форму: " + name + ", " + email + ", " + comment, () -> {
            input("name").fill(name);
            input("email").fill(email);
            input("comment").fill(comment);
        });
        return this;
    }

    /**
     * Нажимает кнопку «Призвать заказ».
     *
     * @return этот же Page Object
     */
    public CheckoutForm submit() {
        Allure.step("Нажать «Призвать заказ»", () -> byTestId("submit-order-btn").click());
        return this;
    }

    /**
     * Возвращает локатор текста ошибки под указанным полем.
     * <p>
     * У всех ошибок одинаковый {@code data-testid="error-message"},
     * а поле различается атрибутом {@code data-field}.
     *
     * @param field имя поля: {@code name} или {@code email}
     * @return локатор {@code [data-testid=error-message][data-field=<field>]}
     */
    public Locator error(String field) {
        return page.locator("[data-testid='error-message'][data-field='" + field + "']");
    }

    /**
     * Возвращает локатор поля ввода.
     *
     * @param field имя поля: {@code name}, {@code email} или {@code comment}
     * @return локатор {@code [data-testid=checkout-<field>]}
     */
    public Locator input(String field) {
        return byTestId("checkout-" + field);
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
     * Возвращает локатор модального окна «Заказ призван!».
     *
     * @return локатор {@code [data-testid=order-success-modal]}
     */
    public Locator successScreen() {
        return byTestId("order-success-modal");
    }

    /**
     * Возвращает локатор номера заказа в окне успеха.
     *
     * @return локатор {@code [data-testid=order-id]}
     */
    public Locator orderId() {
        return byTestId("order-id");
    }

    /**
     * Возвращает локатор списанной суммы в окне успеха.
     *
     * @return локатор {@code [data-testid=order-total]}
     */
    public Locator orderTotal() {
        return byTestId("order-total");
    }
}
