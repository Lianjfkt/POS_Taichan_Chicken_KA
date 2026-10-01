# KA POS v3.0 Streamlined (No Dapur, No Meja) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Dapur (KDS) and Meja (Tables) features completely, and implement all remaining PRD_UIUX.md features (Modifiers & Toppings with dynamic price calculation, Recipe BOM auto-deduction, Blind Cash Drop, and Owner Financial Analytics).

**Architecture:** Vanilla HTML5/CSS/JavaScript PWA, responsive 3-Zone Velocity layout, Stitch Dark Theme design tokens, LocalStorage state with Supabase cloud synchronization.

**Tech Stack:** HTML5, CSS3 (Vanilla), JavaScript (ES6+), Chart.js, Supabase JS client, Web Bluetooth ESC/POS.

## Global Constraints
- Zero external build step (pure Vanilla JS/HTML/CSS for instant local & Vercel deployment).
- Strict adherence to Stitch design tokens in `css/design-system.css`.
- Offline-first resilience: all transactions persist in LocalStorage and queue for Supabase sync.
- Minimum touch target >= 44px x 44px for tablet and mobile cashier use.

---

### Task 1: Clean Removal of Dapur (KDS) and Meja (Tables)

**Files:**
- Modify: `index.html`
- Modify: `js/app.js`
- Modify: `sw.js`
- Modify: `js/views/payment.js`
- Modify: `js/views/pos.js`

**Interfaces:**
- Consumes: None
- Produces: Clean layout without `#view-kds`, without table selectors, without kitchen dispatch

- [ ] **Step 1: Remove Dapur and Table UI from `index.html`**
  - Delete Dapur nav item in `.sidebar-nav` and `.mobile-nav-dock`.
  - Delete `<section id="view-kds">` block.
  - Delete `#pos-table-selector-bar` and `#pos-table-selector-bar-cart`.
  - Remove `<script src="js/views/kds.js"></script>`.
  - Remove "diteruskan ke Dapur" note in payment success modal.

- [ ] **Step 2: Remove Dapur and Table logic from JS files**
  - In `js/app.js`: Remove `kds` from `switchView()`.
  - In `sw.js`: Remove `'js/views/kds.js'` from cached assets and bump cache version to `kapos-v3-cache-v3`.
  - In `js/views/payment.js`: Remove `kitchenOrders` push and emit. Remove `meja` property from transaction object.
  - In `js/views/pos.js`: Remove table selector event listeners and table state references.

- [ ] **Step 3: Verification & Commit**
  - Verify `index.html` opens cleanly without errors in console.
  - Commit: `git commit -m "feat(core): remove Dapur (KDS) and Meja (Tables) modules"`

---

### Task 2: Implement Complete Modifier Modal with Toppings & Quick Tags

**Files:**
- Modify: `index.html`
- Modify: `js/views/pos.js`
- Modify: `js/state.js`

**Interfaces:**
- Consumes: `window.State.addToCart(product, options)`
- Produces: Dynamic modifier sheet with spicy levels, chargeable toppings, and quick tags

- [ ] **Step 1: Update Modifier Modal HTML in `index.html`**
  - Radio options for Level Pedas: `Lv.0 (Ori) [Rp 0]`, `Lv.1 [Rp 0]`, `Lv.2 [Rp 0]`, `Lv.3 [Rp 0]`, `Lv.5 (Mampus) [+Rp 2.000]`.
  - Checkboxes for Toppings:
    - `Ekstra Sambal (+Rp 3.000)`
    - `Ekstra Jeruk Nipis (+Rp 1.000)`
    - `Bawang Goreng (+Rp 2.000)`
  - Quick Tag pills for Notes:
    - `Tanpa Micin`, `Bumbu Dipisah`, `Sedikit Es`, `Manis Sedang`.
  - Dynamic button text displaying total item price with modifiers.

- [ ] **Step 2: Update `js/views/pos.js` logic**
  - Track selected level, selected toppings array, and custom note string.
  - Recalculate price dynamically when toppings or Lv.5 are toggled.
  - Pass structured modifier data and total extra price into `window.State.addToCart()`.

- [ ] **Step 3: Verification & Commit**
  - Test adding product with Lv.5 and Ekstra Sambal, verify cart displays modifiers and correct price.
  - Commit: `git commit -m "feat(pos): implement complete modifier modal with toppings and quick tags"`

