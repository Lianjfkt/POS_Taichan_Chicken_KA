/**
 * KA POS v3.0 - Menu Management View
 * Full CRUD: Categories & Products (Tambah, Edit, Hapus, Toggle Aktif)
 */

class MenuView {
  constructor() {
    this.activeTab = 'products'; // 'products' | 'categories'
    this.editingProduct = null;
    this.editingCategory = null;
    this.searchQuery = '';
    this.filterCategory = 'all';
  }

  init() {
    this.bindEvents();
    this.render();
  }

  bindEvents() {
    const tabProducts = document.getElementById('menu-tab-products');
    const tabCategories = document.getElementById('menu-tab-categories');
    if (tabProducts) tabProducts.onclick = () => this.switchTab('products');
    if (tabCategories) tabCategories.onclick = () => this.switchTab('categories');

    const searchInput = document.getElementById('menu-search');
    if (searchInput) {
      searchInput.oninput = (e) => {
        this.searchQuery = e.target.value.toLowerCase();
        this.renderProductList();
      };
    }

    const catFilter = document.getElementById('menu-cat-filter');
    if (catFilter) {
      catFilter.onchange = (e) => {
        this.filterCategory = e.target.value;
        this.renderProductList();
      };
    }

    const btnAddProduct = document.getElementById('btn-add-product');
    if (btnAddProduct) btnAddProduct.onclick = () => this.openProductModal();

    const btnAddCategory = document.getElementById('btn-add-category');
    if (btnAddCategory) btnAddCategory.onclick = () => this.openCategoryModal();

    const productForm = document.getElementById('menu-product-form');
    if (productForm) {
      productForm.onsubmit = (e) => {
        e.preventDefault();
        this.saveProduct();
      };
    }

    const categoryForm = document.getElementById('menu-category-form');
    if (categoryForm) {
      categoryForm.onsubmit = (e) => {
        e.preventDefault();
        this.saveCategory();
      };
    }

    document.getElementById('menu-product-modal-close')?.addEventListener('click', () => {
      document.getElementById('menu-product-modal')?.classList.remove('open');
    });
    document.getElementById('menu-category-modal-close')?.addEventListener('click', () => {
      document.getElementById('menu-category-modal')?.classList.remove('open');
    });
    document.getElementById('menu-product-modal-cancel')?.addEventListener('click', () => {
      document.getElementById('menu-product-modal')?.classList.remove('open');
    });
    document.getElementById('menu-category-modal-cancel')?.addEventListener('click', () => {
      document.getElementById('menu-category-modal')?.classList.remove('open');
    });

    document.getElementById('menu-product-modal')?.addEventListener('click', (e) => {
      if (e.target === e.currentTarget) e.currentTarget.classList.remove('open');
    });
    document.getElementById('menu-category-modal')?.addEventListener('click', (e) => {
      if (e.target === e.currentTarget) e.currentTarget.classList.remove('open');
    });

    const productEmoji = document.getElementById('mprod-emoji');
    if (productEmoji) {
      productEmoji.oninput = () => {
        const prev = document.getElementById('mprod-emoji-preview');
        if (prev) prev.textContent = productEmoji.value || '🍢';
      };
    }
    const catEmoji = document.getElementById('mcat-emoji');
    if (catEmoji) {
      catEmoji.oninput = () => {
        const prev = document.getElementById('mcat-emoji-preview');
        if (prev) prev.textContent = catEmoji.value || '🍽️';
      };
    }
  }

  switchTab(tab) {
    this.activeTab = tab;
    this.render();
  }

  render() {
    this.renderCategoryFilterOptions();
    this.renderTabState();
    if (this.activeTab === 'products') {
      this.renderProductList();
    } else {
      this.renderCategoryList();
    }
  }

