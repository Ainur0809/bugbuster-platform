/* =====================================================================
   BugBuster Alchemy — бизнес-логика магазина (Vanilla JS, без сборки)
   ===================================================================== */

/**
 * 🔥 CHAOS_MODE — «рубильник» для живого демо на защите.
 *
 * false — нормальная работа: промокод DEPLOYFRIDAY даёт СКИДКУ 20%.
 * true  — «случайный» баг: промокод DEPLOYFRIDAY ПРИБАВЛЯЕТ +20% к цене.
 *
 * Сценарий: меняем на true → коммит → push в main → GitHub Actions
 * деплоит сайт → автотест E2E-покупки с промокодом падает → в Telegram
 * прилетает алерт «🚨 Баги в проде!» со ссылкой на Allure-отчёт.
 */
const CHAOS_MODE = false;

/** Промокод и размер скидки (20%). */
const PROMO_CODE = 'DEPLOYFRIDAY';
const PROMO_RATE = 0.2;

/** Ключ, под которым корзина хранится в localStorage. */
const STORAGE_KEY = 'bugbuster-cart-v1';

/**
 * Каталог товаров. Цены — в сторипоинтах (SP).
 * category: potion (зелья) | scroll (свитки) | artifact (артефакты)
 */
const PRODUCTS = [
  { id: 'anti-burnout-elixir', name: 'Эликсир анти-выгорания', description: 'Мгновенно восстанавливает ману после созвонов.', price: 150, category: 'potion', emoji: '🧪' },
  { id: 'works-locally-scroll', name: 'Свиток «У меня локально работает»', description: 'Защищает от гнева тимлида.', price: 300, category: 'scroll', emoji: '📜' },
  { id: 'friday-deploy-amulet', name: 'Амулет деплоя в пятницу', description: 'Повышает удачу на +99%.', price: 500, category: 'artifact', emoji: '🧿' },
  { id: 'junior-tears-coffee', name: 'Кофе слёз джуна', description: 'Даёт бессонницу на 48 часов.', price: 80, category: 'potion', emoji: '☕' },
  { id: 'omniscient-rubber-duck', name: 'Резиновая уточка всезнания', description: 'Решает любой баг за 5 минут.', price: 250, category: 'artifact', emoji: '🦆' },
  { id: 'daily-invisibility-cloak', name: 'Плащ-невидимка от дейликов', description: 'Вас не видно на стендапе. Совсем.', price: 400, category: 'artifact', emoji: '🧥' },
  { id: 'instant-approve-scroll', name: 'Свиток мгновенного апрува', description: 'Ревьюер ставит LGTM, не открывая diff.', price: 200, category: 'scroll', emoji: '✅' },
  { id: 'green-pipeline-potion', name: 'Зелье зелёного пайплайна', description: 'Изгоняет flaky-тесты на целый спринт.', price: 350, category: 'potion', emoji: '🟢' }
];

/** Состояние приложения. */
const state = {
  filter: 'all',
  query: '',
  cart: loadCart(),       // [{ id, qty }]
  promoApplied: false
};

/* ---------- Короткие помощники для DOM ---------- */
const $ = (testId, root = document) => root.querySelector(`[data-testid="${testId}"]`);
const formatSP = (value) => `${value.toLocaleString('ru-RU').replace(/[  ]/g, ' ')} SP`;
const findProduct = (id) => PRODUCTS.find((p) => p.id === id);

/* =====================================================================
   ХРАНИЛИЩЕ
   ===================================================================== */

/** Загружает корзину из localStorage (с защитой от ошибок приватного режима). */
function loadCart() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((i) => findProductSafe(i.id) && i.qty > 0) : [];
  } catch {
    return [];
  }
}

/** Безопасный поиск товара — используется до инициализации helpers. */
function findProductSafe(id) {
  return PRODUCTS.some((p) => p.id === id);
}

/** Сохраняет корзину в localStorage. */
function saveCart() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.cart));
  } catch {
    /* хранилище недоступно — работаем только в памяти */
  }
}

/* =====================================================================
   РАСЧЁТЫ
   ===================================================================== */

/**
 * Считает итоги корзины.
 * @returns {{subtotal:number, discount:number, total:number, count:number}}
 */
