/**
 * KA POS v3.0 - 3-Zone Velocity POS Kasir & Modifiers
 */

class POSView {
  constructor() {
    this.selectedCategory = 'all';
    this.searchQuery = '';
    this.activeProductForModifier = null;
    this.selectedSambal = null; // { nm, hr }
    this.selectedToppings = [];
    this.modifierNotes = '';
  }

  init() {
    this.renderCategories();
    this.renderProducts();
    this.renderCart();
    this.bindEvents();
    this.setupKeyboardShortcuts();
    this.updateHeaderMetrics();

    // Check low stock on start
    window.State.checkLowStockAlerts();
    this.updateHeaderMetrics();

    // Listen to reactive cart changes
    window.State.on('cart:change', () => {
      this.renderCart();
      this.updateMobileFloatingCart();
    });

    // Listen to catalog updates
    window.State.on(LS_KEYS.prod, () => {
      this.renderProducts();
      this.renderCategories();
      this.updateHeaderMetrics();
    });

    // Listen to transactions & inventory
    window.State.on(LS_KEYS.trx, () => this.updateHeaderMetrics());
    window.State.on(LS_KEYS.inv, () => this.updateHeaderMetrics());
    window.State.on('notifications:updated', () => this.updateHeaderMetrics());
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

    // Order Type buttons (Dine In / Bungkus / Ojol)
    document.querySelectorAll('.btn-order-type').forEach(btn => {
      btn.onclick = () => {
        const type = btn.getAttribute('data-type');
        document.querySelectorAll('.btn-order-type').forEach(b => {
          b.classList.toggle('active', b.getAttribute('data-type') === type);
        });
        window.State.orderType = type;
      };
    });

    // Checkout button
    const btnCheckout = document.getElementById('btn-pos-checkout');
    if (btnCheckout) {
      btnCheckout.onclick = () => {
        if (window.State.cart.length === 0) {
          window.State.toast('Keranjang masih kosong!', 'warning');
          return;
        }
        this.syncCustomerName();
        if (window.PaymentView) {
          window.PaymentView.openPaymentModal();
        }
      };
    }

    // Hold / Save Order button (desktop)
    const btnHold = document.getElementById('btn-hold-cart');
    if (btnHold) {
      btnHold.onclick = () => this.saveCurrentOrder();
    }

    // Customer name sync between desktop and mobile
    const nameDesktop = document.getElementById('pos-customer-name');
    const nameMobile = document.getElementById('pos-customer-name-mobile');
    if (nameDesktop) {
      nameDesktop.oninput = () => {
        window.State.currentCustomerName = nameDesktop.value.trim();
        if (nameMobile) nameMobile.value = nameDesktop.value;
      };
    }
    if (nameMobile) {
      nameMobile.oninput = () => {
        window.State.currentCustomerName = nameMobile.value.trim();
        if (nameDesktop) nameDesktop.value = nameMobile.value;
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

    // Modifier Toppings Checkboxes
    document.querySelectorAll('.mod-topping-cb').forEach(cb => {
      cb.onchange = () => {
        this.selectedToppings = Array.from(document.querySelectorAll('.mod-topping-cb:checked')).map(el => ({
          nm: el.getAttribute('data-topping'),
          price: Number(el.getAttribute('data-price')) || 0
        }));
        this.updateModifierTotalPrice();
      };
    });

    // Modifier Quick Tags
    document.querySelectorAll('.mod-tag-btn').forEach(btn => {
      btn.onclick = () => {
        const tag = btn.getAttribute('data-tag');
        const notesInput = document.getElementById('modifier-custom-notes');
        if (notesInput) {
          const curr = notesInput.value.trim();
          if (!curr.includes(tag)) {
            notesInput.value = curr ? `${curr}, ${tag}` : tag;
          }
        }
      };
    });

    // Save Modifier button
    const btnSaveMod = document.getElementById('btn-save-modifier');
    if (btnSaveMod) {
      btnSaveMod.onclick = () => {
        if (!this.activeProductForModifier) return;
        const notesInput = document.getElementById('modifier-custom-notes');
        const customNotes = notesInput ? notesInput.value.trim() : '';

        const toppingsTotal = this.selectedToppings.reduce((sum, t) => sum + t.price, 0);
        const sambalPrice = this.selectedSambal ? (this.selectedSambal.hr || 0) : 0;
        const extraPrice = toppingsTotal + sambalPrice;

        const parts = [];
        if (this.selectedSambal) parts.push(this.selectedSambal.nm);
        if (this.selectedToppings.length > 0) {
          parts.push(this.selectedToppings.map(t => t.nm).join(', '));
        }
        if (customNotes) parts.push(customNotes);

        window.State.addToCart(this.activeProductForModifier, {
          level: this.selectedSambal ? this.selectedSambal.nm : '',
          notes: parts.join(' • '),
          extraPrice: extraPrice
        });

        this.closeModifierModal();
        window.State.toast(`+1 ${this.activeProductForModifier.nm}`, 'success');
      };
    }
  }

  renderCategories() {
    const rail = document.getElementById('pos-category-rail');
    if (!rail) return;

    const products = window.State.products.filter(p => p.on !== false);
    const totalCount = products.length;

    let html = `<div class="zone-a-label">Kategori Menu</div>`;

    const allIcon = window.FoodIcons ? window.FoodIcons.get('food', { size: 22 }) : '';
    html += `
      <button class="cat-pill ${this.selectedCategory === 'all' ? 'active' : ''}" onclick="window.POSView.selectCategory('all')">
        ${allIcon}
        <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;flex:1;">Semua Menu</span>
        <span class="cat-count">${totalCount}</span>
      </button>
    `;

    window.State.categories.forEach(cat => {
      if (cat.on === false) return;
      const count = products.filter(p => p.kat === cat.nm).length;
      const isActive = this.selectedCategory === cat.nm;
      const catIcon = window.FoodIcons ? window.FoodIcons.get(cat.emj || cat.nm, { size: 22 }) : '';
      html += `
        <button class="cat-pill ${isActive ? 'active' : ''}" onclick="window.POSView.selectCategory('${cat.nm}')">
          ${catIcon}
          <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;flex:1;">${cat.nm}</span>
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

    grid.innerHTML = list.map(p => {
      const isUnavailable = p.habis === true;
      const iconHtml = window.FoodIcons ? window.FoodIcons.get(p.emj || p.kat || '🍢', { size: 38 }) : '';
      return `
        <div class="product-card ${isUnavailable ? 'unavailable' : ''}" onclick="window.POSView.handleProductClick(${p.id})" style="${isUnavailable ? 'opacity:0.5;cursor:not-allowed;' : ''}">
          <div class="product-card-top">
            ${iconHtml}
            <div style="flex:1;min-width:0;">
              <div class="product-title">${p.nm}</div>
              <div style="display:flex;align-items:center;gap:4px;margin-top:3px;flex-wrap:wrap;">
                ${p.lv && !isUnavailable ? `<span class="product-badge" style="position:static;background:rgba(249,115,22,0.85);color:#fff;font-size:9.5px;padding:1px 5px;border-radius:4px;flex-shrink:0;">${p.lv}</span>` : ''}
                ${isUnavailable ? '<span class="product-badge out" style="position:static;flex-shrink:0;">HABIS</span>' : ''}
              </div>
            </div>
          </div>
          <div class="product-foot">
            <span class="product-price">${window.State.formatRp(p.hr)}</span>
            <button class="add-btn" ${isUnavailable ? 'disabled style="opacity:0.3;cursor:not-allowed;"' : ''} onclick="event.stopPropagation();window.POSView.handleProductClick(${p.id})">
              <span class="material-symbols-outlined" style="font-size:18px;">add</span>
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  handleProductClick(productId) {
    const product = window.State.products.find(p => p.id === productId);
    if (!product) return;

    // BUG-10: gunakan p.on untuk cek ketersediaan (konsisten dengan state)
    if (product.on === false) return;

    // BUG-22 fix: buka modifier hanya jika ada sambal aktif ATAU produk punya level
    const hasActiveSambal = (window.State.sambalList || []).some(s => s.aktif !== false);
    const needsModifier = (product.kat === 'Taichan' || product.kat === 'Chicken' || product.lv) && hasActiveSambal;

    if (needsModifier) {
      this.openModifierModal(product);
    } else {
      window.State.addToCart(product);
      window.State.toast(`+1 ${product.nm}`, 'success');
    }
  }

  openModifierModal(product) {
    this.activeProductForModifier = product;
    this.selectedSambal = null;
    this.selectedToppings = [];

    const titleEl = document.getElementById('modifier-product-name');
    const priceEl = document.getElementById('modifier-product-price');
    const modal = document.getElementById('modifier-modal');
    const notesInput = document.getElementById('modifier-custom-notes');

    if (titleEl) titleEl.textContent = product.nm;
    if (priceEl) priceEl.textContent = window.State.formatRp(product.hr);
    if (notesInput) notesInput.value = '';

    // Reset toppings checkboxes
    document.querySelectorAll('.mod-topping-cb').forEach(cb => cb.checked = false);

    // Populate dynamic sambal options
    const sambalContainer = document.getElementById('modifier-sambal-options');
    if (sambalContainer) {
      const activeSambal = (window.State.sambalList || []).filter(s => s.aktif !== false);
      if (activeSambal.length === 0) {
        sambalContainer.innerHTML = `<div style="font-size:12px;color:var(--secondary);padding:4px 0;">Belum ada variasi sambal. Tambah di menu Stok &amp; Resep.</div>`;
      } else {
        sambalContainer.innerHTML = activeSambal.map(s => `
          <button type="button" class="btn btn-secondary mod-sambal-btn"
            data-sambal-id="${s.id}"
            data-sambal-nm="${s.nm}"
            data-sambal-hr="${s.hr || 0}"
            style="font-size:12px;border-radius:var(--radius-pill);">
            ${s.nm}${s.hr > 0 ? ` <span style="color:var(--primary);font-size:10px;">+${window.State.formatRp(s.hr)}</span>` : ''}
          </button>
        `).join('');

        // Auto-select first sambal
        const firstBtn = sambalContainer.querySelector('.mod-sambal-btn');
        if (firstBtn) {
          this.selectedSambal = { nm: firstBtn.getAttribute('data-sambal-nm'), hr: Number(firstBtn.getAttribute('data-sambal-hr')) };
          firstBtn.classList.add('active');
        }

        sambalContainer.querySelectorAll('.mod-sambal-btn').forEach(btn => {
          btn.onclick = () => {
            sambalContainer.querySelectorAll('.mod-sambal-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            this.selectedSambal = { nm: btn.getAttribute('data-sambal-nm'), hr: Number(btn.getAttribute('data-sambal-hr')) };
            this.updateModifierTotalPrice();
          };
        });
      }
    }

    this.updateModifierTotalPrice();
    if (modal) modal.classList.add('open');

    // BUG-14 fix: backdrop click harus memanggil closeModifierModal() agar state di-reset
    if (modal) {
      modal.onclick = (e) => {
        if (e.target === modal) this.closeModifierModal();
      };
    }
  }

  updateModifierTotalPrice() {
    if (!this.activeProductForModifier) return;
    const toppingsTotal = this.selectedToppings.reduce((sum, t) => sum + t.price, 0);
    const sambalPrice = this.selectedSambal ? (this.selectedSambal.hr || 0) : 0;
    const total = Number(this.activeProductForModifier.hr) + sambalPrice + toppingsTotal;
    const totalEl = document.getElementById('modifier-total-price');
    if (totalEl) totalEl.textContent = window.State.formatRp(total);
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
          <div class="cart-empty">
            <span class="material-symbols-outlined" style="font-size:44px;opacity:0.35;">shopping_cart</span>
            <div style="font-weight:700;font-size:14px;">Keranjang Kosong</div>
            <div style="font-size:12px;">Pilih menu dari katalog</div>
          </div>
        `;
        return;
      }

      container.innerHTML = cart.map((item) => {
        // BUG-07 fix: encode id+mod sebagai data attributes, bukan index posisi array
        const safeId = item.id;
        const safeMod = encodeURIComponent(item.mod || '');
        return `
        <div class="cart-item">
          <div class="cart-item-header">
            <span class="cart-item-qty-badge">${item.qty}x</span>
            ${window.FoodIcons ? window.FoodIcons.get(item.emj || item.nm || '🍢', { size: 26 }) : ''}
            <div style="flex:1;min-width:0;">
              <div class="cart-item-title">${item.nm}</div>
              ${item.mod ? `<div class="cart-item-mod"><span class="material-symbols-outlined" style="font-size:12px;vertical-align:middle;">local_fire_department</span> ${item.mod}</div>` : ''}
            </div>
            <button class="cart-item-delete" onclick="window.State.removeCartItemById(${safeId}, decodeURIComponent('${safeMod}'))" title="Hapus">
              <span class="material-symbols-outlined" style="font-size:18px;">close</span>
            </button>
          </div>
          <div class="cart-item-footer">
            <div class="cart-qty-ctrl">
              <button class="qty-btn" onclick="window.State.updateCartQtyById(${safeId}, decodeURIComponent('${safeMod}'), -1)">
                <span class="material-symbols-outlined" style="font-size:14px;">remove</span>
              </button>
              <span class="qty-val">${item.qty}</span>
              <button class="qty-btn" onclick="window.State.updateCartQtyById(${safeId}, decodeURIComponent('${safeMod}'), 1)">
                <span class="material-symbols-outlined" style="font-size:14px;">add</span>
              </button>
            </div>
            <span class="cart-item-price">${window.State.formatRp(item.hr * item.qty)}</span>
          </div>
        </div>
      `;
      }).join('');
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
    const btnHold = document.getElementById('btn-hold-cart');
    const btnDrawerCheckout = document.getElementById('btn-drawer-checkout');
    const btnDrawerHold = document.getElementById('btn-hold-cart-mobile');
    const drawerBadge = document.getElementById('mobile-drawer-badge-count');

    subtotalEls.forEach(el => { if (el) el.textContent = window.State.formatRp(subtotal); });
    totalEls.forEach(el => { if (el) el.textContent = window.State.formatRp(subtotal); });

    const totalQty = cart.reduce((s, i) => s + i.qty, 0);
    if (drawerBadge) drawerBadge.textContent = `${totalQty} Item`;

    if (btnCheckout) btnCheckout.disabled = cart.length === 0;
    if (btnHold) btnHold.disabled = cart.length === 0;
    if (btnDrawerCheckout) btnDrawerCheckout.disabled = cart.length === 0;
    if (btnDrawerHold) btnDrawerHold.disabled = cart.length === 0;

    // Update saved orders badge
    this.updateSavedOrdersBadge();
  }

  syncCustomerName() {
    const nameDesktop = document.getElementById('pos-customer-name');
    const nameMobile = document.getElementById('pos-customer-name-mobile');
    if (nameDesktop) window.State.currentCustomerName = nameDesktop.value.trim();
    else if (nameMobile) window.State.currentCustomerName = nameMobile.value.trim();
  }

  clearCustomerNameInputs() {
    const nameDesktop = document.getElementById('pos-customer-name');
    const nameMobile = document.getElementById('pos-customer-name-mobile');
    window.State.currentCustomerName = '';
    if (nameDesktop) nameDesktop.value = '';
    if (nameMobile) nameMobile.value = '';
  }

  saveCurrentOrder() {
    if (window.State.cart.length === 0) {
      window.State.toast('Keranjang kosong, tidak ada yang bisa disimpan!', 'warning');
      return;
    }
    this.syncCustomerName();
    const cashierName = window.State.currentUser ? window.State.currentUser.nm : 'Kasir';
    const order = {
      id: 'HOLD-' + Date.now(),
      pelanggan: window.State.currentCustomerName || 'Umum',
      tgl: Date.now(),
      items: JSON.parse(JSON.stringify(window.State.cart)),
      subtotal: window.State.getCartTotal(),
      tipe: window.State.orderType,
      staf: cashierName
    };
    window.State.saveOrder(order);
    window.State.clearCart();
    this.clearCustomerNameInputs();
    this.closeMobileCartDrawer();
    this.updateSavedOrdersBadge();
    window.State.toast(`Order a.n. "${order.pelanggan}" berhasil disimpan!`, 'success');
  }

  openSavedOrdersModal() {
    const modal = document.getElementById('saved-orders-modal');
    const listEl = document.getElementById('saved-orders-list');
    if (!modal || !listEl) return;

    const orders = window.State.savedOrders || [];
    if (orders.length === 0) {
      listEl.innerHTML = `<div style="text-align:center;padding:32px;color:var(--secondary);"><span class="material-symbols-outlined" style="font-size:40px;display:block;margin-bottom:8px;">bookmark</span>Belum ada order yang disimpan</div>`;
    } else {
      listEl.innerHTML = orders.map(o => `
        <div style="background:var(--surface-container-low);border:1px solid rgba(255,255,255,0.07);border-radius:var(--radius-lg);padding:14px 16px;margin-bottom:10px;">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
            <div>
              <div style="font-weight:800;font-size:14px;">a.n. ${o.pelanggan}</div>
              <div style="font-size:11px;color:var(--secondary);">${new Date(o.tgl).toLocaleString('id-ID',{hour:'2-digit',minute:'2-digit',day:'2-digit',month:'short'})} · ${o.tipe || 'dine-in'} · ${o.staf}</div>
            </div>
            <div class="font-mono" style="font-size:15px;font-weight:800;color:var(--primary);">${window.State.formatRp(o.subtotal)}</div>
          </div>
          <div style="font-size:11px;color:var(--secondary);margin-bottom:10px;">${(o.items||[]).map(i=>`${i.nm} x${i.qty}`).join(', ')}</div>
          <div style="display:flex;gap:8px;">
            <button class="btn btn-primary" style="flex:1;height:36px;font-size:12px;" onclick="window.POSView.recallSavedOrder('${o.id}')">
              <span class="material-symbols-outlined" style="font-size:16px;">shopping_cart</span>
              Buka ke Keranjang
            </button>
            <button class="btn btn-danger" style="height:36px;padding:0 14px;font-size:12px;" onclick="window.POSView.deleteSavedOrder('${o.id}')">
              <span class="material-symbols-outlined" style="font-size:16px;">delete</span>
            </button>
          </div>
        </div>
      `).join('');
    }
    modal.classList.add('open');
  }

  recallSavedOrder(orderId) {
    const order = (window.State.savedOrders || []).find(o => o.id === orderId);
    if (!order) return;

    if (window.State.cart.length > 0) {
      if (!confirm('Keranjang saat ini tidak kosong. Timpa dengan order tersimpan?')) return;
    }

    window.State.cart = JSON.parse(JSON.stringify(order.items));
    window.State.orderType = order.tipe || 'dine-in';
    window.State.currentCustomerName = order.pelanggan;

    // Sync customer name to inputs
    const nameDesktop = document.getElementById('pos-customer-name');
    const nameMobile = document.getElementById('pos-customer-name-mobile');
    if (nameDesktop) nameDesktop.value = order.pelanggan;
    if (nameMobile) nameMobile.value = order.pelanggan;

    window.State.removeSavedOrder(orderId);
    window.State.emit('cart:change');

    const modal = document.getElementById('saved-orders-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Order a.n. "${order.pelanggan}" dibuka ke keranjang!`, 'success');
  }

  deleteSavedOrder(orderId) {
    window.State.removeSavedOrder(orderId);
    this.updateSavedOrdersBadge();
    this.openSavedOrdersModal(); // refresh list
    window.State.toast('Order tersimpan dihapus.', 'warning');
  }

  updateSavedOrdersBadge() {
    const count = (window.State.savedOrders || []).length;
    const badge = document.getElementById('saved-orders-badge');
    if (badge) {
      badge.textContent = count;
      badge.style.display = count > 0 ? 'flex' : 'none';
    }
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

  openMobileCartDrawer() {
    const drawer = document.getElementById('mobile-cart-drawer');
    if (drawer) {
      drawer.classList.add('open');
      document.body.style.overflow = 'hidden';
      const nameMobile = document.getElementById('pos-customer-name-mobile');
      if (nameMobile && window.State.currentCustomerName) {
        nameMobile.value = window.State.currentCustomerName;
      }
    }
  }

  toggleMobileCartDrawer() {
    const drawer = document.getElementById('mobile-cart-drawer');
    if (drawer) {
      if (drawer.classList.contains('open')) {
        this.closeMobileCartDrawer();
      } else {
        this.openMobileCartDrawer();
      }
    }
  }

  closeMobileCartDrawer() {
    const drawer = document.getElementById('mobile-cart-drawer');
    if (drawer) {
      drawer.classList.remove('open');
      document.body.style.overflow = '';
    }
  }

  // --- Keyboard Shortcuts (F2, F4, F8, F9, Esc) ---
  setupKeyboardShortcuts() {
    window.addEventListener('keydown', (e) => {
      if (e.key === 'F2') {
        e.preventDefault();
        const custInput = document.getElementById('pos-customer-name');
        if (custInput) custInput.focus();
      } else if (e.key === 'F4') {
        e.preventDefault();
        if (window.State.cart.length > 0 && window.PaymentView) {
          window.PaymentView.openPaymentModal();
        } else {
          window.State.toast('Keranjang masih kosong!', 'warning');
        }
      } else if (e.key === 'F8') {
        e.preventDefault();
        if (window.State.cart.length > 0) {
          this.saveCurrentOrder();
        }
      } else if (e.key === 'F9') {
        e.preventDefault();
        const lastTrx = (window.State.transactions || [])[0];
        if (lastTrx && window.PrinterService) {
          window.PrinterService.printReceipt(lastTrx);
          window.State.toast('Mencetak struk terakhir (F9)', 'info');
        }
      } else if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay.open').forEach(m => m.classList.remove('open'));
      }
    });
  }

  // --- Operational Realtime Dashboard Metrics ---
  updateHeaderMetrics() {
    const todayStr = window.State.formatDateShort(Date.now());
    const todayTrx = (window.State.transactions || []).filter(t => window.State.formatDateShort(t.tgl) === todayStr);
    const omzet = todayTrx.reduce((s, t) => s + (t.total || 0), 0);
    const isOwner = window.State && window.State.isOwner();
    const omzetEl = document.getElementById('header-omzet-val');
    const trxEl = document.getElementById('header-trx-val');
    if (omzetEl) omzetEl.textContent = isOwner ? window.State.formatRp(omzet) : '—';
    if (trxEl) trxEl.textContent = `${todayTrx.length} Trx`;

    // Mobile quick panel stats sync
    const mobOmzetEl = document.getElementById('mobile-quick-omzet');
    const mobTrxEl = document.getElementById('mobile-quick-trx');
    if (mobOmzetEl) mobOmzetEl.textContent = isOwner ? window.State.formatRp(omzet) : '—';
    if (mobTrxEl) mobTrxEl.textContent = `${todayTrx.length} Trx`;

    const lowItems = (window.State.inventory || []).filter(i => Number(i.stok) <= Number(i.min));
    const stokPill = document.getElementById('header-stok-pill');
    const stokVal = document.getElementById('header-stok-val');
    if (stokPill && stokVal) {
      stokVal.textContent = `${lowItems.length} Kritis`;
      stokPill.style.display = lowItems.length > 0 ? 'inline-flex' : 'none';
    }

    const mobStokVal = document.getElementById('mobile-quick-stok');
    if (mobStokVal) mobStokVal.textContent = `${lowItems.length} Kritis`;

    const unreadCount = window.State.getUnreadNotificationCount();
    const notifBadge = document.getElementById('header-notif-badge');
    if (notifBadge) {
      notifBadge.textContent = unreadCount;
      notifBadge.style.display = unreadCount > 0 ? 'flex' : 'none';
    }
  }

  showLowStockModal() {
    const modal = document.getElementById('low-stock-modal');
    const body = document.getElementById('low-stock-modal-body');
    if (!modal || !body) return;

    const lowItems = (window.State.inventory || []).filter(i => Number(i.stok) <= Number(i.min));
    if (lowItems.length === 0) {
      body.innerHTML = `<div style="text-align:center;padding:24px;color:var(--tertiary);">✅ Semua stok bahan baku dalam batas aman!</div>`;
    } else {
      body.innerHTML = `
        <table style="width:100%;border-collapse:collapse;font-size:12px;">
          <thead>
            <tr style="border-bottom:1px solid rgba(255,255,255,0.1);color:var(--secondary);text-align:left;">
              <th style="padding:8px;">Bahan</th>
              <th style="padding:8px;">Sisa Stok</th>
              <th style="padding:8px;">Batas Min</th>
              <th style="padding:8px;">Status</th>
            </tr>
          </thead>
          <tbody>
            ${lowItems.map(item => `
              <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
                <td style="padding:8px;font-weight:700;">${item.emj || '📦'} ${item.nm}</td>
                <td style="padding:8px;font-family:var(--font-mono);color:var(--error);font-weight:700;">${item.stok} ${item.sat}</td>
                <td style="padding:8px;font-family:var(--font-mono);">${item.min} ${item.sat}</td>
                <td style="padding:8px;"><span style="color:var(--error);font-weight:700;">KRITIS</span></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }
    modal.classList.add('open');
  }

  toggleNotificationsDrawer() {
    const modal = document.getElementById('notifications-modal');
    const list = document.getElementById('notifications-list');
    if (!modal || !list) return;

    const notifs = window.State.notifications || [];
    if (notifs.length === 0) {
      list.innerHTML = `<div style="text-align:center;padding:32px;color:var(--secondary);">Belum ada notifikasi sistem.</div>`;
    } else {
      list.innerHTML = notifs.map(n => `
        <div style="padding:12px;border-bottom:1px solid rgba(255,255,255,0.06);display:flex;gap:12px;align-items:flex-start;">
          <div style="font-size:20px;">${n.type === 'warning' ? '⚠️' : (n.type === 'error' ? '❌' : (n.type === 'success' ? '✅' : 'ℹ️'))}</div>
          <div style="flex:1;">
            <div style="font-weight:700;font-size:13px;color:var(--on-surface);">${n.title}</div>
            <div style="font-size:12px;color:var(--secondary);margin-top:2px;">${n.message}</div>
            <div style="font-size:10px;color:var(--secondary);margin-top:4px;font-family:var(--font-mono);">${window.State.formatDate(n.time)}</div>
          </div>
        </div>
      `).join('');
    }
    window.State.markNotificationsRead();
    this.updateHeaderMetrics();
    modal.classList.add('open');
  }
}

window.POSView = new POSView();
