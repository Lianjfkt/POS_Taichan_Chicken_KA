# KA POS v3.0: UI/UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform KA POS into a modern, high-velocity, responsive F&B POS and Kitchen Management PWA by adopting the complete Stitch Design System (`stitch_ka_pos_design_system_shell`).

**Architecture:** Refactor the monolithic `index.html` into a modular, zero-build Progressive Web App with dedicated CSS tokens (`css/design-system.css`), central state store (`js/state.js`), Supabase cloud sync (`js/supabase.js`), thermal bluetooth printing (`js/printer.js`), and domain-driven view modules (`js/views/*`) routed through `js/app.js`.

**Tech Stack:** HTML5, Vanilla JavaScript (ES6+), Vanilla CSS + Stitch Design Tokens, Google Fonts (Plus Jakarta Sans & Space Mono), Material Symbols Outlined, Chart.js, Supabase JS v2, Web Bluetooth API, PWA Service Worker.

## Global Constraints
- Target Viewports: Mobile 390px (single-thumb drawer), Tablet 768px (split 2-column), Desktop 1440px (3-Zone Velocity).
- Design System Color Palette: Background `#0f131d`, Primary `#f97316` (Flame Orange), Tertiary `#4edea3` (Mint Emerald), Error `#93000a`.
- Typography: Plus Jakarta Sans for UI/Body/Headlines, Space Mono for Numbers/Currency/Timers.
- No build tools required (must run natively in browser and deploy directly to Vercel/PWA).
- 100% backward-compatibility with existing LocalStorage data keys and `supabase_schema.sql`.

---

### Task 1: Design System CSS & Tokens (`css/design-system.css`)

**Files:**
- Create: `css/design-system.css`
- Test: Manual visual token inspection via test HTML or browser

**Interfaces:**
- Produces: Complete CSS custom properties (`:root`), font declarations, 3-zone layout grid rules, mobile bottom sheet drawer animations, modal backdrop styling, and dark theme utility classes.

- [ ] **Step 1: Create `css/design-system.css` with Stitch design tokens**

```css
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&family=Space+Mono:wght@400;700&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200');

:root {
  --bg: #0f131d;
  --surface: #0f131d;
  --surface-container-lowest: #0a0e18;
  --surface-container-low: #171b26;
  --surface-container: #1c1f2a;
  --surface-container-high: #262a35;
  --surface-container-highest: #313540;
  --surface-bright: #353944;

  --primary: #ffb690;
  --primary-container: #f97316;
  --on-primary: #552100;
  --on-primary-container: #582200;
  --primary-fixed: #ffdbca;

  --tertiary: #4edea3;
  --tertiary-container: #00b07a;
  --on-tertiary: #003824;

  --error: #ffb4ab;
  --error-container: #93000a;
  --on-error: #690005;
  --on-error-container: #ffdad6;

  --on-surface: #dfe2f1;
  --on-surface-variant: #e0c0b1;
  --secondary: #bcc7de;
  --outline: #a78b7d;
  --outline-variant: #584237;

  --font-body: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-mono: 'Space Mono', monospace;

  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-full: 9999px;
}

*, *::before, *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  background-color: var(--bg);
  color: var(--on-surface);
  font-family: var(--font-body);
  font-size: 14px;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
  overflow-x: hidden;
  user-select: none;
}

.font-mono { font-family: var(--font-mono); }
```

- [ ] **Step 2: Add 3-Zone Velocity & responsive layout styles to `css/design-system.css`**

Include `.zone-a` (category rail, w: 180-200px), `.zone-b` (product catalog flex-1), `.zone-c` (persistent cart, w: 360px), and mobile media queries (`@media (max-width: 768px)`).

- [ ] **Step 3: Add modal, toast, and bottom sheet drawer animations**

Include slide-up drawer for mobile cart (`@keyframes slideUp`), fade-in overlay, and toast styling.

- [ ] **Step 4: Verify CSS file creation and syntax validity**

---

### Task 2: State Store & Persistence Layer (`js/state.js`)

**Files:**
- Create: `js/state.js`

**Interfaces:**
- Produces: `State` singleton object exposing:
  - `get(key)`, `set(key, val)`, `save()`
  - Collections: `categories`, `products`, `transactions`, `cashLog`, `activeShift`, `onProcessOrders`, `kitchenOrders`, `inventory`, `stockMutations`, `staff`, `settings`, `offlineQueue`
  - Helpers: `formatRp(amount)`, `formatDate(date)`, `hashPassword(str)`, `generateId()`
  - Event emitter: `on(event, callback)`, `emit(event, data)`

- [ ] **Step 1: Write `js/state.js` defining local storage keys, sample defaults, and reactive store**
- [ ] **Step 2: Implement password hashing (SHA-256 Web Crypto API) and currency formatting**
- [ ] **Step 3: Implement data validation and event dispatchers for state changes**
- [ ] **Step 4: Verify state loading and persistence from existing `localStorage`**

---

### Task 3: Supabase Cloud & Realtime Integration (`js/supabase.js`)