function calculateTotals() {
  const subtotal = state.cart.reduce((sum, item) => sum + findProduct(item.id).price * item.qty, 0);
  const count = state.cart.reduce((sum, item) => sum + item.qty, 0);
  const discount = state.promoApplied ? Math.round(subtotal * PROMO_RATE) : 0;

  // ⚠️ Вот здесь «живёт» баг для демо: в CHAOS_MODE скидка превращается в наценку.
  const total = CHAOS_MODE ? subtotal + discount : subtotal - discount;

  return { subtotal, discount, total, count };
}

/* =====================================================================
   КАТАЛОГ
   ===================================================================== */

/** Возвращает товары с учётом активного фильтра и строки поиска. */
function getVisibleProducts() {
  const q = state.query.trim().toLowerCase();
  return PRODUCTS.filter((p) => {
    const byCategory = state.filter === 'all' || p.category === state.filter;
    const byQuery = !q || p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q);
    return byCategory && byQuery;
  });
}

const CATEGORY_LABEL = { potion: 'Зелье', scroll: 'Свиток', artifact: 'Артефакт' };

/** Перерисовывает сетку товаров. */
function renderCatalog() {
  const grid = $('product-grid');
  const products = getVisibleProducts();

  grid.innerHTML = products
    .map(
      (p) => `
      <article class="product-card" data-testid="product-card" data-product-id="${p.id}" data-category="${p.category}">
        <div class="product-emoji" aria-hidden="true">${p.emoji}</div>
        <span class="tag tag-${p.category} self-start mb-2" data-testid="product-category">${CATEGORY_LABEL[p.category]}</span>
        <h3 class="font-display text-base leading-snug mb-2" data-testid="product-name">${p.name}</h3>
        <p class="text-sm text-slate-400 flex-1 mb-4" data-testid="product-description">${p.description}</p>
        <div class="flex items-center justify-between gap-2">
          <span class="font-mono font-bold text-lg neon-text" data-testid="product-price">${formatSP(p.price)}</span>
          <button type="button" class="btn-primary px-3 py-2 rounded-lg text-sm font-semibold"
                  data-testid="add-to-cart-button" data-id="${p.id}">В корзину</button>
        </div>
      </article>`
    )
    .join('');

  $('products-count').textContent = String(products.length);
  $('empty-catalog').classList.toggle('hidden', products.length > 0);
}

/* =====================================================================
   КОРЗИНА
   ===================================================================== */

/** Добавляет товар в корзину (или увеличивает количество). */
function addToCart(id) {
  const existing = state.cart.find((i) => i.id === id);
  if (existing) existing.qty += 1;
  else state.cart.push({ id, qty: 1 });
  saveCart();
  renderCart();
  bumpBadge();
  showToast(`+1 «${findProduct(id).name}» в корзине`);
}

/** Изменяет количество товара на delta (+1 / -1). При 0 — удаляет позицию. */
function changeQty(id, delta) {
  const item = state.cart.find((i) => i.id === id);
  if (!item) return;
  item.qty += delta;
  if (item.qty <= 0) state.cart = state.cart.filter((i) => i.id !== id);
  afterCartChange();
}

/** Удаляет позицию из корзины целиком. */
function removeFromCart(id) {
  state.cart = state.cart.filter((i) => i.id !== id);
  afterCartChange();
}

/** Полностью очищает корзину и сбрасывает промокод. */
function clearCart() {
  state.cart = [];
  afterCartChange();
  showToast('Корзина очищена. Как прод после rollback.');
}

/** Общие действия после любого изменения корзины. */
function afterCartChange() {
  if (state.cart.length === 0) resetPromo();
  saveCart();
  renderCart();
}

