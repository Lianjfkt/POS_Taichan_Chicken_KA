/**
 * KA POS v3.0 - Payment, Quick Cash & Transaction Finalization
 */

class PaymentView {
  constructor() {
    this.selectedMethod = 'cash'; // 'cash', 'qris', 'transfer'
    this.paidAmount = 0;
    this.currentTotal = 0;
    this.discountAmount = 0;
  }

  init() {
    this.bindEvents();
  }

  bindEvents() {
    // Payment method tabs
    document.querySelectorAll('.btn-pay-method').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('.btn-pay-method').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.selectedMethod = btn.getAttribute('data-method');
        this.togglePaymentSections();
      };
    });

    // Cash Paid Input
    const paidInput = document.getElementById('payment-cash-input');
    if (paidInput) {
      paidInput.oninput = (e) => {
        this.paidAmount = Number(e.target.value.replace(/[^0-9]/g, '')) || 0;
        this.updateCalculations();
      };
    }

    // Quick Cash Chips (Pas, 20k, 50k, 100k, 200k)
    document.querySelectorAll('.quick-cash-chip').forEach(chip => {
      chip.onclick = () => {
        const val = chip.getAttribute('data-val');
        if (val === 'pas') {
          this.paidAmount = this.currentTotal;
        } else {
          this.paidAmount = Number(val);
        }
        if (paidInput) paidInput.value = this.paidAmount;
        this.updateCalculations();
      };
    });

    // Submit Payment Button
    const btnSubmit = document.getElementById('btn-submit-payment');
    if (btnSubmit) {
      btnSubmit.onclick = () => this.processPayment();
    }

    // Success Modal: New Order button
    const btnNewOrder = document.getElementById('btn-success-new-order');
    if (btnNewOrder) {
      btnNewOrder.onclick = () => {
        const modal = document.getElementById('payment-success-modal');
        if (modal) modal.classList.remove('open');
      };
    }
  }

  openPaymentModal() {
    this.currentTotal = window.State.getCartTotal();
    this.paidAmount = this.currentTotal; // Default to exact amount
    this.discountAmount = 0;
    this.selectedMethod = 'cash';

    const modal = document.getElementById('payment-modal');
    const totalEl = document.getElementById('payment-modal-total');
    const paidInput = document.getElementById('payment-cash-input');

    if (totalEl) totalEl.textContent = window.State.formatRp(this.currentTotal);
    if (paidInput) paidInput.value = this.paidAmount;

    // Reset method tabs
    document.querySelectorAll('.btn-pay-method').forEach(b => {
      b.classList.toggle('active', b.getAttribute('data-method') === 'cash');
    });

    this.togglePaymentSections();
    this.updateCalculations();

    if (modal) modal.classList.add('open');
  }

  closePaymentModal() {
    const modal = document.getElementById('payment-modal');
    if (modal) modal.classList.remove('open');
  }

  togglePaymentSections() {
    const cashSection = document.getElementById('pay-section-cash');
    const qrisSection = document.getElementById('pay-section-qris');
    const tfSection   = document.getElementById('pay-section-transfer');

    if (cashSection) cashSection.style.display = this.selectedMethod === 'cash' ? 'block' : 'none';
    if (qrisSection) qrisSection.style.display = this.selectedMethod === 'qris' ? 'block' : 'none';
    if (tfSection)   tfSection.style.display   = this.selectedMethod === 'transfer' ? 'block' : 'none';

    if (this.selectedMethod !== 'cash') {
      this.paidAmount = this.currentTotal;
      this.updateCalculations();
    }
  }

  updateCalculations() {
    const changeEl = document.getElementById('payment-change-amount');
    const btnSubmit = document.getElementById('btn-submit-payment');

    const change = Math.max(0, this.paidAmount - this.currentTotal);

    if (changeEl) {
      changeEl.textContent = window.State.formatRp(change);
    }

    if (btnSubmit) {
      if (this.selectedMethod === 'cash' && this.paidAmount < this.currentTotal) {
        btnSubmit.disabled = true;
        btnSubmit.textContent = 'Uang Tunai Kurang';
      } else {
        btnSubmit.disabled = false;
        btnSubmit.textContent = 'Konfirmasi & Selesaikan Pembayaran';
      }
    }
  }

  processPayment() {
    if (this.selectedMethod === 'cash' && this.paidAmount < this.currentTotal) {
      window.State.toast('Nominal pembayaran kurang dari total tagihan!', 'error');
      return;
    }

    const orderNumber = 'KA-' + String(Date.now()).slice(-6);
    const cashierName = window.State.currentUser ? window.State.currentUser.nm : 'Kasir';
    const orderItems = JSON.parse(JSON.stringify(window.State.cart));
    const change = Math.max(0, this.paidAmount - this.currentTotal);

    const transaction = {
      no: orderNumber,
      tgl: Date.now(),
      items: orderItems,
      subtotal: this.currentTotal,
      diskon: this.discountAmount,
      total: this.currentTotal,
      bayar: this.paidAmount,
      kembali: change,
      metode: this.selectedMethod,
      tipe: window.State.orderType,
      meja: window.State.orderType === 'dine-in' ? window.State.tableNo : '',
      kasir: cashierName
    };

    // 1. Save Transaction to local history
    window.State.transactions.unshift(transaction);
    window.State.save(LS_KEYS.trx, window.State.transactions);

    // 2. Dispatch to Kitchen Display System (KDS)
    const kdsTicket = {
      id: orderNumber,
      meja: transaction.meja || (transaction.tipe === 'dine-in' ? 'Meja' : 'Take Away'),
      tipe: transaction.tipe,
      items: orderItems,
      status: 'menunggu',
      waktu: Date.now()
    };
    window.State.kitchenOrders.unshift(kdsTicket);
    window.State.save(LS_KEYS.dp, window.State.kitchenOrders);
    window.State.emit('kds:new_order', kdsTicket);

    // 3. Deduct Inventory Ingredients
    this.deductInventory(orderItems);

    // 4. Push to Cloud or Queue
    if (window.SupabaseService) {
      window.SupabaseService.queueTransaction(transaction);
    }

    // 5. Update Cash Drawer Log if Cash
    if (this.selectedMethod === 'cash') {
      window.State.cashLog.unshift({
        id: Date.now(),
        tgl: Date.now(),
        tipe: 'masuk',
        kat: 'Penjualan POS',
        jml: this.currentTotal,
        ket: `Nota ${orderNumber}`,
        staf: cashierName
      });
      window.State.save(LS_KEYS.kas, window.State.cashLog);
    }

    // 6. Close payment modal & clear cart
    this.closePaymentModal();
    window.State.clearCart();
    if (window.POSView) {
      window.POSView.closeMobileCartDrawer();
    }

    // 7. Show Success Modal with Print/WA options
    this.showSuccessModal(transaction);
  }

  deductInventory(items) {
    items.forEach(item => {
      // Find matching ingredient by name keywords
      if (item.nm.toLowerCase().includes('taichan') || item.nm.toLowerCase().includes('chicken')) {
        const meat = window.State.inventory.find(i => i.nm.toLowerCase().includes('daging'));
        if (meat && meat.stok > 0) {
          meat.stok = Math.max(0, +(meat.stok - (0.15 * item.qty)).toFixed(2));
        }
      }
      if (item.kat === 'Minuman') {
        const cup = window.State.inventory.find(i => i.nm.toLowerCase().includes('cup'));
        if (cup && cup.stok > 0) {
          cup.stok = Math.max(0, cup.stok - item.qty);
        }
      }
    });
    window.State.save(LS_KEYS.inv, window.State.inventory);
  }

  showSuccessModal(trx) {
    const modal = document.getElementById('payment-success-modal');
    const orderNoEl = document.getElementById('success-order-no');
    const totalEl   = document.getElementById('success-order-total');
    const changeEl  = document.getElementById('success-order-change');

    if (orderNoEl) orderNoEl.textContent = trx.no;
    if (totalEl) totalEl.textContent = window.State.formatRp(trx.total);
    if (changeEl) changeEl.textContent = window.State.formatRp(trx.kembali);

    // Attach actions
    const btnPrint = document.getElementById('btn-success-print');
    const btnWa    = document.getElementById('btn-success-wa');

    if (btnPrint) {
      btnPrint.onclick = () => {
        if (window.PrinterService) window.PrinterService.printReceipt(trx);
      };
    }

    if (btnWa) {
      btnWa.onclick = () => {
        if (window.PrinterService) window.PrinterService.shareWhatsApp(trx);
      };
    }

    if (modal) modal.classList.add('open');
    window.State.toast('Transaksi berhasil diselesaikan!', 'success');
  }
}

window.PaymentView = new PaymentView();
