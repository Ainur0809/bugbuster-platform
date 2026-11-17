/* ======================================================================
   BugBuster Alchemy — бизнес-логика магазина (Vanilla JS, ES6+, без сборки)
   ====================================================================== */

/**
 * 🔥 CHAOS_MODE — «рубильник» для живого демо на защите.
 *
 *  false → нормальная работа: промокод DEPLOYFRIDAY ВЫЧИТАЕТ 20% из суммы заказа.
 *  true  → имитация бага в проде: тот же промокод ПРИБАВЛЯЕТ +20% к сумме.
 *
 * Внешне всё выглядит прилично (сообщение «−20% применено»), поэтому баг
 * не заметен глазами — но его ловит автотест E2E-покупки с промокодом.
 * Сценарий демо: true → commit → push в main → GitHub Actions деплоит сайт →
 * автотесты падают → в Telegram прилетает «🚨 Баги в проде!» со ссылкой на Allure.
 */
const CHAOS_MODE = false;

/** Промокод и размер скидки. */
const PROMO_CODE = 'DEPLOYFRIDAY';
const PROMO_RATE = 0.2;

/** Ключ хранения корзины в localStorage. */
const STORAGE_KEY = 'bugbuster-inventory-v2';

/** Справочник категорий: ключ фильтра → подпись и иконка. */
const CATEGORIES = {
  potions: { label: 'Зелье', icon: '🧪' },
  artifacts: { label: 'Артефакт', icon: '💎' },
  scrolls: { label: 'Свиток', icon: '📜' }
};

/** Справочник редкостей. */
const RARITIES = {
  legendary: 'Legendary',
  epic: 'Epic',
  rare: 'Rare',
  cursed: 'Cursed'
};

/** Ассортимент лавки. Цены — в сторипоинтах (SP). */
const PRODUCTS = [
  {
    id: 'anti-burnout-elixir',
    title: 'Эликсир анти-выгорания',
    description: 'Мгновенно восстанавливает ману после трёхчасовых созвонов.',
    price: 150,
    category: 'potions',
    rarity: 'rare',
    emoji: '🧪',
    stat: 'MANA +100'
  },
  {
    id: 'works-locally-scroll',
    title: 'Свиток «У меня локально работает»',
    description: 'Накладывает щит непробиваемости от гнева тимлида и девопса.',
    price: 300,
    category: 'scrolls',
    rarity: 'epic',
    emoji: '📜',
    stat: 'DEF +∞'
  },
  {
    id: 'friday-deploy-amulet',
    title: 'Амулет деплоя в пятницу',
    description: 'Повышает удачу на +99% при релизе в 18:59.',
    price: 500,
    category: 'artifacts',
    rarity: 'legendary',
    emoji: '🧿',
    stat: 'LUCK +99%'
  },
  {
    id: 'junior-tears-coffee',
    title: 'Кофе слёз джуна',
    description: 'Даёт эффект бессонницы на 48 часов и +200% к скорости печати.',
    price: 80,
    category: 'potions',
    rarity: 'cursed',
    emoji: '☕',
    stat: 'WPM +200%'
  },
  {
    id: 'omniscient-rubber-duck',
    title: 'Резиновая уточка всезнания',
    description: 'Древний артефакт. Решает любой баг за 5 минут молчаливого взгляда.',
    price: 250,
    category: 'artifacts',
    rarity: 'legendary',
    emoji: '🦆',
    stat: 'DEBUG ×10'
  },
  {
    id: 'daily-invisibility-cloak',
    title: 'Плащ-невидимка от дейликов',
    description: 'Позволяет незаметно уйти со статуса, оставив в чате «Всё по плану, блокеров нет».',
    price: 400,
    category: 'artifacts',
    rarity: 'epic',
    emoji: '🧥',
    stat: 'STEALTH 100%'
  },
  {
    id: 'instant-approve-scroll',
    title: 'Свиток мгновенного апрува PR',
    description: 'Заставляет ревьюеров написать «LGTM» без единого комментария.',
    price: 350,
    category: 'scrolls',
    rarity: 'cursed',
    emoji: '✅',
    stat: 'REVIEW −100%'
  }
];

/** Состояние приложения. */
const state = {
  filter: 'all',
  query: '',
  cart: loadCart(), // [{ id, qty }]
  promoApplied: false
};