/** Перерисовывает корзину, счётчик в шапке и итоги. */
function renderCart() {
  const { subtotal, discount, total, count } = calculateTotals();
  const list = $('cart-items');

  list.innerHTML = state.cart
    .map((item) => {
      const p = findProduct(item.id);
      return `
      <li class="flex items-center gap-3 p-3 rounded-xl bg-void/60 border border-neon-violet/15" data-testid="cart-item" data-product-id="${p.id}">
        <span class="text-2xl" aria-hidden="true">${p.emoji}</span>
        <div class="flex-1 min-w-0">
          <p class="text-sm truncate" data-testid="cart-item-name">${p.name}</p>
          <p class="text-xs font-mono text-slate-500">${formatSP(p.price)} × <span data-testid="cart-item-qty">${item.qty}</span></p>
        </div>
        <div class="flex items-center gap-1">
          <button type="button" class="qty-btn" data-testid="qty-decrease" data-id="${p.id}" aria-label="Уменьшить">−</button>
          <button type="button" class="qty-btn" data-testid="qty-increase" data-id="${p.id}" aria-label="Увеличить">+</button>
        </div>
        <span class="w-20 text-right font-mono text-sm" data-testid="cart-item-subtotal">${formatSP(p.price * item.qty)}</span>
        <button type="button" class="text-slate-500 hover:text-neon-pink" data-testid="cart-item-remove" data-id="${p.id}" aria-label="Удалить">🗑</button>
      </li>`;
    })
    .join('');

  const isEmpty = state.cart.length === 0;
  $('cart-empty').classList.toggle('hidden', !isEmpty);
  $('cart-count').textContent = String(count);
  $('cart-subtotal').textContent = formatSP(subtotal);
  $('cart-total').textContent = formatSP(total);
  $('checkout-total').textContent = formatSP(total);
  $('discount-row').classList.toggle('hidden', !state.promoApplied);
  $('cart-discount').textContent = `${CHAOS_MODE ? '+' : '−'}${formatSP(discount)}`;
  $('checkout-button').disabled = isEmpty;
  $('clear-cart').disabled = isEmpty;
}

/* =====================================================================
   ПРОМОКОД
   ===================================================================== */

/** Применяет промокод из поля ввода. */
function applyPromo() {
  const code = $('promo-input').value.trim().toUpperCase();
  const msg = $('promo-message');

  if (state.cart.length === 0) {
    setPromoMessage('Сначала добавьте товары в корзину', false);
  } else if (code === PROMO_CODE) {
    state.promoApplied = true;
    setPromoMessage('Промокод применён: −20% 🎉', true);
  } else {
    state.promoApplied = false;
    setPromoMessage('Промокод недействителен', false);
  }
  msg.dataset.state = state.promoApplied ? 'success' : 'error';
  renderCart();
}

/** Показывает сообщение под полем промокода. */
function setPromoMessage(text, ok) {
  const msg = $('promo-message');
  msg.textContent = text;
  msg.className = `text-xs mt-2 min-h-[1rem] ${ok ? 'text-neon-green' : 'text-neon-pink'}`;
}

/** Сбрасывает применённый промокод. */
function resetPromo() {
  state.promoApplied = false;
  $('promo-input').value = '';
  setPromoMessage('', true);
}

/* =====================================================================
   ОФОРМЛЕНИЕ ЗАКАЗА + ВАЛИДАЦИЯ
   ===================================================================== */

const VALIDATORS = {
  name: (v) => (v.trim().length >= 2 ? '' : 'Введите имя (минимум 2 символа)'),
  email: (v) => (/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v.trim()) ? '' : 'Введите корректный e-mail'),
  address: (v) => (v.trim().length >= 5 ? '' : 'Введите адрес (минимум 5 символов)')
};

/**
 * Валидирует все поля формы. Подсвечивает ошибки.
 * @returns {boolean} true — если форма валидна.
 */
function validateForm() {
  let valid = true;
  for (const [field, validate] of Object.entries(VALIDATORS)) {
    const input = $(`input-${field}`);
    const error = validate(input.value);
    $(`error-${field}`).textContent = error;
    input.classList.toggle('invalid', Boolean(error));
    input.setAttribute('aria-invalid', String(Boolean(error)));
    if (error) valid = false;
  }
  return valid;
}

/** Обработчик отправки формы заказа. */
function submitOrder(event) {
  event.preventDefault();
  if (!validateForm()) {
    showToast('Форма заполнена с ошибками ⚠️');
    return;
  }
  const { total } = calculateTotals();
  const orderId = `BB-${Date.now().toString(36).toUpperCase().slice(-6)}`;

  $('order-id').textContent = orderId;
  $('order-total').textContent = formatSP(total);
  showStep('success');

  state.cart = [];
  resetPromo();
  saveCart();
  renderCart();
  $('checkout-form').reset();
}

