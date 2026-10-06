/**
 * KA POS v3.0 - Payment, Quick Cash, Discounts, Split Bill & Finalization
 */

class PaymentView {
  constructor() {
    this.selectedMethod = 'cash'; // 'cash', 'qris', 'transfer'
    this.paidAmount = 0;
    this.currentTotal = 0;
    this.discountAmount = 0;
    this.discountType = 'nominal'; // 'nominal' | 'percent'
    this.discountValue = 0;
    this.selectedCustomerId = null;
    this.pointsDiscount = 0;
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
        const finalTotal = this.getFinalTotal();
        if (val === 'pas') {
          this.paidAmount = finalTotal;
        } else {
          this.paidAmount = Number(val);
        }
        if (paidInput) paidInput.value = this.paidAmount;
        this.updateCalculations();
      };
    });

    // Discount Type Switcher (Nominal vs Percent)
    document.querySelectorAll('.btn-discount-type').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('.btn-discount-type').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.discountType = btn.getAttribute('data-type');
        this.applyDiscount();
      };
    });

    // Discount Input
    const discInput = document.getElementById('payment-discount-input');
    if (discInput) {
      discInput.oninput = (e) => {
        this.discountValue = Number(e.target.value.replace(/[^0-9]/g, '')) || 0;
        this.applyDiscount();
      };
    }

    // Quick Discount Chips
    document.querySelectorAll('.quick-discount-chip').forEach(chip => {
      chip.onclick = () => {
        const type = chip.getAttribute('data-type');
        const val = Number(chip.getAttribute('data-val')) || 0;
        this.discountType = type;
        this.discountValue = val;

        document.querySelectorAll('.btn-discount-type').forEach(b => {
          b.classList.toggle('active', b.getAttribute('data-type') === type);
        });

        if (discInput) discInput.value = val;
        this.applyDiscount();
      };
    });

    // Reset Discount Button
    const btnResetDisc = document.getElementById('btn-reset-discount');
    if (btnResetDisc) {
      btnResetDisc.onclick = () => {
        this.discountValue = 0;
        this.discountAmount = 0;
        this.pointsDiscount = 0;
        if (discInput) discInput.value = '';
        this.applyDiscount();
      };
    }

    // Customer Select dropdown in Payment Modal
    const custSelect = document.getElementById('payment-customer-select');
    if (custSelect) {
      custSelect.onchange = (e) => {
        this.selectedCustomerId = e.target.value;
        this.updateCustomerLoyaltyBadge();
      };
    }

    // Redeem Points Button
    const btnRedeem = document.getElementById('btn-redeem-points');
    if (btnRedeem) {
      btnRedeem.onclick = () => this.redeemLoyaltyPoints();
    }

    // Split Bill Button
    const btnSplit = document.getElementById('btn-open-split-bill');
    if (btnSplit) {
      btnSplit.onclick = () => this.openSplitBillModal();
    }

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

  getFinalTotal() {
    const raw = this.currentTotal - this.discountAmount - this.pointsDiscount;
    return Math.max(0, raw);
  }

  applyDiscount() {
    if (this.discountType === 'percent') {
      const pct = Math.min(100, Math.max(0, this.discountValue));
      this.discountAmount = Math.round((this.currentTotal * pct) / 100);
    } else {
      this.discountAmount = Math.min(this.currentTotal, Math.max(0, this.discountValue));
    }

    const totalEl = document.getElementById('payment-modal-total');
    const discDisplayEl = document.getElementById('payment-discount-display');
    const finalTotal = this.getFinalTotal();

    if (totalEl) totalEl.textContent = window.State.formatRp(finalTotal);
    if (discDisplayEl) {
      const totalDisc = this.discountAmount + this.pointsDiscount;
      discDisplayEl.textContent = totalDisc > 0 ? `-${window.State.formatRp(totalDisc)}` : 'Rp 0';
      discDisplayEl.parentElement.style.display = totalDisc > 0 ? 'flex' : 'none';
    }

    // Update paid amount default if cash exact
    if (this.selectedMethod !== 'cash') {
      this.paidAmount = finalTotal;
    }
    this.updateCalculations();
  }

  openPaymentModal() {
    this.currentTotal = window.State.getCartTotal();
    this.discountAmount = 0;
    this.discountValue = 0;
    this.pointsDiscount = 0;
    this.discountType = 'nominal';
    this.selectedMethod = 'cash';

    const discInput = document.getElementById('payment-discount-input');
    if (discInput) discInput.value = '';

    // Populate Customer selector
    this.populateCustomerSelector();

    const finalTotal = this.getFinalTotal();
    this.paidAmount = finalTotal;

    const modal = document.getElementById('payment-modal');
    const totalEl = document.getElementById('payment-modal-total');
    const paidInput = document.getElementById('payment-cash-input');

    if (totalEl) totalEl.textContent = window.State.formatRp(finalTotal);
    if (paidInput) paidInput.value = this.paidAmount;

    // Reset method tabs
    document.querySelectorAll('.btn-pay-method').forEach(b => {
      b.classList.toggle('active', b.getAttribute('data-method') === 'cash');
    });

    this.togglePaymentSections();
    this.applyDiscount();

    if (modal) modal.classList.add('open');
  }

  populateCustomerSelector() {
    const custSelect = document.getElementById('payment-customer-select');
    if (!custSelect) return;

    const custs = window.State.customers || [];
    const currentName = (window.State.currentCustomerName || '').trim().toLowerCase();
    let selectedId = '';

    let html = `<option value="">Pelanggan Umum (Non-Member)</option>`;
    custs.forEach(c => {
      const isMatch = currentName && c.nm.toLowerCase() === currentName;
      if (isMatch) selectedId = c.id;
      html += `<option value="${c.id}" ${isMatch ? 'selected' : ''}>${c.nm} (${c.poin || 0} Poin)</option>`;
    });

    custSelect.innerHTML = html;
    this.selectedCustomerId = selectedId;
    this.updateCustomerLoyaltyBadge();
  }

  updateCustomerLoyaltyBadge() {
    const badgeEl = document.getElementById('payment-cust-loyalty-badge');
    const btnRedeem = document.getElementById('btn-redeem-points');
    if (!badgeEl) return;

    const cust = (window.State.customers || []).find(c => c.id === this.selectedCustomerId);
    if (cust && cust.poin > 0) {
      badgeEl.textContent = `Poin: ${cust.poin} (Dapat ditukar diskon)`;
      badgeEl.style.display = 'inline-block';
      if (btnRedeem) btnRedeem.style.display = 'inline-flex';
    } else {
      badgeEl.style.display = 'none';
      if (btnRedeem) btnRedeem.style.display = 'none';
    }
  }

  redeemLoyaltyPoints() {
    const cust = (window.State.customers || []).find(c => c.id === this.selectedCustomerId);
    if (!cust || !cust.poin || cust.poin < 10) {
      window.State.toast('Minimal 10 poin untuk ditukar diskon (10 poin = Rp 5.000)', 'warning');
      return;
    }

    // 10 poin = Rp 5.000 discount
    const redeemableSteps = Math.floor(cust.poin / 10);
    const disc = redeemableSteps * 5000;
    this.pointsDiscount = disc;

    window.State.toast(`Menukarkan ${redeemableSteps * 10} poin dengan Diskon ${window.State.formatRp(disc)}!`, 'success');
    this.applyDiscount();
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
      this.paidAmount = this.getFinalTotal();
      this.updateCalculations();
    }
  }

  updateCalculations() {
    const changeEl = document.getElementById('payment-change-amount');
    const btnSubmit = document.getElementById('btn-submit-payment');

    const finalTotal = this.getFinalTotal();
    const change = Math.max(0, this.paidAmount - finalTotal);

    if (changeEl) {
      changeEl.textContent = window.State.formatRp(change);
    }

    if (btnSubmit) {
      if (this.selectedMethod === 'cash' && this.paidAmount < finalTotal) {
        btnSubmit.disabled = true;
        btnSubmit.textContent = 'Uang Tunai Kurang';
      } else {
        btnSubmit.disabled = false;
        btnSubmit.textContent = 'Konfirmasi & Selesaikan Pembayaran';
      }
    }
  }

  // --- Split Bill Modal ---
  openSplitBillModal() {
    const modal = document.getElementById('split-bill-modal');
    const finalTotal = this.getFinalTotal();
    const countInput = document.getElementById('split-person-count');
    const resultEl = document.getElementById('split-per-person-amount');
    const totalEl = document.getElementById('split-bill-total');

    if (totalEl) totalEl.textContent = window.State.formatRp(finalTotal);

    const updateSplit = () => {
      const n = Math.max(1, Number(countInput?.value) || 2);
      const perPerson = Math.ceil(finalTotal / n);
      if (resultEl) resultEl.textContent = window.State.formatRp(perPerson);
    };

    if (countInput) {
      countInput.value = 2;
      countInput.oninput = updateSplit;
    }
    updateSplit();

    if (modal) modal.classList.add('open');
  }

  processPayment() {
    const finalTotal = this.getFinalTotal();

    if (this.selectedMethod === 'cash' && this.paidAmount < finalTotal) {
      window.State.toast('Nominal pembayaran kurang dari total tagihan!', 'error');
      return;
    }

    const dateStr = window.State.formatDateShort(Date.now()).replace(/-/g, '');
    const counter = String((window.State.transactions.length || 0) + 1).padStart(4, '0');
    const orderNumber = `KA-${dateStr}-${counter}`;
    const cashierName = window.State.currentUser ? window.State.currentUser.nm : 'Kasir';
    const orderItems = JSON.parse(JSON.stringify(window.State.cart));
    const change = Math.max(0, this.paidAmount - finalTotal);
    const totalDiscount = this.discountAmount + this.pointsDiscount;

    let customerName = window.State.currentCustomerName || 'Umum';
    if (this.selectedCustomerId) {
      const c = (window.State.customers || []).find(item => item.id === this.selectedCustomerId);
      if (c) customerName = c.nm;
    }

    const transaction = {
      no: orderNumber,
      tgl: Date.now(),
      items: orderItems,
      subtotal: this.currentTotal,
      diskon: totalDiscount,
      total: finalTotal,
      bayar: this.paidAmount,
      kembali: change,
      metode: this.selectedMethod,
      tipe: window.State.orderType,
      pelanggan: customerName,
      kasir: cashierName
    };

    // 1. Save Transaction to local history
    window.State.transactions.unshift(transaction);
    window.State.save(LS_KEYS.trx, window.State.transactions);

    // 2. Deduct Inventory Ingredients
    this.deductInventory(orderItems);

    // 3. Update Member Loyalty Points (earn & spend)
    if (this.selectedCustomerId) {
      if (this.pointsDiscount > 0) {
        // deduct spent points
        const cust = (window.State.customers || []).find(c => c.id === this.selectedCustomerId);
        if (cust) {
          const usedPoints = (this.pointsDiscount / 5000) * 10;
          cust.poin = Math.max(0, (cust.poin || 0) - usedPoints);
        }
      }
      const earned = window.State.addCustomerLoyalty(this.selectedCustomerId, finalTotal);
      if (earned > 0) {
        window.State.toast(`Member +${earned} Poin Loyalty didapatkan!`, 'success');
      }
    }

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
        jml: finalTotal,
        ket: `Nota ${orderNumber}`,
        staf: cashierName
      });
      window.State.save(LS_KEYS.kas, window.State.cashLog);
    }

    // 6. Log Audit Trail
    window.State.logAudit('TRANSACTION_COMPLETED', {
      orderNo: transaction.no,
      total: finalTotal,
      diskon: totalDiscount,
      metode: this.selectedMethod,
      itemsCount: orderItems.length
    });

    // 7. Close payment modal & clear cart
    this.closePaymentModal();
    window.State.clearCart();
    if (window.POSView) {
      window.POSView.closeMobileCartDrawer();
      window.POSView.updateHeaderMetrics();
    }

    // 8. Show Success Modal with Print/WA/QR options
    this.showSuccessModal(transaction);
  }

  deductInventory(items) {
    let anyDeducted = false;
    items.forEach(cartItem => {
      const product = window.State.products.find(p => p.id === cartItem.id);
      if (product && product.bom && product.bom.length > 0) {
        product.bom.forEach(b => {
          const invItem = window.State.inventory.find(i => i.id === b.invId);
          if (invItem) {
            const deductQty = +(b.qty * (cartItem.qty || 1)).toFixed(3);
            invItem.stok = Math.max(0, +(invItem.stok - deductQty).toFixed(3));
            anyDeducted = true;

            // Log mutation
            window.State.stockMutations.unshift({
              id: Date.now() + Math.random(),
              tgl: Date.now(),
              invId: invItem.id,
              nm: invItem.nm,
              tipe: 'keluar',
              jml: deductQty,
              sat: invItem.sat,
              ket: `POS: ${cartItem.qty}x ${cartItem.nm}`,
              staf: window.State.currentUser ? window.State.currentUser.nm : 'Kasir'
            });
          }
        });
      } else {
        // Fallback deduction
        if (cartItem.nm.toLowerCase().includes('taichan') || cartItem.nm.toLowerCase().includes('chicken')) {
          const meat = window.State.inventory.find(i => i.nm.toLowerCase().includes('daging'));
          if (meat && meat.stok > 0) {
            meat.stok = Math.max(0, +(meat.stok - (0.15 * cartItem.qty)).toFixed(2));
            anyDeducted = true;
          }
        }
        if (cartItem.kat === 'Minuman') {
          const cup = window.State.inventory.find(i => i.nm.toLowerCase().includes('cup'));
          if (cup && cup.stok > 0) {
            cup.stok = Math.max(0, cup.stok - cartItem.qty);
            anyDeducted = true;
          }
        }
      }
    });

    if (anyDeducted) {
      window.State.save(LS_KEYS.inv, window.State.inventory);
      window.State.save(LS_KEYS.mut, window.State.stockMutations);
      window.State.checkLowStockAlerts();
    }
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
    const btnPdf   = document.getElementById('btn-success-pdf');

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

    if (btnPdf) {
      btnPdf.onclick = () => {
        if (window.PrinterService) window.PrinterService.openReceiptModal(trx);
      };
    }

    // Haptic feedback
    if (navigator.vibrate) {
      navigator.vibrate([40, 60, 40]);
    }

    if (modal) modal.classList.add('open');
    window.State.toast('Transaksi berhasil diselesaikan!', 'success');
  }
}

window.PaymentView = new PaymentView();
