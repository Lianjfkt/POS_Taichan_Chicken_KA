# KA POS v3.0: UI/UX Redesign Specification & Architecture

- **Project:** KA POS v3.0 — Culinary Velocity POS & Kitchen Management (Specialized for KA Taichan & Chicken)
- **Target Platform:** Progressive Web App (PWA) static deployment on Vercel + Supabase Backend (PostgreSQL, Auth, Realtime CDC)
- **Design System Source:** `stitch_ka_pos_design_system_shell`
- **Date:** 2026-09-27
- **Status:** Approved

---

## 1. Executive Summary & Design System

### 1.1 Brand Identity & Design Tokens
KA POS v3.0 adopts a modern, high-contrast, eye-friendly dark aesthetic specifically tailored for F&B cashier environments (dim counter lighting, high glare, greasy fingers, rapid touches).

```css
:root {
  /* Surface Layers */
  --bg-canvas: #0f131d;
  --surface-container-lowest: #0a0e18;
  --surface-container-low: #171b26;
  --surface-container: #1c1f2a;
  --surface-container-high: #262a35;
  --surface-container-highest: #313540;

  /* Brand Accents */
  --primary: #ffb690;
  --primary-container: #f97316; /* Flame Orange */
  --on-primary: #552100;
  --on-primary-container: #582200;

  /* Status Colors */
  --tertiary: #4edea3; /* Mint Emerald - Online & Success */
  --tertiary-container: #00b07a;
  --error: #ffb4ab;
  --error-container: #93000a; /* Warning & Urgent Alert */
  --warning: #f59e0b;

  /* Typography Colors */
  --on-surface: #dfe2f1;
  --on-surface-variant: #e0c0b1;
  --secondary: #bcc7de;
  --outline: #a78b7d;
  --outline-variant: #584237;

  /* Fonts */
  --font-body: 'Plus Jakarta Sans', sans-serif;
  --font-mono: 'Space Mono', monospace;
}
```

### 1.2 Layout & Viewport Shell Architecture
A responsive 3-tier layout strategy that seamlessly adapts across all screen form factors:

1. **Desktop & Tablet Landscape (>= 768px):**
   - **Slim Left Sidebar (88px):** Fixed navigation with high-visibility icon buttons (`POS`, `Shift/Kas`, `Dapur KDS`, `Stok Bahan`, `Laporan Owner`, `Pengaturan`, `Kunci Layar`).
   - **Top Operational Header (56px):** Brand logo, branch identifier, Realtime cloud sync badge (`Online`, `Offline Pending`, `Syncing`), active shift/cashier badge, and screen lock trigger.
   - **3-Zone Velocity POS Layout:**
     - **Zone A (Category Rail, ~190px):** Vertical quick category pills with item count badges.
     - **Zone B (Product Catalog Grid, Flexible):** Responsive grid of product cards with price tags in monospace and 1-tap quick add buttons.
     - **Zone C (Persistent Cart Sidebar, 360px):** Order type selector (Dine In, Take Away, Online/Ojol), quick table selector, live order items with modifier indicators, subtotal/tax/discount calculations, and large primary action button "Bayar Sekarang".

2. **Mobile Smartphone (< 768px):**
   - **Compact Header:** Branch name, live sync badge, and lock button.
   - **Bottom Navigation Bar:** Floating thumb-friendly navigation dock.
   - **2-Column Product Grid:** Optimized for single-thumb scrolling.
   - **Floating Bottom Cart Pill & Drawer:** Slides up into a full-height checkout bottom sheet.

---

## 2. Modular File Architecture

To avoid monolithic code bloat while remaining zero-build and instantly deployable to Vercel/PWA:

```
POS KA/
├── index.html                 # Main responsive shell & modal containers
├── manifest.json              # PWA manifest
├── sw.js                      # Service Worker offline cache
├── supabase_schema.sql        # Supabase database schema
├── DEPLOYMENT_GUIDE.md        # Deployment instructions
├── css/
│   └── design-system.css      # Stitch color tokens, typography, and utility styling
└── js/
    ├── state.js               # Central state management & LocalStorage persistence
    ├── supabase.js            # Supabase Cloud sync, realtime subscriptions & offline queue
    ├── printer.js             # Web Bluetooth ESC/POS thermal printing & receipt canvas renderer
    ├── views/
    │   ├── auth.js            # Multi-state login & Quick Lock 4-digit PIN modal
    │   ├── pos.js             # 3-Zone POS catalog, category filtering, cart & modifier modal
    │   ├── payment.js         # Quick cash calculator, QRIS & multi-payment workflow
    │   ├── kds.js             # Realtime Kitchen Display System & station filters
    │   ├── shift.js           # Shift session, petty cash entry & Blind Cash Count modal
    │   ├── inventory.js       # Inventory table, Recipe BOM & Fast Stock Update
    │   ├── reports.js         # Owner financial analytics, Chart.js graphs & A4 print generator
    │   └── settings.js        # Supabase config, bluetooth printer pairing & store profile
    └── app.js                 # Application bootstrap & router
```

