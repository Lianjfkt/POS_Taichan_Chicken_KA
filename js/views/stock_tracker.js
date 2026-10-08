/**
 * KA POS v3.0 - Daily Stock Tracker
 * Tracks: Taichan (tusuk) & Chicken Parts (potong)
 * Features: Raw → Fried → Sold tracking, freshness check (max 3 days)
 */

// LS Key for stock tracker
const ST_KEY = 'ka_daily_stock';

// Chicken parts config
const CHICKEN_PARTS = [
  { id: 'sayap',      label: 'Sayap',      emoji: '🍗', color: '#f97316' },
  { id: 'paha_atas',  label: 'Paha Atas',  emoji: '🍗', color: '#ef4444' },
  { id: 'paha_bawah', label: 'Paha Bawah', emoji: '🍗', color: '#f59e0b' },
  { id: 'dada',       label: 'Dada',       emoji: '🍗', color: '#84cc16' },
];

const MAX_FRESHNESS_DAYS = 3;

class StockTrackerView {
  constructor() {
    this.activeSubTab = 'taichan';
  }

  init() {
    this._bindEvents();
    this.render();
    window.State.on('ka_trx', () => this.render());
  }

  // ── Data Layer ─────────────────────────────────────────────────────────

  _loadData() {
    try {
      const raw = localStorage.getItem(ST_KEY);
      return raw ? JSON.parse(raw) : { taichan: [], chicken: {} };
    } catch { return { taichan: [], chicken: {} }; }
  }

  _saveData(data) { localStorage.setItem(ST_KEY, JSON.stringify(data)); }

