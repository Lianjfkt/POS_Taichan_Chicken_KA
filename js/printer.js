/**
 * KA POS v3.0 - Web Bluetooth ESC/POS Thermal Printer & Receipt Generator
 */

class PrinterService {
  constructor() {
    this.device = null;
    this.characteristic = null;
    this.isConnected = false;
  }

  async connectBluetooth() {
    if (!navigator.bluetooth) {
      window.State.toast('Browser ini tidak mendukung Web Bluetooth API', 'error');
      return false;
    }

    try {
      window.State.toast('Mencari printer Bluetooth...', 'warning');
      this.device = await navigator.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: [
          '000018f0-0000-1000-8000-00805f9b34fb',
          'e7810a71-73ae-499d-8c15-faa9aef0c3f2',
          '49535343-fe7d-4ae5-8fa9-9fafd205e455'
        ]
      });

      const server = await this.device.gatt.connect();
      const services = await server.getPrimaryServices();

      for (const service of services) {
        const characteristics = await service.getCharacteristics();
        for (const char of characteristics) {
          if (char.properties.write || char.properties.writeWithoutResponse) {
            this.characteristic = char;
            this.isConnected = true;
            window.State.toast(`Terhubung ke printer: ${this.device.name || 'Thermal ESC/POS'}`, 'success');
            return true;
          }
        }
      }

