/**
 * KA POS v3.0 - Central Reactive State Store & Persistence
 * Guaranteed backward-compatible with KA POS v2.0 LocalStorage & Supabase Schema
 */

const LS_KEYS = {
  kat:   'ka_kat',
  prod:  'ka_prod',
  trx:   'ka_trx',
  kas:   'ka_kaslog',
  sesi:  'ka_sesi',
  pt:    'ka_pt',
  op:    'ka_op',
  dp:    'ka_dp',
  inv:   'ka_inv',
  mut:   'ka_mut',
  st:    'ka_st',
  stlog: 'ka_stlog',
  set:   'ka_set',
  ofq:   'ka_ofq',
  fb:    'ka_fb',
  sb:    'ka_sb'
};

const DEFAULT_CATEGORIES = [
  { id: 'k1', nm: 'Taichan', emj: '🍢', col: '#f97316', on: true, ord: 0 },
  { id: 'k2', nm: 'Chicken', emj: '🍗', col: '#ef4444', on: true, ord: 1 },
  { id: 'k3', nm: 'Minuman', emj: '🧊', col: '#3b82f6', on: true, ord: 2 },
  { id: 'k4', nm: 'Nasi', emj: '🍚', col: '#22c55e', on: true, ord: 3 },
  { id: 'k5', nm: 'Lainnya', emj: '🍴', col: '#6b7280', on: true, ord: 4 }
];

const DEFAULT_PRODUCTS = [
  { id: 1, nm: 'Taichan Pedas Lv.3', kat: 'Taichan', hr: 18000, md: 5600, emj: '🍢', lv: 'Lv.3', on: true, bom: [{ invId: 1, qty: 0.15 }, { invId: 2, qty: 0.10 }] },
  { id: 2, nm: 'Taichan Original', kat: 'Taichan', hr: 15000, md: 5440, emj: '🍢', lv: '', on: true, bom: [{ invId: 1, qty: 0.15 }, { invId: 2, qty: 0.08 }] },
  { id: 3, nm: 'Taichan Jumbo Lv.2', kat: 'Taichan', hr: 22000, md: 8000, emj: '🍢', lv: 'Lv.2', on: true, bom: [{ invId: 1, qty: 0.22 }, { invId: 2, qty: 0.12 }] },
  { id: 4, nm: 'Chicken Crispy', kat: 'Chicken', hr: 20000, md: 7300, emj: '🍗', lv: '', on: true, bom: [{ invId: 1, qty: 0.20 }, { invId: 3, qty: 0.05 }] },
  { id: 5, nm: 'Chicken Pedas Lv.1', kat: 'Chicken', hr: 20000, md: 7700, emj: '🍗', lv: 'Lv.1', on: true, bom: [{ invId: 1, qty: 0.20 }, { invId: 3, qty: 0.05 }, { invId: 2, qty: 0.05 }] },
  { id: 6, nm: 'Es Teh Manis', kat: 'Minuman', hr: 5000, md: 950, emj: '🧊', lv: '', on: true, bom: [{ invId: 4, qty: 1 }, { invId: 5, qty: 0.15 }] },
  { id: 7, nm: 'Es Jeruk', kat: 'Minuman', hr: 7000, md: 950, emj: '🍊', lv: '', on: true, bom: [{ invId: 4, qty: 1 }, { invId: 5, qty: 0.15 }] },
  { id: 8, nm: 'Air Mineral', kat: 'Minuman', hr: 4000, md: 1200, emj: '💧', lv: '', on: true, bom: [] },
  { id: 9, nm: 'Nasi Putih', kat: 'Nasi', hr: 5000, md: 1800, emj: '🍚', lv: '', on: true, bom: [] },
  { id: 10, nm: 'Nasi Goreng', kat: 'Nasi', hr: 18000, md: 540, emj: '🍳', lv: '', on: true, bom: [{ invId: 3, qty: 0.03 }] }
];

