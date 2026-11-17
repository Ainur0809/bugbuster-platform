/**
 * Allure 3: отчёт только для Telegram-уведомления (allure-notifications).
 * Публичный отчёт на GitHub Pages по-прежнему строит Allure 2 (simple-elf/allure-report-action).
 *
 * historyPath — файл с историей прошлых запусков. CI скачивает его из gh-pages
 * перед генерацией и публикует обратно после, поэтому графики «динамики» на коллаже растут.
 * Пути считаются от корня репозитория (CI запускает генерацию оттуда).
 */
import { defineConfig } from "allure";

export default defineConfig({
  name: "BugBuster Alchemy",
  output: "./allure3-report",
  historyPath: "./allure3-history.jsonl",
  // Два плагина → каждый пишет в свою подпапку (allure3-report/awesome, allure3-report/dashboard).
  // С одним плагином Allure кладёт отчёт прямо в корень, и путь в config.json перестал бы совпадать.
  plugins: {
    awesome: {},
    dashboard: {},
  },
});
