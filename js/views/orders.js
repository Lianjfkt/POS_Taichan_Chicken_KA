/**
 * KA POS v3.0 - Riwayat Transaksi (Orders View) & Owner Alarm System
 * Fitur Riwayat Transaksi untuk Kasir & Owner
 * Sistem Alarm Real-time (Web Audio Synthesizer + Visual Alert) saat Transaksi Batal / Salah Input
 */

class AlarmService {
  constructor() {
    this.audioCtx = null;
    this.sirenInterval = null;
    this.isPlaying = false;
    this.currentAlert = null;
    this.STORAGE_KEY = 'ka_owner_alarm_alert';
  }

  init() {
    // Listen to storage events from other tabs/windows (e.g. Kasir submits void in cashier tab, Owner tab receives it)
    window.addEventListener('storage', (e) => {
      if (e.key === this.STORAGE_KEY && e.newValue) {
        try {
          const alertData = JSON.parse(e.newValue);
          if (alertData && !alertData.dismissed) {
            this.handleIncomingAlarm(alertData);
          }
        } catch (err) {
          console.warn('[AlarmService] Failed to parse storage alert:', err);
        }
      }
    });

    // Check on startup if there's an active undismissed alarm
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (saved) {
        const alertData = JSON.parse(saved);
        if (alertData && !alertData.dismissed) {
          // If active user is owner, show alert
          setTimeout(() => {
            if (window.State.currentUser && window.State.currentUser.role === 'owner') {
              this.handleIncomingAlarm(alertData);
            }
          }, 800);
        }
      }
    } catch (e) {
      console.warn('[AlarmService] Storage check error:', e);
    }
  }

  getAudioContext() {
    if (!this.audioCtx) {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (AudioContextClass) {
        this.audioCtx = new AudioContextClass();
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
    return this.audioCtx;
  }

  // Play synthesized emergency siren dual-tone (880Hz / 587Hz alternating)
  playSiren() {
    if (this.isPlaying) return;
    const ctx = this.getAudioContext();
    if (!ctx) return;

    this.isPlaying = true;
    let high = true;

    const beep = () => {
      if (!this.isPlaying) return;
      try {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(high ? 880 : 587, ctx.currentTime);

        gain.gain.setValueAtTime(0.35, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.28);

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start();
        osc.stop(ctx.currentTime + 0.3);
        high = !high;
      } catch (err) {
        console.warn('[AlarmService] Web Audio beep error:', err);
      }
    };

    beep();
    this.sirenInterval = setInterval(beep, 320);
  }

  stopSiren() {
    this.isPlaying = false;
    if (this.sirenInterval) {
      clearInterval(this.sirenInterval);
      this.sirenInterval = null;
    }
  }

  triggerAlarm(alertData) {
    this.currentAlert = alertData;
    // Broadcast via localStorage for cross-tab communication
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(alertData));

    // Also add to system notifications
    window.State.addNotification(
      '🚨 ALARM PERINGATAN: Transaksi Batal / Salah Input',
      `Nota ${alertData.orderNo} (${window.State.formatRp(alertData.total)}) dibatalkan oleh ${alertData.kasir}. Alasan: ${alertData.reason}`,
      'error'
    );

    // If current logged-in user is Owner, show popup & sound immediately!
    const isOwner = window.State.currentUser && window.State.currentUser.role === 'owner';
    if (isOwner) {
      this.showAlarmModal(alertData);
      this.playSiren();
    } else {
      // Kasir also hears a brief warning chime so they know the alarm fired
      this.playBriefWarning();
    }
  }

  playBriefWarning() {
    const ctx = this.getAudioContext();
    if (!ctx) return;
    try {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(440, ctx.currentTime);
      gain.gain.setValueAtTime(0.2, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.5);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.5);
    } catch (e) {}
  }

  handleIncomingAlarm(alertData) {
    this.currentAlert = alertData;
    // Only alarm if current user is Owner
    if (window.State.currentUser && window.State.currentUser.role === 'owner') {
      this.showAlarmModal(alertData);
      this.playSiren();
    }
  }

  showAlarmModal(alertData) {
    const modal = document.getElementById('owner-alarm-modal');
    if (!modal) return;

    const noEl = document.getElementById('alarm-modal-order-no');
    const cashierEl = document.getElementById('alarm-modal-cashier');
    const timeEl = document.getElementById('alarm-modal-time');
    const totalEl = document.getElementById('alarm-modal-total');
    const reasonEl = document.getElementById('alarm-modal-reason');
    const notesEl = document.getElementById('alarm-modal-notes');

    if (noEl) noEl.textContent = alertData.orderNo || '-';
    if (cashierEl) cashierEl.textContent = alertData.kasir || 'Kasir';
    if (timeEl) timeEl.textContent = window.State.formatDate(alertData.time || Date.now());
    if (totalEl) totalEl.textContent = window.State.formatRp(alertData.total || 0);
    if (reasonEl) reasonEl.textContent = alertData.reason || 'Salah input transaksi';
    if (notesEl) {
      notesEl.textContent = alertData.notes ? `"${alertData.notes}"` : 'Tidak ada keterangan tambahan.';
    }

    modal.classList.add('open');
  }

  dismissAlarm() {
    this.stopSiren();
    const modal = document.getElementById('owner-alarm-modal');
    if (modal) modal.classList.remove('open');

    // Mark dismissed in localStorage
    if (this.currentAlert) {
      const updated = { ...this.currentAlert, dismissed: true, dismissedAt: Date.now() };
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(updated));
    }
    window.State.toast('Alarm dimatikan. Laporan pembatalan telah dikonfirmasi.', 'info');
  }
}

