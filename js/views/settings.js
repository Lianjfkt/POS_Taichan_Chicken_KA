/**
 * KA POS v3.0 - Settings, Supabase Cloud Configuration & Staff Management
 */

class SettingsView {
  constructor() {}

  init() {
    this.bindEvents();
    this.render();
  }

  bindEvents() {
    // Store Settings Form
    const storeForm = document.getElementById('store-settings-form');
    if (storeForm) {
      storeForm.onsubmit = (e) => {
        e.preventDefault();
        this.saveStoreSettings();
      };
    }

    // Supabase Config Form
    const sbForm = document.getElementById('supabase-config-form');
    if (sbForm) {
      sbForm.onsubmit = (e) => {
        e.preventDefault();
        this.saveSupabaseConfig();
      };
    }

    // Test Supabase Connection Button
    const btnTestSb = document.getElementById('btn-test-supabase');
    if (btnTestSb) {
      btnTestSb.onclick = () => this.testSupabaseConnection();
    }

    // Push Local Data to Supabase Button
    const btnPushSb = document.getElementById('btn-push-supabase');
    if (btnPushSb) {
      btnPushSb.onclick = () => this.pushDataToSupabase();
    }

    // Bluetooth Printer Pair Button
    const btnPairBt = document.getElementById('btn-pair-printer');
    if (btnPairBt) {
      btnPairBt.onclick = () => {
        if (window.PrinterService) window.PrinterService.connectBluetooth();
      };
    }

    // Test Print Button
    const btnTestPrint = document.getElementById('btn-test-print');
    if (btnTestPrint) {
      btnTestPrint.onclick = () => this.printTestReceipt();
    }

    // Add Staff Form
    const staffForm = document.getElementById('add-staff-form');
    if (staffForm) {
      staffForm.onsubmit = (e) => {
        e.preventDefault();
        this.handleAddStaff();
      };
    }
  }

  render() {
    const s = window.State.settings;

    // Fill store inputs
    const nmInput = document.getElementById('set-store-name');
    const addrInput = document.getElementById('set-store-addr');
    const hpInput = document.getElementById('set-store-hp');
    const footerInput = document.getElementById('set-store-footer');
    const rekInput = document.getElementById('set-store-rek');
    const norekInput = document.getElementById('set-store-norek');

    if (nmInput) nmInput.value = s.nm || '';
    if (addrInput) addrInput.value = s.addr || '';
    if (hpInput) hpInput.value = s.hp || '';
    if (footerInput) footerInput.value = s.footer || '';
    if (rekInput) rekInput.value = s.rek || '';
    if (norekInput) norekInput.value = s.norek || '';

    // Fill Supabase inputs
    const sb = window.State.supabaseConfig;
    const sbUrlInput = document.getElementById('set-sb-url');
    const sbKeyInput = document.getElementById('set-sb-key');

    if (sbUrlInput && sb) sbUrlInput.value = sb.url || '';
    if (sbKeyInput && sb) sbKeyInput.value = sb.anonKey || '';

    this.renderStaffList();
  }

  saveStoreSettings() {
    const nmInput = document.getElementById('set-store-name');
    const addrInput = document.getElementById('set-store-addr');
    const hpInput = document.getElementById('set-store-hp');
    const footerInput = document.getElementById('set-store-footer');
    const rekInput = document.getElementById('set-store-rek');
    const norekInput = document.getElementById('set-store-norek');

    window.State.settings = {
      ...window.State.settings,
      nm: nmInput ? nmInput.value.trim() : '',
      addr: addrInput ? addrInput.value.trim() : '',
      hp: hpInput ? hpInput.value.trim() : '',
      footer: footerInput ? footerInput.value.trim() : '',
      rek: rekInput ? rekInput.value.trim() : '',
      norek: norekInput ? norekInput.value.trim() : ''
    };

    window.State.save(LS_KEYS.set, window.State.settings);
    window.State.toast('Pengaturan kedai berhasil disimpan!', 'success');
  }

  async saveSupabaseConfig() {
    const sbUrlInput = document.getElementById('set-sb-url');
    const sbKeyInput = document.getElementById('set-sb-key');

    if (!sbUrlInput || !sbKeyInput) return;

    const url = sbUrlInput.value.trim();
    const anonKey = sbKeyInput.value.trim();

    window.State.supabaseConfig = { url, anonKey };
    window.State.save(LS_KEYS.sb, window.State.supabaseConfig);

    window.State.toast('Konfigurasi Supabase disimpan. Menguji koneksi...', 'warning');
    if (window.SupabaseService) {
      window.SupabaseService.init();
      const res = await window.SupabaseService.testConnection(url, anonKey);
      if (res.success) {
        window.State.toast(res.message, 'success');
      } else {
        window.State.toast(res.message, 'error');
      }
    }
  }

