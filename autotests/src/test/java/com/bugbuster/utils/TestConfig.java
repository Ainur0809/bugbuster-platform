package com.bugbuster.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Утилитный класс конфигурации автотестов.
 * <p>
 * Читает системные свойства JVM, которые передаются через Gradle
 * ({@code ./gradlew test -DbaseUrl=... -Dheadless=true}) или из Run Configuration в IntelliJ IDEA.
 * Если свойство не задано — подставляется разумное значение по умолчанию,
 * чтобы тесты запускались «из коробки» на локальной машине.
 */
public final class TestConfig {

    /**
     * Приватный конструктор: класс содержит только статические методы,
     * создавать его экземпляры не нужно.
     */
    private TestConfig() {
    }

    /**
     * Возвращает адрес тестируемого магазина.
     * <p>
     * Порядок поиска:
     * <ol>
     *     <li>системное свойство {@code baseUrl} (так его передаёт CI: адрес GitHub Pages);</li>
     *     <li>локальный файл {@code ../shop/index.html} (запуск из папки autotests);</li>
     *     <li>локальный файл {@code shop/index.html} (запуск из корня монорепозитория).</li>
     * </ol>
     *
     * @return URL главной страницы магазина (http(s):// или file://)
     * @throws IllegalStateException если ни baseUrl, ни локальный файл магазина не найдены
     */
    public static String baseUrl() {
        String fromProperty = System.getProperty("baseUrl");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        List<Path> candidates = List.of(Path.of("..", "shop", "index.html"), Path.of("shop", "index.html"));
        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .map(path -> path.toAbsolutePath().normalize().toUri().toString())
                .orElseThrow(() -> new IllegalStateException(
                        "Не задан -DbaseUrl и не найден локальный shop/index.html"));
    }

    /**
     * Определяет, запускать ли браузер без окна (headless).
     * <p>
     * По умолчанию {@code false}: локально на защите удобно видеть браузер.
     * В CI передаётся {@code -Dheadless=true}.
     *
     * @return {@code true}, если браузер должен работать в headless-режиме
     */
    public static boolean headless() {
        return Boolean.parseBoolean(System.getProperty("headless", "false"));
    }

    /**
     * Возвращает задержку между действиями Playwright в миллисекундах.
     * <p>
     * Полезно для живого демо: {@code -DslowMo=500} замедляет клики,
     * чтобы зрители успевали следить за тестом.
     *
     * @return задержка в мс (0 — без замедления)
     */
    public static double slowMo() {
        return Double.parseDouble(System.getProperty("slowMo", "0"));
    }
}