---

### Task 3: Implement Recipe BOM & Automatic Inventory Deduction

**Files:**
- Modify: `js/state.js`
- Modify: `js/views/inventory.js`
- Modify: `js/views/payment.js`
- Modify: `index.html`

**Interfaces:**
- Consumes: `window.State.inventory`, `window.State.products`
- Produces: Precise Recipe BOM tracking, automatic raw ingredient deduction upon transaction, and BOM management UI

- [ ] **Step 1: Add Recipe BOM definition in `js/state.js`**
  - Define `recipes` structure linking product ID to inventory item ID and quantity.
  - Default recipes for Taichan, Chicken Crispy, Es Teh, Es Jeruk.
  - Add helper `calculateProductHPP(productId)` in `State`.

- [ ] **Step 2: Update Inventory view in `index.html` and `js/views/inventory.js`**
  - Add tab switcher in Inventory: `Bahan Baku` vs `Resep Menu & HPP`.
  - Render BOM table showing Menu Name, Required Ingredients, Calculated HPP, Selling Price, and Margin Profit %.
  - Modal to edit ingredients in a menu's BOM.

- [ ] **Step 3: Update `payment.js` to auto-deduct inventory using BOM**
  - When payment is confirmed, iterate through ordered items and decrement ingredient stock based on BOM.
  - Log mutation to `ka_mut`.
  - Show warning if ingredients are insufficient.

- [ ] **Step 4: Verification & Commit**
  - Perform test transaction of 2x Taichan and verify Daging Ayam and Bumbu Taichan decrement by 0.30 kg and 0.20 pak.
  - Commit: `git commit -m "feat(inventory): implement recipe BOM, real-time HPP and auto-deduction"`

---

### Task 4: Complete Blind Cash Drop, X-Report & WhatsApp Receipt

**Files:**
- Modify: `index.html`
- Modify: `js/views/shift.js`
- Modify: `js/views/payment.js`
- Modify: `js/printer.js`

**Interfaces:**
- Consumes: `window.State.activeShift`, `window.State.transactions`
- Produces: Blind cash count reconciliation with variance calculation, thermal X/Z-report, and WhatsApp receipt share

- [ ] **Step 1: Ensure Blind Cash Count in `js/views/shift.js` covers all standard IDR denominations**
  - 100k, 50k, 20k, 10k, 5k, 2k, 1k, and Coin.
  - Compute Cash Variance: Physical Cash - System Expected Cash.
  - Display variance with status `Sesuai (Rp 0)`, `Lebih (+Rp X)`, or `Kurang (-Rp X)`.
  - Generate printable X-Report (mid-shift summary) and Z-Report (closing shift audit).

- [ ] **Step 2: WhatsApp receipt link generator in `js/printer.js`**
  - Format transaction items, subtotal, and payment into clean text.
  - Generate `https://wa.me/?text=...` URI.
  - Connect to WhatsApp share buttons in `payment-success-modal` and `receipt-preview-modal`.

- [ ] **Step 3: Verification & Commit**
  - Test blind count modal and WhatsApp link generation.
  - Commit: `git commit -m "feat(shift): finalize blind cash drop, X/Z reports and WhatsApp receipt"`

---

### Task 5: Complete Owner Financial Analytics in Reports View

**Files:**
- Modify: `index.html`
- Modify: `js/views/reports.js`

**Interfaces:**
- Consumes: `window.State.transactions`
- Produces: Full financial metrics dashboard with period filters, method breakdown, and A4 print layout

- [ ] **Step 1: Enhance KPI Cards & Charts in `js/views/reports.js`**
  - Calculate Average Basket Size (Average Order Value).
  - Add Period selector: `Hari Ini`, `Kemarin`, `7 Hari Terakhir`, `Bulan Ini`.
  - Render payment method breakdown (Tunai vs QRIS vs Transfer).
  - List Top 5 Menu Terlaris with quantity and profit contribution.

- [ ] **Step 2: Clean up A4 Print and CSV Export**
  - Ensure A4 print report formats cleanly without screen clutter.
  - Ensure CSV export contains all transaction details.

- [ ] **Step 3: Final Verification & Commit**
  - Verify all 5 views (POS, Shift, Inventory, Reports, Settings) function seamlessly.
  - Commit: `git commit -m "feat(reports): complete owner financial analytics and period filtering"`
