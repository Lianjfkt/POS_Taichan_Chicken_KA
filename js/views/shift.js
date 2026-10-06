/**
 * KA POS v3.0 - Shift Management, Petty Cash & Blind Cash Count Reconciliation
 */

class ShiftView {
  constructor() {
    this.blindCountBreakdown = {
      100000: 0,
      50000: 0,
      20000: 0,
      10000: 0,
      5000: 0,
      2000: 0,
      1000: 0,
      coin: 0
    };
    this.lastClosedReport = null;
  }

  init() {
    this.bindEvents();
    this.render();

    window.State.on(LS_KEYS.sesi, () => this.render());
    window.State.on(LS_KEYS.kas, () => this.render());
  }

  bindEvents() {
    // Open Shift Form Submit
    const openShiftForm = document.getElementById('open-shift-form');
    if (openShiftForm) {
      openShiftForm.onsubmit = (e) => {
        e.preventDefault();
        const input = document.getElementById('open-shift-starter-cash');
        const amount = Number(input.value.replace(/[^0-9]/g, '')) || 0;
        this.startShift(amount);
      };
    }

    // Petty Cash Form Submit
    const pettyForm = document.getElementById('petty-cash-form');
    if (pettyForm) {
      pettyForm.onsubmit = (e) => {
        e.preventDefault();
        this.handlePettyCashSubmit();
      };
    }

    // Blind Cash Count inputs
    document.querySelectorAll('.blind-cash-input').forEach(input => {
      input.oninput = () => {
        const denom = input.getAttribute('data-denom');
        const qty = Number(input.value) || 0;
        this.blindCountBreakdown[denom] = qty;
        this.updateBlindCountTotal();
      };
    });

    // Close Shift Submit
    const btnSubmitBlind = document.getElementById('btn-submit-blind-count');
    if (btnSubmitBlind) {
      btnSubmitBlind.onclick = () => this.submitBlindCount();
    }
  }