  _today() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  }

  _daysAgo(dateStr) {
    const then = new Date(dateStr + 'T00:00:00');
    const now = new Date(); now.setHours(0,0,0,0);
    return Math.floor((now - then) / 86400000);
  }

  _freshnessBadge(dateStr) {
    const d = this._daysAgo(dateStr);
    if (d === 0) return `<span class="st-badge fresh">Segar (Hari Ini)</span>`;
    if (d === 1) return `<span class="st-badge ok">Hari ke-2</span>`;
    if (d === 2) return `<span class="st-badge warn">Hari ke-3 ⚠️</span>`;
    return `<span class="st-badge expired">TIDAK LAYAK JUAL ❌</span>`;
  }

  _isFresh(dateStr) { return this._daysAgo(dateStr) < MAX_FRESHNESS_DAYS; }

  _taichanSoldToday() {
    const today = this._today();
    const result = {};
    (window.State.transactions || []).forEach(tx => {
      if (tx.status === 'void') return;
      const d = tx.tgl ? new Date(tx.tgl) : null;
      if (!d) return;
      const txDay = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
      if (txDay !== today) return;
      (tx.items || []).forEach(item => {
        const prod = (window.State.products || []).find(p => p.id === item.id);
        if (prod && prod.kat === 'Taichan') result[item.id] = (result[item.id] || 0) + item.qty;
      });
    });
    return result;
  }

  // ── Taichan CRUD ──────────────────────────────────────────────────────

  _addTaichanBatch(qty, notes) {
    const data = this._loadData();
    data.taichan.push({ id: 'TC-'+Date.now(), date: this._today(), raw: qty, fried: 0, notes: notes || '' });
    this._saveData(data);
    if (window.State && window.State.addStockAuditLog) {
      window.State.addStockAuditLog({
        itemId: 'taichan',
        itemName: 'Sate Taichan',
        qtyBefore: 0,
        qtyAfter: qty,
        satuan: 'tusuk',
        tipe: 'batch_taichan_raw',
        keterangan: notes ? `Tambah batch mentah: ${notes}` : 'Tambah batch mentah'
      });
    }
  }

  _updateTaichanFried(batchId, fried) {
    const data = this._loadData();
    const b = data.taichan.find(x => x.id === batchId);
    if (b) {
      const prevFried = b.fried || 0;
      b.fried = Math.min(Number(fried)||0, b.raw);
      this._saveData(data);
      if (window.State && window.State.addStockAuditLog) {
        window.State.addStockAuditLog({
          itemId: 'taichan',
          itemName: `Sate Taichan (${b.date})`,
          qtyBefore: prevFried,
          qtyAfter: b.fried,
          satuan: 'tusuk',
          tipe: 'batch_taichan_fried',
          keterangan: `Update goreng batch ${b.date}`
        });
      }
    }
  }

  _deleteTaichanBatch(batchId) {
    const data = this._loadData();
    const b = data.taichan.find(x => x.id === batchId);
    data.taichan = data.taichan.filter(x => x.id !== batchId);
    this._saveData(data);
    if (b && window.State && window.State.addStockAuditLog) {
      window.State.addStockAuditLog({
        itemId: 'taichan',
        itemName: `Sate Taichan (${b.date})`,
        qtyBefore: b.raw,
        qtyAfter: 0,
        satuan: 'tusuk',
        tipe: 'delete_batch',
        keterangan: `Hapus batch taichan ${b.id}`
      });
    }
  }

  // ── Chicken CRUD ──────────────────────────────────────────────────────

  _addChickenBatch(part, qty, notes) {
    const data = this._loadData();
    if (!data.chicken[part]) data.chicken[part] = [];
    data.chicken[part].push({ id: `CH-${part}-${Date.now()}`, date: this._today(), raw: qty, fried: 0, notes: notes || '' });
    this._saveData(data);
    const partCfg = CHICKEN_PARTS.find(p => p.id === part);
    const partLabel = partCfg ? partCfg.label : part;
    if (window.State && window.State.addStockAuditLog) {
      window.State.addStockAuditLog({
        itemId: `chicken_${part}`,
        itemName: `Ayam Potong - ${partLabel}`,
        qtyBefore: 0,
        qtyAfter: qty,
        satuan: 'potong',
        tipe: 'batch_chicken_raw',
        keterangan: notes ? `Tambah batch mentah: ${notes}` : 'Tambah batch mentah'
      });
    }
  }

  _updateChickenFried(part, batchId, fried) {
    const data = this._loadData();
    if (!data.chicken[part]) return;
    const b = data.chicken[part].find(x => x.id === batchId);
    if (b) {
      const prevFried = b.fried || 0;
      b.fried = Math.min(Number(fried)||0, b.raw);
      this._saveData(data);
      const partCfg = CHICKEN_PARTS.find(p => p.id === part);
      const partLabel = partCfg ? partCfg.label : part;
      if (window.State && window.State.addStockAuditLog) {
        window.State.addStockAuditLog({
          itemId: `chicken_${part}`,
          itemName: `Ayam Potong - ${partLabel} (${b.date})`,
          qtyBefore: prevFried,
          qtyAfter: b.fried,
          satuan: 'potong',
          tipe: 'batch_chicken_fried',
          keterangan: `Update goreng batch ${b.date}`
        });
      }
    }
  }

  _deleteChickenBatch(part, batchId) {
    const data = this._loadData();
    if (!data.chicken[part]) return;
    const b = data.chicken[part].find(x => x.id === batchId);
    data.chicken[part] = data.chicken[part].filter(x => x.id !== batchId);
    this._saveData(data);
    const partCfg = CHICKEN_PARTS.find(p => p.id === part);
    const partLabel = partCfg ? partCfg.label : part;
    if (b && window.State && window.State.addStockAuditLog) {
      window.State.addStockAuditLog({
        itemId: `chicken_${part}`,
        itemName: `Ayam Potong - ${partLabel} (${b.date})`,
        qtyBefore: b.raw,
        qtyAfter: 0,
        satuan: 'potong',
        tipe: 'delete_batch',
        keterangan: `Hapus batch ayam ${partLabel}`
      });
    }
  }

  // ── Summaries ─────────────────────────────────────────────────────────

  _taichanSummary(data) {
    const soldMap = this._taichanSoldToday();
    const totalSold = Object.values(soldMap).reduce((a,b) => a+b, 0);
    const totalRaw   = data.taichan.reduce((s,b) => s+b.raw, 0);
    const totalFried = data.taichan.reduce((s,b) => s+b.fried, 0);
    return { totalRaw, totalFried, totalSold, sisa: Math.max(0, totalFried - totalSold) };
  }

  // ── Render ────────────────────────────────────────────────────────────

  render() {
    this._renderSubTabs();
    this._renderTaichan();
    this._renderChicken();
  }

  _renderSubTabs() {
    const tEl = document.getElementById('st-tab-taichan-content');
    const cEl = document.getElementById('st-tab-chicken-content');
    const bT  = document.getElementById('st-subtab-taichan');
    const bC  = document.getElementById('st-subtab-chicken');
    if (!tEl || !cEl) return;
    const isT = this.activeSubTab === 'taichan';
    tEl.style.display = isT ? 'block' : 'none';
    cEl.style.display = isT ? 'none'  : 'block';
    if (bT) bT.classList.toggle('active', isT);
    if (bC) bC.classList.toggle('active', !isT);
  }

  _summaryCard(emoji, label, value, color) {
    const iconHtml = window.FoodIcons ? window.FoodIcons.get(emoji, { size: 30 }) : `<div style="font-size:22px;">${emoji}</div>`;
    return `<div style="background:var(--surface-container);border:1px solid rgba(255,255,255,0.06);border-radius:var(--radius-md);padding:14px 12px;display:flex;flex-direction:column;justify-content:space-between;">
      <div style="margin-bottom:6px;">${iconHtml}</div>
      <div style="font-size:10px;color:var(--secondary);font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">${label}</div>
      <div class="font-mono" style="font-size:20px;font-weight:800;color:${color};margin-top:4px;">${value}</div>
    </div>`;
  }

  _renderTaichan() {
    const el = document.getElementById('st-taichan-content');
    if (!el) return;
    const data = this._loadData();
    const sum  = this._taichanSummary(data);
    const soldMap = this._taichanSoldToday();
    const taichanProds = (window.State.products || []).filter(p => p.kat === 'Taichan');
    const isOwner = window.State && window.State.isOwner();

    const soldRows = taichanProds.length
      ? taichanProds.map(p => {
          const qty = soldMap[p.id] || 0;
          const icon = window.FoodIcons ? window.FoodIcons.get(p.emj || '🍢', { size: 20 }) : (p.emj || '🍢');
          return `<div style="display:flex;justify-content:space-between;align-items:center;padding:7px 0;border-bottom:1px solid rgba(255,255,255,0.04);">
            <span style="font-size:13px;display:inline-flex;align-items:center;gap:6px;">${icon} ${p.nm}</span>
            <span class="font-mono" style="font-weight:700;color:${qty>0?'var(--primary)':'var(--secondary)'};">${qty} tusuk</span>
          </div>`;
        }).join('')
      : `<div style="text-align:center;padding:12px;color:var(--secondary);font-size:12px;">Belum ada menu Taichan terdaftar</div>`;

    const batchRows = data.taichan.length
      ? data.taichan.map(b => {
          const fresh = this._isFresh(b.date);
          return `<tr style="border-bottom:1px solid rgba(255,255,255,0.04);${!fresh?'opacity:0.65;background:rgba(239,68,68,0.05);':''}">
            <td style="padding:10px 12px;font-size:12px;color:var(--secondary);">${b.date}</td>
            <td style="padding:10px 12px;">${this._freshnessBadge(b.date)}</td>
            <td class="font-mono" style="padding:10px 12px;font-weight:700;">${b.raw} tusuk</td>
            <td style="padding:10px 12px;">
              <div style="display:flex;align-items:center;gap:6px;">
                <input type="number" min="0" max="${b.raw}" value="${b.fried}"
                  class="form-input font-mono" style="width:72px;padding:4px 6px;font-size:12px;text-align:center;"
                  onchange="window.StockTracker._updateTaichanFried('${b.id}',this.value);window.StockTracker.render();"
                  ${!fresh?'disabled':''}>
                <span style="font-size:11px;color:var(--secondary);">/ ${b.raw}</span>
              </div>
            </td>
            <td class="font-mono" style="padding:10px 12px;color:${b.fried>0?'var(--tertiary)':'var(--secondary)'};">${isOwner ? b.fried + ' tusuk' : '••'}</td>
            <td style="padding:10px 12px;font-size:12px;color:var(--secondary);">${b.notes||'-'}</td>
            <td style="padding:10px 12px;text-align:right;">
              <button class="btn btn-secondary" style="padding:4px 8px;color:var(--error);"
                onclick="if(confirm('Hapus batch ini?')){window.StockTracker._deleteTaichanBatch('${b.id}');window.StockTracker.render();}">
                <span class="material-symbols-outlined" style="font-size:14px;">delete</span>
              </button>
            </td>
          </tr>`;
        }).join('')
      : `<tr><td colspan="7" style="text-align:center;padding:24px;color:var(--secondary);font-size:13px;">Belum ada batch. Klik "Tambah Batch" untuk mulai.</td></tr>`;

    const summarySection = isOwner
      ? `
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:10px;margin-bottom:18px;">
          ${this._summaryCard('🍢','Stok Mentah',sum.totalRaw+' tusuk','var(--on-surface)')}
          ${this._summaryCard('🔥','Sudah Digoreng',sum.totalFried+' tusuk','var(--warning)')}
          ${this._summaryCard('💸','Terjual Hari Ini',sum.totalSold+' tusuk','var(--primary)')}
          ${this._summaryCard('📦','Sisa Matang',sum.sisa+' tusuk',sum.sisa>0?'var(--tertiary)':'var(--secondary)')}
        </div>
        <div class="section-card" style="margin-bottom:14px;">
          <div style="font-size:13px;font-weight:700;margin-bottom:10px;display:flex;align-items:center;gap:6px;">
            <span class="material-symbols-outlined" style="font-size:16px;color:var(--primary);">point_of_sale</span>
            Penjualan Taichan Hari Ini (dari POS)
          </div>
          ${soldRows}
        </div>`
      : `
        <div style="background:var(--surface-container);border:1px dashed rgba(255,255,255,0.12);border-radius:var(--radius-md);padding:14px 16px;margin-bottom:16px;display:flex;align-items:center;gap:12px;">
          <span class="material-symbols-outlined" style="font-size:26px;color:var(--primary);">verified_user</span>
          <div>
            <div style="font-weight:700;font-size:13px;color:var(--on-surface);">Mode Input Stok Kasir (Blind Audit)</div>
            <div style="font-size:12px;color:var(--secondary);margin-top:2px;">Rekonsiliasi total proses &amp; sisa dihitung otomatis untuk audit Owner. Masukkan jumlah mentah &amp; goreng riil di bawah.</div>
          </div>
        </div>`;

    el.innerHTML = `
      ${summarySection}
      <div class="section-card">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;flex-wrap:wrap;gap:8px;">
          <div>
            <div style="font-size:13px;font-weight:700;">📋 Batch Stok Taichan</div>
            <div style="font-size:11px;color:var(--secondary);margin-top:2px;">Catat stok mentah &amp; update jumlah yang digoreng</div>
          </div>
          <button class="btn btn-primary" style="font-size:12px;" onclick="window.StockTracker.openAddTaichanModal()">
            <span class="material-symbols-outlined" style="font-size:16px;">add</span> Tambah Batch
          </button>
        </div>
        <div style="overflow-x:auto;">
          <table class="data-table" style="min-width:640px;">
            <thead><tr>
              <th>Tanggal</th><th>Kelayakan</th><th>Stok Mentah</th>
              <th>Digoreng</th><th>Total Goreng</th><th>Keterangan</th>
              <th style="text-align:right;">Aksi</th>
            </tr></thead>
            <tbody>${batchRows}</tbody>
          </table>
        </div>
      </div>`;
  }

  _renderChicken() {
    const el = document.getElementById('st-chicken-content');
    if (!el) return;
    const data = this._loadData();
    const isOwner = window.State && window.State.isOwner();

    const cards = CHICKEN_PARTS.map(part => {
      const batches  = data.chicken[part.id] || [];
      const freshB   = batches.filter(b => this._isFresh(b.date));
      const expiredB = batches.filter(b => !this._isFresh(b.date));
      const totalRaw   = batches.reduce((s,b) => s+b.raw,   0);
      const totalFried = batches.reduce((s,b) => s+b.fried, 0);

      const miniBatches = freshB.length
        ? freshB.map(b => `<div style="display:flex;justify-content:space-between;padding:4px 0;border-top:1px solid rgba(255,255,255,0.04);font-size:11px;">
            <span style="color:var(--secondary);">${b.date}</span>
            <span class="font-mono">${b.raw}<span style="color:var(--secondary);"> mentah</span> / <span style="color:var(--warning);">${isOwner ? b.fried + ' goreng' : '••'}</span></span>
          </div>`).join('')
        : `<div style="text-align:center;font-size:11px;color:var(--secondary);padding:8px 0;">Tidak ada stok aktif</div>`;

      return `<div style="background:var(--surface-container);border:1px solid rgba(255,255,255,0.06);border-radius:var(--radius-md);padding:14px;">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:10px;">
          <div>
            <div style="font-size:14px;font-weight:800;">${part.emoji} ${part.label}</div>
            <div style="font-size:11px;color:var(--secondary);">Ayam Potong</div>
          </div>
          <button class="btn btn-primary" style="font-size:10px;padding:4px 10px;"
            onclick="window.StockTracker.openAddChickenModal('${part.id}','${part.label}')">
            <span class="material-symbols-outlined" style="font-size:14px;">add</span>
          </button>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-bottom:10px;">
          <div style="background:var(--surface-container-low);border-radius:6px;padding:8px;text-align:center;">
            <div style="font-size:10px;color:var(--secondary);">Total Mentah</div>
            <div class="font-mono" style="font-size:18px;font-weight:800;">${isOwner ? totalRaw : '••'}</div>
            <div style="font-size:10px;color:var(--secondary);">potong</div>
          </div>
          <div style="background:var(--surface-container-low);border-radius:6px;padding:8px;text-align:center;">
            <div style="font-size:10px;color:var(--secondary);">Total Goreng</div>
            <div class="font-mono" style="font-size:18px;font-weight:800;color:var(--warning);">${isOwner ? totalFried : '••'}</div>
            <div style="font-size:10px;color:var(--secondary);">potong</div>
          </div>
        </div>
        ${expiredB.length > 0 ? `<div style="background:rgba(239,68,68,0.1);border:1px solid rgba(239,68,68,0.3);border-radius:6px;padding:7px 10px;font-size:11px;color:#ef4444;margin-bottom:8px;">
          ⚠️ ${expiredB.length} batch TIDAK LAYAK JUAL — segera buang</div>` : ''}
        ${miniBatches}
      </div>`;
    }).join('');

    const allRows = CHICKEN_PARTS.flatMap(part => {
      const batches = data.chicken[part.id] || [];
      return batches.map(b => {
        const fresh = this._isFresh(b.date);
        return `<tr style="border-bottom:1px solid rgba(255,255,255,0.04);${!fresh?'opacity:0.65;background:rgba(239,68,68,0.05);':''}">
          <td style="padding:10px 12px;">
            <div style="display:flex;align-items:center;gap:6px;">
              <span>${part.emoji}</span><span style="font-weight:700;font-size:13px;">${part.label}</span>
            </div>
          </td>
          <td style="padding:10px 12px;font-size:12px;color:var(--secondary);">${b.date}</td>
          <td style="padding:10px 12px;">${this._freshnessBadge(b.date)}</td>
          <td class="font-mono" style="padding:10px 12px;font-weight:700;">${b.raw} potong</td>
          <td style="padding:10px 12px;">
            <div style="display:flex;align-items:center;gap:6px;">
              <input type="number" min="0" max="${b.raw}" value="${b.fried}"
                class="form-input font-mono" style="width:70px;padding:4px 6px;font-size:12px;text-align:center;"
                onchange="window.StockTracker._updateChickenFried('${part.id}','${b.id}',this.value);window.StockTracker.render();"
                ${!fresh?'disabled':''}>
              <span style="font-size:11px;color:var(--secondary);">/ ${b.raw}</span>
            </div>
          </td>
          <td class="font-mono" style="padding:10px 12px;color:${b.fried>0?'var(--tertiary)':'var(--secondary)'};">${isOwner ? b.fried + ' potong' : '••'}</td>
          <td style="padding:10px 12px;font-size:12px;color:var(--secondary);">${b.notes||'-'}</td>
          <td style="padding:10px 12px;text-align:right;">
            <button class="btn btn-secondary" style="padding:4px 8px;color:var(--error);"
              onclick="if(confirm('Hapus batch ini?')){window.StockTracker._deleteChickenBatch('${part.id}','${b.id}');window.StockTracker.render();}">
              <span class="material-symbols-outlined" style="font-size:14px;">delete</span>
            </button>
          </td>
        </tr>`;
      });
    });

    const hasData = allRows.length > 0;

    el.innerHTML = `
      <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:12px;margin-bottom:20px;">
        ${cards}
      </div>
      <div class="section-card">
        <div style="font-size:13px;font-weight:700;margin-bottom:14px;display:flex;align-items:center;gap:6px;">
          <span class="material-symbols-outlined" style="font-size:16px;color:var(--warning);">table_rows</span>
          Detail Semua Batch Ayam
        </div>
        <div style="overflow-x:auto;">
          <table class="data-table" style="min-width:720px;">
            <thead><tr>
              <th>Bagian</th><th>Tanggal</th><th>Kelayakan</th><th>Mentah</th>
              <th>Input Goreng</th><th>Total Goreng</th><th>Keterangan</th><th style="text-align:right;">Aksi</th>
            </tr></thead>
            <tbody>
              ${hasData ? allRows.join('') : `<tr><td colspan="8" style="text-align:center;padding:24px;color:var(--secondary);font-size:13px;">Belum ada data. Klik tombol + di tiap bagian untuk tambah stok.</td></tr>`}
            </tbody>
          </table>
        </div>
      </div>`;
  }

  // ── Modals ────────────────────────────────────────────────────────────

  openAddTaichanModal() {
    document.getElementById('st-modal-taichan-qty').value = '';
    document.getElementById('st-modal-taichan-notes').value = '';
    document.getElementById('st-modal-taichan').classList.add('open');
    setTimeout(() => document.getElementById('st-modal-taichan-qty').focus(), 100);
  }

  submitTaichanBatch() {
    const qty   = parseInt(document.getElementById('st-modal-taichan-qty').value) || 0;
    const notes = document.getElementById('st-modal-taichan-notes').value.trim();
    if (qty <= 0) { window.State.toast('Masukkan jumlah tusuk yang valid!', 'error'); return; }
    this._addTaichanBatch(qty, notes);
    document.getElementById('st-modal-taichan').classList.remove('open');
    this.render();
    window.State.toast(`Batch ${qty} tusuk taichan ditambahkan!`, 'success');
  }

  openAddChickenModal(partId, partLabel) {
    document.getElementById('st-modal-chicken-part-id').value = partId;
    document.getElementById('st-modal-chicken-part-label').textContent = partLabel;
    document.getElementById('st-modal-chicken-qty').value = '';
    document.getElementById('st-modal-chicken-notes').value = '';
    document.getElementById('st-modal-chicken').classList.add('open');
    setTimeout(() => document.getElementById('st-modal-chicken-qty').focus(), 100);
  }

  submitChickenBatch() {
    const partId = document.getElementById('st-modal-chicken-part-id').value;
    const qty    = parseInt(document.getElementById('st-modal-chicken-qty').value) || 0;
    const notes  = document.getElementById('st-modal-chicken-notes').value.trim();
    if (!partId || qty <= 0) { window.State.toast('Masukkan jumlah potong yang valid!', 'error'); return; }
    this._addChickenBatch(partId, qty, notes);
    document.getElementById('st-modal-chicken').classList.remove('open');
    this.render();
    const part = CHICKEN_PARTS.find(p => p.id === partId);
    window.State.toast(`Batch ${qty} potong ${part ? part.label : ''} ditambahkan!`, 'success');
  }

  // ── Events ────────────────────────────────────────────────────────────

  _bindEvents() {
    const bT = document.getElementById('st-subtab-taichan');
    const bC = document.getElementById('st-subtab-chicken');
    if (bT) bT.onclick = () => { this.activeSubTab = 'taichan'; this.render(); };
    if (bC) bC.onclick = () => { this.activeSubTab = 'chicken'; this.render(); };

    const fT = document.getElementById('st-form-taichan');
    if (fT) fT.onsubmit = e => { e.preventDefault(); this.submitTaichanBatch(); };

    const fC = document.getElementById('st-form-chicken');
    if (fC) fC.onsubmit = e => { e.preventDefault(); this.submitChickenBatch(); };
  }
}

// Inject freshness badge styles
(function() {
  const s = document.createElement('style');
  s.textContent = `.st-badge{display:inline-block;padding:3px 9px;border-radius:999px;font-size:11px;font-weight:700;white-space:nowrap}.st-badge.fresh{background:rgba(76,175,80,.15);color:#4caf50}.st-badge.ok{background:rgba(251,191,36,.15);color:#fbbf24}.st-badge.warn{background:rgba(245,158,11,.2);color:#f59e0b}.st-badge.expired{background:rgba(239,68,68,.15);color:#ef4444;border:1px solid rgba(239,68,68,.3)}`;
  document.head.appendChild(s);
})();

window.StockTracker = new StockTrackerView();