  renderTabState() {
    const tabProducts = document.getElementById('menu-tab-products');
    const tabCategories = document.getElementById('menu-tab-categories');
    const panelProducts = document.getElementById('menu-panel-products');
    const panelCategories = document.getElementById('menu-panel-categories');

    if (tabProducts) tabProducts.classList.toggle('active', this.activeTab === 'products');
    if (tabCategories) tabCategories.classList.toggle('active', this.activeTab === 'categories');
    if (panelProducts) panelProducts.style.display = this.activeTab === 'products' ? '' : 'none';
    if (panelCategories) panelCategories.style.display = this.activeTab === 'categories' ? '' : 'none';
  }

  renderCategoryFilterOptions() {
    const catFilter = document.getElementById('menu-cat-filter');
    if (!catFilter) return;
    const categories = window.State.categories || [];
    catFilter.innerHTML = '<option value="all">Semua Kategori</option>' +
      categories.map(c => '<option value="' + c.nm + '" ' + (this.filterCategory === c.nm ? 'selected' : '') + '>' + (c.emj || '') + ' ' + c.nm + '</option>').join('');
  }

  renderProductList() {
    const container = document.getElementById('menu-product-grid');
    if (!container) return;

    let products = window.State.products || [];

    if (this.searchQuery) {
      products = products.filter(p =>
        p.nm.toLowerCase().includes(this.searchQuery) ||
        (p.kat || '').toLowerCase().includes(this.searchQuery)
      );
    }

    if (this.filterCategory !== 'all') {
      products = products.filter(p => p.kat === this.filterCategory);
    }

    if (products.length === 0) {
      container.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:60px 20px;color:var(--secondary);"><span class="material-symbols-outlined" style="font-size:48px;display:block;margin-bottom:12px;opacity:0.4;">restaurant_menu</span><div style="font-size:14px;">Tidak ada produk ditemukan</div></div>';
      return;
    }

    container.innerHTML = products.map(p => this._productCard(p)).join('');
  }

  _productCard(p) {
    const isActive = p.on !== false;
    const margin = p.hr - (p.md || 0);
    const marginPct = p.hr > 0 ? Math.round((margin / p.hr) * 100) : 0;
    const activeClass = isActive ? '' : 'mpc-inactive';

    return '<div class="menu-product-card ' + activeClass + '">' +
      '<div class="mpc-header">' +
        '<div class="mpc-emoji">' + (p.emj || '🍢') + '</div>' +
        '<div style="display:flex;gap:4px;flex-wrap:wrap;">' +
          '<span class="badge ' + (isActive ? 'success' : 'danger') + '" style="font-size:10px;padding:2px 7px;">' + (isActive ? 'Aktif' : 'Nonaktif') + '</span>' +
          (p.lv ? '<span class="badge warning" style="font-size:10px;padding:2px 7px;">' + p.lv + '</span>' : '') +
        '</div>' +
      '</div>' +
      '<div class="mpc-body">' +
        '<div class="mpc-name">' + p.nm + '</div>' +
        '<div class="mpc-cat">' + (p.kat || '—') + '</div>' +
        '<div class="mpc-price-row">' +
          '<span class="mpc-price">' + window.State.formatRp(p.hr) + '</span>' +
          '<span class="mpc-margin"><span class="material-symbols-outlined" style="font-size:13px;vertical-align:middle;">trending_up</span>' + marginPct + '%</span>' +
        '</div>' +
      '</div>' +
      '<div class="mpc-actions">' +
        '<button class="btn btn-secondary mpc-btn" onclick="window.MenuView.openProductModal(\'' + p.id + '\')">' +
          '<span class="material-symbols-outlined" style="font-size:15px;">edit</span> Edit' +
        '</button>' +
        '<button class="btn ' + (isActive ? 'btn-warning' : 'btn-success') + ' mpc-btn" onclick="window.MenuView.toggleProduct(\'' + p.id + '\')">' +
          '<span class="material-symbols-outlined" style="font-size:15px;">' + (isActive ? 'visibility_off' : 'visibility') + '</span>' +
          (isActive ? 'Off' : 'On') +
        '</button>' +
        '<button class="btn btn-danger mpc-btn" onclick="window.MenuView.deleteProduct(\'' + p.id + '\')">' +
          '<span class="material-symbols-outlined" style="font-size:15px;">delete</span>' +
        '</button>' +
      '</div>' +
    '</div>';
  }