/** Сбрасывает ошибки валидации формы. */
function clearFormErrors() {
  Object.keys(VALIDATORS).forEach((field) => {
    $(`error-${field}`).textContent = '';
    $(`input-${field}`).classList.remove('invalid');
    $(`input-${field}`).removeAttribute('aria-invalid');
  });
}

/* =====================================================================
   МОДАЛЬНОЕ ОКНО
   ===================================================================== */

/** Переключает шаг внутри модалки: 'cart' | 'checkout' | 'success'. */
function showStep(step) {
  $('cart-step').classList.toggle('hidden', step !== 'cart');
  $('checkout-form').classList.toggle('hidden', step !== 'checkout');
  $('order-success').classList.toggle('hidden', step !== 'success');
  $('cart-title').textContent =
    step === 'checkout' ? '📝 Оформление заказа' : step === 'success' ? '✨ Готово' : '🧺 Корзина алхимика';
}

function openCart() {
  showStep('cart');
  $('cart-modal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeCart() {
  $('cart-modal').classList.add('hidden');
  document.body.style.overflow = '';
  clearFormErrors();
}

/* =====================================================================
   UI-МЕЛОЧИ
   ===================================================================== */

let toastTimer;
/** Показывает всплывающее уведомление на 2 секунды. */
function showToast(text) {
  const toast = $('toast');
  toast.textContent = text;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 2000);
}

/** Анимирует бейдж корзины. */
function bumpBadge() {
  const badge = $('cart-count');
  badge.classList.remove('bump');
  void badge.offsetWidth; // перезапуск CSS-анимации
  badge.classList.add('bump');
}

/* =====================================================================
   ИНИЦИАЛИЗАЦИЯ И ОБРАБОТЧИКИ СОБЫТИЙ
   ===================================================================== */

function init() {
  // Поиск
  $('search-input').addEventListener('input', (e) => {
    state.query = e.target.value;
    renderCatalog();
  });

  // Фильтры по категориям
  document.querySelectorAll('[data-filter]').forEach((btn) =>
    btn.addEventListener('click', () => {
      state.filter = btn.dataset.filter;
      document.querySelectorAll('[data-filter]').forEach((b) => {
        b.classList.toggle('active', b === btn);
        b.setAttribute('aria-pressed', String(b === btn));
      });
      renderCatalog();
    })
  );

  // «В корзину» — делегирование событий на сетку
  $('product-grid').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-testid="add-to-cart-button"]');
    if (btn) addToCart(btn.dataset.id);
  });

  // Кнопки внутри списка корзины
  $('cart-items').addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-id]');
    if (!btn) return;
    const { id } = btn.dataset;
    const action = btn.dataset.testid;
    if (action === 'qty-increase') changeQty(id, +1);
    if (action === 'qty-decrease') changeQty(id, -1);
    if (action === 'cart-item-remove') removeFromCart(id);
  });

  // Модалка
  $('cart-button').addEventListener('click', openCart);
  $('cart-close').addEventListener('click', closeCart);
  $('cart-modal').addEventListener('click', (e) => {
    if (e.target === $('cart-modal')) closeCart();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !$('cart-modal').classList.contains('hidden')) closeCart();
  });

  // Промокод
  $('promo-apply').addEventListener('click', applyPromo);
  $('promo-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') applyPromo();
  });

  // Корзина → оформление → успех
  $('clear-cart').addEventListener('click', clearCart);
  $('checkout-button').addEventListener('click', () => {
    if (state.cart.length > 0) showStep('checkout');
  });
  $('back-to-cart').addEventListener('click', () => {
    clearFormErrors();
    showStep('cart');
  });
  $('checkout-form').addEventListener('submit', submitOrder);
  $('continue-shopping').addEventListener('click', closeCart);

  renderCatalog();
  renderCart();

  if (CHAOS_MODE) console.warn('⚠️ CHAOS_MODE включён: промокод работает как наценка!');
}

document.addEventListener('DOMContentLoaded', init);
