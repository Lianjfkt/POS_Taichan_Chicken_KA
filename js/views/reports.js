/**
 * KA POS v3.0 - Owner Financial Intelligence, Deep Analytics & Reports Center
 */

class ReportsView {
  constructor() {
    this.chartInstance = null;
    this.activePeriod = 'today'; // 'today', 'yesterday', 'week', 'month'
    this.searchQuery = '';
    this.filterPayment = 'all';
    this.filterType = 'all';
    this.itemsPerPage = 15;
    this.currentPage = 1;
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
        this.currentPage = 1;
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

    // Transaction search & filters
    const searchInput = document.getElementById('report-search-input');
    if (searchInput) {
      searchInput.oninput = (e) => {
        this.searchQuery = e.target.value.toLowerCase().trim();
        this.currentPage = 1;
        this.renderRecentTransactions(this.getFilteredTransactions());
      };
    }

    const payFilter = document.getElementById('report-filter-payment');
    if (payFilter) {
      payFilter.onchange = (e) => {
        this.filterPayment = e.target.value;
        this.currentPage = 1;
        this.renderRecentTransactions(this.getFilteredTransactions());
      };
    }

    const typeFilter = document.getElementById('report-filter-type');
    if (typeFilter) {
      typeFilter.onchange = (e) => {
        this.filterType = e.target.value;
        this.currentPage = 1;
        this.renderRecentTransactions(this.getFilteredTransactions());
      };
    }

    // Load More Button
    const btnLoadMore = document.getElementById('btn-load-more-trx');
    if (btnLoadMore) {
      btnLoadMore.onclick = () => {
        this.currentPage++;
        this.renderRecentTransactions(this.getFilteredTransactions());
      };
    }

    // Collapsible Section Cards (Accordion)
    document.querySelectorAll('.section-card-header[data-toggle-target]').forEach(header => {
      header.onclick = (e) => {
        if (e.target.closest('input, select, button, a')) return;
        const targetSelector = header.getAttribute('data-toggle-target');
        const targetEl = document.querySelector(targetSelector);
        const icon = header.querySelector('.accordion-icon');
        if (!targetEl) return;

        const isCurrentlyHidden = targetEl.style.display === 'none';
        if (isCurrentlyHidden) {
          const defaultDisplay = targetEl.getAttribute('data-default-display') || (targetSelector.includes('payment') ? 'grid' : 'block');
          targetEl.style.display = defaultDisplay;
          header.classList.remove('collapsed');
          if (icon) icon.textContent = 'expand_more';
        } else {
          targetEl.setAttribute('data-default-display', targetEl.style.display || (targetSelector.includes('payment') ? 'grid' : 'block'));
          targetEl.style.display = 'none';
          header.classList.add('collapsed');
          if (icon) icon.textContent = 'expand_more';
        }
      };
    });
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
    let trxs = [];
    try {
      trxs = this.getFilteredTransactions();
    } catch (e) {
      console.error('[ReportsView] getFilteredTransactions error:', e);
      trxs = window.State.transactions || [];
    }
    const validTrxs = trxs.filter(t => t.status !== 'void');

    try {
      this.calculateKPIs(validTrxs);
    } catch (e) {
      console.error('[ReportsView] calculateKPIs error:', e);
    }

    try {
      this.renderChart();
    } catch (e) {
      console.error('[ReportsView] renderChart error:', e);
    }

    try {
      this.renderTopProducts(validTrxs);
    } catch (e) {
      console.error('[ReportsView] renderTopProducts error:', e);
    }

    try {
      this.renderPaymentBreakdown(validTrxs);
    } catch (e) {
      console.error('[ReportsView] renderPaymentBreakdown error:', e);
    }

    try {
      this.renderPeakHours(validTrxs);
    } catch (e) {
      console.error('[ReportsView] renderPeakHours error:', e);
    }

    try {
      this.renderPNLBreakdown(validTrxs);
    } catch (e) {
      console.error('[ReportsView] renderPNLBreakdown error:', e);
    }

    try {
      this.renderRecentTransactions(trxs);
    } catch (e) {
      console.error('[ReportsView] renderRecentTransactions error:', e);
    }
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

  renderPNLBreakdown(trxs) {
    const pnlContainer = document.getElementById('pnl-breakdown-container');
    if (!pnlContainer) return;

    const omzet = trxs.reduce((s, t) => s + (t.total || 0), 0);
    let cogs = 0;
    trxs.forEach(t => {
      (t.items || []).forEach(i => {
        const product = (window.State.products || []).find(p => p.id === i.id);
        const itemHpp = product ? window.State.calculateProductHPP(product) : (i.md || 0);
        cogs += itemHpp * (i.qty || 1);
      });
    });

    const grossProfit = omzet - cogs;
    const grossMargin = omzet > 0 ? ((grossProfit / omzet) * 100).toFixed(1) : 0;
    const cogsPct = omzet > 0 ? ((cogs / omzet) * 100).toFixed(1) : 0;

    const now = new Date();
    let pettyOut = 0;
    const cashLogs = window.State.cashLog || [];
    if (this.activePeriod === 'today') {
      const todayStr = window.State.formatDateShort(now);
      pettyOut = cashLogs.filter(l => l.tipe === 'keluar' && window.State.formatDateShort(l.tgl) === todayStr).reduce((s, l) => s + l.jml, 0);
    } else if (this.activePeriod === 'yesterday') {
      const yesterday = new Date(now);
      yesterday.setDate(now.getDate() - 1);
      const yesterdayStr = window.State.formatDateShort(yesterday);
      pettyOut = cashLogs.filter(l => l.tipe === 'keluar' && window.State.formatDateShort(l.tgl) === yesterdayStr).reduce((s, l) => s + l.jml, 0);
    } else if (this.activePeriod === 'week') {
      const sevenDaysAgo = new Date(now);
      sevenDaysAgo.setDate(now.getDate() - 7);
      pettyOut = cashLogs.filter(l => l.tipe === 'keluar' && new Date(l.tgl) >= sevenDaysAgo).reduce((s, l) => s + l.jml, 0);
    } else if (this.activePeriod === 'month') {
      const thirtyDaysAgo = new Date(now);
      thirtyDaysAgo.setDate(now.getDate() - 30);
      pettyOut = cashLogs.filter(l => l.tipe === 'keluar' && new Date(l.tgl) >= thirtyDaysAgo).reduce((s, l) => s + l.jml, 0);
    } else {
      pettyOut = cashLogs.filter(l => l.tipe === 'keluar').reduce((s, l) => s + l.jml, 0);
    }

    const netProfit = grossProfit - pettyOut;
    const netMargin = omzet > 0 ? ((netProfit / omzet) * 100).toFixed(1) : 0;
    const pettyPct = omzet > 0 ? ((pettyOut / omzet) * 100).toFixed(1) : 0;

    // Update status pill in header
    const statusPill = document.getElementById('pnl-status-pill');
    if (statusPill) {
      if (omzet === 0) {
        statusPill.textContent = 'Menunggu Transaksi';
        statusPill.className = 'badge secondary';
      } else if (netMargin >= 30) {
        statusPill.textContent = `🌟 Sangat Sehat (Net ${netMargin}%)`;
        statusPill.className = 'badge tertiary';
      } else if (netProfit >= 0) {
        statusPill.textContent = `✅ Profitabel (Net ${netMargin}%)`;
        statusPill.className = 'badge info';
      } else {
        statusPill.textContent = `⚠️ Defisit (${netMargin}%)`;
        statusPill.className = 'badge error';
      }
    }

    // Visual Waterfall bar widths
    const cogsBarWidth = Math.min(100, Math.max(0, cogsPct));
    const pettyBarWidth = Math.min(100 - cogsBarWidth, Math.max(0, pettyPct));
    const netBarWidth = Math.min(100 - cogsBarWidth - pettyBarWidth, Math.max(0, netMargin));

    pnlContainer.innerHTML = `
      <!-- Flow Diagram Bar -->
      <div style="background:var(--surface-container-low);border:1px solid rgba(255,255,255,0.06);border-radius:var(--radius-lg);padding:14px 18px;margin-bottom:14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;font-size:12px;">
          <span style="font-weight:700;color:var(--on-surface);">Arus Pendapatan ke Laba Bersih</span>
          <span style="font-family:var(--font-mono);font-size:11px;color:var(--secondary);">HPP: ${cogsPct}% | Beban: ${pettyPct}% | Net: ${netMargin}%</span>
        </div>
        <div style="display:flex;height:12px;border-radius:6px;overflow:hidden;background:rgba(255,255,255,0.05);gap:2px;">
          <div style="width:${cogsBarWidth}%;background:var(--error);border-radius:4px 0 0 4px;transition:width 0.4s;" title="HPP Bahan: ${cogsPct}%"></div>
          <div style="width:${pettyBarWidth}%;background:var(--warning);transition:width 0.4s;" title="Beban Kas: ${pettyPct}%"></div>
          <div style="width:${netBarWidth}%;background:var(--tertiary);border-radius:0 4px 4px 0;transition:width 0.4s;" title="Laba Bersih: ${netMargin}%"></div>
        </div>
        <div style="display:flex;gap:16px;margin-top:10px;font-size:11px;color:var(--secondary);flex-wrap:wrap;">
          <span style="display:inline-flex;align-items:center;gap:6px;"><span style="width:8px;height:8px;border-radius:50%;background:var(--error);"></span> HPP Bahan Pokok (${cogsPct}%)</span>
          <span style="display:inline-flex;align-items:center;gap:6px;"><span style="width:8px;height:8px;border-radius:50%;background:var(--warning);"></span> Beban Operasional Kas (${pettyPct}%)</span>
          <span style="display:inline-flex;align-items:center;gap:6px;"><span style="width:8px;height:8px;border-radius:50%;background:var(--tertiary);"></span> Estimasi Laba Bersih (${netMargin}%)</span>
        </div>
      </div>

      <!-- 5 Metric Cards -->
      <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(180px, 1fr));gap:12px;">
        <div class="report-pnl-card primary">
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <span style="font-size:11px;color:var(--secondary);font-weight:700;">TOTAL PENDAPATAN</span>
            <span class="material-symbols-outlined" style="font-size:18px;color:var(--primary);">trending_up</span>
          </div>
          <div class="font-mono" style="font-size:20px;font-weight:800;color:var(--primary);">${window.State.formatRp(omzet)}</div>
          <div style="font-size:11px;color:var(--secondary);">100% Arus Masuk Bruto</div>
        </div>

        <div class="report-pnl-card error">
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <span style="font-size:11px;color:var(--secondary);font-weight:700;">TOTAL HPP (COGS)</span>
            <span class="material-symbols-outlined" style="font-size:18px;color:var(--error);">inventory_2</span>
          </div>
          <div class="font-mono" style="font-size:20px;font-weight:800;color:var(--error);">-${window.State.formatRp(cogs)}</div>
          <div style="font-size:11px;color:var(--secondary);">${cogsPct}% dari Pendapatan</div>
        </div>

        <div class="report-pnl-card tertiary">
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <span style="font-size:11px;color:var(--secondary);font-weight:700;">LABA KOTOR (GROSS)</span>
            <span class="material-symbols-outlined" style="font-size:18px;color:var(--tertiary);">account_balance_wallet</span>
          </div>
          <div class="font-mono" style="font-size:20px;font-weight:800;color:var(--tertiary);">${window.State.formatRp(grossProfit)}</div>
          <div style="font-size:11px;color:var(--secondary);">Gross Margin: <strong style="color:var(--tertiary);">${grossMargin}%</strong></div>
        </div>

        <div class="report-pnl-card warning">
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <span style="font-size:11px;color:var(--secondary);font-weight:700;">BEBAN KAS KELUAR</span>
            <span class="material-symbols-outlined" style="font-size:18px;color:var(--warning);">receipt_long</span>
          </div>
          <div class="font-mono" style="font-size:20px;font-weight:800;color:var(--warning);">-${window.State.formatRp(pettyOut)}</div>
          <div style="font-size:11px;color:var(--secondary);">${pettyPct}% Operasional Kas</div>
        </div>

        <div class="report-pnl-card ${netProfit >= 0 ? 'tertiary' : 'error'}">
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <span style="font-size:11px;color:var(--secondary);font-weight:700;">ESTIMASI LABA BERSIH</span>
            <span class="material-symbols-outlined" style="font-size:18px;color:${netProfit >= 0 ? 'var(--tertiary)' : 'var(--error)'};">verified</span>
          </div>
          <div class="font-mono" style="font-size:20px;font-weight:800;color:${netProfit >= 0 ? 'var(--tertiary)' : 'var(--error)'};">${window.State.formatRp(netProfit)}</div>
          <div style="font-size:11px;color:var(--secondary);">Net Margin: <strong style="color:${netProfit >= 0 ? 'var(--tertiary)' : 'var(--error)'};">${netMargin}%</strong></div>
        </div>
      </div>
    `;
  }

  renderPeakHours(trxs) {
    const container = document.getElementById('peak-hours-container');
    if (!container) return;

    const hourCounts = new Array(24).fill(0);
    const hourRevenue = new Array(24).fill(0);

    trxs.forEach(t => {
      const h = new Date(t.tgl).getHours();
      hourCounts[h]++;
      hourRevenue[h] += (t.total || 0);
    });

    const relevantHours = [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23];
    let maxCount = 0;
    let peakHour = 19;
    relevantHours.forEach(h => {
      if (hourCounts[h] > maxCount) {
        maxCount = hourCounts[h];
        peakHour = h;
      }
    });

    const peakBadge = document.getElementById('peak-hour-badge');
    if (peakBadge) {
      if (maxCount > 0) {
        peakBadge.textContent = `🔥 Jam ${peakHour}:00 (${maxCount} Order)`;
        peakBadge.className = 'badge warning';
      } else {
        peakBadge.textContent = 'Menunggu Pesanan';
        peakBadge.className = 'badge secondary';
      }
    }

    const maxScale = Math.max(1, maxCount);

    container.innerHTML = `
      ${maxCount > 0 ? `
        <div style="display:flex;align-items:center;justify-content:space-between;background:rgba(249,115,22,0.08);border:1px solid rgba(249,115,22,0.25);border-radius:var(--radius-md);padding:10px 14px;margin-bottom:12px;font-size:12px;">
          <div style="display:flex;align-items:center;gap:8px;">
            <span class="material-symbols-outlined" style="color:var(--primary);font-size:18px;">local_fire_department</span>
            <span>Jam Emas: <strong>${peakHour}:00 - ${peakHour+1}:00</strong> (${maxCount} pesanan • ${window.State.formatRp(hourRevenue[peakHour])})</span>
          </div>
          <span style="font-weight:700;color:var(--primary);font-size:11px;">Peak Traffic</span>
        </div>
      ` : `
        <div style="background:var(--surface-container-low);border:1px solid rgba(255,255,255,0.05);border-radius:var(--radius-md);padding:10px 14px;margin-bottom:12px;font-size:12px;color:var(--secondary);text-align:center;">
          Belum ada transaksi di rentang jam ini. Grafik akan terisi otomatis seiring order masuk.
        </div>
      `}

      <!-- Bar Chart -->
      <div style="display:flex;gap:6px;align-items:flex-end;height:120px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.08);">
        ${relevantHours.map(h => {
          const cnt = hourCounts[h];
          const pct = Math.round((cnt / maxScale) * 100);
          const isPeak = cnt === maxCount && maxCount > 0;
          return `
            <div class="peak-hour-col" title="Jam ${h}:00 - ${cnt} order (${window.State.formatRp(hourRevenue[h])})">
              <span style="font-size:9px;color:${isPeak ? 'var(--primary)' : 'var(--secondary)'};font-family:var(--font-mono);font-weight:${isPeak ? '800' : '500'};">${cnt > 0 ? cnt : ''}</span>
              <div class="peak-bar ${isPeak ? 'busy' : (cnt > 0 ? 'normal' : 'empty')}" style="height:${Math.max(4, pct)}%;"></div>
              <span style="font-size:10px;font-weight:${isPeak ? '800' : '600'};color:${isPeak ? 'var(--primary)' : 'var(--secondary)'};margin-top:2px;">${h}</span>
            </div>
          `;
        }).join('')}
      </div>

      <div style="display:flex;justify-content:space-between;align-items:center;font-size:11px;color:var(--secondary);margin-top:10px;">
        <span>Operasional: 10:00 s.d 23:00 WIB</span>
        <span>Arahkan kursor pada batang untuk omzet</span>
      </div>
    `;
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

    // Safely cleanup existing Chart.js instances
    try {
      if (window.Chart && typeof window.Chart.getChart === 'function') {
        const existing = window.Chart.getChart(canvas);
        if (existing) existing.destroy();
      }
    } catch (e) {
      console.warn('[ReportsView] Chart getChart cleanup warning:', e);
    }

    if (this.chartInstance) {
      try {
        this.chartInstance.destroy();
      } catch (e) {}
      this.chartInstance = null;
    }

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let gradient;
    try {
      gradient = ctx.createLinearGradient(0, 0, 0, 240);
      gradient.addColorStop(0, 'rgba(249, 115, 22, 0.45)');
      gradient.addColorStop(1, 'rgba(249, 115, 22, 0.0)');
    } catch (e) {
      gradient = 'rgba(249, 115, 22, 0.2)';
    }

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
            callbacks: {
              label: (context) => `Omzet: ${window.State.formatRp(context.raw)}`
            }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(255,255,255,0.04)' },
            ticks: { color: '#bcc7de', font: { family: "'Plus Jakarta Sans', sans-serif", size: 10 } }
          },
          y: {
            min: 0,
            grid: { color: 'rgba(255,255,255,0.04)' },
            ticks: {
              color: '#bcc7de',
              font: { family: "'Space Mono', monospace", size: 10 },
              callback: (v) => v >= 1000000 ? `${(v/1000000).toFixed(1)}jt` : (v >= 1000 ? `${Math.round(v/1000)}rb` : v)
            }
          }
        }
      }
    });
  }

  renderTopProducts(trxs) {
    const tbody = document.getElementById('top-products-tbody');
    if (!tbody) return;

    const map = {};
    trxs.forEach(t => {
      (t.items || []).forEach(i => {
        if (!map[i.id]) {
          map[i.id] = { id: i.id, nm: i.nm, kat: i.kat, qty: 0, revenue: 0, profit: 0 };
        }
        const itemRev = (i.hr || 0) * (i.qty || 1);
        const product = (window.State.products || []).find(p => p.id === i.id);
        const itemHpp = product ? window.State.calculateProductHPP(product) : (i.md || 0);
        map[i.id].qty += (i.qty || 1);
        map[i.id].revenue += itemRev;
        map[i.id].profit += (itemRev - (itemHpp * (i.qty || 1)));
      });
    });

    const sorted = Object.values(map).sort((a, b) => b.qty - a.qty).slice(0, 5);

    if (sorted.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--secondary)">Belum ada penjualan menu</td></tr>`;
      return;
    }

    tbody.innerHTML = sorted.map((p, idx) => {
      const prod = (window.State.products || []).find(pr => pr.id === p.id);
      const icon = window.FoodIcons ? window.FoodIcons.get(prod?.emj || p.kat || p.nm, { size: 18 }) : '';
      return `
        <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
          <td style="padding:8px 12px;font-weight:700;">
            <div style="display:flex;align-items:center;gap:6px;">
              <span style="display:inline-flex;align-items:center;justify-content:center;width:18px;height:18px;border-radius:50%;background:rgba(255,255,255,0.08);font-size:10px;font-weight:800;flex-shrink:0;">${idx + 1}</span>
              ${icon}
              <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100px;" title="${p.nm}">${p.nm}</span>
            </div>
          </td>
          <td class="font-mono" style="padding:8px 12px;font-weight:700;text-align:right;white-space:nowrap;">${p.qty}x</td>
          <td class="font-mono" style="padding:8px 12px;font-weight:700;text-align:right;color:var(--primary);white-space:nowrap;">${window.State.formatRp(p.revenue)}</td>
          <td class="font-mono" style="padding:8px 12px;font-weight:700;text-align:right;color:var(--tertiary);white-space:nowrap;">${window.State.formatRp(p.profit)}</td>
        </tr>
      `;
    }).join('');
  }

  renderPaymentBreakdown(trxs) {
    const container = document.getElementById('payment-methods-breakdown');
    if (!container) return;

    const totalRevenue = trxs.reduce((s, t) => s + (t.total || 0), 0);
    const methods = {
      cash:     { name: 'Tunai (Cash)', count: 0, total: 0, color: '#4edea3', icon: 'payments' },
      qris:     { name: 'QRIS Instant', count: 0, total: 0, color: '#f97316', icon: 'qr_code_2' },
      transfer: { name: 'Transfer BCA', count: 0, total: 0, color: '#38bdf8', icon: 'account_balance' }
    };

    trxs.forEach(t => {
      const m = t.metode || 'cash';
      if (methods[m]) {
        methods[m].count++;
        methods[m].total += (t.total || 0);
      } else {
        methods.cash.count++;
        methods.cash.total += (t.total || 0);
      }
    });

    const cashPct = totalRevenue > 0 ? ((methods.cash.total / totalRevenue) * 100).toFixed(1) : 0;
    const qrisPct = totalRevenue > 0 ? ((methods.qris.total / totalRevenue) * 100).toFixed(1) : 0;
    const transferPct = totalRevenue > 0 ? ((methods.transfer.total / totalRevenue) * 100).toFixed(1) : 0;
    const cashlessTotal = methods.qris.total + methods.transfer.total;
    const cashlessPct = totalRevenue > 0 ? ((cashlessTotal / totalRevenue) * 100).toFixed(1) : 0;

    const cashlessBadge = document.getElementById('payment-cashless-badge');
    if (cashlessBadge) {
      cashlessBadge.textContent = `${cashlessPct}% Non-Tunai`;
    }

    container.innerHTML = `
      <!-- Segmented Share Bar -->
      <div style="background:var(--surface-container-low);border:1px solid rgba(255,255,255,0.06);border-radius:var(--radius-md);padding:10px 14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;font-size:12px;">
          <span style="font-weight:700;">Share of Wallet (Pangsa Metode)</span>
          <span style="font-family:var(--font-mono);font-size:11px;color:var(--secondary);">${trxs.length} Total Transaksi</span>
        </div>
        <div style="display:flex;height:10px;border-radius:5px;overflow:hidden;background:rgba(255,255,255,0.06);gap:2px;">
          <div style="width:${cashPct}%;background:#4edea3;transition:width 0.4s;" title="Tunai: ${cashPct}%"></div>
          <div style="width:${qrisPct}%;background:#f97316;transition:width 0.4s;" title="QRIS: ${qrisPct}%"></div>
          <div style="width:${transferPct}%;background:#38bdf8;transition:width 0.4s;" title="Transfer: ${transferPct}%"></div>
        </div>
        <div style="display:flex;justify-content:space-between;margin-top:8px;font-size:11px;color:var(--secondary);flex-wrap:wrap;gap:8px;">
          <span style="color:#4edea3;font-weight:600;">● Tunai: ${cashPct}%</span>
          <span style="color:#f97316;font-weight:600;">● QRIS: ${qrisPct}%</span>
          <span style="color:#38bdf8;font-weight:600;">● Transfer: ${transferPct}%</span>
        </div>
      </div>

      <!-- 3 Channel Cards -->
      <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(130px, 1fr));gap:10px;">
        ${Object.keys(methods).map(key => {
          const item = methods[key];
          const pct = totalRevenue > 0 ? ((item.total / totalRevenue) * 100).toFixed(1) : 0;
          return `
            <div style="background:var(--surface-container-low);border:1px solid rgba(255,255,255,0.06);border-radius:var(--radius-md);padding:12px;position:relative;overflow:hidden;">
              <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px;">
                <div style="width:28px;height:28px;border-radius:6px;background:rgba(255,255,255,0.05);display:flex;align-items:center;justify-content:center;">
                  <span class="material-symbols-outlined" style="color:${item.color};font-size:18px;">${item.icon}</span>
                </div>
                <span class="badge" style="font-size:10px;font-weight:800;color:${item.color};background:rgba(255,255,255,0.05);">${pct}%</span>
              </div>
              <div style="font-weight:700;font-size:12px;color:var(--on-surface);margin-bottom:2px;">${item.name}</div>
              <div class="font-mono" style="font-size:16px;font-weight:800;color:var(--text);margin-bottom:4px;">
                ${window.State.formatRp(item.total)}
              </div>
              <div style="font-size:11px;color:var(--secondary);display:flex;justify-content:space-between;">
                <span>${item.count} order</span>
                <span>${pct}% omzet</span>
              </div>
            </div>
          `;
        }).join('')}
      </div>

      <!-- Cashless Ratio Insight -->
      <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 14px;background:var(--surface-container-low);border-radius:var(--radius-md);border:1px solid rgba(255,255,255,0.05);font-size:12px;">
        <span style="display:inline-flex;align-items:center;gap:6px;">
          <span class="material-symbols-outlined" style="font-size:16px;color:var(--primary);">contactless</span>
          <span><strong>Rasio Cashless:</strong> ${cashlessPct}% Non-Tunai</span>
        </span>
        <span style="font-family:var(--font-mono);font-size:12px;color:var(--primary);font-weight:700;">${window.State.formatRp(cashlessTotal)}</span>
      </div>
    `;
  }

  renderRecentTransactions(trxs) {
    const tbody = document.getElementById('recent-transactions-tbody');
    const loadMoreContainer = document.getElementById('report-load-more-container');
    if (!tbody) return;

    // Filter by search, payment method, order type
    let filtered = [...trxs];
    if (this.searchQuery) {
      filtered = filtered.filter(t => 
        (t.no && t.no.toLowerCase().includes(this.searchQuery)) ||
        (t.pelanggan && t.pelanggan.toLowerCase().includes(this.searchQuery)) ||
        (t.kasir && t.kasir.toLowerCase().includes(this.searchQuery))
      );
    }

    if (this.filterPayment !== 'all') {
      filtered = filtered.filter(t => (t.metode || 'cash') === this.filterPayment);
    }

    if (this.filterType !== 'all') {
      filtered = filtered.filter(t => (t.tipe || 'dine-in') === this.filterType);
    }

    const totalVisible = this.currentPage * this.itemsPerPage;
    const paginated = filtered.slice(0, totalVisible);

    if (loadMoreContainer) {
      loadMoreContainer.style.display = filtered.length > totalVisible ? 'block' : 'none';
    }

    if (paginated.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;color:var(--secondary)">Tidak ada transaksi ditemukan</td></tr>`;
      return;
    }

    window._reportTrxCache = window._reportTrxCache || {};
    paginated.forEach(t => { window._reportTrxCache[t.no] = t; });

    tbody.innerHTML = paginated.map(t => {
      const isVoid = t.status === 'void';
      const items = t.items || [];
      let itemsSummary = '-';
      if (items.length === 1) {
        itemsSummary = `${items[0].nm} (${items[0].qty})`;
      } else if (items.length > 1) {
        itemsSummary = `${items[0].nm} (${items[0].qty}) <span style="color:var(--secondary);font-size:10px;">+${items.length - 1} lainnya</span>`;
      }
      return `
      <tr style="border-bottom:1px solid rgba(255,255,255,0.05);${isVoid ? 'opacity:0.45;' : ''}">
        <td class="font-mono" style="padding:10px 14px;font-weight:700;color:${isVoid ? 'var(--secondary)' : 'var(--primary)'}">${t.no}</td>
        <td style="padding:10px 14px;color:var(--secondary);font-size:12px">${window.State.formatDate(t.tgl)}</td>
        <td style="padding:10px 14px;">
          <span class="badge info">${(t.tipe || 'dine-in').toUpperCase()}</span>
        </td>
        <td style="padding:10px 14px;font-size:12px;font-weight:600;">${t.pelanggan || 'Umum'}</td>
        <td style="padding:10px 14px;font-size:12px;max-width:160px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${itemsSummary}</td>
        <td style="padding:10px 14px;font-size:11px;text-transform:uppercase;color:var(--secondary)">${t.metode || 'cash'}</td>
        <td class="font-mono" style="padding:10px 14px;font-weight:700;text-align:right;${isVoid ? 'text-decoration:line-through;color:var(--secondary);' : ''}">${window.State.formatRp(t.total)}</td>
        <td style="padding:10px 14px;text-align:center;white-space:nowrap;">
          ${isVoid
            ? `<span style="font-size:10px;font-weight:700;color:var(--error);background:rgba(239,68,68,0.12);padding:3px 8px;border-radius:4px;letter-spacing:0.5px;">VOID</span>`
            : `<button class="btn btn-secondary" style="padding:4px 8px;font-size:11px;margin-right:4px;" onclick="window.PrinterService.openReceiptModal(window._reportTrxCache['${t.no}'])" title="Lihat Struk">
                <span class="material-symbols-outlined" style="font-size:15px">receipt</span>
              </button>
              <button class="btn" style="padding:4px 8px;font-size:11px;background:rgba(239,68,68,0.15);color:var(--error);border:1px solid rgba(239,68,68,0.35);border-radius:var(--radius-sm);cursor:pointer;transition:background 0.2s;" onmouseover="this.style.background='rgba(239,68,68,0.28)'" onmouseout="this.style.background='rgba(239,68,68,0.15)'" onclick="window.ReportsView.voidTransaction('${t.no}')" title="Batalkan Transaksi (Void)">
                <span class="material-symbols-outlined" style="font-size:15px">cancel</span>
              </button>`
          }
        </td>
      </tr>
    `}).join('');
  }

  voidTransaction(orderNo) {
    if (window.OrdersView && window.OrdersView.openVoidModal) {
      window.OrdersView.openVoidModal(orderNo);
      return;
    }
    const trx = (window.State.transactions || []).find(t => t.no === orderNo);
    if (!trx) return;

    if (!confirm(`Konfirmasi pembatalan nota ${trx.no} senilai ${window.State.formatRp(trx.total)}?\n\n• Stok bahan baku akan dikembalikan\n• Kas masuk akan disesuaikan (Void)`)) {
      return;
    }

    // 1. Restore inventory
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
              ket: `VOID Nota ${trx.no}`,
              staf: window.State.currentUser ? window.State.currentUser.nm : 'Sistem'
            });
          }
        });
      }
    });
    window.State.save(LS_KEYS.inv, window.State.inventory);
    window.State.save(LS_KEYS.mut, window.State.stockMutations);

    // 2. Adjust cash log if was cash
    if (trx.metode === 'cash') {
      window.State.cashLog.unshift({
        id: Date.now(),
        tgl: Date.now(),
        tipe: 'keluar',
        kat: 'Void Transaksi',
        jml: trx.total,
        ket: `Batal Nota ${trx.no}`,
        staf: window.State.currentUser ? window.State.currentUser.nm : 'Sistem'
      });
      window.State.save(LS_KEYS.kas, window.State.cashLog);
    }

    // 3. Remove transaction from list
    window.State.transactions = window.State.transactions.filter(t => t.no !== orderNo);
    window.State.save(LS_KEYS.trx, window.State.transactions);

    // 4. Log audit
    window.State.logAudit('TRANSACTION_VOID', { orderNo: trx.no, total: trx.total, itemsCount: (trx.items || []).length });
    window.State.toast(`Nota ${trx.no} berhasil dibatalkan (VOID)`, 'info');
    this.render();
  }

  exportCSV() {
    const trxs = this.getFilteredTransactions();
    if (trxs.length === 0) {
      window.State.toast('Tidak ada transaksi pada periode ini untuk diekspor', 'warning');
      return;
    }

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
              <th>Pelanggan</th>
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
                <td>${t.pelanggan || 'Umum'}</td>
                <td>${t.tipe || 'Dine-In'}</td>
                <td>${(t.metode || 'Cash').toUpperCase()}</td>
                <td class="text-right">${window.State.formatRp(t.total)}</td>
              </tr>
            `).join('')}
            <tr class="total-row">
              <td colspan="6" class="text-right">TOTAL KESELURUHAN</td>
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