  renderCategoryList() {
    const container = document.getElementById('menu-category-list');
    if (!container) return;

    const categories = window.State.categories || [];

    if (categories.length === 0) {
      container.innerHTML = '<div style="text-align:center;padding:60px 20px;color:var(--secondary);"><span class="material-symbols-outlined" style="font-size:48px;display:block;margin-bottom:12px;opacity:0.4;">category</span><div>Belum ada kategori</div></div>';
      return;
    }

    const sorted = [...categories].sort((a, b) => (a.ord || 0) - (b.ord || 0));

    container.innerHTML = sorted.map(c => {
      const prodCount = (window.State.products || []).filter(p => p.kat === c.nm).length;
      const isActive = c.on !== false;
      const inactiveClass = isActive ? '' : 'mcr-inactive';
      return '<div class="menu-cat-row ' + inactiveClass + '">' +
        '<div class="mcr-left">' +
          '<div class="mcr-emoji" style="background:' + (c.col || '#f97316') + '22;border:2px solid ' + (c.col || '#f97316') + '55;">' +
            (c.emj || '🍽️') +
          '</div>' +
          '<div>' +
            '<div class="mcr-name">' + c.nm + '</div>' +
            '<div class="mcr-meta">' + prodCount + ' produk · Urutan ' + (c.ord ?? 0) + '</div>' +
          '</div>' +
        '</div>' +
        '<div class="mcr-actions">' +
          '<div style="width:10px;height:10px;border-radius:50%;background:' + (c.col || '#f97316') + ';"></div>' +
          '<span class="badge ' + (isActive ? 'success' : 'danger') + '" style="font-size:10px;">' + (isActive ? 'Aktif' : 'Nonaktif') + '</span>' +
          '<button class="btn btn-secondary" style="padding:4px 10px;font-size:12px;" onclick="window.MenuView.openCategoryModal(\'' + c.id + '\')">' +
            '<span class="material-symbols-outlined" style="font-size:13px;">edit</span> Edit' +
          '</button>' +
          '<button class="btn ' + (isActive ? 'btn-warning' : 'btn-success') + '" style="padding:4px 10px;font-size:12px;" onclick="window.MenuView.toggleCategory(\'' + c.id + '\')">' +
            (isActive ? 'Nonaktifkan' : 'Aktifkan') +
          '</button>' +
          (prodCount === 0 ? '<button class="btn btn-danger" style="padding:4px 10px;font-size:12px;" onclick="window.MenuView.deleteCategory(\'' + c.id + '\')"><span class="material-symbols-outlined" style="font-size:13px;">delete</span></button>' : '') +
        '</div>' +
      '</div>';
    }).join('');
  }

  // ─── Product Modal ─────────────────────────────────────────────────────

  openProductModal(productId) {
    const modal = document.getElementById('menu-product-modal');
    if (!modal) return;

    this.editingProduct = productId || null;
    const isEdit = !!productId;
    const p = isEdit ? window.State.products.find(x => String(x.id) === String(productId)) : null;

    document.getElementById('mprod-modal-title').textContent = isEdit ? 'Edit Produk' : 'Tambah Produk Baru';

    document.getElementById('mprod-name').value = p ? p.nm : '';
    document.getElementById('mprod-price').value = p ? p.hr : '';
    document.getElementById('mprod-cost').value = p ? (p.md || 0) : '';
    document.getElementById('mprod-emoji').value = p ? (p.emj || '🍢') : '🍢';
    document.getElementById('mprod-emoji-preview').textContent = p ? (p.emj || '🍢') : '🍢';
    document.getElementById('mprod-spice').value = p ? (p.lv || '') : '';
    document.getElementById('mprod-desc').value = p ? (p.desc || '') : '';
    document.getElementById('mprod-active').checked = p ? (p.on !== false) : true;

    const catSelect = document.getElementById('mprod-category');
    const categories = window.State.categories || [];
    catSelect.innerHTML = '<option value="">-- Pilih Kategori --</option>' +
      categories.map(c => '<option value="' + c.nm + '" ' + (p && p.kat === c.nm ? 'selected' : '') + '>' + (c.emj || '') + ' ' + c.nm + '</option>').join('');

    modal.classList.add('open');
    document.getElementById('mprod-name').focus();
  }