/* ----------------------------------------------------------------------
   Помощники
   ---------------------------------------------------------------------- */

/** Находит один элемент по data-testid. */
const $ = (testId, root = document) => root.querySelector(`[data-testid="${testId}"]`);

/** Форматирует число как цену: 1200 → «1 200 SP». */
const formatSP = (value) => `${Math.abs(value).toLocaleString('ru-RU').replace(/[  ]/g, ' ')} SP`;

/** Ищет товар по id. */
const findProduct = (id) => PRODUCTS.find((p) => p.id === id);

/** Экранирует пользовательский текст перед вставкой в HTML. */
const escapeHtml = (text) =>
  String(text).replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch]);

/* ----------------------------------------------------------------------
   Хранилище корзины
   ---------------------------------------------------------------------- */

/** Загружает корзину из localStorage (с защитой от приватного режима и мусора). */
function loadCart() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    return Array.isArray(parsed)
      ? parsed.filter((i) => PRODUCTS.some((p) => p.id === i.id) && Number.isInteger(i.qty) && i.qty > 0)
      : [];
  } catch {
    return [];
  }
}

/** Сохраняет корзину в localStorage. */
function saveCart() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.cart));
  } catch {
    /* хранилище недоступно — корзина живёт только в памяти */
  }
}

/* ----------------------------------------------------------------------
   Расчёты
   ---------------------------------------------------------------------- */

/**
 * Считает итоги корзины.
 * @returns {{subtotal:number, discount:number, total:number, count:number}}
 */
function calculateTotals() {
  const subtotal = state.cart.reduce((sum, item) => sum + findProduct(item.id).price * item.qty, 0);
  const count = state.cart.reduce((sum, item) => sum + item.qty, 0);
  const discount = state.promoApplied ? Math.round(subtotal * PROMO_RATE) : 0;

  // ⚠️ Здесь «живёт» баг для демо: в CHAOS_MODE скидка превращается в наценку.
  const total = CHAOS_MODE ? subtotal + discount : subtotal - discount;

  return { subtotal, discount, total, count };
}

/* ----------------------------------------------------------------------
   Каталог
   ---------------------------------------------------------------------- */

/** Возвращает товары с учётом выбранной категории и строки поиска (по названию). */
function getVisibleProducts() {
  const q = state.query.trim().toLowerCase();
  return PRODUCTS.filter(
    (p) => (state.filter === 'all' || p.category === state.filter) && (!q || p.title.toLowerCase().includes(q))
  );
}

/** Создаёт HTML одной карточки товара. */
function productCardHtml(p, index) {
  const cat = CATEGORIES[p.category];
  return `
    <article class="product-card rarity-${p.rarity}" style="animation-delay:${index * 45}ms"
             data-testid="product-card" data-product-id="${p.id}" data-category="${p.category}" data-rarity="${p.rarity}">
      <div class="flex items-center justify-between gap-2">
        <span class="cat-badge" data-testid="product-category">${cat.icon} ${cat.label}</span>
        <span class="rarity-badge" data-testid="product-rarity">${RARITIES[p.rarity]}</span>
      </div>
      <div class="flask" aria-hidden="true"><span class="flask-emoji">${p.emoji}</span></div>
      <h3 class="text-base font-bold leading-snug mb-1.5" data-testid="product-title">${p.title}</h3>
      <p class="text-sm text-slate-400 leading-relaxed flex-1" data-testid="product-description">${p.description}</p>
      <div class="mt-3 mb-4"><span class="stat-chip" data-testid="product-stat">${p.stat}</span></div>
      <div class="flex items-center justify-between gap-3">
        <span class="font-mono text-xl font-bold price-glow" data-testid="product-price">${formatSP(p.price)}</span>
        <button type="button" class="btn btn-primary btn-add" data-testid="add-to-cart-btn" data-id="${p.id}"
                aria-label="Добавить «${p.title}» в инвентарь">+ В инвентарь</button>
      </div>
    </article>`;
}

/** Перерисовывает сетку товаров, счётчик найденного и пустое состояние. */
function renderCatalog() {
  const products = getVisibleProducts();
  $('product-grid').innerHTML = products.map(productCardHtml).join('');
  $('products-count').textContent = String(products.length);
  $('empty-catalog').classList.toggle('hidden', products.length > 0);
}

