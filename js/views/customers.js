/**
 * KA POS v3.0 - Customer CRM & Loyalty Points Management
 */

class CustomersView {
  constructor() {
    this.searchQuery = '';
    this.selectedCustomerId = null;
  }

  init() {
    this.bindEvents();
    this.render();

    window.State.on('customers:updated', () => {
      this.render();
      this.updatePOSCustomerAutocomplete();
    });
  }

  bindEvents() {
    // Search input
    const searchInput = document.getElementById('cust-search-input');
    if (searchInput) {
      searchInput.oninput = (e) => {
        this.searchQuery = e.target.value.toLowerCase().trim();
        this.renderList();
      };
    }

    // Add Customer Form
    const form = document.getElementById('cust-form');
    if (form) {
      form.onsubmit = (e) => {
        e.preventDefault();
        this.saveCustomerForm();
      };
    }

    // Add New Customer Modal trigger
    const btnAdd = document.getElementById('btn-add-customer');
    if (btnAdd) {
      btnAdd.onclick = () => this.openCustomerModal();
    }
  }

  render() {
    this.renderKPIs();
    this.renderList();
  }

  renderKPIs() {
    const custs = window.State.customers || [];
    const totalCust = custs.length;
    const totalPoints = custs.reduce((sum, c) => sum + (Number(c.poin) || 0), 0);
    const totalSpend = custs.reduce((sum, c) => sum + (Number(c.totalSpend) || 0), 0);

    const countEl = document.getElementById('cust-kpi-total');
    const pointsEl = document.getElementById('cust-kpi-points');
    const spendEl = document.getElementById('cust-kpi-spend');

    if (countEl) countEl.textContent = `${totalCust} Member`;
    if (pointsEl) pointsEl.textContent = `${totalPoints.toLocaleString('id-ID')} Poin`;
    if (spendEl) spendEl.textContent = window.State.formatRp(totalSpend);
  }

