package com.bugbuster.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Базовый Page Object — общий предок всех страниц и компонентов магазина.
 * <p>
 * Хранит ссылку на вкладку Playwright и даёт короткий способ
 * найти элемент по атрибуту {@code data-testid}.
 */
public abstract class BasePage {

    /** Вкладка браузера, с которой работает Page Object. */
    protected final Page page;

    /**
     * Создаёт Page Object для указанной вкладки.
     *
     * @param page вкладка Playwright, полученная в {@code BaseTest}
     */
    protected BasePage(Page page) {
        this.page = page;
    }

    /**
     * Находит элемент по атрибуту {@code data-testid}.
     * <p>
     * Playwright по умолчанию ищет именно {@code data-testid}, поэтому локаторы
     * не зависят от CSS-классов и текста — дизайнер может менять вёрстку без поломки тестов.
     *
     * @param testId значение атрибута, например {@code "total-price"}
     * @return ленивый локатор элемента (поиск происходит в момент действия/проверки)
     */
    protected Locator byTestId(String testId) {
        return page.getByTestId(testId);
    }
}
