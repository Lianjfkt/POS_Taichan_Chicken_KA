/**
 * KA POS v3.0 - Inventory, Recipe BOM & Fast Stock Update
 */

class InventoryView {
  constructor() {
    this.searchQuery = '';
    this.filterCategory = 'all';
  }

  init() {
    this.bindEvents();
    this.render();

    window.State.on(LS_KEYS.inv, () => this.render());
  }

  bindEvents() {
    // Search inventory
    const searchInput = document.getElementById('inventory-search');
    if (searchInput) {
      searchInput.oninput = (e) => {
        this.searchQuery = e.target.value.toLowerCase().trim();
        this.render();
      };
    }

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
  }

  render() {
    const tbody = document.getElementById('inventory-table-tbody');
    const fastStockGrid = document.getElementById('fast-stock-grid');

    let items = window.State.inventory;

    if (this.searchQuery) {
      items = items.filter(i => 
        i.nm.toLowerCase().includes(this.searchQuery) ||
        (i.kat && i.kat.toLowerCase().includes(this.searchQuery))
      );
    }

    // 1. Render Table for Owner Inventory View
    if (tbody) {
      if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:24px;color:var(--secondary)">Tidak ada item inventaris ditemukan</td></tr>`;
      } else {
        tbody.innerHTML = items.map(item => {
          const isCritical = item.stok <= item.min;
          return `
            <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
              <td style="padding:12px 16px;display:flex;align-items:center;gap:10px">
                <span style="font-size:24px">${item.emj || '📦'}</span>
                <div>
                  <div style="font-weight:700;color:var(--on-surface)">${item.nm}</div>
                  <div style="font-size:11px;color:var(--secondary)">${item.kat || 'Umum'}</div>
                </div>
              </td>
              <td class="font-mono" style="padding:12px 16px;font-weight:700;color:${isCritical ? 'var(--error)' : 'var(--on-surface)'}">
                ${item.stok} ${item.sat}
                ${isCritical ? '<span class="status-pill offline" style="font-size:9px;margin-left:6px;padding:2px 6px">KRITIS</span>' : ''}
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
    }

    // 2. Render Fast Stock Update Grid (Cashier Quick Modal)
    if (fastStockGrid) {
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

    const type = typeSelect.value; // 'tambah', 'kurang', 'set'
    const qty = Number(qtyInput.value) || 0;
    const reason = reasonInput ? reasonInput.value.trim() : '';

    if (qty <= 0 && type !== 'set') {
      window.State.toast('Masukkan jumlah yang valid!', 'error');
      return;
    }

    const prevStock = item.stok;
    if (type === 'tambah') item.stok += qty;
    else if (type === 'kurang') item.stok = Math.max(0, item.stok - qty);
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
      stokAwal: prevStock,
      stokAkhir: item.stok,
      ket: reason || 'Penyesuaian stok manual'
    });
    window.State.save(LS_KEYS.mut, window.State.stockMutations);

    const modal = document.getElementById('adjust-stock-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Stok ${item.nm} berhasil diperbarui menjadi ${item.stok} ${item.sat}`, 'success');
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
}

window.InventoryView = new InventoryView();