  renderList() {
    const container = document.getElementById('cust-table-body');
    if (!container) return;

    let custs = window.State.customers || [];
    if (this.searchQuery) {
      custs = custs.filter(c => 
        (c.nm && c.nm.toLowerCase().includes(this.searchQuery)) ||
        (c.hp && c.hp.includes(this.searchQuery))
      );
    }

    if (custs.length === 0) {
      container.innerHTML = `
        <tr>
          <td colspan="6" style="text-align:center;padding:32px;color:var(--secondary);">
            <div style="font-size:28px;margin-bottom:8px;">👥</div>
            <div>Tidak ada data pelanggan yang cocok.</div>
          </td>
        </tr>
      `;
      return;
    }

    container.innerHTML = custs.map(c => `
      <tr style="border-bottom:1px solid rgba(255,255,255,0.05);transition:background var(--transition-fast);">
        <td style="padding:12px 14px;font-weight:700;color:var(--on-surface);">
          <div style="display:flex;align-items:center;gap:10px;">
            <div style="width:34px;height:34px;border-radius:50%;background:rgba(249,115,22,0.15);color:var(--primary);display:flex;align-items:center;justify-content:center;font-weight:800;font-size:14px;">
              ${(c.nm || 'U').charAt(0).toUpperCase()}
            </div>
            <div>
              <div>${c.nm || 'Umum'}</div>
              <div style="font-size:10px;color:var(--secondary);">${c.id || '-'}</div>
            </div>
          </div>
        </td>
        <td style="padding:12px 14px;font-family:var(--font-mono);font-size:13px;color:var(--secondary);">
          ${c.hp || '-'}
        </td>
        <td style="padding:12px 14px;">
          <span style="display:inline-flex;align-items:center;gap:4px;padding:3px 8px;border-radius:var(--radius-full);background:rgba(245,158,11,0.15);color:var(--warning);font-weight:700;font-size:12px;font-family:var(--font-mono);">
            ⭐ ${Number(c.poin) || 0} Poin
          </span>
        </td>
        <td style="padding:12px 14px;font-size:13px;color:var(--on-surface);">
          ${Number(c.trxCount) || 0}x Order
        </td>
        <td style="padding:12px 14px;font-family:var(--font-mono);font-weight:700;color:var(--primary);">
          ${window.State.formatRp(c.totalSpend || 0)}
        </td>
        <td style="padding:12px 14px;text-align:right;">
          <div style="display:inline-flex;gap:6px;">
            <button class="btn btn-secondary" style="padding:4px 8px;font-size:11px;" onclick="window.CustomersView.selectForPOS('${c.id}')" title="Pilih ke POS">
              <span class="material-symbols-outlined" style="font-size:15px;color:var(--tertiary);">point_of_sale</span>
            </button>
            <button class="btn btn-secondary" style="padding:4px 8px;font-size:11px;" onclick="window.CustomersView.openCustomerModal('${c.id}')" title="Edit Pelanggan">
              <span class="material-symbols-outlined" style="font-size:15px;">edit</span>
            </button>
            <button class="btn btn-secondary" style="padding:4px 8px;font-size:11px;" onclick="window.CustomersView.deleteCustomerPrompt('${c.id}')" title="Hapus Pelanggan">
              <span class="material-symbols-outlined" style="font-size:15px;color:var(--error);">delete</span>
            </button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  openCustomerModal(customerId = null) {
    const modal = document.getElementById('cust-modal');
    const title = document.getElementById('cust-modal-title');
    const idInput = document.getElementById('cust-id-input');
    const nameInput = document.getElementById('cust-name-input');
    const phoneInput = document.getElementById('cust-phone-input');
    const pointsInput = document.getElementById('cust-points-input');

    if (customerId) {
      const c = (window.State.customers || []).find(item => item.id === customerId);
      if (c) {
        if (title) title.textContent = 'Edit Data Pelanggan';
        if (idInput) idInput.value = c.id;
        if (nameInput) nameInput.value = c.nm || '';
        if (phoneInput) phoneInput.value = c.hp || '';
        if (pointsInput) pointsInput.value = c.poin || 0;
      }
    } else {
      if (title) title.textContent = 'Tambah Pelanggan Baru';
      if (idInput) idInput.value = '';
      if (nameInput) nameInput.value = '';
      if (phoneInput) phoneInput.value = '';
      if (pointsInput) pointsInput.value = '0';
    }

    if (modal) modal.classList.add('open');
  }

  saveCustomerForm() {
    const id = document.getElementById('cust-id-input')?.value;
    const nm = document.getElementById('cust-name-input')?.value.trim();
    const hp = document.getElementById('cust-phone-input')?.value.trim();
    const poin = Number(document.getElementById('cust-points-input')?.value) || 0;

    if (!nm) {
      window.State.toast('Nama pelanggan wajib diisi!', 'warning');
      return;
    }

    const saved = window.State.saveCustomer({
      id: id || undefined,
      nm,
      hp: hp || '-',
      poin
    });

    window.State.logAudit(id ? 'CUSTOMER_UPDATED' : 'CUSTOMER_CREATED', { id: saved.id, nm });
    window.State.toast(`Pelanggan "${nm}" berhasil disimpan!`, 'success');

    const modal = document.getElementById('cust-modal');
    if (modal) modal.classList.remove('open');
    this.render();
  }

  deleteCustomerPrompt(id) {
    const c = (window.State.customers || []).find(item => item.id === id);
    if (!c) return;
    if (confirm(`Hapus pelanggan "${c.nm}"? Data poin dan riwayat akan dihapus.`)) {
      window.State.deleteCustomer(id);
      window.State.logAudit('CUSTOMER_DELETED', { id, nm: c.nm });
      window.State.toast(`Pelanggan "${c.nm}" dihapus`, 'info');
      this.render();
    }
  }

  selectForPOS(id) {
    const c = (window.State.customers || []).find(item => item.id === id);
    if (!c) return;

    window.State.currentCustomerName = c.nm;
    const input = document.getElementById('pos-customer-name');
    if (input) input.value = c.nm;

    window.State.toast(`Pelanggan "${c.nm}" dipilih untuk POS`, 'success');
    if (window.Router) window.Router.navigate('pos');
  }

  updatePOSCustomerAutocomplete() {
    const datalist = document.getElementById('pos-customer-datalist');
    if (!datalist) return;

    datalist.innerHTML = (window.State.customers || []).map(c => 
      `<option value="${c.nm}">${c.hp !== '-' ? c.hp + ' • ' : ''}${c.poin} Poin</option>`
    ).join('');
  }
}

window.CustomersView = new CustomersView();
