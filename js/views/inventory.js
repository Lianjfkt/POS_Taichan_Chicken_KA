/**
 * KA POS v3.0 - Inventory, Recipe BOM & Fast Stock Update
 */

class InventoryView {
  constructor() {
    this.searchQuery = '';
    this.activeTab = 'raw'; // 'raw', 'bom', or 'sambal'
    this.activeEditingProduct = null;
  }

  init() {
    this.bindEvents();
    this.render();

    window.State.on(LS_KEYS.inv, () => this.render());
    window.State.on(LS_KEYS.prod, () => this.render());
  }

  bindEvents() {
    // Search inventory & menu
    const searchInput = document.getElementById('inventory-search');
    if (searchInput) {
      searchInput.oninput = (e) => {
        this.searchQuery = e.target.value.toLowerCase().trim();
        this.render();
      };
    }

    // Subtab Buttons (Bahan Baku / Resep BOM / Sambal / Stock Harian)
    document.querySelectorAll('.inv-subtab-btn').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('.inv-subtab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.activeTab = btn.getAttribute('data-tab');

        const rawTab    = document.getElementById('inventory-raw-tab');
        const bomTab    = document.getElementById('inventory-bom-tab');
        const sambalTab = document.getElementById('inventory-sambal-tab');
        const stockTab  = document.getElementById('inventory-stock-harian-tab');

        if (rawTab)    rawTab.style.display    = (this.activeTab === 'raw')           ? 'flex' : 'none';
        if (bomTab)    bomTab.style.display    = (this.activeTab === 'bom')           ? 'flex' : 'none';
        if (sambalTab) sambalTab.style.display = (this.activeTab === 'sambal')        ? 'flex' : 'none';
        if (stockTab)  stockTab.style.display  = (this.activeTab === 'stock_harian')  ? 'flex' : 'none';

        // Init stock tracker on first open
        if (this.activeTab === 'stock_harian' && window.StockTracker) {
          window.StockTracker.render();
        }
      };
    });

    // Add Item Form Submit
    const addForm = document.getElementById('add-inventory-form');
    if (addForm) {
      addForm.onsubmit = (e) => {
        e.preventDefault();
        this.handleAddItem();
      };
    }

    // Stock Adjust Form Submit
    const adjustForm = document.getElementById('adjust-stock-form');
    if (adjustForm) {
      adjustForm.onsubmit = (e) => {
        e.preventDefault();
        this.handleAdjustStock();
      };
    }

    // Edit Recipe Form Submit
    const recipeForm = document.getElementById('edit-recipe-form');
    if (recipeForm) {
      recipeForm.onsubmit = (e) => {
        e.preventDefault();
        this.handleSaveRecipe();
      };
    }

    // Add Recipe Row Button
    const btnAddRow = document.getElementById('btn-add-recipe-row');
    if (btnAddRow) {
      btnAddRow.onclick = () => this.addRecipeIngredientRow();
    }

    // Sambal Form Submit
    const sambalForm = document.getElementById('sambal-form');
    if (sambalForm) {
      sambalForm.onsubmit = (e) => {
        e.preventDefault();
        this.handleSaveSambal();
      };
    }
  }

  render() {
    this.renderRawTable();
    this.renderBomTable();
    this.renderFastStock();
    this.renderSambalTable();
  }

  // 1. Render Raw Ingredients Table
  renderRawTable() {
    const tbody = document.getElementById('inventory-table-tbody');
    if (!tbody) return;

    let items = window.State.inventory || [];
    if (this.searchQuery) {
      items = items.filter(i => 
        i.nm.toLowerCase().includes(this.searchQuery) ||
        (i.kat && i.kat.toLowerCase().includes(this.searchQuery))
      );
    }

    if (items.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:var(--secondary)">Tidak ada bahan baku ditemukan</td></tr>`;
      return;
    }

    tbody.innerHTML = items.map(item => {
      const isCritical = item.stok <= item.min;
      const isOut = item.stok <= 0;
      return `
        <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
          <td style="padding:12px 16px;display:flex;align-items:center;gap:10px">
            <span style="font-size:24px">${item.emj || '📦'}</span>
            <div>
              <div style="font-weight:700;color:var(--on-surface)">${item.nm}</div>
              <div style="font-size:11px;color:var(--secondary)">${item.kat || 'Umum'}</div>
            </div>
          </td>
          <td style="padding:12px 16px;color:var(--secondary);font-size:13px;">${item.kat || '-'}</td>
          <td class="font-mono" style="padding:12px 16px;font-weight:700;color:${isOut ? 'var(--error)' : (isCritical ? 'var(--warning)' : 'var(--tertiary)')}">
            ${item.stok} ${item.sat}
            ${isOut ? '<span class="badge error" style="margin-left:6px;">HABIS</span>' : (isCritical ? '<span class="badge warning" style="margin-left:6px;">MENIPIS</span>' : '')}
          </td>
          <td class="font-mono" style="padding:12px 16px;color:var(--secondary)">${item.min} ${item.sat}</td>
          <td class="font-mono" style="padding:12px 16px;color:var(--primary)">${window.State.formatRp(item.hr)}</td>
          <td class="font-mono" style="padding:12px 16px;font-weight:700">${window.State.formatRp(item.stok * item.hr)}</td>
          <td style="padding:12px 16px;text-align:right">
            <button class="btn btn-secondary" style="padding:6px 12px;font-size:11px" onclick="window.InventoryView.openAdjustModal(${item.id})">
              <span class="material-symbols-outlined" style="font-size:14px">tune</span> Sesuaikan
            </button>
          </td>
        </tr>
      `;
    }).join('');
  }

  // 2. Render Recipe BOM & HPP Table
  renderBomTable() {
    const tbody = document.getElementById('inventory-bom-tbody');
    if (!tbody) return;

    let products = window.State.products || [];
    if (this.searchQuery) {
      products = products.filter(p => 
        p.nm.toLowerCase().includes(this.searchQuery) ||
        (p.kat && p.kat.toLowerCase().includes(this.searchQuery))
      );
    }

    if (products.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:24px;color:var(--secondary)">Tidak ada produk ditemukan</td></tr>`;
      return;
    }

    tbody.innerHTML = products.map(product => {
      const hpp = window.State.calculateProductHPP(product);
      const profit = product.hr - hpp;
      const marginPct = product.hr > 0 ? ((profit / product.hr) * 100).toFixed(1) : 0;

      // Format ingredients pills
      let ingredientsHtml = '';
      if (product.bom && product.bom.length > 0) {
        ingredientsHtml = product.bom.map(b => {
          const inv = (window.State.inventory || []).find(i => i.id === b.invId);
          if (!inv) return '';
          return `<span style="display:inline-flex;align-items:center;gap:4px;background:var(--surface-container-high);padding:3px 8px;border-radius:var(--radius-full);font-size:11px;margin:2px;">
            ${inv.emj || '📦'} ${b.qty} ${inv.sat} ${inv.nm}
          </span>`;
        }).filter(Boolean).join('');
      } else {
        ingredientsHtml = `<span style="font-size:11px;color:var(--secondary);font-style:italic;">Belum ada resep bahan baku</span>`;
      }

      return `
        <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
          <td style="padding:12px 16px;display:flex;align-items:center;gap:10px">
            <span style="font-size:24px">${product.emj || '🍢'}</span>
            <div>
              <div style="font-weight:700;color:var(--on-surface)">${product.nm}</div>
              <div style="font-size:11px;color:var(--secondary)">${product.kat}</div>
            </div>
          </td>
          <td style="padding:12px 16px;max-width:320px;">
            <div style="display:flex;flex-wrap:wrap;">${ingredientsHtml}</div>
          </td>
          <td class="font-mono" style="padding:12px 16px;font-weight:700;color:var(--primary);">${window.State.formatRp(hpp)}</td>
          <td class="font-mono" style="padding:12px 16px;font-weight:700;">${window.State.formatRp(product.hr)}</td>
          <td class="font-mono" style="padding:12px 16px;font-weight:700;color:${marginPct >= 50 ? 'var(--tertiary)' : 'var(--warning)'};">
            ${marginPct}% <span style="font-size:11px;font-weight:400;color:var(--secondary);">(${window.State.formatRp(profit)})</span>
          </td>
          <td style="padding:12px 16px;text-align:right">
            <button class="btn btn-secondary" style="padding:6px 12px;font-size:11px;" onclick="window.InventoryView.openRecipeModal(${product.id})">
              <span class="material-symbols-outlined" style="font-size:14px">restaurant_menu</span> Atur Resep
            </button>
          </td>
        </tr>
      `;
    }).join('');
  }

  // 3. Render Fast Stock Update Grid (Cashier Quick Modal)
  renderFastStock() {
    const fastStockGrid = document.getElementById('fast-stock-grid');
    if (!fastStockGrid) return;

    const products = window.State.products.filter(p => p.on !== false);
    fastStockGrid.innerHTML = products.map(p => `
      <div style="background:var(--surface-container);border:1px solid rgba(255,255,255,0.06);border-radius:var(--radius-md);padding:12px;display:flex;align-items:center;justify-content:space-between">
        <div style="display:flex;align-items:center;gap:8px">
          <span style="font-size:24px">${p.emj || '🍢'}</span>
          <div>
            <div style="font-weight:700;font-size:13px">${p.nm}</div>
            <div style="font-size:11px;color:var(--primary)">${window.State.formatRp(p.hr)}</div>
          </div>
        </div>
        <div style="display:flex;align-items:center;gap:6px">
          <button class="btn ${p.on ? 'btn-primary' : 'btn-secondary'}" style="padding:6px 12px;font-size:11px" onclick="window.InventoryView.toggleProductAvailability(${p.id})">
            ${p.on ? 'Tersedia' : 'Habis'}
          </button>
        </div>
      </div>
    `).join('');
  }

  toggleProductAvailability(productId) {
    const product = window.State.products.find(p => p.id === productId);
    if (!product) return;

    product.on = !product.on;
    window.State.save(LS_KEYS.prod, window.State.products);
    this.render();
    window.State.toast(`Status ${product.nm}: ${product.on ? 'Tersedia' : 'Habis'}`, 'success');
  }

  openAdjustModal(itemId) {
    const item = window.State.inventory.find(i => i.id === itemId);
    if (!item) return;

    const modal = document.getElementById('adjust-stock-modal');
    const nameEl = document.getElementById('adjust-item-name');
    const curStockEl = document.getElementById('adjust-current-stock');
    const idInput = document.getElementById('adjust-item-id');

    if (nameEl) nameEl.textContent = item.nm;
    if (curStockEl) curStockEl.textContent = `${item.stok} ${item.sat}`;
    if (idInput) idInput.value = item.id;

    if (modal) modal.classList.add('open');
  }

  handleAdjustStock() {
    const idInput = document.getElementById('adjust-item-id');
    const typeSelect = document.getElementById('adjust-type');
    const qtyInput = document.getElementById('adjust-qty');
    const reasonInput = document.getElementById('adjust-reason');

    if (!idInput || !typeSelect || !qtyInput) return;

    const itemId = Number(idInput.value);
    const item = window.State.inventory.find(i => i.id === itemId);
    if (!item) return;

    const type = typeSelect.value;
    const qty = Number(qtyInput.value) || 0;
    const reason = reasonInput ? reasonInput.value.trim() : '';

    if (qty <= 0 && type !== 'set') {
      window.State.toast('Masukkan jumlah yang valid!', 'error');
      return;
    }

    const prevStock = item.stok;
    if (type === 'tambah') item.stok = +(item.stok + qty).toFixed(3);
    else if (type === 'kurang') item.stok = Math.max(0, +(item.stok - qty).toFixed(3));
    else if (type === 'set') item.stok = qty;

    window.State.save(LS_KEYS.inv, window.State.inventory);

    // Record Mutation Log
    window.State.stockMutations.unshift({
      id: Date.now(),
      itemId: item.id,
      itemNm: item.nm,
      tgl: Date.now(),
      tipe: type,
      jml: qty,
      sat: item.sat,
      stokAwal: prevStock,
      stokAkhir: item.stok,
      ket: reason || 'Penyesuaian stok manual'
    });
    window.State.save(LS_KEYS.mut, window.State.stockMutations);

    const modal = document.getElementById('adjust-stock-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Stok ${item.nm} diperbarui menjadi ${item.stok} ${item.sat}`, 'success');
    this.render();
  }

  handleAddItem() {
    const nameInput = document.getElementById('add-inv-name');
    const catInput  = document.getElementById('add-inv-category');
    const unitInput = document.getElementById('add-inv-unit');
    const stockInput= document.getElementById('add-inv-stock');
    const minInput  = document.getElementById('add-inv-min');
    const priceInput= document.getElementById('add-inv-price');

    if (!nameInput || !nameInput.value.trim()) {
      window.State.toast('Nama bahan wajib diisi!', 'error');
      return;
    }

    const newItem = {
      id: Date.now(),
      nm: nameInput.value.trim(),
      kat: catInput ? catInput.value.trim() : 'Umum',
      sat: unitInput ? unitInput.value.trim() : 'pcs',
      stok: Number(stockInput.value) || 0,
      min: Number(minInput.value) || 5,
      hr: Number(priceInput.value) || 0,
      emj: '📦'
    };

    window.State.inventory.push(newItem);
    window.State.save(LS_KEYS.inv, window.State.inventory);

    const modal = document.getElementById('add-inventory-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Bahan ${newItem.nm} berhasil ditambahkan!`, 'success');
    this.render();
  }

  // --- Recipe BOM Editor Operations ---
  openRecipeModal(productId) {
    const product = window.State.products.find(p => p.id === productId);
    if (!product) return;

    this.activeEditingProduct = product;
    if (!product.bom) product.bom = [];

    const modal = document.getElementById('edit-recipe-modal');
    const titleEl = document.getElementById('edit-recipe-product-name');
    const sellPriceEl = document.getElementById('edit-recipe-sell-price');
    const idInput = document.getElementById('edit-recipe-product-id');
    const container = document.getElementById('recipe-ingredients-list');

    if (titleEl) titleEl.textContent = `${product.emj || '🍢'} ${product.nm}`;
    if (sellPriceEl) sellPriceEl.textContent = window.State.formatRp(product.hr);
    if (idInput) idInput.value = product.id;

    if (container) {
      container.innerHTML = '';
      if (product.bom.length === 0) {
        this.addRecipeIngredientRow();
      } else {
        product.bom.forEach(b => {
          this.addRecipeIngredientRow(b.invId, b.qty);
        });
      }
    }

    this.updateRecipeLiveHPP();
    if (modal) modal.classList.add('open');
  }

  addRecipeIngredientRow(selectedInvId = '', qty = 1) {
    const container = document.getElementById('recipe-ingredients-list');
    if (!container) return;

    const inventoryItems = window.State.inventory || [];
    const rowId = 'row-' + Date.now() + Math.random().toString(36).substr(2, 4);

    const row = document.createElement('div');
    row.className = 'recipe-row';
    row.id = rowId;
    row.style = 'display:flex;align-items:center;gap:8px;background:var(--surface-container);padding:8px 12px;border-radius:var(--radius-md);border:1px solid rgba(255,255,255,0.06);';

    let optionsHtml = inventoryItems.map(item => `
      <option value="${item.id}" ${item.id == selectedInvId ? 'selected' : ''}>
        ${item.emj || '📦'} ${item.nm} (${window.State.formatRp(item.hr)}/${item.sat})
      </option>
    `).join('');

    row.innerHTML = `
      <select class="form-select recipe-inv-select" style="flex:2;font-size:12px;padding:6px 10px;">
        <option value="">-- Pilih Bahan Baku --</option>
        ${optionsHtml}
      </select>
      <div style="display:flex;align-items:center;gap:4px;flex:1;">
        <input type="number" step="0.01" min="0.001" class="form-input font-mono recipe-qty-input" value="${qty}" style="padding:6px;font-size:12px;text-align:center;">
        <span class="recipe-unit-label font-mono" style="font-size:11px;color:var(--secondary);min-width:28px;">sat</span>
      </div>
      <button type="button" class="btn btn-secondary" style="padding:6px 8px;color:var(--error);" onclick="document.getElementById('${rowId}').remove();window.InventoryView.updateRecipeLiveHPP();">
        <span class="material-symbols-outlined" style="font-size:16px;">delete</span>
      </button>
    `;

    container.appendChild(row);

    const selectEl = row.querySelector('.recipe-inv-select');
    const qtyEl = row.querySelector('.recipe-qty-input');
    const unitEl = row.querySelector('.recipe-unit-label');

    const updateUnit = () => {
      const selected = inventoryItems.find(i => i.id == selectEl.value);
      if (selected && unitEl) unitEl.textContent = selected.sat;
      this.updateRecipeLiveHPP();
    };

    selectEl.onchange = updateUnit;
    qtyEl.oninput = () => this.updateRecipeLiveHPP();

    updateUnit();
  }

  updateRecipeLiveHPP() {
    const liveHppEl = document.getElementById('edit-recipe-live-hpp');
    const container = document.getElementById('recipe-ingredients-list');
    if (!container || !liveHppEl) return;

    let totalHPP = 0;
    const inventoryItems = window.State.inventory || [];

    container.querySelectorAll('.recipe-row').forEach(row => {
      const select = row.querySelector('.recipe-inv-select');
      const qtyInput = row.querySelector('.recipe-qty-input');
      if (select && qtyInput && select.value) {
        const item = inventoryItems.find(i => i.id == select.value);
        const qty = Number(qtyInput.value) || 0;
        if (item) {
          totalHPP += (item.hr || 0) * qty;
        }
      }
    });

    liveHppEl.textContent = window.State.formatRp(totalHPP);
  }

  handleSaveRecipe() {
    if (!this.activeEditingProduct) return;

    const container = document.getElementById('recipe-ingredients-list');
    const newBOM = [];

    if (container) {
      container.querySelectorAll('.recipe-row').forEach(row => {
        const select = row.querySelector('.recipe-inv-select');
        const qtyInput = row.querySelector('.recipe-qty-input');
        if (select && qtyInput && select.value) {
          const invId = Number(select.value);
          const qty = Number(qtyInput.value) || 0;
          if (invId && qty > 0) {
            newBOM.push({ invId, qty });
          }
        }
      });
    }

    this.activeEditingProduct.bom = newBOM;
    this.activeEditingProduct.md = window.State.calculateProductHPP(this.activeEditingProduct);

    window.State.save(LS_KEYS.prod, window.State.products);

    const modal = document.getElementById('edit-recipe-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Resep ${this.activeEditingProduct.nm} berhasil disimpan! (HPP: ${window.State.formatRp(this.activeEditingProduct.md)})`, 'success');
    this.render();
  }

  // =============================================
  // SAMBAL MANAGEMENT
  // =============================================

  renderSambalTable() {
    const tbody = document.getElementById('inventory-sambal-tbody');
    if (!tbody) return;

    const list = window.State.sambalList || [];

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:32px;color:var(--secondary);">Belum ada variasi sambal. Klik "Tambah Sambal" untuk mulai.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map((s, i) => `
      <tr>
        <td>${i + 1}</td>
        <td style="font-weight:700;">
          <span class="material-symbols-outlined" style="font-size:14px;vertical-align:middle;color:var(--primary);">local_fire_department</span>
          ${s.nm}
        </td>
        <td class="font-mono">${s.hr > 0 ? `+${window.State.formatRp(s.hr)}` : '<span style="color:var(--secondary);">Gratis</span>'}</td>
        <td>
          <span style="padding:3px 10px;border-radius:var(--radius-pill);font-size:11px;font-weight:700;background:${s.aktif !== false ? 'rgba(76,175,80,0.15)' : 'rgba(255,255,255,0.06)'};color:${s.aktif !== false ? '#4caf50' : 'var(--secondary)'};"
          >${s.aktif !== false ? 'Aktif' : 'Nonaktif'}</span>
        </td>
        <td style="text-align:right;">
          <div style="display:flex;gap:6px;justify-content:flex-end;">
            <button class="btn btn-secondary" style="padding:4px 10px;font-size:11px;" onclick="window.InventoryView.openSambalModal('${s.id}')">
              <span class="material-symbols-outlined" style="font-size:14px;">edit</span>
            </button>
            <button class="btn btn-danger" style="padding:4px 10px;font-size:11px;" onclick="window.InventoryView.deleteSambal('${s.id}')">
              <span class="material-symbols-outlined" style="font-size:14px;">delete</span>
            </button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  openSambalModal(editId = null) {
    const modal = document.getElementById('sambal-modal');
    const titleEl = document.getElementById('sambal-modal-title');
    const idInput = document.getElementById('sambal-edit-id');
    const nameInput = document.getElementById('sambal-name');
    const priceInput = document.getElementById('sambal-price');
    const aktifInput = document.getElementById('sambal-aktif');
    if (!modal) return;

    if (editId) {
      const item = (window.State.sambalList || []).find(s => s.id === editId);
      if (!item) return;
      if (titleEl) titleEl.textContent = 'Edit Variasi Sambal';
      if (idInput) idInput.value = item.id;
      if (nameInput) nameInput.value = item.nm;
      if (priceInput) priceInput.value = item.hr || 0;
      if (aktifInput) aktifInput.checked = item.aktif !== false;
    } else {
      if (titleEl) titleEl.textContent = 'Tambah Variasi Sambal';
      if (idInput) idInput.value = '';
      if (nameInput) nameInput.value = '';
      if (priceInput) priceInput.value = 0;
      if (aktifInput) aktifInput.checked = true;
    }

    modal.classList.add('open');
    if (nameInput) nameInput.focus();
  }

  handleSaveSambal() {
    const idInput = document.getElementById('sambal-edit-id');
    const nameInput = document.getElementById('sambal-name');
    const priceInput = document.getElementById('sambal-price');
    const aktifInput = document.getElementById('sambal-aktif');

    const nm = nameInput ? nameInput.value.trim() : '';
    if (!nm) { window.State.toast('Nama sambal wajib diisi!', 'warning'); return; }

    const item = {
      id: (idInput && idInput.value) ? idInput.value : 'SBL-' + Date.now(),
      nm: nm,
      hr: Number(priceInput ? priceInput.value : 0) || 0,
      aktif: aktifInput ? aktifInput.checked : true
    };

    window.State.addOrUpdateSambal(item);

    const modal = document.getElementById('sambal-modal');
    if (modal) modal.classList.remove('open');

    this.renderSambalTable();
    window.State.toast(`Sambal "${item.nm}" berhasil disimpan!`, 'success');
  }

  deleteSambal(id) {
    if (!confirm('Hapus variasi sambal ini?')) return;
    window.State.deleteSambal(id);
    this.renderSambalTable();
    window.State.toast('Variasi sambal dihapus.', 'warning');
  }
}

window.InventoryView = new InventoryView();