---

## 3. Detailed Module Specifications

### 3.1 Authentication & Quick Lock PIN
- **Multi-State Login:** Supports Owner and Cashier credentials, backward-compatible with existing SHA-256 hashed and plaintext passwords.
- **Quick Lock PIN:** Quick lock lockpad overlay with 4-digit numeric keypad for rapid cashier handover without logging out.

### 3.2 POS Kasir & Modifier Checkout
- **Fast Table Selector:** Quick buttons for Tables (01, 02, 04, 08, 12, etc.) or custom manual input.
- **Modifier Modal:** Dynamic selection for Spicy Levels (Lv 0 s/d Lv 5), Sambal Pisah, and Extra Toppings.
- **Payment Sheet:** Quick Cash chips (Uang Pas, Rp 20.000, Rp 50.000, Rp 100.000, Rp 200.000), immediate change calculation, dynamic QRIS code display, and receipt printing trigger.

### 3.3 Kitchen Display System (KDS)
- **Live Ticket Cards:** Display table number/takeaway tag, customer notes, item modifiers, and status (`Menunggu`, `Dimasak`, `Siap Saji`).
- **Urgency Aging Badges:** Visual color transitions based on elapsed time:
  - `< 5 mins`: Green status
  - `5–12 mins`: Amber warning
  - `> 12 mins`: Red pulsing rush alert
- **Audio Chime:** Synthesized web audio chime on incoming new orders.
- **Station Filter:** Filter items by cooking station (Bakaran Sate, Gorengan Ayam, Bar Minuman).

### 3.4 Shift Management & Blind Cash Count
- **Open Shift:** Initial drawer cash floating amount entry.
- **Petty Cash:** Cash in (Kas Masuk) / Cash out (Kas Keluar) operational tracking with reason and category.
- **Blind Cash Count:** Cashiers enter actual physical cash count without seeing system totals. Upon submission, system calculates discrepancy (surplus/deficit) and generates a shift reconciliation summary.

### 3.5 Inventory & Recipe BOM (Bill of Materials)
- **Fast Stock Update:** Modal to adjust available portion counts on the fly during rush hours.
- **Recipe BOM:** Automatic deduction of raw ingredients (chicken, chili sauce, cups) when products are sold.
- **Critical Stock Badges:** Alerts when inventory falls below minimum threshold.

### 3.6 Owner Dashboard & Deep Financial Analytics
- **Live KPI Metric Cards:** Omzet Hari Ini, Total Transaksi, Laba Kotor, HPP (COGS), and Laba Bersih.
- **Interactive Graphs:** Chart.js revenue trend over 7 days styled with Stitch dark theme and flame orange lines.
- **A4 Print-Friendly Documents:** Clean printable layout for audit reports, shift summaries, and financial statements.

### 3.7 Offline-First & Supabase Cloud Integration
- **Zero-Latency Offline Mode:** Orders are immediately stored in LocalStorage and queued in `ka_ofq` when disconnected.
- **Realtime CDC:** Automatically syncs changes from Supabase when online.
- **Status Indicator:** Persistent header indicator showing `ONLINE` (green), `SYNCING` (amber pulse), or `OFFLINE` (red error banner with pending queue count).

---

## 4. Verification & Testing Criteria
1. **Responsive Viewport Test:** Validate 1440px desktop, 768px tablet, and 390px mobile viewports.
2. **Transaction Flow Test:** Add items with modifiers -> checkout with quick cash -> verify change calculation -> verify receipt modal.
3. **KDS Flow Test:** Complete order -> verify ticket appears in KDS with correct station filter and timer -> mark as done.
4. **Shift & Blind Cash Count Test:** Open shift -> record petty cash -> submit blind cash count -> verify discrepancy audit log.
5. **Supabase & Offline Queue Test:** Disconnect simulation -> create transaction -> reconnect -> verify sync queue flushes successfully.
