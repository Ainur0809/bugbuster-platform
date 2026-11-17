package com.bugbuster.utils;

/**
 * Утилитный класс для работы с ценами в сторипоинтах (SP).
 * <p>
 * Магазин выводит суммы в виде строк: {@code "1 200 SP"}, {@code "−130 SP"}.
 * Чтобы сравнивать их в проверках, строку нужно превратить в число.
 */
public final class PriceParser {

    /** Размер скидки по промокоду DEPLOYFRIDAY (20%). */
    public static final double PROMO_RATE = 0.2;

    /**
     * Приватный конструктор: класс содержит только статические методы.
     */
    private PriceParser() {
    }

    /**
     * Извлекает целое число из строки с ценой.
     * <p>
     * Удаляет всё, кроме цифр: пробелы-разделители тысяч, знаки «+»/«−» и суффикс «SP».
     * Знак не учитывается — метод возвращает абсолютное значение.
     *
     * @param text строка из UI, например {@code "1 200 SP"}
     * @return число, например {@code 1200}
     * @throws IllegalArgumentException если в строке нет ни одной цифры
     */
    public static int parse(String text) {
        String digits = text == null ? "" : text.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("В строке нет цены: '" + text + "'");
        }
        return Integer.parseInt(digits);
    }

    /**
     * Рассчитывает ожидаемую итоговую сумму после применения промокода -20%.
     * <p>
     * Формула совпадает с бизнес-требованием: скидка округляется до целого SP.
     *
     * @param subtotal сумма корзины без скидки
     * @return ожидаемая сумма к оплате со скидкой
     */
    public static int expectedTotalWithPromo(int subtotal) {
        return subtotal - (int) Math.round(subtotal * PROMO_RATE);
    }
}