      throw new Error('Karakteristik write printer tidak ditemukan');
    } catch (err) {
      console.warn('[Printer] Bluetooth connection error:', err);
      this.isConnected = false;
      window.State.toast(`Gagal konek printer: ${err.message || 'Dibatalkan'}`, 'error');
      return false;
    }
  }

  // Generate ESC/POS byte sequence for regular receipt
  buildEscPosCommands(trx) {
    const s = window.State.settings;
    const encoder = new TextEncoder();
    const parts = [];

    // Init & Center
    parts.push(new Uint8Array([0x1B, 0x40])); // ESC @ Initialize
    parts.push(new Uint8Array([0x1B, 0x61, 0x01])); // ESC a 1 (Center)

    // Store Name & Header
    parts.push(new Uint8Array([0x1B, 0x21, 0x20])); // Double height & width
    parts.push(encoder.encode((s.nm || 'KA POS') + '\n'));
    parts.push(new Uint8Array([0x1B, 0x21, 0x00])); // Reset font
    parts.push(encoder.encode((s.addr || '') + '\n'));
    if (s.hp) parts.push(encoder.encode('Telp: ' + s.hp + '\n'));
    parts.push(encoder.encode('--------------------------------\n'));

    // Left Align Transaction Info
    parts.push(new Uint8Array([0x1B, 0x61, 0x00])); // Align left
    parts.push(encoder.encode(`No. Nota : ${trx.no}\n`));
    parts.push(encoder.encode(`Tanggal  : ${window.State.formatDate(trx.tgl)}\n`));
    parts.push(encoder.encode(`Kasir    : ${trx.kasir || 'Kasir'}\n`));
    parts.push(encoder.encode(`Tipe     : ${trx.tipe || 'Dine-In'}\n`));
    parts.push(encoder.encode('--------------------------------\n'));

    // Items List (32 chars line width for 58mm)
    (trx.items || []).forEach(item => {
      const name = item.nm.substring(0, 20);
      const priceStr = window.State.formatRp(item.hr * item.qty).replace('Rp ', '');
      const spacing = 32 - name.length - priceStr.length;
      parts.push(encoder.encode(name + ' '.repeat(Math.max(1, spacing)) + priceStr + '\n'));
      if (item.mod) {
        parts.push(encoder.encode(`  * ${item.mod}\n`));
      }
    });

    parts.push(encoder.encode('--------------------------------\n'));

    // Totals
    const addLine = (label, val) => {
      const v = window.State.formatRp(val);
      const sp = 32 - label.length - v.length;
      return label + ' '.repeat(Math.max(1, sp)) + v + '\n';
    };

    parts.push(encoder.encode(addLine('Subtotal', trx.subtotal || trx.total)));
    if (trx.diskon) parts.push(encoder.encode(addLine('Diskon', -trx.diskon)));
    parts.push(new Uint8Array([0x1B, 0x45, 0x01])); // ESC E 1 (Bold)
    parts.push(encoder.encode(addLine('TOTAL', trx.total)));
    parts.push(new Uint8Array([0x1B, 0x45, 0x00])); // ESC E 0 (Normal)
    parts.push(encoder.encode(addLine('Bayar (' + (trx.metode || 'cash').toUpperCase() + ')', trx.bayar || trx.total)));
    if (trx.kembali > 0) parts.push(encoder.encode(addLine('Kembali', trx.kembali)));

    // Footer
    parts.push(new Uint8Array([0x1B, 0x61, 0x01])); // Center
    parts.push(encoder.encode('--------------------------------\n'));
    parts.push(encoder.encode((s.footer || 'Terima kasih atas kunjungannya!') + '\n\n\n\n'));

    // Paper Cut
    parts.push(new Uint8Array([0x1D, 0x56, 0x41, 0x10]));

    // Merge buffers
    const totalLen = parts.reduce((acc, p) => acc + p.length, 0);
    const merged = new Uint8Array(totalLen);
    let offset = 0;
    for (const p of parts) {
      merged.set(p, offset);
      offset += p.length;
    }
    return merged;
  }

  // Generate ESC/POS byte sequence for Shift Report (X or Z)
  buildShiftEscPosCommands(report, type = 'X') {
    const s = window.State.settings;
    const encoder = new TextEncoder();
    const parts = [];

    parts.push(new Uint8Array([0x1B, 0x40])); // Init
    parts.push(new Uint8Array([0x1B, 0x61, 0x01])); // Center

    parts.push(new Uint8Array([0x1B, 0x21, 0x20])); // Large
    parts.push(encoder.encode((s.nm || 'KA POS') + '\n'));
    parts.push(new Uint8Array([0x1B, 0x21, 0x00])); // Normal
    parts.push(encoder.encode(type === 'X' ? '*** LAPORAN X (MID-SHIFT) ***\n' : '*** LAPORAN Z (TUTUP SHIFT) ***\n'));
    parts.push(encoder.encode('--------------------------------\n'));

    parts.push(new Uint8Array([0x1B, 0x61, 0x00])); // Left
    parts.push(encoder.encode(`Shift ID : ${report.shiftId || '-'}\n`));
    parts.push(encoder.encode(`Kasir    : ${report.kasir || 'Kasir'}\n`));
    parts.push(encoder.encode(`Mulai    : ${window.State.formatDate(report.mulai)}\n`));
    if (report.selesai) {
      parts.push(encoder.encode(`Selesai  : ${window.State.formatDate(report.selesai)}\n`));
    }
    parts.push(encoder.encode('--------------------------------\n'));

    const addLine = (label, val) => {
      const v = typeof val === 'number' ? window.State.formatRp(val) : String(val);
      const sp = 32 - label.length - v.length;
      return label + ' '.repeat(Math.max(1, sp)) + v + '\n';
    };

    parts.push(encoder.encode(addLine('Modal Awal', report.modalAwal || 0)));
    parts.push(encoder.encode(addLine('Omzet Tunai', report.omzetTunai || 0)));
    parts.push(encoder.encode(addLine('Omzet Non-Tunai', report.omzetNonTunai || 0)));
    parts.push(encoder.encode(addLine('Kas Masuk (Lain)', report.totalMasuk || 0)));
    parts.push(encoder.encode(addLine('Kas Keluar (Petty)', report.totalKeluar || 0)));
    parts.push(encoder.encode('--------------------------------\n'));

    parts.push(new Uint8Array([0x1B, 0x45, 0x01])); // Bold
    parts.push(encoder.encode(addLine('SALDO SISTEM', report.saldoSistem || 0)));
    if (type === 'Z') {
      parts.push(encoder.encode(addLine('UANG FISIK', report.uangFisik || 0)));
      parts.push(encoder.encode(addLine('SELISIH (' + (report.status || 'PAS') + ')', report.selisih || 0)));
    }
    parts.push(new Uint8Array([0x1B, 0x45, 0x00])); // Normal
    parts.push(encoder.encode('--------------------------------\n'));

    parts.push(new Uint8Array([0x1B, 0x61, 0x01])); // Center
    parts.push(encoder.encode('Dicetak: ' + window.State.formatDate(Date.now()) + '\n\n\n\n'));
    parts.push(new Uint8Array([0x1D, 0x56, 0x41, 0x10])); // Cut

    const totalLen = parts.reduce((acc, p) => acc + p.length, 0);
    const merged = new Uint8Array(totalLen);
    let offset = 0;
    for (const p of parts) {
      merged.set(p, offset);
      offset += p.length;
    }
    return merged;
  }

  async printReceipt(trx) {
    if (!this.isConnected || !this.characteristic) {
      const connected = await this.connectBluetooth();
      if (!connected) {
        this.openReceiptModal(trx);
        return false;
      }
    }

    try {
      window.State.toast('Mencetak struk ke printer...', 'warning');
      const bytes = this.buildEscPosCommands(trx);
      
      const chunkSize = 100;
      for (let i = 0; i < bytes.length; i += chunkSize) {
        const chunk = bytes.slice(i, i + chunkSize);
        await this.characteristic.writeValue(chunk);
      }

      window.State.toast('Struk berhasil dicetak!', 'success');
      return true;
    } catch (e) {
      console.error('[Printer] Print execution failed:', e);
      window.State.toast('Gagal mencetak struk. Buka pratinjau virtual.', 'error');
      this.openReceiptModal(trx);
      return false;
    }
  }

  async printShiftReport(report, type = 'X') {
    if (!this.isConnected || !this.characteristic) {
      const connected = await this.connectBluetooth();
      if (!connected) {
        this.openShiftReportModal(report, type);
        return false;
      }
    }

    try {
      window.State.toast(`Mencetak Laporan ${type}-Report ke printer...`, 'warning');
      const bytes = this.buildShiftEscPosCommands(report, type);
      
      const chunkSize = 100;
      for (let i = 0; i < bytes.length; i += chunkSize) {
        const chunk = bytes.slice(i, i + chunkSize);
        await this.characteristic.writeValue(chunk);
      }

      window.State.toast(`Laporan ${type}-Report berhasil dicetak!`, 'success');
      return true;
    } catch (e) {
      console.error('[Printer] Shift print failed:', e);
      window.State.toast(`Buka pratinjau virtual Laporan ${type}-Report.`, 'warning');
      this.openShiftReportModal(report, type);
      return false;
    }
  }

  // Virtual Receipt HTML Generator
  generateReceiptHTML(trx) {
    const s = window.State.settings;
    return `
      <div style="font-family:'Space Mono',monospace;font-size:12px;color:#111;background:#fff;padding:24px;border-radius:12px;max-width:340px;margin:0 auto;box-shadow:0 8px 30px rgba(0,0,0,0.5);line-height:1.4">
        <div style="text-align:center;border-bottom:1px dashed #999;padding-bottom:12px;margin-bottom:12px">
          <div style="font-size:16px;font-weight:700;letter-spacing:0.05em">${s.nm || 'KA POS'}</div>
          <div style="font-size:10px;color:#555">${s.addr || ''}</div>
          <div style="font-size:10px;color:#555">Telp: ${s.hp || '-'}</div>
        </div>
        <div style="font-size:11px;margin-bottom:8px">
          <div>Nota  : <strong>${trx.no}</strong></div>
          <div>Waktu : ${window.State.formatDate(trx.tgl)}</div>
          <div>Kasir : ${trx.kasir || 'Kasir'}</div>
          <div>Tipe  : ${trx.tipe || 'Dine-In'}</div>
        </div>
        <div style="border-top:1px dashed #999;border-bottom:1px dashed #999;padding:8px 0;margin-bottom:10px">
          ${(trx.items || []).map(i => `
            <div style="display:flex;justify-content:space-between;margin-bottom:4px">
              <div>
                <div>${i.nm} x${i.qty}</div>
                ${i.mod ? `<div style="font-size:10px;color:#f97316">* ${i.mod}</div>` : ''}
              </div>
              <div style="font-weight:700">${window.State.formatRp(i.hr * i.qty)}</div>
            </div>
          `).join('')}
        </div>
        <div style="display:flex;flex-direction:column;gap:3px;margin-bottom:12px">
          <div style="display:flex;justify-content:space-between">
            <span>Subtotal</span>
            <span>${window.State.formatRp(trx.subtotal || trx.total)}</span>
          </div>
          ${trx.diskon ? `
            <div style="display:flex;justify-content:space-between;color:#dc2626">
              <span>Diskon</span>
              <span>-${window.State.formatRp(trx.diskon)}</span>
            </div>
          ` : ''}
          <div style="display:flex;justify-content:space-between;font-size:14px;font-weight:800;border-top:1px solid #111;padding-top:4px;margin-top:2px">
            <span>TOTAL</span>
            <span>${window.State.formatRp(trx.total)}</span>
          </div>
          <div style="display:flex;justify-content:space-between;font-size:11px;color:#555">
            <span>Metode Bayar</span>
            <span style="text-transform:uppercase">${trx.metode || 'Cash'}</span>
          </div>
          <div style="display:flex;justify-content:space-between;font-size:11px;color:#555">
            <span>Diterima</span>
            <span>${window.State.formatRp(trx.bayar || trx.total)}</span>
          </div>
          ${trx.kembali > 0 ? `
            <div style="display:flex;justify-content:space-between;font-size:12px;font-weight:700">
              <span>Kembali</span>
              <span>${window.State.formatRp(trx.kembali)}</span>
            </div>
          ` : ''}
        </div>
        <div style="text-align:center;font-size:10px;color:#555;border-top:1px dashed #999;padding-top:10px">
          ${s.footer || 'Terima kasih atas kunjungannya!'}
        </div>
      </div>
    `;
  }

  // Virtual Shift Report HTML
  generateShiftReportHTML(report, type = 'X') {
    const s = window.State.settings;
    const isZ = type === 'Z';
    return `
      <div style="font-family:'Space Mono',monospace;font-size:12px;color:#111;background:#fff;padding:24px;border-radius:12px;max-width:340px;margin:0 auto;box-shadow:0 8px 30px rgba(0,0,0,0.5);line-height:1.4">
        <div style="text-align:center;border-bottom:1px dashed #999;padding-bottom:12px;margin-bottom:12px">
          <div style="font-size:16px;font-weight:700;">${s.nm || 'KA POS'}</div>
          <div style="font-size:10px;color:#555">${s.addr || ''}</div>
          <div style="font-size:13px;font-weight:800;margin-top:6px;letter-spacing:0.05em;color:${isZ ? '#dc2626' : '#2563eb'}">
            ${isZ ? 'LAPORAN Z (TUTUP SHIFT)' : 'LAPORAN X (MID-SHIFT)'}
          </div>
        </div>

        <div style="font-size:11px;margin-bottom:10px;line-height:1.5;">
          <div>Shift ID : <strong>${report.shiftId || '-'}</strong></div>
          <div>Kasir    : <strong>${report.kasir || 'Kasir'}</strong></div>
          <div>Mulai    : ${window.State.formatDate(report.mulai)}</div>
          ${report.selesai ? `<div>Selesai  : ${window.State.formatDate(report.selesai)}</div>` : ''}
        </div>

        <div style="border-top:1px dashed #999;border-bottom:1px dashed #999;padding:8px 0;margin-bottom:10px;display:flex;flex-direction:column;gap:4px;">
          <div style="display:flex;justify-content:space-between">
            <span>Modal Awal Kas</span>
            <span style="font-weight:700">${window.State.formatRp(report.modalAwal || 0)}</span>
          </div>
          <div style="display:flex;justify-content:space-between">
            <span>Omzet Tunai</span>
            <span>+${window.State.formatRp(report.omzetTunai || 0)}</span>
          </div>
          <div style="display:flex;justify-content:space-between">
            <span>Omzet Non-Tunai</span>
            <span>${window.State.formatRp(report.omzetNonTunai || 0)}</span>
          </div>
          <div style="display:flex;justify-content:space-between">
            <span>Kas Masuk Operasional</span>
            <span>+${window.State.formatRp(report.totalMasuk || 0)}</span>
          </div>
          <div style="display:flex;justify-content:space-between;color:#dc2626">
            <span>Kas Keluar (Petty Cash)</span>
            <span>-${window.State.formatRp(report.totalKeluar || 0)}</span>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:4px;margin-bottom:12px;">
          <div style="display:flex;justify-content:space-between;font-size:13px;font-weight:800;border-bottom:1px solid #111;padding-bottom:4px;">
            <span>SALDO SISTEM</span>
            <span>${window.State.formatRp(report.saldoSistem || 0)}</span>
          </div>
          ${isZ ? `
            <div style="display:flex;justify-content:space-between;font-size:13px;font-weight:700;">
              <span>UANG FISIK</span>
              <span>${window.State.formatRp(report.uangFisik || 0)}</span>
            </div>
            <div style="display:flex;justify-content:space-between;font-size:14px;font-weight:800;color:${report.selisih === 0 ? '#059669' : (report.selisih > 0 ? '#2563eb' : '#dc2626')}">
              <span>SELISIH (${report.status || 'PAS'})</span>
              <span>${report.selisih >= 0 ? '+' : ''}${window.State.formatRp(report.selisih || 0)}</span>
            </div>
          ` : ''}
        </div>

        <div style="text-align:center;font-size:10px;color:#555;border-top:1px dashed #999;padding-top:10px;">
          Dicetak pada ${window.State.formatDate(Date.now())}
        </div>
      </div>
    `;
  }

  openReceiptModal(trx) {
    const modal = document.getElementById('receipt-preview-modal');
    const container = document.getElementById('receipt-preview-content');
    if (!modal || !container) return;

    container.innerHTML = this.generateReceiptHTML(trx);
    modal.classList.add('open');

    // Attach actions
    const btnPrint = document.getElementById('btn-receipt-modal-print');
    const btnWa = document.getElementById('btn-receipt-modal-wa');
    
    if (btnPrint) {
      btnPrint.onclick = () => this.printReceipt(trx);
    }
    if (btnWa) {
      btnWa.onclick = () => this.shareWhatsApp(trx);
    }
  }

  openShiftReportModal(report, type = 'X') {
    const modal = document.getElementById('receipt-preview-modal');
    const container = document.getElementById('receipt-preview-content');
    if (!modal || !container) return;

    container.innerHTML = this.generateShiftReportHTML(report, type);
    modal.classList.add('open');

    const btnPrint = document.getElementById('btn-receipt-modal-print');
    const btnWa = document.getElementById('btn-receipt-modal-wa');
    
    if (btnPrint) {
      btnPrint.onclick = () => this.printShiftReport(report, type);
    }
    if (btnWa) {
      btnWa.onclick = () => this.shareShiftWhatsApp(report, type);
    }
  }

  shareWhatsApp(trx) {
    const s = window.State.settings;
    let text = `*STRUK PEMBAYARAN ${s.nm || 'KA POS'}*\n`;
    text += `No. Nota: ${trx.no}\n`;
    text += `Waktu   : ${window.State.formatDate(trx.tgl)}\n`;
    text += `Kasir   : ${trx.kasir || 'Kasir'}\n`;
    text += `Tipe    : ${trx.tipe || 'Dine-In'}\n`;
    text += `--------------------------------\n`;

    (trx.items || []).forEach(i => {
      text += `${i.nm} x${i.qty} = ${window.State.formatRp(i.hr * i.qty)}\n`;
      if (i.mod) text += `  _${i.mod}_\n`;
    });

    text += `--------------------------------\n`;
    text += `*TOTAL   : ${window.State.formatRp(trx.total)}*\n`;
    text += `Bayar   : ${window.State.formatRp(trx.bayar || trx.total)} (${(trx.metode || 'cash').toUpperCase()})\n`;
    if (trx.kembali > 0) text += `Kembali : ${window.State.formatRp(trx.kembali)}\n`;
    text += `--------------------------------\n`;
    text += `${s.footer || 'Terima kasih!'}\n`;

    const encoded = encodeURI(text);
    window.open(`https://api.whatsapp.com/send?text=${encoded}`, '_blank');
  }

  shareShiftWhatsApp(report, type = 'Z') {
    const s = window.State.settings;
    const isZ = type === 'Z';
    let text = `*${isZ ? 'LAPORAN TUTUP SHIFT (Z-REPORT)' : 'LAPORAN SEMENTARA (X-REPORT)'}*\n`;
    text += `Outlet   : *${s.nm || 'KA POS'}*\n`;
    text += `Kasir    : ${report.kasir || 'Kasir'}\n`;
    text += `Mulai    : ${window.State.formatDate(report.mulai)}\n`;
    if (report.selesai) {
      text += `Selesai  : ${window.State.formatDate(report.selesai)}\n`;
    }
    text += `--------------------------------\n`;
    text += `Modal Awal       : ${window.State.formatRp(report.modalAwal || 0)}\n`;
    text += `Omzet Tunai      : ${window.State.formatRp(report.omzetTunai || 0)}\n`;
    text += `Omzet Non-Tunai  : ${window.State.formatRp(report.omzetNonTunai || 0)}\n`;
    text += `Kas Masuk (Lain) : ${window.State.formatRp(report.totalMasuk || 0)}\n`;
    text += `Kas Keluar       : -${window.State.formatRp(report.totalKeluar || 0)}\n`;
    text += `--------------------------------\n`;
    text += `*SALDO SISTEM    : ${window.State.formatRp(report.saldoSistem || 0)}*\n`;
    if (isZ) {
      text += `*UANG FISIK      : ${window.State.formatRp(report.uangFisik || 0)}*\n`;
      text += `*SELISIH KAS     : ${report.selisih >= 0 ? '+' : ''}${window.State.formatRp(report.selisih || 0)} [${report.status || 'PAS'}]*\n`;
    }
    text += `--------------------------------\n`;
    text += `Dicetak via KA POS v3.0`;

    const encoded = encodeURI(text);
    window.open(`https://api.whatsapp.com/send?text=${encoded}`, '_blank');
  }
}

window.PrinterService = new PrinterService();