/** Переключает активный фильтр категорий. */
function setFilter(filter) {
  state.filter = filter;
  document.querySelectorAll('[data-filter]').forEach((btn) => {
    const active = btn.dataset.filter === filter;
    btn.classList.toggle('is-active', active);
    btn.setAttribute('aria-pressed', String(active));
  });
  renderCatalog();
}

/* ----------------------------------------------------------------------
   Корзина
   ---------------------------------------------------------------------- */

/** Добавляет товар в корзину (или увеличивает количество на 1). */
function addToCart(id, button) {
  const existing = state.cart.find((i) => i.id === id);
  if (existing) existing.qty += 1;
  else state.cart.push({ id, qty: 1 });

  saveCart();
  renderCart();
  bump($('cart-badge'), 'is-bumped');
  if (button) bump(button, 'is-added');

  const p = findProduct(id);
  showToast(`${p.emoji} «${p.title}» добавлен в инвентарь`);
}

/** Меняет количество позиции на delta. При 0 позиция удаляется. */
function changeQty(id, delta) {
  const item = state.cart.find((i) => i.id === id);
  if (!item) return;
  item.qty += delta;
  if (item.qty <= 0) state.cart = state.cart.filter((i) => i.id !== id);
  afterCartChange();
}

/** Удаляет позицию целиком. */
function removeItem(id) {
  const p = findProduct(id);
  state.cart = state.cart.filter((i) => i.id !== id);
  afterCartChange();
  showToast(`🗑 «${p.title}» выброшен из инвентаря`);
}

/** Полностью очищает корзину. */
function clearCart() {
  state.cart = [];
  afterCartChange();
  showToast('🧹 Инвентарь очищен. Как прод после rollback.');
}

/** Общие действия после изменения корзины. */
function afterCartChange() {
  if (state.cart.length === 0) resetPromo();
  saveCart();
  renderCart();
}

/** Создаёт HTML строки корзины. */
function cartRowHtml(item) {
  const p = findProduct(item.id);
  return `
    <li class="cart-row" data-testid="cart-item" data-product-id="${p.id}">
      <span class="cart-row-icon" aria-hidden="true">${p.emoji}</span>
      <div class="min-w-0">
        <p class="text-sm font-semibold truncate" data-testid="cart-item-name">${p.title}</p>
        <p class="font-mono text-xs text-slate-500">${formatSP(p.price)} / шт.</p>
        <div class="mt-2 flex items-center gap-3">
          <div class="qty-group">
            <button type="button" class="qty-btn" data-testid="decrease-qty-btn" data-action="decrease" data-id="${p.id}" aria-label="Уменьшить количество">−</button>
            <span class="qty-value" data-testid="cart-item-qty">${item.qty}</span>
            <button type="button" class="qty-btn" data-testid="increase-qty-btn" data-action="increase" data-id="${p.id}" aria-label="Увеличить количество">+</button>
          </div>
          <button type="button" class="remove-btn" data-testid="remove-item-btn" data-action="remove" data-id="${p.id}">Удалить</button>
        </div>
      </div>
      <span class="font-mono text-sm font-bold text-right" data-testid="cart-item-subtotal">${formatSP(p.price * item.qty)}</span>
    </li>`;
}

/** Перерисовывает корзину, шапку и все итоги. */
function renderCart() {
  const { subtotal, discount, total, count } = calculateTotals();
  const isEmpty = state.cart.length === 0;

  $('cart-items').innerHTML = state.cart.map(cartRowHtml).join('');
  $('cart-empty').classList.toggle('hidden', !isEmpty);

  // Шапка
  $('cart-badge').textContent = String(count);
  $('cart-header-total').textContent = formatSP(total);

  // Итоги
  $('subtotal-price').textContent = formatSP(subtotal);
  $('discount-row').classList.toggle('hidden', !state.promoApplied);
  $('discount-price').textContent = `${CHAOS_MODE ? '+' : '−'}${formatSP(discount)}`;
  $('total-price').textContent = formatSP(total);
  $('checkout-total').textContent = formatSP(total);

  $('checkout-btn').disabled = isEmpty;
  $('clear-cart-btn').disabled = isEmpty;
}

