/**
 * KA POS v3.0 - 3-Zone Velocity POS Kasir & Modifiers
 */

class POSView {
  constructor() {
    this.selectedCategory = 'all';
    this.searchQuery = '';
    this.activeProductForModifier = null;
    this.selectedModifierLevel = 'Lv.3';
    this.modifierNotes = '';
  }

  init() {
    this.renderCategories();
    this.renderProducts();
    this.renderCart();
    this.bindEvents();

    // Listen to reactive cart changes
    window.State.on('cart:change', () => {
      this.renderCart();
      this.updateMobileFloatingCart();
    });

    // Listen to catalog updates
    window.State.on(LS_KEYS.prod, () => {
      this.renderProducts();
      this.renderCategories();
    });
  }

  bindEvents() {
    // Search input
    const searchInput = document.getElementById('pos-search-input');
    const clearBtn = document.getElementById('pos-search-clear');
    if (searchInput) {
      searchInput.oninput = (e) => {
        this.searchQuery = e.target.value.toLowerCase().trim();
        this.renderProducts();
      };
      // Keyboard shortcut Ctrl+F / Cmd+F to focus search
      window.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'f') {
          e.preventDefault();
          searchInput.focus();
        }
      });
    }
    if (clearBtn && searchInput) {
      clearBtn.onclick = () => {
        searchInput.value = '';
        this.searchQuery = '';
        this.renderProducts();
      };
    }

    // Order Type buttons
    document.querySelectorAll('.btn-order-type').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('.btn-order-type').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        window.State.orderType = btn.getAttribute('data-type');
        const tableBar = document.getElementById('pos-table-selector-bar');
        if (tableBar) {
          tableBar.style.display = window.State.orderType === 'dine-in' ? 'flex' : 'none';
        }
      };
    });

    // Quick Table number pills
    document.querySelectorAll('.quick-table-btn').forEach(btn => {
      btn.onclick = () => {
        const tableNo = btn.getAttribute('data-table');
        const input = document.getElementById('pos-table-input');
        if (input) input.value = tableNo;
        window.State.tableNo = tableNo;
        document.querySelectorAll('.quick-table-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
      };
    });

    // Table input change
    const tableInput = document.getElementById('pos-table-input');
    if (tableInput) {
      tableInput.oninput = (e) => {
        window.State.tableNo = e.target.value.trim();
      };
    }

    // Checkout button
    const btnCheckout = document.getElementById('btn-pos-checkout');
    if (btnCheckout) {
      btnCheckout.onclick = () => {
        if (window.State.cart.length === 0) {
          window.State.toast('Keranjang masih kosong!', 'warning');
          return;
        }
        if (window.PaymentView) {
          window.PaymentView.openPaymentModal();
        }
      };
    }

    // Mobile floating cart button
    const mobileCartPill = document.getElementById('mobile-floating-cart-pill');
    if (mobileCartPill) {
      mobileCartPill.onclick = () => this.toggleMobileCartDrawer();
    }

    // Close mobile cart drawer
    const btnCloseDrawer = document.getElementById('btn-close-cart-drawer');
    if (btnCloseDrawer) {
      btnCloseDrawer.onclick = () => this.closeMobileCartDrawer();
    }

    // Modifier Level Buttons
    document.querySelectorAll('.mod-level-btn').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('.mod-level-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.selectedModifierLevel = btn.getAttribute('data-level');
      };
    });

    // Save Modifier button
    const btnSaveMod = document.getElementById('btn-save-modifier');
    if (btnSaveMod) {
      btnSaveMod.onclick = () => {
        if (!this.activeProductForModifier) return;
        const notesInput = document.getElementById('modifier-custom-notes');
        const notes = notesInput ? notesInput.value.trim() : '';

        window.State.addToCart(this.activeProductForModifier, {
          level: this.selectedModifierLevel,
          notes: notes
        });

        this.closeModifierModal();
        window.State.toast(`Ditambahkan: ${this.activeProductForModifier.nm} (${this.selectedModifierLevel})`, 'success');
      };
    }
  }

  renderCategories() {
    const rail = document.getElementById('pos-category-rail');
    if (!rail) return;

    const products = window.State.products.filter(p => p.on !== false);
    const totalCount = products.length;

    let html = `
      <button class="cat-pill ${this.selectedCategory === 'all' ? 'active' : ''}" onclick="window.POSView.selectCategory('all')">
        <div style="display:flex;align-items:center;gap:8px">
          <span class="material-symbols-outlined">restaurant_menu</span>
          <span>Semua Menu</span>
        </div>
        <span class="cat-count">${totalCount}</span>
      </button>
    `;

    window.State.categories.forEach(cat => {
      if (cat.on === false) return;
      const count = products.filter(p => p.kat === cat.nm).length;
      const isActive = this.selectedCategory === cat.nm;
      html += `
        <button class="cat-pill ${isActive ? 'active' : ''}" onclick="window.POSView.selectCategory('${cat.nm}')">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-size:16px">${cat.emj || '🍽️'}</span>
            <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${cat.nm}</span>
          </div>
          <span class="cat-count">${count}</span>
        </button>
      `;
    });

    rail.innerHTML = html;
  }

  selectCategory(catName) {
    this.selectedCategory = catName;
    this.renderCategories();
    this.renderProducts();
  }

  renderProducts() {
    const grid = document.getElementById('pos-product-grid');
    if (!grid) return;

    let list = window.State.products.filter(p => p.on !== false);

    if (this.selectedCategory !== 'all') {
      list = list.filter(p => p.kat === this.selectedCategory);
    }

    if (this.searchQuery) {
      list = list.filter(p => 
        p.nm.toLowerCase().includes(this.searchQuery) ||
        (p.kat && p.kat.toLowerCase().includes(this.searchQuery))
      );
    }

    if (list.length === 0) {
      grid.innerHTML = `
        <div style="grid-column:1/-1;text-align:center;padding:48px 16px;color:var(--secondary)">
          <span class="material-symbols-outlined" style="font-size:48px;opacity:0.4;margin-bottom:8px">search_off</span>
          <div style="font-size:16px;font-weight:700">Tidak ada produk ditemukan</div>
          <div style="font-size:12px;margin-top:4px">Coba kata kunci lain atau pilih kategori berbeda.</div>
        </div>
      `;
      return;
    }

    grid.innerHTML = list.map(p => `
      <div class="product-card" onclick="window.POSView.handleProductClick(${p.id})">
        <div class="product-img-box">
          ${p.emj || '🍢'}
          ${p.lv ? `<span style="position:absolute;top:6px;right:6px;background:rgba(249,115,22,0.9);color:#fff;font-size:10px;font-weight:700;padding:2px 6px;border-radius:4px">${p.lv}</span>` : ''}
        </div>
        <div class="product-title">${p.nm}</div>
        <div class="product-foot">
          <span class="product-price">${window.State.formatRp(p.hr)}</span>
          <button class="add-btn" onclick="event.stopPropagation();window.POSView.handleProductClick(${p.id})">
            <span class="material-symbols-outlined" style="font-size:18px">add</span>
          </button>
        </div>
      </div>
    `).join('');
  }

  handleProductClick(productId) {
    const product = window.State.products.find(p => p.id === productId);
    if (!product) return;

    // If product has spicy levels or customizable options, open modifier modal
    if (product.kat === 'Taichan' || product.kat === 'Chicken' || product.lv) {
      this.openModifierModal(product);
    } else {
      window.State.addToCart(product);
      window.State.toast(`+1 ${product.nm}`, 'success');
    }
  }

  openModifierModal(product) {
    this.activeProductForModifier = product;
    this.selectedModifierLevel = product.lv || 'Lv.3';

    const titleEl = document.getElementById('modifier-product-name');
    const priceEl = document.getElementById('modifier-product-price');
    const modal = document.getElementById('modifier-modal');
    const notesInput = document.getElementById('modifier-custom-notes');

    if (titleEl) titleEl.textContent = product.nm;
    if (priceEl) priceEl.textContent = window.State.formatRp(product.hr);
    if (notesInput) notesInput.value = '';

    // Set active button for level
    document.querySelectorAll('.mod-level-btn').forEach(btn => {
      if (btn.getAttribute('data-level') === this.selectedModifierLevel) {
        btn.classList.add('active');
      } else {
        btn.classList.remove('active');
      }
    });

    if (modal) modal.classList.add('open');
  }

  closeModifierModal() {
    const modal = document.getElementById('modifier-modal');
    if (modal) modal.classList.remove('open');
    this.activeProductForModifier = null;
  }

  renderCart() {
    const cartContainers = [
      document.getElementById('pos-cart-items'),
      document.getElementById('mobile-drawer-cart-items')
    ];

    const cart = window.State.cart;

    cartContainers.forEach(container => {
      if (!container) return;

      if (cart.length === 0) {
        container.innerHTML = `
          <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;min-height:180px;color:var(--secondary);opacity:0.6;text-align:center">
            <span class="material-symbols-outlined" style="font-size:42px;margin-bottom:8px">shopping_cart</span>
            <div style="font-weight:600;font-size:13px">Keranjang Kosong</div>
            <div style="font-size:11px">Pilih menu dari katalog di sebelah kiri</div>
          </div>
        `;
        return;
      }

      container.innerHTML = cart.map((item, idx) => `
        <div class="cart-item">
          <div class="cart-item-top">
            <div class="cart-item-title">${item.nm}</div>
            <button class="qty-btn" style="color:var(--error)" onclick="window.State.updateCartQty(${idx}, -${item.qty})">
              <span class="material-symbols-outlined" style="font-size:16px">close</span>
            </button>
          </div>
          ${item.mod ? `<div class="cart-item-mod"><span class="material-symbols-outlined" style="font-size:13px">local_fire_department</span> ${item.mod}</div>` : ''}
          <div class="cart-item-bot">
            <span class="font-mono" style="font-weight:700;color:var(--primary);font-size:13px">${window.State.formatRp(item.hr * item.qty)}</span>
            <div class="cart-qty-ctrl">
              <button class="qty-btn" onclick="window.State.updateCartQty(${idx}, -1)">
                <span class="material-symbols-outlined" style="font-size:14px">remove</span>
              </button>
              <span class="qty-val">${item.qty}</span>
              <button class="qty-btn" onclick="window.State.updateCartQty(${idx}, 1)">
                <span class="material-symbols-outlined" style="font-size:14px">add</span>
              </button>
            </div>
          </div>
        </div>
      `).join('');
    });

    // Update Totals
    const subtotal = window.State.getCartTotal();
    const subtotalEls = [
      document.getElementById('pos-cart-subtotal'),
      document.getElementById('mobile-drawer-subtotal')
    ];
    const totalEls = [
      document.getElementById('pos-cart-total'),
      document.getElementById('mobile-drawer-total')
    ];
    const btnCheckout = document.getElementById('btn-pos-checkout');
    const btnDrawerCheckout = document.getElementById('btn-drawer-checkout');

    subtotalEls.forEach(el => {
      if (el) el.textContent = window.State.formatRp(subtotal);
    });
    totalEls.forEach(el => {
      if (el) el.textContent = window.State.formatRp(subtotal);
    });

    if (btnCheckout) btnCheckout.disabled = cart.length === 0;
    if (btnDrawerCheckout) btnDrawerCheckout.disabled = cart.length === 0;
  }

  updateMobileFloatingCart() {
    const pill = document.getElementById('mobile-floating-cart-pill');
    const countEl = document.getElementById('mobile-pill-count');
    const totalEl = document.getElementById('mobile-pill-total');

    const totalQty = window.State.cart.reduce((s, i) => s + i.qty, 0);
    const totalPrice = window.State.getCartTotal();

    if (pill) {
      if (totalQty > 0) {
        pill.classList.add('has-items');
        if (countEl) countEl.textContent = `${totalQty} Item`;
        if (totalEl) totalEl.textContent = window.State.formatRp(totalPrice);
      } else {
        pill.classList.remove('has-items');
      }
    }
  }

  toggleMobileCartDrawer() {
    const drawer = document.getElementById('mobile-cart-drawer');
    if (drawer) {
      drawer.classList.toggle('open');
    }
  }

  closeMobileCartDrawer() {
    const drawer = document.getElementById('mobile-cart-drawer');
    if (drawer) {
      drawer.classList.remove('open');
    }
  }
}

window.POSView = new POSView();