  saveProduct() {
    const name = document.getElementById('mprod-name').value.trim();
    const price = parseFloat(document.getElementById('mprod-price').value) || 0;
    const cost = parseFloat(document.getElementById('mprod-cost').value) || 0;
    const emoji = document.getElementById('mprod-emoji').value.trim() || '🍢';
    const category = document.getElementById('mprod-category').value;
    const spice = document.getElementById('mprod-spice').value.trim();
    const desc = document.getElementById('mprod-desc').value.trim();
    const isActive = document.getElementById('mprod-active').checked;

    if (!name) { window.State.toast('Nama produk wajib diisi!', 'error'); return; }
    if (!category) { window.State.toast('Pilih kategori produk!', 'error'); return; }
    if (price <= 0) { window.State.toast('Harga jual harus lebih dari 0!', 'error'); return; }

    if (this.editingProduct) {
      const idx = window.State.products.findIndex(p => String(p.id) === String(this.editingProduct));
      if (idx > -1) {
        window.State.products[idx] = {
          ...window.State.products[idx],
          nm: name, hr: price, md: cost, emj: emoji,
          kat: category, lv: spice, desc: desc, on: isActive
        };
        window.State.toast('Produk "' + name + '" berhasil diperbarui!', 'success');
      }
    } else {
      const newId = Date.now();
      window.State.products.push({
        id: newId, nm: name, hr: price, md: cost, emj: emoji,
        kat: category, lv: spice, desc: desc, on: isActive, bom: []
      });
      window.State.toast('Produk "' + name + '" berhasil ditambahkan!', 'success');
    }

    window.State.save(LS_KEYS.prod, window.State.products);
    document.getElementById('menu-product-modal').classList.remove('open');
    if (window.POSView) window.POSView.renderProducts();
    this.renderProductList();
  }

  toggleProduct(productId) {
    const idx = window.State.products.findIndex(p => String(p.id) === String(productId));
    if (idx === -1) return;
    window.State.products[idx].on = !(window.State.products[idx].on !== false);
    window.State.save(LS_KEYS.prod, window.State.products);
    if (window.POSView) window.POSView.renderProducts();
    this.renderProductList();
  }

  deleteProduct(productId) {
    const p = window.State.products.find(x => String(x.id) === String(productId));
    if (!p) return;
    if (!confirm('Hapus produk "' + p.nm + '"? Tindakan ini tidak bisa dibatalkan.')) return;
    window.State.products = window.State.products.filter(x => String(x.id) !== String(productId));
    window.State.save(LS_KEYS.prod, window.State.products);
    if (window.POSView) window.POSView.renderProducts();
    this.renderProductList();
    window.State.toast('Produk "' + p.nm + '" telah dihapus.', 'warning');
  }

  // ─── Category Modal ────────────────────────────────────────────────────