**Files:**
- Create: `js/supabase.js`

**Interfaces:**
- Consumes: `State`
- Produces: `SupabaseService` with:
  - `init()`, `testConnection(url, key)`
  - `pushLocalData()`, `pullRemoteData()`
  - `subscribeRealtime()`, `syncOfflineQueue()`
  - Status updates: `online`, `offline`, `syncing`

- [ ] **Step 1: Write `js/supabase.js` wrapping `@supabase/supabase-js`**
- [ ] **Step 2: Implement offline queue processor (`syncOfflineQueue`) to flush pending orders**
- [ ] **Step 3: Implement realtime CDC channel for `orders` and `inventory_items`**
- [ ] **Step 4: Verify error handling and connection status broadcasting**

---

### Task 4: Bluetooth ESC/POS Thermal Printer & Receipt Canvas (`js/printer.js`)

**Files:**
- Create: `js/printer.js`

**Interfaces:**
- Consumes: `State.settings`, transaction object
- Produces: `PrinterService` with:
  - `connectBluetooth()`, `printReceipt(trx)`
  - `renderReceiptHTML(trx)`, `renderReceiptCanvas(trx)`
  - `shareReceiptWhatsApp(trx)`

- [ ] **Step 1: Implement Bluetooth ESC/POS printer byte command generator**
- [ ] **Step 2: Implement clean thermal receipt preview HTML/Canvas generator matching Stitch receipt design**
- [ ] **Step 3: Implement WhatsApp text receipt formatter**
- [ ] **Step 4: Verify printer connection and preview render**

---

### Task 5: Authentication & Quick Lock PIN View (`js/views/auth.js`)

**Files:**
- Create: `js/views/auth.js`

**Interfaces:**
- Consumes: `State`, `Router`
- Produces: `AuthView` with:
  - `renderLogin()`, `handleLogin(username, password)`
  - `renderQuickLock()`, `handleUnlockPIN(pin)`
  - `lockScreen()`, `logout()`

- [ ] **Step 1: Implement full-screen login view with Stitch dark visual style**
- [ ] **Step 2: Implement 4-digit quick lock terminal overlay with numeric keypad**
- [ ] **Step 3: Implement auto-lock inactivity timer (15 minutes configurable)**
- [ ] **Step 4: Verify login, PIN unlock, and role permission checks**

---

### Task 6: 3-Zone Velocity POS Kasir View & Modifiers (`js/views/pos.js`)

**Files:**
- Create: `js/views/pos.js`

**Interfaces:**
- Consumes: `State`, `Router`, `PaymentView`
- Produces: `POSView` with:
  - `render()`, `filterCategory(catId)`, `searchMenu(query)`
  - `addToCart(product, modifiers)`, `updateCartQty(index, delta)`, `removeFromCart(index)`
  - `openModifierModal(product)`
  - `openMobileCartDrawer()`, `closeMobileCartDrawer()`

- [ ] **Step 1: Implement Zone A (Category Rail) with active state indicators and count badges**
- [ ] **Step 2: Implement Zone B (Product Catalog Grid) with high-res cards, price tags, and quick-add buttons**
- [ ] **Step 3: Implement Zone C (Persistent Cart on Desktop, Bottom Sheet Drawer on Mobile)**
- [ ] **Step 4: Implement Modifier Sheet (Spicy Lv 0-5, Sambal Pisah, Add-on Toppings)**
- [ ] **Step 5: Verify cart calculations (subtotal, tax, discount) and responsive drawer behavior**

---

### Task 7: Payment & Quick Cash Modal (`js/views/payment.js`)

**Files:**
- Create: `js/views/payment.js`

**Interfaces:**
- Consumes: `State`, `PrinterService`, `KDSView`
- Produces: `PaymentView` with:
  - `openPaymentModal(cartData)`
  - `selectPaymentMethod(method)`: Cash, QRIS, Transfer
  - `selectQuickCash(amount)`: Pas, 20k, 50k, 100k, 200k
  - `processPayment()`, `showSuccessModal(trx)`

- [ ] **Step 1: Implement payment modal with method selector and quick cash chips**
- [ ] **Step 2: Implement dynamic change calculation in Space Mono tabular font**
- [ ] **Step 3: Implement QRIS display and bank transfer instructions**
- [ ] **Step 4: Implement payment success dialog with receipt print trigger and KDS dispatch**

---

### Task 8: Kitchen Display System (KDS) View (`js/views/kds.js`)

**Files:**
- Create: `js/views/kds.js`

**Interfaces:**
- Consumes: `State`
- Produces: `KDSView` with:
  - `render()`, `filterStation(station)`: Semua, Bakaran, Gorengan, Minuman
  - `updateOrderStatus(orderId, status)`
  - `playOrderAlert()` (Web Audio API sound chime)
  - `updateAgingTimers()`

- [ ] **Step 1: Implement KDS ticket cards showing table number, notes, and items**
- [ ] **Step 2: Implement urgency aging timer (<5m green, 5-12m amber, >12m pulsing red)**
- [ ] **Step 3: Implement station filter selector (Bakaran Sate, Gorengan Ayam, Bar Minuman)**
- [ ] **Step 4: Implement synthesized Web Audio alert chime on new incoming tickets**