  async testSupabaseConnection() {
    const sbUrlInput = document.getElementById('set-sb-url');
    const sbKeyInput = document.getElementById('set-sb-key');

    const url = sbUrlInput ? sbUrlInput.value.trim() : null;
    const key = sbKeyInput ? sbKeyInput.value.trim() : null;

    if (window.SupabaseService) {
      const res = await window.SupabaseService.testConnection(url, key);
      if (res.success) {
        window.State.toast(res.message, 'success');
      } else {
        window.State.toast(res.message, 'error');
      }
    }
  }

  async pushDataToSupabase() {
    if (window.SupabaseService) {
      window.State.toast('Mengunggah data lokal ke Cloud...', 'warning');
      const res = await window.SupabaseService.pushLocalData();
      if (res.success) {
        window.State.toast(res.message, 'success');
      } else {
        window.State.toast(res.message, 'error');
      }
    }
  }

  printTestReceipt() {
    const mockTrx = {
      no: 'TEST-001',
      tgl: Date.now(),
      kasir: 'Tester',
      tipe: 'Dine-In',
      meja: '99',
      items: [
        { nm: 'Taichan Pedas Lv.3', qty: 2, hr: 18000, mod: 'Lv.3, Sambal Pisah' },
        { nm: 'Es Teh Manis', qty: 2, hr: 5000, mod: 'Manis Sedang' }
      ],
      subtotal: 46000,
      diskon: 0,
      total: 46000,
      bayar: 50000,
      kembali: 4000,
      metode: 'cash'
    };

    if (window.PrinterService) {
      window.PrinterService.printReceipt(mockTrx);
    }
  }

  renderStaffList() {
    const tbody = document.getElementById('staff-table-tbody');
    if (!tbody) return;

    const staffList = window.State.staff || [];

    tbody.innerHTML = staffList.map(st => `
      <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
        <td style="padding:10px 14px;font-weight:700">${st.nm}</td>
        <td style="padding:10px 14px;color:var(--secondary)">@${st.user}</td>
        <td style="padding:10px 14px">
          <span class="badge ${st.role === 'owner' ? 'primary' : 'info'}">
            ${st.role.toUpperCase()}
          </span>
        </td>
        <td class="font-mono" style="padding:10px 14px;color:var(--primary);font-weight:700">${st.pin || '1234'}</td>
        <td style="padding:10px 14px;text-align:right">
          ${st.role !== 'owner' ? `
            <button class="btn btn-danger" style="padding:4px 8px;font-size:11px" onclick="window.SettingsView.deleteStaff(${st.id})">
              Hapus
            </button>
          ` : '<span style="font-size:11px;color:var(--secondary)">Utama</span>'}
        </td>
      </tr>
    `).join('');
  }

  handleAddStaff() {
    const nameInput = document.getElementById('staff-name');
    const userInput = document.getElementById('staff-user');
    const passInput = document.getElementById('staff-pass');
    const pinInput  = document.getElementById('staff-pin');
    const roleSelect= document.getElementById('staff-role');

    if (!nameInput || !userInput || !passInput) return;

    const newStaff = {
      id: Date.now(),
      nm: nameInput.value.trim(),
      user: userInput.value.trim().toLowerCase(),
      pw: passInput.value,
      pin: pinInput ? pinInput.value.trim() : '1234',
      role: roleSelect ? roleSelect.value : 'kasir',
      on: true
    };

    window.State.staff.push(newStaff);
    window.State.save(LS_KEYS.st, window.State.staff);

    nameInput.value = '';
    userInput.value = '';
    passInput.value = '';

    const modal = document.getElementById('add-staff-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Akun staf @${newStaff.user} berhasil dibuat!`, 'success');
    this.renderStaffList();
  }

  deleteStaff(id) {
    if (confirm('Yakin ingin menghapus akun staf ini?')) {
      window.State.staff = window.State.staff.filter(s => s.id !== id);
      window.State.save(LS_KEYS.st, window.State.staff);
      this.renderStaffList();
      window.State.toast('Akun staf berhasil dihapus.', 'warning');
    }
  }
}

window.SettingsView = new SettingsView();