/* ----------------------------------------------------------------------
   Промокод
   ---------------------------------------------------------------------- */

/** Применяет промокод из поля ввода и показывает статус. */
function applyPromo() {
  const code = $('promo-input').value.trim().toUpperCase();

  if (state.cart.length === 0) {
    state.promoApplied = false;
    setPromoMessage('Сначала положите артефакты в инвентарь', 'error');
  } else if (!code) {
    state.promoApplied = false;
    setPromoMessage('Введите промокод', 'error');
  } else if (code === PROMO_CODE) {
    state.promoApplied = true;
    setPromoMessage('✓ Промокод применён: −20% к заказу', 'success');
    showToast('🎉 DEPLOYFRIDAY активирован!');
  } else {
    state.promoApplied = false;
    setPromoMessage('✗ Промокод недействителен', 'error');
  }
  renderCart();
}

/** Пишет сообщение под полем промокода. state: idle | success | error. */
function setPromoMessage(text, msgState) {
  const msg = $('promo-message');
  msg.textContent = text;
  msg.dataset.state = msgState;
}

/** Сбрасывает промокод. */
function resetPromo() {
  state.promoApplied = false;
  $('promo-input').value = '';
  setPromoMessage('', 'idle');
}

/* ----------------------------------------------------------------------
   Оформление заказа и валидация
   ---------------------------------------------------------------------- */

/** Правила валидации обязательных полей. Возвращают текст ошибки или ''. */
const VALIDATORS = {
  name: (v) => (v.trim().length >= 2 ? '' : 'Назовите своего героя (минимум 2 символа)'),
  email: (v) => (/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v.trim()) ? '' : 'Нужен корректный email для отправки свитка')
};

/** Возвращает элемент ошибки для поля. */
const errorFor = (field) => document.querySelector(`[data-testid="error-message"][data-field="${field}"]`);

/** Проверяет одно поле и подсвечивает ошибку. */
function validateField(field) {
  const input = $(`checkout-${field}`);
  const error = VALIDATORS[field](input.value);
  errorFor(field).textContent = error;
  input.classList.toggle('is-invalid', Boolean(error));
  input.setAttribute('aria-invalid', String(Boolean(error)));
  return !error;
}

/** Проверяет все поля формы. */
function validateForm() {
  return Object.keys(VALIDATORS).map(validateField).every(Boolean);
}

/** Сбрасывает ошибки формы. */
function clearFormErrors() {
  Object.keys(VALIDATORS).forEach((field) => {
    errorFor(field).textContent = '';
    const input = $(`checkout-${field}`);
    input.classList.remove('is-invalid');
    input.removeAttribute('aria-invalid');
  });
}

/** Обработчик отправки формы заказа. */
function submitOrder(event) {
  event.preventDefault();
  if (!validateForm()) {
    showToast('⚠️ Заклинание не сработало: проверьте поля формы', 'error');
    const firstInvalid = document.querySelector('.field.is-invalid');
    if (firstInvalid) firstInvalid.focus();
    return;
  }

  const { total } = calculateTotals();
  const orderId = `BB-${Date.now().toString(36).toUpperCase().slice(-6)}`;

  // Очищаем корзину и форму, закрываем drawer, показываем модалку успеха
  state.cart = [];
  resetPromo();
  saveCart();
  renderCart();
  $('checkout-form').reset();
  clearFormErrors();
  closeCart();

  $('order-id').textContent = orderId;
  $('order-total').textContent = formatSP(total);
  $('order-success-modal').classList.remove('hidden');
  $('close-success-btn').focus();
}

/* ----------------------------------------------------------------------
   Drawer корзины: открытие, шаги, закрытие
   ---------------------------------------------------------------------- */

/** Переключает шаг внутри drawer: 'cart' | 'checkout'. */
function showStep(step) {
  const isCheckout = step === 'checkout';
  $('cart-step').classList.toggle('hidden', isCheckout);
  $('checkout-form').classList.toggle('hidden', !isCheckout);
  $('cart-title').textContent = isCheckout ? '📝 Оформление заказа' : '🎒 Инвентарь героя';
  $('cart-step-label').textContent = isCheckout ? 'шаг 2/2 · данные героя' : 'шаг 1/2 · проверка снаряжения';
  if (isCheckout) $('checkout-name').focus();
}

