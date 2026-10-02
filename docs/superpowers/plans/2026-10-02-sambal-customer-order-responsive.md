# Dynamic Sambal Variations, Customer Name & Saved Orders, and Mobile Responsive UI Implementation Plan

> **For:** Senior Full-Stack Engineer  
> **Topic:** Dynamic Sambal Management, Customer Name Input, Hold/Saved Orders, and Mobile Responsive Polish  
> **Spec Reference:** `docs/superpowers/specs/2026-10-02-sambal-customer-order-responsive-design.md`  

---

### Task 1: State Management & Schemas for Sambal & Saved Orders
- **Files to modify:** `js/state.js`
- **Actions:**
  - Define `LS_KEYS.sambal = 'ka_sambal'` and `LS_KEYS.saved_orders = 'ka_saved_orders'`.
  - Add `DEFAULT_SAMBAL` initial list (Taichan Original, Sambal Bawang, Sambal Matah Bali, Sambal Ijo Spesial, Sambal Terasi Bakar).
  - Initialize `this.sambalList` and `this.savedOrders`.
  - Add helper methods: `saveOrder(order)`, `removeSavedOrder(id)`, `addOrUpdateSambal(sambal)`, `deleteSambal(id)`.
  - Ensure backward compatibility and event firing.
- **Verification:** Run `node -c js/state.js`.

---

### Task 2: Owner Sambal Variation Management in Inventory View
- **Files to modify:** `index.html`, `js/views/inventory.js`
- **Actions:**
  - Add 3rd subtab `Variasi Sambal & Pelengkap` to `#view-inventory`.
  - Add table container `#inventory-tab-sambal` with `+ Tambah Variasi Sambal` button.
  - Add `#sambal-modal` (Nama Sambal, Tambahan Harga, Status Aktif switch).
  - In `js/views/inventory.js`: handle subtab switching, render sambal list, open modal, save/edit, toggle active status, delete.
- **Verification:** Syntax check `node -c js/views/inventory.js`.

---

### Task 3: Dynamic Sambal Selection in POS Modifier Modal
- **Files to modify:** `index.html`, `js/views/pos.js`
- **Actions:**
  - In `index.html` (`#modifier-modal`): Remove static spicy levels (`Lv.0–Lv.5`).
  - Add container `#modifier-sambal-options` for dynamic sambal choices.
  - In `js/views/pos.js`:
    - On `openModifierModal`, populate `#modifier-sambal-options` with active sambal list from `window.State.sambalList`.
    - Handle sambal selection, update `selectedSambal`, and compute dynamic price addition if `sambal.hr > 0`.
    - Store chosen sambal name in cart item modifier string.
- **Verification:** Syntax check `node -c js/views/pos.js`.

---

### Task 4: Customer Name ("Atas Nama") & Hold/Saved Orders Workflow
- **Files to modify:** `index.html`, `js/views/pos.js`, `js/views/payment.js`
- **Actions:**
  - In `index.html` cart header: Add button `#btn-open-saved-orders` with badge counter.
  - In `index.html` cart body: Add input `#pos-customer-name` (Desktop & Mobile drawer).
  - In `index.html` cart footer: Add button `#btn-hold-cart` (Simpan Order).
  - Add `#saved-orders-modal` to view pending orders, with `Recall ke Keranjang` and `Hapus`.
  - In `js/views/pos.js`:
    - Synchronize customer name between desktop and mobile drawers.
    - Implement `saveCurrentOrder()` (saves cart to `savedOrders`, resets cart).
    - Implement `openSavedOrdersModal()` and `recallSavedOrder(id)` (loads back to active cart).
  - In `js/views/payment.js`:
    - Include `pelanggan: customerName || 'Umum'` in finalized transaction object.
- **Verification:** Syntax check `node -c js/views/pos.js js/views/payment.js`.

---

### Task 5: Print, Reports & Owner Monitoring Integration
- **Files to modify:** `js/printer.js`, `js/views/reports.js`, `index.html`
- **Actions:**
  - In `js/printer.js`: Display `Atas Nama: <pelanggan>` in receipt ESC/POS, virtual receipt HTML, and WhatsApp message.
  - In `index.html` and `js/views/reports.js`:
    - Add `Pelanggan` column in Transaksi Terkini table.
    - Include customer name in CSV export and A4 print report.
    - Show badge for order status (`LUNAS / SELESAI`).
- **Verification:** Syntax check `node -c js/printer.js js/views/reports.js`.

---

### Task 6: Mobile Responsive UI/UX Audit & Polish
- **Files to modify:** `css/design-system.css`, `index.html`
- **Actions:**
  - Audit mobile viewports (< 768px):
    - Subtab horizontal scrolling in Inventory view.
    - Mobile cart drawer height, sticky CTA footer, backdrop tap to dismiss.
    - Modal sizing: `max-height: calc(100vh - 40px)` with internal scroll for body.
    - Responsive KPI cards on smaller mobile widths.
- **Verification:** Full `node -c` syntax check, curl `http://localhost:8000`.
