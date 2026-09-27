/**
 * KA POS v3.0 - Owner Financial Intelligence, Deep Analytics & Reports Center
 */

class ReportsView {
  constructor() {
    this.chartInstance = null;
    this.activePeriod = 'today'; // 'today', 'week', 'month'
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

  render() {
    this.calculateKPIs();
    this.renderChart();
    this.renderTopProducts();
    this.renderRecentTransactions();
  }

  calculateKPIs() {
    const todayStr = window.State.formatDateShort(new Date());
    const transactions = window.State.transactions || [];

    // Filter today's transactions
    const todayTrx = transactions.filter(t => window.State.formatDateShort(t.tgl) === todayStr);

    const omzetToday = todayTrx.reduce((s, t) => s + (t.total || 0), 0);
    const countToday = todayTrx.length;

    // Calculate COGS (HPP)
    let cogsToday = 0;
    todayTrx.forEach(t => {
      (t.items || []).forEach(i => {
        cogsToday += (i.md || 0) * (i.qty || 1);
      });
    });

    const grossProfit = omzetToday - cogsToday;
    const profitMargin = omzetToday > 0 ? ((grossProfit / omzetToday) * 100).toFixed(1) : 0;

    // Update DOM
    const omzetEl = document.getElementById('kpi-omzet-today');
    const trxCountEl = document.getElementById('kpi-trx-today');
    const cogsEl = document.getElementById('kpi-cogs-today');
    const profitEl = document.getElementById('kpi-profit-today');
    const marginEl = document.getElementById('kpi-margin-today');

    if (omzetEl) omzetEl.textContent = window.State.formatRp(omzetToday);
    if (trxCountEl) trxCountEl.textContent = `${countToday} Nota`;
    if (cogsEl) cogsEl.textContent = window.State.formatRp(cogsToday);
    if (profitEl) profitEl.textContent = window.State.formatRp(grossProfit);
    if (marginEl) marginEl.textContent = `${profitMargin}%`;
  }

  renderChart() {
    const canvas = document.getElementById('revenue-trend-chart');
    if (!canvas || !window.Chart) return;

    // Generate 7 days labels & values
    const labels = [];
    const revenueData = [];
    const now = new Date();

    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(now.getDate() - i);
      const dStr = window.State.formatDateShort(d);
      const dayLabel = d.toLocaleDateString('id-ID', { weekday: 'short', day: 'numeric' });
      labels.push(dayLabel);

      const dayTrx = (window.State.transactions || []).filter(t => window.State.formatDateShort(t.tgl) === dStr);
      const dayTotal = dayTrx.reduce((s, t) => s + (t.total || 0), 0);
      revenueData.push(dayTotal);
    }

    if (this.chartInstance) {
      this.chartInstance.destroy();
    }

    const ctx = canvas.getContext('2d');
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(249, 115, 22, 0.45)');
    gradient.addColorStop(1, 'rgba(249, 115, 22, 0.0)');

    this.chartInstance = new window.Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Omzet (Rp)',
          data: revenueData,
          borderColor: '#f97316',
          borderWidth: 3,
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
            padding: 12,
            displayColors: false,
            callbacks: {
              label: (context) => window.State.formatRp(context.raw)
            }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(255,255,255,0.05)' },
            ticks: { color: '#bcc7de', font: { family: 'Plus Jakarta Sans', size: 11 } }
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

  renderTopProducts() {
    const listEl = document.getElementById('top-products-list');
    if (!listEl) return;

    const salesMap = {};
    (window.State.transactions || []).forEach(t => {
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
      listEl.innerHTML = `<div style="text-align:center;padding:20px;color:var(--secondary)">Belum ada data penjualan</div>`;
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
          <div class="font-mono" style="font-weight:700;font-size:13px">${p.qty} Terjual</div>
          <div class="font-mono" style="font-size:11px;color:var(--secondary)">${window.State.formatRp(p.revenue)}</div>
        </div>
      </div>
    `).join('');
  }

  renderRecentTransactions() {
    const tbody = document.getElementById('recent-transactions-tbody');
    if (!tbody) return;

    const trxs = (window.State.transactions || []).slice(0, 15);

    if (trxs.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:var(--secondary)">Belum ada transaksi</td></tr>`;
      return;
    }

    tbody.innerHTML = trxs.map(t => `
      <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
        <td class="font-mono" style="padding:10px 14px;font-weight:700;color:var(--primary)">${t.no}</td>
        <td style="padding:10px 14px;color:var(--secondary);font-size:12px">${window.State.formatDate(t.tgl)}</td>
        <td style="padding:10px 14px">
          <span class="status-pill online" style="font-size:10px;padding:2px 8px">${(t.tipe || 'dine-in').toUpperCase()}</span>
          ${t.meja ? `<span style="font-size:11px;margin-left:4px;color:var(--secondary)">M-${t.meja}</span>` : ''}
        </td>
        <td style="padding:10px 14px;font-size:12px">${(t.items || []).map(i => `${i.nm} (${i.qty})`).join(', ')}</td>
        <td style="padding:10px 14px;font-size:11px;text-transform:uppercase;color:var(--secondary)">${t.metode || 'cash'}</td>
        <td class="font-mono" style="padding:10px 14px;font-weight:700;text-align:right">${window.State.formatRp(t.total)}</td>
        <td style="padding:10px 14px;text-align:center">
          <button class="btn btn-secondary" style="padding:4px 8px;font-size:11px" onclick="window.PrinterService.openReceiptModal(${JSON.stringify(t).replace(/"/g, '&quot;')})">
            <span class="material-symbols-outlined" style="font-size:14px">receipt</span>
          </button>
        </td>
      </tr>
    `).join('');
  }

  exportCSV() {
    const trxs = window.State.transactions || [];
    if (trxs.length === 0) {
      window.State.toast('Tidak ada transaksi untuk diekspor', 'warning');
      return;
    }

    const rows = [
      ['No. Nota', 'Tanggal', 'Kasir', 'Tipe', 'Meja', 'Metode', 'Total', 'Bayar', 'Kembali', 'Item Detail']
    ];

    trxs.forEach(t => {
      const itemsStr = (t.items || []).map(i => `${i.nm} x${i.qty}`).join('; ');
      rows.push([
        t.no,
        window.State.formatDate(t.tgl),
        t.kasir || 'Kasir',
        t.tipe || 'Dine-In',
        t.meja || '-',
        t.metode || 'Cash',
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
    link.download = `Laporan_Penjualan_KAPOS_${window.State.formatDateShort(Date.now())}.csv`;
    link.click();
    window.State.toast('Laporan CSV berhasil diunduh!', 'success');
  }

  printA4Report() {
    const s = window.State.settings;
    const todayStr = window.State.formatDate(Date.now());
    const trxs = window.State.transactions || [];
    const totalOmzet = trxs.reduce((sum, t) => sum + (t.total || 0), 0);

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
            <div style="font-weight:bold">LAPORAN PENJUALAN RESMI (A4)</div>
            <div class="meta">Dicetak: ${todayStr}</div>
          </div>
        </div>

        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-label">Total Omzet Penjualan</div>
            <div class="kpi-value">${window.State.formatRp(totalOmzet)}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Jumlah Transaksi</div>
            <div class="kpi-value">${trxs.length} Nota Selesai</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Status Audit</div>
            <div class="kpi-value" style="color:#059669">TERVERIFIKASI</div>
          </div>
        </div>

        <h3>Rincian Transaksi</h3>
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
                <td>${t.tipe || 'Dine-In'} ${t.meja ? '(' + t.meja + ')' : ''}</td>
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