---

### Task 9: Shift & Blind Cash Count View (`js/views/shift.js`)

**Files:**
- Create: `js/views/shift.js`

**Interfaces:**
- Consumes: `State`
- Produces: `ShiftView` with:
  - `render()`, `openShift(startingCash)`
  - `recordPettyCash(type, amount, reason)`: Kas Masuk / Kas Keluar
  - `openBlindCashCountModal()`, `submitBlindCashCount(actualCash)`
  - `renderReconciliationSummary(shiftReport)`

- [ ] **Step 1: Implement active shift dashboard showing drawer summary and petty cash log**
- [ ] **Step 2: Implement Petty Cash In/Out modal with reason categories**
- [ ] **Step 3: Implement Blind Cash Count modal (cashier enters physical count without seeing system balance)**
- [ ] **Step 4: Implement reconciliation result modal showing surplus/deficit calculation**

---

### Task 10: Inventory, Recipe BOM & Fast Stock Update View (`js/views/inventory.js`)

**Files:**
- Create: `js/views/inventory.js`

**Interfaces:**
- Consumes: `State`
- Produces: `InventoryView` with:
  - `render()`, `updateStock(itemId, newStock)`
  - `openFastStockModal()`, `quickAdjustStock(itemId, delta)`
  - `renderRecipeBOM()`, `saveRecipe(productId, ingredients)`

- [ ] **Step 1: Implement inventory dashboard with critical stock warning alerts**
- [ ] **Step 2: Implement Fast Stock Update quick dialog for rush hour stock toggling**
- [ ] **Step 3: Implement Recipe BOM editor linking products to raw ingredient quantities**
- [ ] **Step 4: Implement automatic ingredient deduction on transaction completion**

---

### Task 11: Owner Dashboard, Deep Analytics & Reports View (`js/views/reports.js`)

**Files:**
- Create: `js/views/reports.js`

**Interfaces:**
- Consumes: `State`
- Produces: `ReportsView` with:
  - `renderDashboard()`, `renderChart(canvasId, days)`
  - `renderFinancialIntelligence()`: Revenue, COGS, Gross Profit, Net Margin
  - `exportCSV(type)`, `printA4Report(reportData)`

- [ ] **Step 1: Implement KPI metric cards (Omzet, Total Trx, Laba Kotor, HPP, Margin)**
- [ ] **Step 2: Implement Chart.js revenue trend chart in Stitch dark aesthetic**
- [ ] **Step 3: Implement A4 print-friendly report generator with clean invoice styling**
- [ ] **Step 4: Implement CSV export for transactions and financial audits**

---

### Task 12: Settings & Supabase Configuration View (`js/views/settings.js`)

**Files:**
- Create: `js/views/settings.js`

**Interfaces:**
- Consumes: `State`, `SupabaseService`, `PrinterService`
- Produces: `SettingsView` with:
  - `renderStoreSettings()`, `saveStoreSettings()`
  - `renderSupabaseConfig()`, `testSupabase()`
  - `renderPrinterSettings()`, `pairBluetoothPrinter()`
  - `renderStaffManagement()`, `saveStaff()`

- [ ] **Step 1: Implement store profile & receipt footer settings form**
- [ ] **Step 2: Implement Supabase Cloud URL & Anon Key config with connection test**
- [ ] **Step 3: Implement Bluetooth printer pairing and test print button**
- [ ] **Step 4: Implement staff role & PIN management**

---

### Task 13: Core Application Shell & Router Integration (`index.html` & `js/app.js`)

**Files:**
- Modify: `index.html`
- Create: `js/app.js`

**Interfaces:**
- Produces: `Router` managing active view switching, responsive shell layout, sidebar active tabs, mobile bottom navigation, and global offline banner.

- [ ] **Step 1: Restructure `index.html` to load `design-system.css`, modular scripts, and shell layout**
- [ ] **Step 2: Implement fixed 88px slim sidebar on desktop and bottom navigation dock on mobile**
- [ ] **Step 3: Implement sticky top operational header with realtime sync badge and quick lock trigger**
- [ ] **Step 4: Implement view router in `js/app.js` with role-based access control (Owner vs Cashier)**
- [ ] **Step 5: Verify seamless view transitions and responsive adaptation**

---

### Task 14: End-to-End Verification & PWA Testing

**Files:**
- Test all views across viewports and simulated offline state

- [ ] **Step 1: Test Desktop Viewport (1440px) 3-Zone Velocity ordering flow**
- [ ] **Step 2: Test Mobile Viewport (390px) single-thumb ordering & bottom sheet drawer**
- [ ] **Step 3: Test KDS order lifecycle from POS checkout to kitchen completion**
- [ ] **Step 4: Test Shift open, petty cash entry, and Blind Cash Count close**
- [ ] **Step 5: Test Offline simulation, pending queue counter, and Supabase auto-sync**
