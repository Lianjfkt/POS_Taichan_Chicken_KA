/**
 * KA POS v3.0 - Owner Financial Intelligence, Deep Analytics & Reports Center
 */

class ReportsView {
  constructor() {
    this.chartInstance = null;
    this.activePeriod = 'today'; // 'today', 'yesterday', 'week', 'month'
  }

  init() {
    this.bindEvents();
    this.render();

    window.State.on(LS_KEYS.trx, () => this.render());
  }

  bindEvents() {
    // Period buttons
    document.querySelectorAll('.report-period-btn').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('.report-period-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.activePeriod = btn.getAttribute('data-period');
        this.render();
      };
    });

    // CSV Export Button
    const btnExport = document.getElementById('btn-export-sales-csv');
    if (btnExport) {
      btnExport.onclick = () => this.exportCSV();
    }

    // A4 Print Report Button
    const btnPrintA4 = document.getElementById('btn-print-a4-report');
    if (btnPrintA4) {
      btnPrintA4.onclick = () => this.printA4Report();
    }
  }

  getFilteredTransactions() {
    const all = window.State.transactions || [];
    const now = new Date();
    const todayStr = window.State.formatDateShort(now);

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const yesterdayStr = window.State.formatDateShort(yesterday);

    if (this.activePeriod === 'today') {
      return all.filter(t => window.State.formatDateShort(t.tgl) === todayStr);
    } else if (this.activePeriod === 'yesterday') {
      return all.filter(t => window.State.formatDateShort(t.tgl) === yesterdayStr);
    } else if (this.activePeriod === 'week') {
      const sevenDaysAgo = new Date(now);
      sevenDaysAgo.setDate(now.getDate() - 7);
      return all.filter(t => new Date(t.tgl) >= sevenDaysAgo);
    } else if (this.activePeriod === 'month') {
      const thirtyDaysAgo = new Date(now);
      thirtyDaysAgo.setDate(now.getDate() - 30);
      return all.filter(t => new Date(t.tgl) >= thirtyDaysAgo);
    }
    return all;
  }

  getPeriodLabel() {
    switch (this.activePeriod) {
      case 'today': return 'Hari Ini';
      case 'yesterday': return 'Kemarin';
      case 'week': return '7 Hari Terakhir';
      case 'month': return '30 Hari Terakhir';
      default: return 'Semua Waktu';
    }
  }

  render() {
    const trxs = this.getFilteredTransactions();
    this.calculateKPIs(trxs);
    this.renderChart();
    this.renderTopProducts(trxs);
    this.renderPaymentBreakdown(trxs);
    this.renderRecentTransactions(trxs);
  }

  calculateKPIs(trxs) {
    const omzet = trxs.reduce((s, t) => s + (t.total || 0), 0);
    const count = trxs.length;

    // Calculate COGS (HPP)
    let cogs = 0;
    trxs.forEach(t => {
      (t.items || []).forEach(i => {
        let itemHpp = i.md;
        if (itemHpp === undefined || itemHpp === null) {
          const product = (window.State.products || []).find(p => p.id === i.id);
          itemHpp = product ? window.State.calculateProductHPP(product) : 0;
        }
        cogs += (itemHpp || 0) * (i.qty || 1);
      });
    });

    const grossProfit = omzet - cogs;
    const profitMargin = omzet > 0 ? ((grossProfit / omzet) * 100).toFixed(1) : 0;
    const avgBasket = count > 0 ? Math.round(omzet / count) : 0;

    // Update DOM
    const periodLabel = this.getPeriodLabel();
    const labelOmzetEl = document.getElementById('kpi-label-omzet');
    if (labelOmzetEl) labelOmzetEl.textContent = `Omzet (${periodLabel})`;

    const omzetEl = document.getElementById('kpi-omzet-today');
    const trxCountEl = document.getElementById('kpi-trx-today');
    const cogsEl = document.getElementById('kpi-cogs-today');
    const profitEl = document.getElementById('kpi-profit-today');
    const basketEl = document.getElementById('kpi-basket-size');
    const marginEl = document.getElementById('kpi-margin-today');

    if (omzetEl) omzetEl.textContent = window.State.formatRp(omzet);
    if (trxCountEl) trxCountEl.textContent = `${count} Nota`;
    if (cogsEl) cogsEl.textContent = window.State.formatRp(cogs);
    if (profitEl) profitEl.textContent = window.State.formatRp(grossProfit);
    if (basketEl) basketEl.textContent = window.State.formatRp(avgBasket);
    if (marginEl) marginEl.textContent = `${profitMargin}%`;
  }

  renderChart() {
    const canvas = document.getElementById('revenue-trend-chart');
    const titleEl = document.getElementById('chart-period-title');
    if (!canvas || !window.Chart) return;

    let labels = [];
    let revenueData = [];
    const now = new Date();

    if (this.activePeriod === 'today' || this.activePeriod === 'yesterday') {
      const targetDate = this.activePeriod === 'today' ? now : new Date(now.getTime() - 86400000);
      const targetStr = window.State.formatDateShort(targetDate);
      if (titleEl) titleEl.textContent = `Tren Jam Sibuk (${this.getPeriodLabel()})`;

      // 10:00 to 23:00 hourly buckets
      for (let h = 10; h <= 23; h++) {
        const hourLabel = `${String(h).padStart(2, '0')}:00`;
        labels.push(hourLabel);

        const hourTotal = (window.State.transactions || []).filter(t => {
          if (window.State.formatDateShort(t.tgl) !== targetStr) return false;
          const tHour = new Date(t.tgl).getHours();
          return tHour === h;
        }).reduce((s, t) => s + (t.total || 0), 0);

        revenueData.push(hourTotal);
      }
    } else if (this.activePeriod === 'week') {
      if (titleEl) titleEl.textContent = 'Tren Omzet 7 Hari Terakhir';
      for (let i = 6; i >= 0; i--) {
        const d = new Date();
        d.setDate(now.getDate() - i);
        const dStr = window.State.formatDateShort(d);
        const dayLabel = d.toLocaleDateString('id-ID', { weekday: 'short', day: 'numeric' });
        labels.push(dayLabel);

        const dayTotal = (window.State.transactions || []).filter(t => window.State.formatDateShort(t.tgl) === dStr)
          .reduce((s, t) => s + (t.total || 0), 0);
        revenueData.push(dayTotal);
      }
    } else {
      // BUG-12 fix: step 1 agar semua 30 hari tampil (bukan i -= 2 yang hanya 15 titik)
      if (titleEl) titleEl.textContent = 'Tren Omzet 30 Hari Terakhir';
      for (let i = 29; i >= 0; i--) {
        const d = new Date();
        d.setDate(now.getDate() - i);
        const dStr = window.State.formatDateShort(d);
        const dayLabel = d.toLocaleDateString('id-ID', { day: 'numeric', month: 'short' });
        labels.push(dayLabel);

        const dayTotal = (window.State.transactions || []).filter(t => window.State.formatDateShort(t.tgl) === dStr)
          .reduce((s, t) => s + (t.total || 0), 0);
        revenueData.push(dayTotal);
      }
    }

    if (this.chartInstance) {
      this.chartInstance.destroy();
    }

    const ctx = canvas.getContext('2d');
    const gradient = ctx.createLinearGradient(0, 0, 0, 240);
    gradient.addColorStop(0, 'rgba(249, 115, 22, 0.45)');
    gradient.addColorStop(1, 'rgba(249, 115, 22, 0.0)');

    this.chartInstance = new window.Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Omzet',
          data: revenueData,
          borderColor: '#f97316',
          borderWidth: 2.5,
          pointBackgroundColor: '#ffb690',
          pointBorderColor: '#0f131d',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
          backgroundColor: gradient,
          tension: 0.35,
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1c1f2a',
            titleColor: '#dfe2f1',
            bodyColor: '#ffb690',
            borderColor: 'rgba(255,255,255,0.1)',
            borderWidth: 1,
            padding: 10,
            displayColors: false,
            callbacks: {
              label: (context) => window.State.formatRp(context.raw)
            }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(255,255,255,0.05)' },
            ticks: { color: '#bcc7de', font: { family: 'Plus Jakarta Sans', size: 10 } }
          },
          y: {
            grid: { color: 'rgba(255,255,255,0.05)' },
            ticks: {
              color: '#bcc7de',
              font: { family: 'Space Mono', size: 10 },
              callback: (val) => window.State.formatRpShort(val)
            }
          }
        }
      }
    });
  }

  renderTopProducts(trxs) {
    const listEl = document.getElementById('top-products-list');
    if (!listEl) return;

    const salesMap = {};
    trxs.forEach(t => {
      (t.items || []).forEach(i => {
        if (!salesMap[i.nm]) {
          salesMap[i.nm] = { nm: i.nm, qty: 0, revenue: 0, emj: i.emj || '🍢' };
        }
        salesMap[i.nm].qty += (i.qty || 1);
        salesMap[i.nm].revenue += (i.hr * i.qty);
      });
    });

    const sorted = Object.values(salesMap).sort((a, b) => b.qty - a.qty).slice(0, 5);

    if (sorted.length === 0) {
      listEl.innerHTML = `<div style="text-align:center;padding:24px;color:var(--secondary);font-size:12px;">Belum ada data penjualan pada periode ini</div>`;
      return;
    }

    listEl.innerHTML = sorted.map((p, idx) => `
      <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.05)">
        <div style="display:flex;align-items:center;gap:10px">
          <span style="font-family:var(--font-mono);font-size:12px;font-weight:700;color:var(--primary);width:18px">#${idx + 1}</span>
          <span style="font-size:20px">${p.emj}</span>
          <span style="font-weight:600;font-size:13px">${p.nm}</span>
        </div>
        <div style="text-align:right">
          <div class="font-mono" style="font-weight:700;font-size:13px">${p.qty} Porsi</div>
          <div class="font-mono" style="font-size:11px;color:var(--secondary)">${window.State.formatRp(p.revenue)}</div>
        </div>
      </div>
    `).join('');
  }

  renderPaymentBreakdown(trxs) {
    const container = document.getElementById('payment-methods-breakdown');
    if (!container) return;

    const totalRevenue = trxs.reduce((s, t) => s + (t.total || 0), 0);

    const methods = {
      cash: { name: 'Tunai (Cash)', icon: 'payments', count: 0, total: 0, color: 'var(--tertiary)' },
      qris: { name: 'QRIS Dinamis', icon: 'qr_code_scanner', count: 0, total: 0, color: 'var(--primary)' },
      transfer: { name: 'Transfer Bank', icon: 'account_balance', count: 0, total: 0, color: '#38bdf8' }
    };

    trxs.forEach(t => {
      const m = (t.metode || 'cash').toLowerCase();
      if (methods[m]) {
        methods[m].count++;
        methods[m].total += (t.total || 0);
      } else {
        methods.cash.count++;
        methods.cash.total += (t.total || 0);
      }
    });

    container.innerHTML = Object.keys(methods).map(key => {
      const item = methods[key];
      const pct = totalRevenue > 0 ? ((item.total / totalRevenue) * 100).toFixed(1) : 0;
      return `
        <div style="background:var(--surface-container-low);border:1px solid rgba(255,255,255,0.06);border-radius:var(--radius-lg);padding:16px;">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span class="material-symbols-outlined" style="color:${item.color};font-size:20px;">${item.icon}</span>
              <span style="font-weight:700;font-size:13px;">${item.name}</span>
            </div>
            <span class="badge" style="font-size:11px;font-weight:700;color:${item.color};">${pct}%</span>
          </div>
          <div class="font-mono" style="font-size:18px;font-weight:800;color:var(--text);margin-bottom:4px;">
            ${window.State.formatRp(item.total)}
          </div>
          <div style="display:flex;justify-content:space-between;font-size:11px;color:var(--secondary);margin-bottom:8px;">
            <span>${item.count} Transaksi</span>
            <span>Kontribusi Omzet</span>
          </div>
          <div style="width:100%;height:6px;background:rgba(255,255,255,0.08);border-radius:3px;overflow:hidden;">
            <div style="width:${pct}%;height:100%;background:${item.color};border-radius:3px;transition:width 0.4s ease;"></div>
          </div>
        </div>
      `;
    }).join('');
  }

  renderRecentTransactions(trxs) {
    const tbody = document.getElementById('recent-transactions-tbody');
    if (!tbody) return;

    const list = trxs.slice(0, 20);

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:var(--secondary)">Belum ada transaksi pada periode ini</td></tr>`;
      return;
    }

    // BUG-13 fix: gunakan transaction cache global, hindari JSON.stringify di onclick (XSS risk)
    window._reportTrxCache = window._reportTrxCache || {};
    list.forEach(t => { window._reportTrxCache[t.no] = t; });

    tbody.innerHTML = list.map(t => `
      <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
        <td class="font-mono" style="padding:10px 14px;font-weight:700;color:var(--primary)">${t.no}</td>
        <td style="padding:10px 14px;color:var(--secondary);font-size:12px">${window.State.formatDate(t.tgl)}</td>
        <td style="padding:10px 14px">
          <span class="badge info">${(t.tipe || 'dine-in').toUpperCase()}</span>
        </td>
        <td style="padding:10px 14px;font-size:12px">${(t.items || []).map(i => `${i.nm} (${i.qty})`).join(', ')}</td>
        <td style="padding:10px 14px;font-size:11px;text-transform:uppercase;color:var(--secondary)">${t.metode || 'cash'}</td>
        <td class="font-mono" style="padding:10px 14px;font-weight:700;text-align:right">${window.State.formatRp(t.total)}</td>
        <td style="padding:10px 14px;text-align:center">
          <button class="btn btn-secondary" style="padding:4px 8px;font-size:11px" onclick="window.PrinterService.openReceiptModal(window._reportTrxCache['${t.no}'])">
            <span class="material-symbols-outlined" style="font-size:14px">receipt</span>
          </button>
        </td>
      </tr>
    `).join('');
  }

  exportCSV() {
    const trxs = this.getFilteredTransactions();
    if (trxs.length === 0) {
      window.State.toast('Tidak ada transaksi pada periode ini untuk diekspor', 'warning');
      return;
    }

    // BUG-24 fix: tambahkan kolom Pelanggan
    const rows = [
      ['No. Nota', 'Tanggal', 'Kasir', 'Pelanggan', 'Tipe Order', 'Metode', 'Total', 'Bayar', 'Kembali', 'Item Detail']
    ];

    trxs.forEach(t => {
      const itemsStr = (t.items || []).map(i => `${i.nm} x${i.qty}`).join('; ');
      rows.push([
        t.no,
        window.State.formatDate(t.tgl),
        t.kasir || 'Kasir',
        t.pelanggan || 'Umum',
        t.tipe || 'Dine-In',
        (t.metode || 'cash').toUpperCase(),
        t.total,
        t.bayar || t.total,
        t.kembali || 0,
        itemsStr
      ]);
    });

    const csvContent = '\uFEFF' + rows.map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `Laporan_Penjualan_${this.activePeriod}_${window.State.formatDateShort(Date.now())}.csv`;
    link.click();
    window.State.toast('Laporan CSV berhasil diunduh!', 'success');
  }

  printA4Report() {
    const s = window.State.settings;
    const printDateStr = window.State.formatDate(Date.now());
    const trxs = this.getFilteredTransactions();
    const totalOmzet = trxs.reduce((sum, t) => sum + (t.total || 0), 0);
    const countTrx = trxs.length;
    const avgBasket = countTrx > 0 ? Math.round(totalOmzet / countTrx) : 0;
    const periodLabel = this.getPeriodLabel();

    const printWin = window.open('', '_blank');
    printWin.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>Laporan Keuangan & Penjualan — ${s.nm || 'KA POS'}</title>
        <style>
          @page { size: A4; margin: 15mm; }
          body { font-family: 'Plus Jakarta Sans', Arial, sans-serif; font-size: 12px; color: #111; line-height: 1.4; margin: 0; padding: 20px; }
          .header { border-bottom: 2px solid #111; padding-bottom: 12px; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: flex-end; }
          .title { font-size: 20px; font-weight: bold; }
          .meta { font-size: 11px; color: #555; }
          .kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; }
          .kpi-card { border: 1px solid #ddd; padding: 12px; border-radius: 6px; }
          .kpi-label { font-size: 10px; text-transform: uppercase; color: #666; font-weight: bold; }
          .kpi-value { font-size: 16px; font-weight: bold; margin-top: 4px; }
          table { width: 100%; border-collapse: collapse; margin-top: 12px; }
          th, td { border: 1px solid #ddd; padding: 8px 10px; text-align: left; }
          th { background-color: #f3f4f6; font-weight: bold; font-size: 11px; }
          .total-row { font-weight: bold; background: #fafafa; }
          .text-right { text-align: right; }
          @media print { button { display: none; } }
        </style>
      </head>
      <body>
        <div class="header">
          <div>
            <div class="title">${s.nm || 'KA POS'}</div>
            <div class="meta">${s.addr || ''} • Telp: ${s.hp || '-'}</div>
          </div>
          <div style="text-align:right">
            <div style="font-weight:bold">LAPORAN KEUANGAN & PENJUALAN RESMI (A4)</div>
            <div class="meta">Periode: <strong>${periodLabel}</strong> | Dicetak: ${printDateStr}</div>
          </div>
        </div>

        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-label">Total Omzet Penjualan</div>
            <div class="kpi-value">${window.State.formatRp(totalOmzet)}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Jumlah Transaksi</div>
            <div class="kpi-value">${countTrx} Nota Selesai</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Rata-rata Order (Basket)</div>
            <div class="kpi-value">${window.State.formatRp(avgBasket)}</div>
          </div>
        </div>

        <h3>Rincian Transaksi (${countTrx} Nota)</h3>
        <table>
          <thead>
            <tr>
              <th>No. Nota</th>
              <th>Waktu</th>
              <th>Kasir</th>
              <th>Tipe Order</th>
              <th>Metode</th>
              <th class="text-right">Total Transaksi</th>
            </tr>
          </thead>
          <tbody>
            ${trxs.map(t => `
              <tr>
                <td><strong>${t.no}</strong></td>
                <td>${window.State.formatDate(t.tgl)}</td>
                <td>${t.kasir || 'Kasir'}</td>
                <td>${t.tipe || 'Dine-In'}</td>
                <td>${(t.metode || 'Cash').toUpperCase()}</td>
                <td class="text-right">${window.State.formatRp(t.total)}</td>
              </tr>
            `).join('')}
            <tr class="total-row">
              <td colspan="5" class="text-right">TOTAL KESELURUHAN</td>
              <td class="text-right">${window.State.formatRp(totalOmzet)}</td>
            </tr>
          </tbody>
        </table>

        <div style="margin-top:40px;display:flex;justify-content:space-between">
          <div style="text-align:center;width:200px">
            <div>Dibuat oleh,</div>
            <div style="margin-top:50px;border-top:1px solid #111;font-weight:bold">${window.State.currentUser ? window.State.currentUser.nm : 'Kasir'}</div>
          </div>
          <div style="text-align:center;width:200px">
            <div>Disetujui oleh,</div>
            <div style="margin-top:50px;border-top:1px solid #111;font-weight:bold">Owner / Management</div>
          </div>
        </div>

        <script>
          window.onload = function() { window.print(); }
        </script>
      </body>
      </html>
    `);
    printWin.document.close();
  }
}

window.ReportsView = new ReportsView();