const DEFAULT_INVENTORY = [
  { id: 1, nm: 'Daging Ayam', kat: 'Daging', sat: 'kg', stok: 8, min: 3, hr: 32000, emj: '🍗' },
  { id: 2, nm: 'Bumbu Taichan', kat: 'Bumbu', sat: 'pak', stok: 12, min: 5, hr: 8000, emj: '🌶' },
  { id: 3, nm: 'Minyak Goreng', kat: 'Lainnya', sat: 'liter', stok: 4, min: 3, hr: 18000, emj: '🫙' },
  { id: 4, nm: 'Gelas Cup', kat: 'Kemasan', sat: 'pcs', stok: 200, min: 50, hr: 500, emj: '🥤' },
  { id: 5, nm: 'Es Batu', kat: 'Minuman', sat: 'kg', stok: 2, min: 5, hr: 3000, emj: '🧊' }
];

const DEFAULT_SETTINGS = {
  nm: 'Taichan & Chicken KA',
  addr: 'Bandar Lampung',
  hp: '081234567890',
  footer: 'Terima kasih sudah mampir! Selamat menikmati 🍢',
  pw: '58',
  pc: 'bluetooth',
  ap: false,
  rek: 'a.n. Taichan & Chicken KA',
  norek: 'BCA — 1234567890'
};

const DEFAULT_STAFF = [
  { id: 1, nm: 'Owner (Hendra)', user: 'owner', pw: 'owner123', role: 'owner', hp: '', on: true, pin: '1234' },
  { id: 2, nm: 'Kasir (Budi)', user: 'kasir', pw: 'kasir123', role: 'kasir', hp: '081234567890', on: true, pin: '1111' }
];

class StateManager {
  constructor() {
    this.listeners = new Map();
    this.currentUser = null;
    this.activeView = 'pos';
    this.isLocked = false;
    this.cart = [];
    this.orderType = 'dine-in';
    this.tableNo = '08';

    this.categories = this.load(LS_KEYS.kat, DEFAULT_CATEGORIES);
    this.products   = this.load(LS_KEYS.prod, DEFAULT_PRODUCTS);

    // Ensure products have BOM arrays even if loaded from older localStorage
    this.products.forEach(p => {
      if (!p.bom) {
        const def = DEFAULT_PRODUCTS.find(d => d.id === p.id);
        p.bom = def && def.bom ? JSON.parse(JSON.stringify(def.bom)) : [];
      }
    });
    this.transactions = this.load(LS_KEYS.trx, []);
    this.cashLog    = this.load(LS_KEYS.kas, []);
    this.activeShift = this.load(LS_KEYS.sesi, null);
    this.receivables = this.load(LS_KEYS.pt, []);
    this.onProcessOrders = this.load(LS_KEYS.op, []);
    this.kitchenOrders = this.load(LS_KEYS.dp, []);
    this.inventory  = this.load(LS_KEYS.inv, DEFAULT_INVENTORY);
    this.stockMutations = this.load(LS_KEYS.mut, []);
    this.staff      = this.load(LS_KEYS.st, DEFAULT_STAFF);
    this.staffLog   = this.load(LS_KEYS.stlog, []);
    this.settings   = this.load(LS_KEYS.set, DEFAULT_SETTINGS);
    this.offlineQueue = this.load(LS_KEYS.ofq, []);
    this.supabaseConfig = this.load(LS_KEYS.sb, null);
  }

  load(key, fallback) {
    try {
      const item = localStorage.getItem(key);
      return item !== null ? JSON.parse(item) : fallback;
    } catch (e) {
      console.warn(`[State] Failed to read ${key}:`, e);
      return fallback;
    }
  }

  save(key, data) {
    try {
      localStorage.setItem(key, JSON.stringify(data));
      this.emit(key, data);
    } catch (e) {
      console.error(`[State] Failed to save ${key}:`, e);
      this.toast('Gagal menyimpan data ke memori lokal', 'error');
    }
  }