  startShift(starterCash) {
    // BUG-11 fix: pastikan ada user aktif sebelum buka shift
    if (!window.State.currentUser) {
      window.State.toast('Harap login terlebih dahulu sebelum membuka shift!', 'error');
      return;
    }
    const cashierName = window.State.currentUser.nm;
    const newShift = {
      id: 'SH-' + Date.now(),
      staf: cashierName,
      mulai: Date.now(),
      modalAwal: starterCash,
      status: 'aktif'
    };

    window.State.activeShift = newShift;
    window.State.save(LS_KEYS.sesi, newShift);

    // Add entry to cash log
    window.State.cashLog.unshift({
      id: Date.now(),
      tgl: Date.now(),
      tipe: 'masuk',
      kat: 'Modal Awal Kasir',
      jml: starterCash,
      ket: `Buka Shift oleh ${cashierName}`,
      staf: cashierName
    });
    window.State.save(LS_KEYS.kas, window.State.cashLog);
    window.State.logAudit('SHIFT_OPENED', { shiftId: newShift.id, kasir: cashierName, modalAwal: starterCash });

    const modal = document.getElementById('open-shift-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast('Shift baru berhasil dibuka!', 'success');
    this.render();
  }

  handlePettyCashSubmit() {
    const typeSelect = document.getElementById('petty-type');
    const amountInput = document.getElementById('petty-amount');
    const reasonInput = document.getElementById('petty-reason');

    if (!typeSelect || !amountInput || !reasonInput) return;

    const tipe = typeSelect.value; // 'masuk' or 'keluar'
    const jml = Number(amountInput.value.replace(/[^0-9]/g, '')) || 0;
    const ket = reasonInput.value.trim();

    if (jml <= 0) {
      window.State.toast('Masukkan nominal yang valid!', 'error');
      return;
    }
    if (!ket) {
      window.State.toast('Masukkan keterangan pengeluaran/pemasukan!', 'error');
      return;
    }

    const cashierName = window.State.currentUser ? window.State.currentUser.nm : 'Kasir';

    window.State.cashLog.unshift({
      id: Date.now(),
      tgl: Date.now(),
      tipe: tipe,
      kat: tipe === 'masuk' ? 'Kas Masuk Operasional' : 'Petty Cash Keluar',
      jml: jml,
      ket: ket,
      staf: cashierName
    });
    window.State.save(LS_KEYS.kas, window.State.cashLog);

    amountInput.value = '';
    reasonInput.value = '';

    const modal = document.getElementById('petty-cash-modal');
    if (modal) modal.classList.remove('open');

    window.State.toast(`Arus kas (${tipe.toUpperCase()}) berhasil dicatat!`, 'success');
    this.render();
  }

  openPettyCashModal(type = 'keluar') {
    const modal = document.getElementById('petty-cash-modal');
    const typeSelect = document.getElementById('petty-type');
    if (typeSelect) typeSelect.value = type;
    if (modal) modal.classList.add('open');
  }

  openBlindCountModal() {
    const modal = document.getElementById('blind-cash-count-modal');
    // Reset inputs
    document.querySelectorAll('.blind-cash-input').forEach(i => i.value = '');
    this.blindCountBreakdown = { 100000: 0, 50000: 0, 20000: 0, 10000: 0, 5000: 0, 2000: 0, 1000: 0, coin: 0 };
    this.updateBlindCountTotal();

    if (modal) modal.classList.add('open');
  }

  updateBlindCountTotal() {
    let total = 0;
    Object.keys(this.blindCountBreakdown).forEach(denom => {
      const qty = this.blindCountBreakdown[denom] || 0;
      if (denom === 'coin') {
        total += qty;
      } else {
        total += Number(denom) * qty;
      }
    });

    const totalEl = document.getElementById('blind-count-calculated-total');
    if (totalEl) totalEl.textContent = window.State.formatRp(total);
    return total;
  }

  getShiftFinancialSummary(shift) {
    if (!shift) return null;

    const starterCash = Number(shift.modalAwal) || 0;
    // BUG-17 fix: filter dengan batas waktu selesai agar tidak overlap antar shift
    const endTime = shift.selesai || Date.now();
    const shiftLogs = (window.State.cashLog || []).filter(l => l.tgl >= shift.mulai && l.tgl <= endTime);
    const shiftTrx = (window.State.transactions || []).filter(t => t.tgl >= shift.mulai && t.tgl <= endTime);
    const shiftTrxValid = shiftTrx.filter(t => t.status !== 'void');

    const cashSales = shiftLogs.filter(l => l.kat === 'Penjualan POS').reduce((s, l) => s + l.jml, 0);
    const nonCashSales = shiftTrxValid.filter(t => t.metode !== 'cash').reduce((s, t) => s + (t.total || 0), 0);
    const cashInOther = shiftLogs.filter(l => l.tipe === 'masuk' && l.kat !== 'Modal Awal Kasir' && l.kat !== 'Penjualan POS').reduce((s, l) => s + l.jml, 0);
    const cashOut = shiftLogs.filter(l => l.tipe === 'keluar').reduce((s, l) => s + l.jml, 0);

    const expectedSystemCash = starterCash + cashSales + cashInOther - cashOut;

    return {
      shiftId: shift.id,
      kasir: shift.staf,
      mulai: shift.mulai,
      modalAwal: starterCash,
      omzetTunai: cashSales,
      omzetNonTunai: nonCashSales,
      totalMasuk: cashInOther,
      totalKeluar: cashOut,
      saldoSistem: expectedSystemCash,
      trxCount: shiftTrx.length
    };
  }

  printXReport() {
    const shift = window.State.activeShift;
    if (!shift) {
      window.State.toast('Tidak ada shift aktif yang berjalan!', 'error');
      return;
    }

    const data = this.getShiftFinancialSummary(shift);
    if (window.PrinterService) {
      window.PrinterService.printShiftReport(data, 'X');
    }
  }

  submitBlindCount() {
    const physicalCash = this.updateBlindCountTotal();
    const shift = window.State.activeShift;

    if (!shift) {
      window.State.toast('Tidak ada shift aktif yang berjalan!', 'error');
      return;
    }

    const summary = this.getShiftFinancialSummary(shift);
    const discrepancy = physicalCash - summary.saldoSistem;
    let statusDiscrepancy = 'PAS';
    if (discrepancy > 0) statusDiscrepancy = 'LEBIH';
    if (discrepancy < 0) statusDiscrepancy = 'KURANG';

    const closedReport = {
      ...summary,
      selesai: Date.now(),
      uangFisik: physicalCash,
      selisih: discrepancy,
      status: statusDiscrepancy,
      breakdown: { ...this.blindCountBreakdown }
    };

    this.lastClosedReport = closedReport;

    // Close shift
    window.State.activeShift = null;
    window.State.save(LS_KEYS.sesi, null);

    // Save report to staff shift log & shift history
    window.State.staffLog.unshift(closedReport);
    window.State.save(LS_KEYS.stlog, window.State.staffLog);
    window.State.saveShiftHistory(closedReport);
    window.State.logAudit('SHIFT_CLOSED', { shiftId: closedReport.shiftId, kasir: closedReport.kasir, selisih: closedReport.selisih });

    // Close blind count modal
    const blindModal = document.getElementById('blind-cash-count-modal');
    if (blindModal) blindModal.classList.remove('open');

    // Show reconciliation modal
    this.showReconciliationModal(closedReport);
  }

  showReconciliationModal(report) {
    const modal = document.getElementById('reconciliation-modal');
    const kasirEl = document.getElementById('recon-kasir');
    const sistemEl = document.getElementById('recon-sistem');
    const fisikEl = document.getElementById('recon-fisik');
    const selisihEl = document.getElementById('recon-selisih');
    const badgeEl = document.getElementById('recon-badge');

    if (kasirEl) kasirEl.textContent = report.kasir;
    if (sistemEl) sistemEl.textContent = window.State.formatRp(report.saldoSistem);
    if (fisikEl) fisikEl.textContent = window.State.formatRp(report.uangFisik);
    if (selisihEl) {
      selisihEl.textContent = (report.selisih >= 0 ? '+' : '') + window.State.formatRp(report.selisih);
      selisihEl.style.color = report.selisih === 0 ? 'var(--tertiary)' : (report.selisih > 0 ? 'var(--primary)' : 'var(--error)');
    }
    if (badgeEl) {
      badgeEl.textContent = `REKONSILIASI: ${report.status}`;
      badgeEl.className = `badge ${report.status === 'PAS' ? 'success' : (report.status === 'LEBIH' ? 'warning' : 'error')}`;
    }

    // Attach Z-Report Print & WhatsApp handlers
    const btnPrintZ = document.getElementById('btn-recon-print-z');
    const btnShareWa = document.getElementById('btn-recon-wa');

    if (btnPrintZ) {
      btnPrintZ.onclick = () => {
        if (window.PrinterService) window.PrinterService.printShiftReport(report, 'Z');
      };
    }
    if (btnShareWa) {
      btnShareWa.onclick = () => {
        if (window.PrinterService) window.PrinterService.shareShiftWhatsApp(report, 'Z');
      };
    }

    if (modal) modal.classList.add('open');
  }

  render() {
    const shift = window.State.activeShift;
    const noShiftCard = document.getElementById('shift-no-active-card');
    const activeShiftCard = document.getElementById('shift-active-card');

    this.renderShiftHistory();

    if (!shift) {
      if (noShiftCard) noShiftCard.style.display = 'block';
      if (activeShiftCard) activeShiftCard.style.display = 'none';
      return;
    }

    if (noShiftCard) noShiftCard.style.display = 'none';
    if (activeShiftCard) activeShiftCard.style.display = 'block';

    const summary = this.getShiftFinancialSummary(shift);
    const shiftLogs = (window.State.cashLog || []).filter(l => l.tgl >= shift.mulai);

    const nameEl = document.getElementById('shift-active-staff');
    const timeEl = document.getElementById('shift-active-time');
    const startEl = document.getElementById('shift-active-starter');
    const inEl = document.getElementById('shift-active-in');
    const outEl = document.getElementById('shift-active-out');
    const estEl = document.getElementById('shift-active-est');

    if (nameEl) nameEl.textContent = shift.staf;
    if (timeEl) timeEl.textContent = window.State.formatDate(shift.mulai);
    if (startEl) startEl.textContent = window.State.formatRp(summary.modalAwal);
    if (inEl) inEl.textContent = window.State.formatRp(summary.omzetTunai + summary.totalMasuk);
    if (outEl) outEl.textContent = window.State.formatRp(summary.totalKeluar);
    if (estEl) estEl.textContent = window.State.formatRp(summary.saldoSistem);

    // Render Cash Log table
    const logTableBody = document.getElementById('shift-cash-log-tbody');
    if (logTableBody) {
      logTableBody.innerHTML = shiftLogs.slice(0, 25).map(log => `
        <tr style="border-bottom:1px solid rgba(255,255,255,0.05)">
          <td style="padding:10px 14px;color:var(--secondary)">${new Date(log.tgl).toLocaleTimeString('id-ID', {hour:'2-digit',minute:'2-digit'})}</td>
          <td style="padding:10px 14px">
            <span class="badge ${log.tipe === 'masuk' ? 'success' : 'error'}">
              ${log.tipe.toUpperCase()}
            </span>
          </td>
          <td style="padding:10px 14px;font-weight:600">${log.kat || '-'}</td>
          <td style="padding:10px 14px;color:var(--secondary)">${log.ket || '-'}</td>
          <td class="font-mono" style="padding:10px 14px;font-weight:700;text-align:right;color:${log.tipe === 'masuk' ? 'var(--tertiary)' : 'var(--error)'}">
            ${log.tipe === 'masuk' ? '+' : '-'}${window.State.formatRp(log.jml)}
          </td>
        </tr>
      `).join('');
    }
  }

  renderShiftHistory() {
    const tbody = document.getElementById('shift-history-tbody');
    if (!tbody) return;

    const list = window.State.shiftHistory || [];
    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:var(--secondary)">Belum ada riwayat shift yang ditutup.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.slice(0, 20).map(s => `
      <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
        <td style="padding:10px 14px;font-weight:700;color:var(--on-surface);">${s.kasir || 'Kasir'}</td>
        <td style="padding:10px 14px;font-size:12px;color:var(--secondary);">${window.State.formatDate(s.mulai)}</td>
        <td style="padding:10px 14px;font-size:12px;color:var(--secondary);">${s.selesai ? window.State.formatDate(s.selesai) : '-'}</td>
        <td style="padding:10px 14px;font-family:var(--font-mono);">${window.State.formatRp(s.saldoSistem || 0)}</td>
        <td style="padding:10px 14px;font-family:var(--font-mono);font-weight:700;">${window.State.formatRp(s.uangFisik || 0)}</td>
        <td style="padding:10px 14px;font-family:var(--font-mono);font-weight:700;color:${s.selisih === 0 ? 'var(--tertiary)' : (s.selisih > 0 ? 'var(--primary)' : 'var(--error)')};">
          ${s.selisih >= 0 ? '+' : ''}${window.State.formatRp(s.selisih || 0)} (${s.status || 'PAS'})
        </td>
        <td style="padding:10px 14px;text-align:center;">
          <button class="btn btn-secondary" style="padding:4px 8px;font-size:11px;" onclick="window.PrinterService.openShiftReportModal(window.State.shiftHistory.find(item => item.shiftId === '${s.shiftId}'), 'Z')" title="Cetak Ulang Laporan Z">
            <span class="material-symbols-outlined" style="font-size:14px;">print</span>
          </button>
        </td>
      </tr>
    `).join('');
  }
}

window.ShiftView = new ShiftView();
