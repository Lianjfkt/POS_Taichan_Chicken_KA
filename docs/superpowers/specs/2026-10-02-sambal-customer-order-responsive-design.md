# Design Specification: Dynamic Sambal Variations, Customer Name & Saved Orders, and Mobile Responsive UI/UX

**Date:** 2026-10-02  
**Branch:** `main`  
**Status:** Approved by User  

---

## 1. Overview & Objectives
This specification implements three new requested core capabilities into KA POS v3.0, and refines the mobile responsive experience:
1. **Dynamic Sambal Variations (Owner Managed):**
   - Remove static spice levels (Lv.0–Lv.5).
   - Allow the owner to manage a customizable list of Sambal variations (Name, Extra Price, Active status) inside the **Stok & Resep (Inventory)** view under a 3rd subtab.
   - Dynamically display active sambal variations as selectable options inside the POS modifier modal.
2. **Customer Name ("Atas Nama Pelanggan") & Order Lifecycle:**
   - Add a Customer Name input field directly in the POS cart panel (Desktop & Mobile drawer).
   - Introduce **Simpan Order (Hold/Queue)** to save active carts with customer name and timestamp for later inspection or checkout.
   - Provide a **Daftar Order Tersimpan** modal accessible from the cart header to view, recall (restore to cart), or cancel saved orders.
   - Save customer name in final transactions, displayed in receipts, reports, and transaction logs for Admin check and Owner monitoring.
3. **Responsive UI/UX Audit & Fixes:**
   - Fix all potential clipping, overflowing, or misaligned elements on mobile viewports (< 768px).
   - Ensure modals, cart drawer, KPI grids, subtabs, and tables behave smoothly on both desktop and mobile devices.

---

## 2. Architecture & Data Structures

### 2.1 State & LocalStorage Schema
- **New Key:** `ka_sambal` (`LS_KEYS.sambal`)
  ```javascript
  [
    { id: 1, nm: 'Sambal Taichan Original', hr: 0, aktif: true },
    { id: 2, nm: 'Sambal Bawang Gurih', hr: 0, aktif: true },
    { id: 3, nm: 'Sambal Matah Bali', hr: 2000, aktif: true },
    { id: 4, nm: 'Sambal Ijo Spesial', hr: 2000, aktif: true },
    { id: 5, nm: 'Sambal Terasi Bakar', hr: 0, aktif: true }
  ]
  ```
- **New Key:** `ka_saved_orders` (`LS_KEYS.saved_orders`)
  ```javascript
  [
    {
      id: 'HOLD-' + Date.now(),
      pelanggan: 'Mas Doni',
      tgl: Date.now(),
      items: [...],
      subtotal: 45000,
      diskon: 0,
      tipe: 'dine-in',
      staf: 'Kasir Utama'
    }
  ]
  ```
- **Updated Transaction Schema:**
  - `trx.pelanggan`: Customer name string (defaults to `'Umum'` if blank).
  - Included in receipts, CSV exports, A4 print reports, and WhatsApp share messages.

---

## 3. UI/UX Changes

### 3.1 Inventory View (`#view-inventory`)
- Add 3rd Subtab: `Variasi Sambal & Pelengkap`.
- Container `#inventory-tab-sambal`:
  - Header: Subtitle + Button `+ Tambah Variasi Sambal`.
  - Table: No, Nama Sambal, Harga Tambahan (Rp), Status (Badge Aktif/Nonaktif), Aksi (Edit, Toggle Status, Hapus).
- Modal: `#sambal-modal`:
  - Fields: Nama Sambal, Tambahan Harga (Rp), Switch Aktif/Nonaktif.

### 3.2 Product Modifier Modal (`#modifier-modal`)
- Remove old section: `Tingkat Kepedasan (Lv.0–Lv.5)`.
- Add new section: `Pilihan Variasi Sambal`:
  - Dynamically generated from active sambal list (`window.State.sambalList`).
  - Single selection (radio/chips). If sambal has extra price (e.g. `+Rp 2.000`), automatically reflected in badge and total price.
- Keep optional toppings checkboxes and quick note tags.

### 3.3 POS Cart & Customer Name
- In `#pos-cart-panel` (desktop) and `#mobile-cart-drawer` (mobile):
  - Cart Header: Add badge button `Order Tersimpan (N)` displaying saved orders count.
  - Customer input: `<input id="pos-customer-name" class="form-input" placeholder="Atas Nama Pelanggan (misal: Budi)...">`.
  - Action footer:
    - Button `Simpan Order (Hold)`: saves cart without requiring payment.
    - Button `Bayar Sekarang`: opens payment modal.
- Modal: `#saved-orders-modal`:
  - Lists pending saved orders with customer name, time, item summary, total.
  - Buttons: `Buka Kembali (Recall ke Keranjang)`, `Hapus`.

### 3.4 Reports & Monitoring (`#view-reports`)
- Include `Pelanggan` column in Transaksi Terkini.
- Show status badge `LUNAS / SELESAI` and include pending orders summary count for Owner monitoring.
- Receipts, Thermal ESC/POS, WhatsApp, and A4 printouts include `Atas Nama: <Pelanggan>`.

### 3.5 Responsive UI/UX Polish
- Ensure mobile drawer `#mobile-cart-drawer` has proper touch scroll, height limits, and safe-area padding.
- Ensure subtabs in inventory wrap gracefully on small screens.
- Ensure `.data-table` wrappers have `overflow-x: auto` with `-webkit-overflow-scrolling: touch`.
- Ensure modals have `max-height: calc(100vh - 32px)` and sticky header/footer for mobile usability.

---

## 4. Verification & Testing Plan
1. Validate all JS files with `node -c`.
2. Test adding, editing, and deleting a sambal variety in Inventory view.
3. Test opening a Taichan product in POS and verify dynamic sambal chips appear and compute extra price accurately.
4. Test filling customer name, clicking "Simpan Order", verifying it shows in "Order Tersimpan", and recalling it back to cart.
5. Test completing transaction and verifying customer name appears on receipt, reports table, and WhatsApp message.
6. Verify responsive layout at 375px (mobile) and 1280px (desktop).