  saveAll() {
    this.save(LS_KEYS.kat, this.categories);
    this.save(LS_KEYS.prod, this.products);
    this.save(LS_KEYS.trx, this.transactions);
    this.save(LS_KEYS.kas, this.cashLog);
    this.save(LS_KEYS.sesi, this.activeShift);
    this.save(LS_KEYS.pt, this.receivables);
    this.save(LS_KEYS.op, this.onProcessOrders);
    this.save(LS_KEYS.dp, this.kitchenOrders);
    this.save(LS_KEYS.inv, this.inventory);
    this.save(LS_KEYS.mut, this.stockMutations);
    this.save(LS_KEYS.st, this.staff);
    this.save(LS_KEYS.stlog, this.staffLog);
    this.save(LS_KEYS.set, this.settings);
    this.save(LS_KEYS.ofq, this.offlineQueue);
    this.save(LS_KEYS.sb, this.supabaseConfig);
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(cb => cb(data));
    }
  }

  // --- Cart Operations ---
  addToCart(product, options = {}) {
    const modifierText = options.level ? `${options.level}${options.notes ? ', ' + options.notes : ''}` : (options.notes || '');
    const price = Number(product.hr) + Number(options.extraPrice || 0);

    const existingIndex = this.cart.findIndex(item => 
      item.id === product.id && item.mod === modifierText
    );

    if (existingIndex > -1) {
      this.cart[existingIndex].qty += 1;
    } else {
      this.cart.push({
        id: product.id,
        nm: product.nm,
        hr: price,
        md: product.md || 0,
        kat: product.kat || '',
        emj: product.emj || '🍢',
        mod: modifierText,
        qty: 1
      });
    }

    this.emit('cart:change', this.cart);
  }

  updateCartQty(index, delta) {
    if (!this.cart[index]) return;
    this.cart[index].qty += delta;
    if (this.cart[index].qty <= 0) {
      this.cart.splice(index, 1);
    }
    this.emit('cart:change', this.cart);
  }

  clearCart() {
    this.cart = [];
    this.emit('cart:change', this.cart);
  }

  getCartTotal() {
    return this.cart.reduce((sum, item) => sum + (item.hr * item.qty), 0);
  }

  // --- Toast Notification Helper ---
  toast(message, type = 'success') {
    const wrap = document.getElementById('toast-wrap');
    if (!wrap) return;

    const t = document.createElement('div');
    t.className = `toast-msg ${type}`;
    const icon = type === 'error' ? 'error' : (type === 'warning' ? 'warning' : 'check_circle');
    t.innerHTML = `<span class="material-symbols-outlined">${icon}</span><span>${message}</span>`;
    wrap.appendChild(t);

    setTimeout(() => {
      t.style.opacity = '0';
      t.style.transform = 'translateY(10px)';
      t.style.transition = 'all 0.2s';
      setTimeout(() => t.remove(), 250);
    }, 3000);
  }

  // --- Utility & Formatting ---
  formatRp(num) {
    return 'Rp ' + Math.round(num || 0).toLocaleString('id-ID');
  }

  formatRpShort(num) {
    const n = Math.round(num || 0);
    if (n >= 1e6) return 'Rp ' + (n / 1e6).toFixed(1).replace('.0', '') + 'jt';
    if (n >= 1e3) return 'Rp ' + (n / 1e3).toFixed(0) + 'rb';
    return this.formatRp(n);
  }

  formatDate(date) {
    const d = new Date(date || Date.now());
    return d.toLocaleDateString('id-ID', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatDateShort(date) {
    const d = new Date(date || Date.now());
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  async hashPassword(plain) {
    if (!window.crypto || !window.crypto.subtle) return plain;
    try {
      const enc = new TextEncoder();
      const buf = await window.crypto.subtle.digest('SHA-256', enc.encode(plain));
      return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('');
    } catch {
      return plain;
    }
  // --- Recipe BOM & HPP Calculation ---
  calculateProductHPP(product) {
    if (!product) return 0;
    if (product.bom && Array.isArray(product.bom) && product.bom.length > 0) {
      let sum = 0;
      product.bom.forEach(b => {
        const inv = this.inventory.find(i => i.id === b.invId);
        if (inv) {
          sum += (inv.hr || 0) * (b.qty || 0);
        }
      });
      return Math.round(sum);
    }
    return product.md || 0;
  }
}

// Global State Instance
window.State = new StateManager();