  openCategoryModal(categoryId) {
    const modal = document.getElementById('menu-category-modal');
    if (!modal) return;

    this.editingCategory = categoryId || null;
    const isEdit = !!categoryId;
    const c = isEdit ? window.State.categories.find(x => x.id === categoryId) : null;

    document.getElementById('mcat-modal-title').textContent = isEdit ? 'Edit Kategori' : 'Tambah Kategori Baru';

    document.getElementById('mcat-name').value = c ? c.nm : '';
    document.getElementById('mcat-emoji').value = c ? (c.emj || '🍽️') : '🍽️';
    document.getElementById('mcat-emoji-preview').textContent = c ? (c.emj || '🍽️') : '🍽️';
    document.getElementById('mcat-color').value = c ? (c.col || '#f97316') : '#f97316';
    document.getElementById('mcat-order').value = c ? (c.ord ?? 0) : (window.State.categories.length);
    document.getElementById('mcat-active').checked = c ? (c.on !== false) : true;

    modal.classList.add('open');
    document.getElementById('mcat-name').focus();
  }

  saveCategory() {
    const name = document.getElementById('mcat-name').value.trim();
    const emoji = document.getElementById('mcat-emoji').value.trim() || '🍽️';
    const color = document.getElementById('mcat-color').value || '#f97316';
    const order = parseInt(document.getElementById('mcat-order').value) || 0;
    const isActive = document.getElementById('mcat-active').checked;

    if (!name) { window.State.toast('Nama kategori wajib diisi!', 'error'); return; }

    if (this.editingCategory) {
      const idx = window.State.categories.findIndex(c => c.id === this.editingCategory);
      if (idx > -1) {
        const oldName = window.State.categories[idx].nm;
        // BUG-20 fix: cek duplikat nama saat edit (exclude diri sendiri)
        if (name.toLowerCase() !== oldName.toLowerCase() &&
            window.State.categories.some(c => c.id !== this.editingCategory && c.nm.toLowerCase() === name.toLowerCase())) {
          window.State.toast('Nama kategori sudah ada!', 'error');
          return;
        }
        window.State.categories[idx] = {
          ...window.State.categories[idx],
          nm: name, emj: emoji, col: color, ord: order, on: isActive
        };
        if (oldName !== name) {
          window.State.products.forEach(p => { if (p.kat === oldName) p.kat = name; });
          window.State.save(LS_KEYS.prod, window.State.products);
        }
        window.State.toast('Kategori "' + name + '" berhasil diperbarui!', 'success');
      }
    } else {
      if (window.State.categories.some(c => c.nm.toLowerCase() === name.toLowerCase())) {
        window.State.toast('Nama kategori sudah ada!', 'error');
        return;
      }
      const newId = 'k' + Date.now();
      window.State.categories.push({ id: newId, nm: name, emj: emoji, col: color, ord: order, on: isActive });
      window.State.toast('Kategori "' + name + '" berhasil ditambahkan!', 'success');
    }

    window.State.save(LS_KEYS.kat, window.State.categories);
    document.getElementById('menu-category-modal').classList.remove('open');
    if (window.POSView) window.POSView.renderProducts();
    this.renderCategoryFilterOptions();
    this.renderCategoryList();
  }

  toggleCategory(categoryId) {
    const idx = window.State.categories.findIndex(c => c.id === categoryId);
    if (idx === -1) return;
    window.State.categories[idx].on = !(window.State.categories[idx].on !== false);
    window.State.save(LS_KEYS.kat, window.State.categories);
    if (window.POSView) window.POSView.renderProducts();
    this.renderCategoryList();
  }

  deleteCategory(categoryId) {
    const c = window.State.categories.find(x => x.id === categoryId);
    if (!c) return;
    const prodCount = (window.State.products || []).filter(p => p.kat === c.nm).length;
    if (prodCount > 0) {
      window.State.toast('Kategori "' + c.nm + '" masih memiliki ' + prodCount + ' produk!', 'error');
      return;
    }
    if (!confirm('Hapus kategori "' + c.nm + '"?')) return;
    window.State.categories = window.State.categories.filter(x => x.id !== categoryId);
    window.State.save(LS_KEYS.kat, window.State.categories);
    this.render();
    window.State.toast('Kategori "' + c.nm + '" telah dihapus.', 'warning');
  }
}

window.MenuView = new MenuView();