class OrdersView {
  constructor() {
    this.searchQuery = '';
    this.activePeriod = 'today'; // 'today', 'yesterday', 'week', 'month', 'all'
    this.filterCashier = 'all';
    this.filterPayment = 'all';
    this.filterStatus = 'all'; // 'all', 'success', 'void'
    this.selectedVoidOrderNo = null;
    this.itemsPerPage = 15;
    this.currentPage = 1;
  }

  init() {
    // 1. Period buttons
    document.querySelectorAll('.orders-period-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        document.querySelectorAll('.orders-period-btn').forEach(b => b.classList.remove('active'));
        e.currentTarget.classList.add('active');
        this.activePeriod = e.currentTarget.getAttribute('data-period') || 'today';
        this.currentPage = 1;
        this.render();
      });
    });

    // 2. Search input
    const searchInput = document.getElementById('orders-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.searchQuery = (e.target.value || '').trim().toLowerCase();
        this.currentPage = 1;
        this.renderList();
      });
    }

    // 3. Filter selects
    const filterCashierEl = document.getElementById('orders-filter-cashier');
    if (filterCashierEl) {
      filterCashierEl.addEventListener('change', (e) => {
        this.filterCashier = e.target.value;
        this.currentPage = 1;
        this.renderList();
      });
    }

    const filterPaymentEl = document.getElementById('orders-filter-payment');
    if (filterPaymentEl) {
      filterPaymentEl.addEventListener('change', (e) => {
        this.filterPayment = e.target.value;
        this.currentPage = 1;
        this.renderList();
      });
    }

    const filterStatusEl = document.getElementById('orders-filter-status');
    if (filterStatusEl) {
      filterStatusEl.addEventListener('change', (e) => {
        this.filterStatus = e.target.value;
        this.currentPage = 1;
        this.renderList();
      });
    }

    // 4. Void Modal Submit
    const btnConfirmVoid = document.getElementById('btn-confirm-order-void');
    if (btnConfirmVoid) {
      btnConfirmVoid.addEventListener('click', () => {
        this.submitVoid();
      });
    }

    // 5. Alarm Dismiss Button
    const btnDismissAlarm = document.getElementById('btn-dismiss-owner-alarm');
    if (btnDismissAlarm) {
      btnDismissAlarm.addEventListener('click', () => {
        if (window.AlarmService) window.AlarmService.dismissAlarm();
      });
    }

    // 6. Listen to state transactions changes
    window.State.on(LS_KEYS.trx, () => {
      if (window.State.activeView === 'orders') {
        this.render();
      }
    });
  }

  getFilteredTransactions() {
    const all = window.State.transactions || [];
    const now = new Date();
    const todayStr = window.State.formatDateShort(now);

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const yesterdayStr = window.State.formatDateShort(yesterday);

    let list = all;
    if (this.activePeriod === 'today') {
      list = all.filter(t => window.State.formatDateShort(t.tgl) === todayStr);
    } else if (this.activePeriod === 'yesterday') {
      list = all.filter(t => window.State.formatDateShort(t.tgl) === yesterdayStr);
    } else if (this.activePeriod === 'week') {
      const sevenDaysAgo = new Date(now);
      sevenDaysAgo.setDate(now.getDate() - 7);
      list = all.filter(t => new Date(t.tgl) >= sevenDaysAgo);
    } else if (this.activePeriod === 'month') {
      const thirtyDaysAgo = new Date(now);
      thirtyDaysAgo.setDate(now.getDate() - 30);
      list = all.filter(t => new Date(t.tgl) >= thirtyDaysAgo);
    }

    // Filter Cashier
    if (this.filterCashier !== 'all') {
      list = list.filter(t => (t.kasir || '') === this.filterCashier);
    }

    // Filter Payment
    if (this.filterPayment !== 'all') {
      list = list.filter(t => (t.metode || 'cash') === this.filterPayment);
    }

    // Filter Status
    if (this.filterStatus === 'success') {
      list = list.filter(t => t.status !== 'void');
    } else if (this.filterStatus === 'void') {
      list = list.filter(t => t.status === 'void');
    }

    // Search Query
    if (this.searchQuery) {
      list = list.filter(t => {
        const no = (t.no || '').toLowerCase();
        const cust = (t.pelanggan || '').toLowerCase();
        const kasir = (t.kasir || '').toLowerCase();
        const items = (t.items || []).map(i => (i.nm || '').toLowerCase()).join(' ');
        return no.includes(this.searchQuery) ||
               cust.includes(this.searchQuery) ||
               kasir.includes(this.searchQuery) ||
               items.includes(this.searchQuery);
      });
    }

    return list;
  }

  populateCashierFilterOptions() {
    const filterCashierEl = document.getElementById('orders-filter-cashier');
    if (!filterCashierEl) return;

    const currentVal = filterCashierEl.value || 'all';
    const allTrx = window.State.transactions || [];
    const cashiers = new Set();
    allTrx.forEach(t => { if (t.kasir) cashiers.add(t.kasir); });
    (window.State.staff || []).forEach(s => { if (s.nm) cashiers.add(s.nm); });

    let html = '<option value="all">Semua Kasir</option>';
    cashiers.forEach(c => {
      html += `<option value="${c}">${c}</option>`;
    });
    filterCashierEl.innerHTML = html;
    filterCashierEl.value = currentVal;
  }

  render() {
    this.populateCashierFilterOptions();
    this.renderKPIs();
    this.renderList();
  }

  renderKPIs() {
    const trxs = this.getFilteredTransactions();
    const successfulTrxs = trxs.filter(t => t.status !== 'void');
    const voidTrxs = trxs.filter(t => t.status === 'void');
    const isOwner = window.State.isOwner();

    const totalSales = successfulTrxs.reduce((sum, t) => sum + (t.total || 0), 0);
    const avgSales = successfulTrxs.length > 0 ? Math.round(totalSales / successfulTrxs.length) : 0;

    const elTotalTrx = document.getElementById('orders-kpi-total-count');
    const elTotalSales = document.getElementById('orders-kpi-total-sales');
    const elAvgSales = document.getElementById('orders-kpi-avg-sales');
    const elVoidCount = document.getElementById('orders-kpi-void-count');

    // Kasir hanya bisa lihat jumlah transaksi, BUKAN nominal Rp
    if (elTotalTrx) elTotalTrx.textContent = successfulTrxs.length;
    if (elTotalSales) elTotalSales.textContent = isOwner ? window.State.formatRp(totalSales) : '—';
    if (elAvgSales) elAvgSales.textContent = isOwner ? window.State.formatRp(avgSales) : '—';
    if (elVoidCount) {
      elVoidCount.textContent = voidTrxs.length;
      const card = elVoidCount.closest('.metric-card');
      if (card) {
        if (voidTrxs.length > 0) {
          card.style.borderColor = 'rgba(239, 68, 68, 0.4)';
        } else {
          card.style.borderColor = '';
        }
      }
    }

    // Sembunyikan tombol Ekspor CSV dari kasir
    const btnExport = document.querySelector('[onclick*="exportCSV"]');
    if (btnExport) btnExport.style.display = isOwner ? '' : 'none';
  }

  renderList() {
    const tbody = document.getElementById('orders-table-tbody');
    const countInfo = document.getElementById('orders-count-info');
    const loadMoreBtn = document.getElementById('orders-load-more-btn');
    if (!tbody) return;

    const trxs = this.getFilteredTransactions();
    if (countInfo) {
      countInfo.textContent = `Menampilkan ${Math.min(this.currentPage * this.itemsPerPage, trxs.length)} dari ${trxs.length} transaksi`;
    }

    const totalVisible = this.currentPage * this.itemsPerPage;
    const paginated = trxs.slice(0, totalVisible);

    if (loadMoreBtn) {
      loadMoreBtn.style.display = trxs.length > totalVisible ? 'inline-flex' : 'none';
    }

    if (paginated.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="8" style="text-align:center;padding:36px;color:var(--secondary);">
            <span class="material-symbols-outlined" style="font-size:36px;opacity:0.4;display:block;margin-bottom:8px;">receipt_long</span>
            Tidak ada riwayat transaksi yang sesuai filter.
          </td>
        </tr>
      `;
      return;
    }

    window._ordersCache = window._ordersCache || {};
    paginated.forEach(t => { window._ordersCache[t.no] = t; });

    const isOwner = window.State.isOwner();

    tbody.innerHTML = paginated.map(t => {
      const isVoid = t.status === 'void';
      const statusBadge = isVoid 
        ? `<span class="badge error" style="display:inline-flex;align-items:center;gap:3px;" title="${t.voidReason || 'Batal'}"><span class="material-symbols-outlined" style="font-size:12px;">cancel</span> BATAL / VOID</span>`
        : `<span class="badge success" style="display:inline-flex;align-items:center;gap:3px;"><span class="material-symbols-outlined" style="font-size:12px;">check_circle</span> SELESAI</span>`;

      const orderTypeBadge = (t.tipe === 'takeaway' || t.tipe === 'take-away')
        ? `<span class="badge warning" style="font-size:10px;">BUNGKUS</span>`
        : `<span class="badge info" style="font-size:10px;">DINE-IN</span>`;

      const paymentMethodBadge = `<span style="font-size:11px;font-weight:700;text-transform:uppercase;color:var(--secondary);">${t.metode || 'cash'}</span>`;

      const itemsSummary = (t.items || []).map(i => {
        const mod = i.mod ? ` <span style="color:var(--secondary);font-size:10px;">(${i.mod})</span>` : '';
        const icon = window.FoodIcons ? window.FoodIcons.get(i.emj || i.nm, { size: 18 }) : '';
        return `<div style="display:flex;align-items:center;gap:5px;margin:2px 0;">${icon}<span style="font-weight:600;">${i.nm}</span> <span style="color:var(--primary);font-weight:700;">x${i.qty}</span>${mod}</div>`;
      }).join('');

      // Nominal total: owner bisa lihat, kasir hanya lihat tanda '••••'
      const totalDisplay = isOwner
        ? `<div style="font-weight:800;font-size:14px;${isVoid ? 'text-decoration:line-through;color:var(--secondary);' : 'color:var(--on-surface);'}">${window.State.formatRp(t.total)}</div>${t.diskon ? `<div style="font-size:10px;color:var(--tertiary);">Hemat ${window.State.formatRp(t.diskon)}</div>` : ''}`
        : `<div style="font-weight:700;font-size:14px;color:var(--secondary);letter-spacing:0.1em;" title="Nominal tidak ditampilkan untuk kasir">••••</div>`;

      return `
        <tr style="border-bottom:1px solid rgba(255,255,255,0.05);${isVoid ? 'opacity:0.65;background:rgba(239,68,68,0.03);' : ''}">
          <td style="padding:12px 14px;vertical-align:top;">
            <div class="font-mono" style="font-weight:700;color:var(--primary);font-size:13px;">${t.no}</div>
            <div style="font-size:11px;color:var(--secondary);margin-top:2px;">${window.State.formatDate(t.tgl)}</div>
          </td>
          <td style="padding:12px 14px;vertical-align:top;">
            <div style="font-weight:600;font-size:13px;color:var(--on-surface);">${t.pelanggan || 'Pelanggan Umum'}</div>
            <div style="display:flex;align-items:center;gap:6px;margin-top:4px;">
              ${orderTypeBadge}
              <span style="font-size:11px;color:var(--secondary);">Kasir: <b>${t.kasir || '-'}</b></span>
            </div>
          </td>
          <td style="padding:12px 14px;font-size:12px;vertical-align:top;max-width:240px;">
            ${itemsSummary}
          </td>
          <td style="padding:12px 14px;vertical-align:top;">
            ${paymentMethodBadge}
          </td>
          <td style="padding:12px 14px;vertical-align:top;">
            ${statusBadge}
            ${isVoid && t.voidReason ? `<div style="font-size:10px;color:var(--error);margin-top:4px;font-style:italic;line-height:1.2;">Alasan: ${t.voidReason}</div>` : ''}
          </td>
          <td class="font-mono" style="padding:12px 14px;text-align:right;vertical-align:top;">
            ${totalDisplay}
          </td>
          <td style="padding:12px 14px;text-align:center;white-space:nowrap;vertical-align:top;">
            <div style="display:flex;align-items:center;justify-content:center;gap:6px;">
              <!-- Lihat & Cetak Struk -->
              <button class="btn btn-secondary" style="padding:6px 10px;font-size:11px;" onclick="window.PrinterService.openReceiptModal(window._ordersCache['${t.no}'])" title="Lihat & Cetak Ulang Struk">
                <span class="material-symbols-outlined" style="font-size:16px;">receipt</span>
                <span class="orders-action-text" style="margin-left:2px;">Struk</span>
              </button>

              <!-- Detail Transaksi -->
              <button class="btn btn-secondary" style="padding:6px 8px;font-size:11px;" onclick="window.OrdersView.openDetailModal('${t.no}')" title="Detail Rincian Transaksi">
                <span class="material-symbols-outlined" style="font-size:16px;">visibility</span>
              </button>

              <!-- Tombol Batal / Salah Input (Void) -->
              ${!isVoid ? `
                <button class="btn btn-secondary btn-void-action" style="padding:6px 10px;font-size:11px;color:var(--error);border-color:rgba(239,68,68,0.25);" onclick="window.OrdersView.openVoidModal('${t.no}')" title="Batalkan Transaksi / Salah Input (Kirim Alarm ke Owner)">
                  <span class="material-symbols-outlined" style="font-size:16px;">warning</span>
                  <span class="orders-action-text" style="margin-left:2px;">Batal / Void</span>
                </button>
              ` : `
                <button class="btn btn-secondary" style="padding:6px 8px;font-size:11px;opacity:0.4;cursor:not-allowed;" disabled title="Transaksi sudah dibatalkan">
                  <span class="material-symbols-outlined" style="font-size:16px;">block</span>
                </button>
              `}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  loadMore() {
    this.currentPage += 1;
    this.renderList();
  }

  // Open transaction detail modal
  openDetailModal(orderNo) {
    const trx = (window.State.transactions || []).find(t => t.no === orderNo);
    if (!trx) return;

    const modal = document.getElementById('order-detail-modal');
    if (!modal) return;

    const body = document.getElementById('order-detail-modal-body');
    if (!body) return;

    const isVoid = trx.status === 'void';

    const isOwner = window.State.isOwner();

    body.innerHTML = `
      <div style="background:var(--surface-container-low);padding:14px;border-radius:12px;margin-bottom:14px;">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;">
          <div>
            <div class="font-mono" style="font-size:15px;font-weight:800;color:var(--primary);">${trx.no}</div>
            <div style="font-size:11px;color:var(--secondary);margin-top:2px;">${window.State.formatDate(trx.tgl)}</div>
          </div>
          <div style="text-align:right;">
            ${isVoid 
              ? `<span class="badge error">BATAL / VOID</span>`
              : `<span class="badge success">SELESAI</span>`}
            <div style="font-size:11px;color:var(--secondary);margin-top:4px;">Tipe: <b>${(trx.tipe || 'dine-in').toUpperCase()}</b></div>
          </div>
        </div>

        ${isVoid ? `
          <div style="margin-top:10px;padding:8px 12px;background:rgba(239,68,68,0.1);border-left:3px solid var(--error);border-radius:4px;font-size:12px;color:var(--error);">
            <div style="font-weight:700;">Informasi Pembatalan / Void:</div>
            <div>Dibatalkan oleh: <b>${trx.voidBy || 'Kasir'}</b></div>
            <div>Waktu: ${window.State.formatDate(trx.voidAt || trx.tgl)}</div>
            <div>Alasan: <b>${trx.voidReason || '-'}</b></div>
            ${trx.voidNotes ? `<div>Catatan: "${trx.voidNotes}"</div>` : ''}
          </div>
        ` : ''}
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;font-size:12px;margin-bottom:14px;">
        <div><span style="color:var(--secondary);">Kasir:</span> <b>${trx.kasir || '-'}</b></div>
        <div><span style="color:var(--secondary);">Pelanggan:</span> <b>${trx.pelanggan || 'Umum'}</b></div>
        <div><span style="color:var(--secondary);">Metode Bayar:</span> <b style="text-transform:uppercase;">${trx.metode || 'cash'}</b></div>
        <div><span style="color:var(--secondary);">Total Item:</span> <b>${(trx.items || []).reduce((s, i) => s + (i.qty || 1), 0)} porsi</b></div>
      </div>

      <div style="font-weight:700;font-size:13px;margin-bottom:8px;border-bottom:1px solid rgba(255,255,255,0.08);padding-bottom:4px;">Rincian Menu:</div>
      <div style="max-height:180px;overflow-y:auto;margin-bottom:14px;border:1px solid rgba(255,255,255,0.06);border-radius:8px;padding:8px;">
        ${(trx.items || []).map(i => `
          <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px dashed rgba(255,255,255,0.05);font-size:12px;align-items:center;">
            <div style="display:flex;align-items:center;gap:8px;">
              ${window.FoodIcons ? window.FoodIcons.get(i.emj || i.nm, { size: 24 }) : ''}
              <div>
                <div style="font-weight:600;">${i.nm} x${i.qty}</div>
                ${i.mod ? `<div style="font-size:11px;color:var(--secondary);">${i.mod}</div>` : ''}
              </div>
            </div>
            <div class="font-mono" style="font-weight:700;">${isOwner ? window.State.formatRp(i.hr * i.qty) : '—'}</div>
          </div>
        `).join('')}
      </div>

      <div style="background:var(--surface-container-low);padding:12px;border-radius:10px;font-size:12px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:4px;">
          <span style="color:var(--secondary);">Subtotal:</span>
          <span class="font-mono">${isOwner ? window.State.formatRp(trx.subtotal || trx.total) : '••••'}</span>
        </div>
        ${trx.diskon ? `
          <div style="display:flex;justify-content:space-between;margin-bottom:4px;color:var(--success);">
            <span>Diskon / Potongan:</span>
            <span class="font-mono">${isOwner ? '- ' + window.State.formatRp(trx.diskon) : '••••'}</span>
          </div>
        ` : ''}
        <div style="display:flex;justify-content:space-between;font-size:14px;font-weight:800;border-top:1px solid rgba(255,255,255,0.08);padding-top:6px;margin-top:4px;">
          <span>TOTAL:</span>
          <span class="font-mono" style="color:var(--primary);">${isOwner ? window.State.formatRp(trx.total) : '••••'}</span>
        </div>
        <div style="display:flex;justify-content:space-between;margin-top:4px;color:var(--secondary);">
          <span>Bayar (${(trx.metode || 'cash').toUpperCase()}):</span>
          <span class="font-mono">${isOwner ? window.State.formatRp(trx.bayar || trx.total) : '••••'}</span>
        </div>
        <div style="display:flex;justify-content:space-between;margin-top:2px;color:var(--secondary);">
          <span>Kembalian:</span>
          <span class="font-mono">${isOwner ? window.State.formatRp(trx.kembali || 0) : '••••'}</span>
        </div>
      </div>
    `;

    modal.classList.add('open');
  }

  // Open modal to confirm void / mistaken input
  openVoidModal(orderNo) {
    const trx = (window.State.transactions || []).find(t => t.no === orderNo);
    if (!trx) return;

    this.selectedVoidOrderNo = orderNo;
    const modal = document.getElementById('order-void-modal');
    if (!modal) return;

    const isOwner = window.State.isOwner();
    const infoEl = document.getElementById('order-void-info');
    if (infoEl) {
      infoEl.innerHTML = `
        <div style="background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.25);border-radius:10px;padding:12px;margin-bottom:12px;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div class="font-mono" style="font-weight:800;color:var(--primary);font-size:14px;">${trx.no}</div>
            <div class="font-mono" style="font-weight:800;color:var(--error);font-size:15px;">${isOwner ? window.State.formatRp(trx.total) : '••••'}</div>
          </div>
          <div style="font-size:12px;color:var(--secondary);margin-top:4px;">
            Kasir: <b>${trx.kasir || '-'}</b> • Pelanggan: <b>${trx.pelanggan || 'Umum'}</b> • Waktu: ${window.State.formatDate(trx.tgl)}
          </div>
          <div style="font-size:11px;color:var(--secondary);margin-top:4px;">
            Item: ${(trx.items || []).map(i => `${i.nm} (${i.qty})`).join(', ')}
          </div>
        </div>
      `;
    }

    // Reset form inputs
    const reasonSelect = document.getElementById('order-void-reason-select');
    const notesInput = document.getElementById('order-void-notes');
    if (reasonSelect) reasonSelect.selectedIndex = 0;
    if (notesInput) notesInput.value = '';

    modal.classList.add('open');
  }

  // Execute void and sound alarm to Owner
  submitVoid() {
    if (!this.selectedVoidOrderNo) return;
    const orderNo = this.selectedVoidOrderNo;
    const trx = (window.State.transactions || []).find(t => t.no === orderNo);
    if (!trx) return;

    const reasonSelect = document.getElementById('order-void-reason-select');
    const notesInput = document.getElementById('order-void-notes');
    const reason = reasonSelect ? reasonSelect.value : 'Salah input pesanan';
    const notes = notesInput ? notesInput.value.trim() : '';
    const currentUser = window.State.currentUser ? window.State.currentUser.nm : 'Kasir';

    // 1. Mark transaction as void in state
    trx.status = 'void';
    trx.voidReason = reason;
    trx.voidNotes = notes;
    trx.voidBy = currentUser;
    trx.voidAt = Date.now();
    window.State.save(LS_KEYS.trx, window.State.transactions);

    // 2. Restore inventory ingredients (BOM)
    (trx.items || []).forEach(item => {
      const product = (window.State.products || []).find(p => p.id === item.id);
      if (product && product.bom && product.bom.length > 0) {
        product.bom.forEach(b => {
          const invItem = (window.State.inventory || []).find(i => i.id === b.invId);
          if (invItem) {
            const addBack = +(b.qty * (item.qty || 1)).toFixed(3);
            invItem.stok = +(invItem.stok + addBack).toFixed(3);
            window.State.stockMutations.unshift({
              id: Date.now() + Math.random(),
              tgl: Date.now(),
              invId: invItem.id,
              nm: invItem.nm,
              tipe: 'masuk',
              jml: addBack,
              sat: invItem.sat,
              ket: `VOID Nota ${trx.no} (${reason})`,
              staf: currentUser
            });
          }
        });
      }
    });
    window.State.save(LS_KEYS.inv, window.State.inventory);
    window.State.save(LS_KEYS.mut, window.State.stockMutations);

    // 3. Adjust cash log if it was cash payment
    if (trx.metode === 'cash') {
      window.State.cashLog.unshift({
        id: Date.now(),
        tgl: Date.now(),
        tipe: 'keluar',
        kat: 'Void Transaksi',
        jml: trx.total,
        ket: `Batal Nota ${trx.no} - ${reason}`,
        staf: currentUser
      });
      window.State.save(LS_KEYS.kas, window.State.cashLog);
    }

    // 4. Log audit trail
    window.State.logAudit('TRANSACTION_VOID_ALARM', {
      orderNo: trx.no,
      total: trx.total,
      reason,
      notes,
      kasir: currentUser
    });

    // 5. TRIGGER REAL-TIME ALARM KE OWNER!
    const alarmData = {
      orderNo: trx.no,
      total: trx.total,
      kasir: currentUser,
      reason,
      notes,
      time: Date.now(),
      dismissed: false
    };

    if (window.AlarmService) {
      window.AlarmService.triggerAlarm(alarmData);
    }

    // 6. Close void modal & feedback
    const modal = document.getElementById('order-void-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Nota ${trx.no} berhasil dibatalkan. Alarm darurat telah dikirim ke Owner!`, 'warning');

    // 7. Re-render views
    this.render();
    if (window.ReportsView) window.ReportsView.render();
    if (window.ShiftView) window.ShiftView.render();
    if (window.POSView) window.POSView.updateHeaderMetrics();
  }

  exportCSV() {
    const trxs = this.getFilteredTransactions();
    if (trxs.length === 0) {
      window.State.toast('Tidak ada transaksi untuk diekspor', 'warning');
      return;
    }

    const rows = [
      ['No. Nota', 'Tanggal & Waktu', 'Kasir', 'Pelanggan', 'Tipe Order', 'Metode Bayar', 'Status', 'Total (Rp)', 'Diskon (Rp)', 'Alasan Void', 'Detail Menu']
    ];

    trxs.forEach(t => {
      const itemsStr = (t.items || []).map(i => `${i.nm} x${i.qty}`).join('; ');
      rows.push([
        t.no,
        window.State.formatDate(t.tgl),
        t.kasir || '-',
        t.pelanggan || 'Umum',
        t.tipe || 'dine-in',
        t.metode || 'cash',
        t.status === 'void' ? 'BATAL / VOID' : 'SELESAI',
        t.total || 0,
        t.diskon || 0,
        t.voidReason ? `${t.voidReason} (${t.voidNotes || ''})` : '-',
        itemsStr
      ]);
    });

    const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + rows.map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `riwayat_transaksi_ka_${window.State.formatDateShort(Date.now())}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();

    window.State.toast(`Berhasil mengekspor ${trxs.length} data riwayat transaksi.`, 'success');
  }
}

// Global Instances
window.AlarmService = new AlarmService();
window.OrdersView = new OrdersView();

// Auto-init AlarmService on load
window.addEventListener('DOMContentLoaded', () => {
  if (window.AlarmService) window.AlarmService.init();
});