/** Открывает корзину. */
function openCart() {
  showStep('cart');
  document.body.classList.add('cart-open');
  $('cart-modal').setAttribute('aria-hidden', 'false');
  $('cart-button').setAttribute('aria-expanded', 'true');
}

/** Закрывает корзину. */
function closeCart() {
  document.body.classList.remove('cart-open');
  $('cart-modal').setAttribute('aria-hidden', 'true');
  $('cart-button').setAttribute('aria-expanded', 'false');
  clearFormErrors();
}

/** Закрывает модалку успешного заказа. */
function closeSuccess() {
  $('order-success-modal').classList.add('hidden');
}

/* ----------------------------------------------------------------------
   Микро-взаимодействия
   ---------------------------------------------------------------------- */

/** Показывает тост на 2.5 секунды. type: 'success' | 'error'. */
function showToast(text, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast ${type === 'error' ? 'toast-error' : ''}`;
  toast.dataset.testid = 'toast';
  toast.textContent = text;

  const stack = $('toast-container');
  stack.appendChild(toast);
  while (stack.children.length > 3) stack.firstElementChild.remove();

  setTimeout(() => {
    toast.classList.add('is-leaving');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
  }, 2500);
}

/** Перезапускает CSS-анимацию на элементе. */
function bump(el, className) {
  el.classList.remove(className);
  void el.offsetWidth; // форсируем reflow, чтобы анимация проигралась заново
  el.classList.add(className);
}

/* ----------------------------------------------------------------------
   Инициализация и обработчики событий
   ---------------------------------------------------------------------- */

function init() {
  // Живой поиск
  $('search-input').addEventListener('input', (e) => {
    state.query = e.target.value;
    renderCatalog();
  });

  // Горячая клавиша «/» — фокус в поиск
  document.addEventListener('keydown', (e) => {
    const typing = ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName);
    if (e.key === '/' && !typing) {
      e.preventDefault();
      $('search-input').focus();
    }
    if (e.key === 'Escape') {
      if (!$('order-success-modal').classList.contains('hidden')) closeSuccess();
      else if (document.body.classList.contains('cart-open')) closeCart();
    }
  });

  // Фильтры
  document.querySelectorAll('[data-filter]').forEach((btn) =>
    btn.addEventListener('click', () => setFilter(btn.dataset.filter))
  );

  // «В инвентарь» — делегирование кликов на сетку
  $('product-grid').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-testid="add-to-cart-btn"]');
    if (btn) addToCart(btn.dataset.id, btn);
  });

  // Кнопки внутри списка корзины
  $('cart-items').addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const { id, action } = btn.dataset;
    if (action === 'increase') changeQty(id, +1);
    if (action === 'decrease') changeQty(id, -1);
    if (action === 'remove') removeItem(id);
  });

  // Drawer
  $('cart-button').addEventListener('click', openCart);
  $('cart-close-btn').addEventListener('click', closeCart);
  $('cart-overlay').addEventListener('click', closeCart);

  // Промокод
  $('apply-promo-btn').addEventListener('click', applyPromo);
  $('promo-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') applyPromo();
  });

  // Корзина → оформление
  $('clear-cart-btn').addEventListener('click', clearCart);
  $('checkout-btn').addEventListener('click', () => {
    if (state.cart.length > 0) showStep('checkout');
  });
  $('back-to-cart-btn').addEventListener('click', () => {
    clearFormErrors();
    showStep('cart');
  });

  // Форма: проверка при отправке + «живое» снятие ошибки при исправлении поля
  $('checkout-form').addEventListener('submit', submitOrder);
  Object.keys(VALIDATORS).forEach((field) =>
    $(`checkout-${field}`).addEventListener('input', () => {
      if (errorFor(field).textContent) validateField(field);
    })
  );

  // Модалка успеха
  $('close-success-btn').addEventListener('click', closeSuccess);
  $('order-success-modal').addEventListener('click', (e) => {
    if (e.target === $('order-success-modal')) closeSuccess();
  });

  renderCatalog();
  renderCart();

  if (CHAOS_MODE) console.warn('⚠️ CHAOS_MODE включён: DEPLOYFRIDAY работает как наценка +20%!');
}

document.addEventListener('DOMContentLoaded', init);
