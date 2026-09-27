---
name: Culinary Velocity
colors:
  surface: '#0f131d'
  surface-dim: '#0f131d'
  surface-bright: '#353944'
  surface-container-lowest: '#0a0e18'
  surface-container-low: '#171b26'
  surface-container: '#1c1f2a'
  surface-container-high: '#262a35'
  surface-container-highest: '#313540'
  on-surface: '#dfe2f1'
  on-surface-variant: '#e0c0b1'
  inverse-surface: '#dfe2f1'
  inverse-on-surface: '#2c303b'
  outline: '#a78b7d'
  outline-variant: '#584237'
  surface-tint: '#ffb690'
  primary: '#ffb690'
  on-primary: '#552100'
  primary-container: '#f97316'
  on-primary-container: '#582200'
  inverse-primary: '#9d4300'
  secondary: '#bcc7de'
  on-secondary: '#263143'
  secondary-container: '#3e495d'
  on-secondary-container: '#aeb9d0'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#00b07a'
  on-tertiary-container: '#003b26'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdbca'
  primary-fixed-dim: '#ffb690'
  on-primary-fixed: '#341100'
  on-primary-fixed-variant: '#783200'
  secondary-fixed: '#d8e3fb'
  secondary-fixed-dim: '#bcc7de'
  on-secondary-fixed: '#111c2d'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#0f131d'
  on-background: '#dfe2f1'
  surface-variant: '#313540'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-numeric-lg:
    fontFamily: Space Mono
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  label-numeric-md:
    fontFamily: Space Mono
    fontSize: 16px
    fontWeight: '700'
    lineHeight: 20px
  label-numeric-sm:
    fontFamily: Space Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 16px
  label-caps:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.06em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.5rem
  margin: 1rem
  margin-mobile: 0.75rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system delivers an ultra-responsive, ergonomic, touch-first POS and Kitchen Display System (KDS) tailored for high-volume F&B operations. Designed specifically for fast casual dining and high-throughput street-food kitchen logistics, the interface eliminates cognitive friction through an assertive dark-mode canvas punctuated by high-heat thermal accents.

### Design Movement & Aesthetic
A tactical synthesis of **Modern Utilitarianism** and **Tactile Dark-Mode Glassmorphism**. The system relies on deep atmospheric blues and midnight obsidian slates to reduce eye fatigue under harsh restaurant and kitchen lighting. High-impact operational cues are rendered in culinary flame tones, creating an unmistakable visual hierarchy where actionable elements (order status, checkout, fire tickets) leap forward.

### Tone & Ergonomics
- **Fast & Tactile:** Large, physics-backed tap targets (minimum 44×44px, optimized for grease-resistant tablet operation) with distinct pressed states.
- **Authoritative & Legible:** High contrast thresholds adhering strictly to WCAG AA across dimly lit cashier booths and high-temperature kitchen lines.
- **Indonesian Operational Tone:** Direct, functional Indonesian F&B vernacular (*Pesanan Baru*, *Sedang Dimasak*, *Siap Saji*, *Bayar*, *Meja*, *Bungkus*), omitting decorative prose in favor of operational brevity.

## Colors
The color architecture is built around a low-luminance, high-efficiency palette calibrated for OLED/LCD screens in high-turnover culinary environments.

### Palette Architecture
- **Primary (Flame Orange / `#F97316`):** Reserved for primary order actions, fire commands, active kitchen timers, and conversion buttons. Hover/active shifts to `#EA580C`.
- **Neutral Core (Midnight Obsidian / `#0B0F19`):** The foundational viewport floor. Prevents peripheral glare and frames data surfaces.
- **Surface Containers:**
  - `Dark Surface (#151D2F)`: Canvas for persistent sidebars, receipt panels, and category trays.
  - `Dark Elevated (#1E293B)`: Menu cards, active modal windows, and KDS ticket surfaces.
- **Typography & Semantic Neutrals:**
  - `Main Text (#F8FAFC)`: Critical values, menu item headers, active quantities.
  - `Muted (#94A3B8)`: Secondary attributes, modifier lists, customer notes.
  - `Dim (#64748B)`: Disabled states, divider strokes, placeholder text.
- **Operational Status Roles:**
  - `Success (#10B981)`: Paid status, complete kitchen tickets, cash drawer balance.
  - `Warning (#F59E0B)`: Items delayed beyond target prep time (>15 mins), low stock indicators.
  - `Danger (#EF4444)`: Void items, urgent tickets (>25 mins), cancelled transactions.
  - `Info (#3B82F6)`: Table tracking, dine-in indicators, cloud sync confirmation.

## Typography
Typography is split into two specialized engines:

1. **Plus Jakarta Sans (UI Engine):** Handles natural language scanning. Its generous x-height and open counters guarantee that items such as *Sate Taichan Paha 10 Tusuk* remain effortlessly legible from arm’s length on fixed countertop registers.
2. **Space Mono (Financial & Metric Engine):** Used for all numerical values, monetary amounts (`Rp 45.000`), order ticket counters (`#TRX-0829`), timestamps (`14:22:05`), and table assignments (`MEJA 12`). Tabular figures prevent layout jumping during live cart recalculations.

### Case & Sizing Conventions
- All metadata labels, order type badges (*DINE-IN*, *TAKEAWAY*), and ticket states use `label-caps` in uppercase.
- Currency symbols are kept flush against the values (`Rp25.000`) without intervening whitespace to safeguard columnar alignment on 80mm thermal receipt previews.

## Layout & Spacing
The layout follows a rigid 8px spatial grid designed for industrial multi-column tablet POS layouts (iPad 10.2", 10.9", and 15.6" Android touch screens).

### Layout System
- **Desktop & Counter POS (Horizontal Split):** A fluid 3-pane structure:
  - Navigation & Category Bar: 88px fixed left spine.
  - Item Catalog Grid: Flexible fluid column (min 480px, auto-fill cards at 140px min-width).
  - Active Cart / Order Summary: Fixed 380px right rail.
- **Kitchen Display System (KDS):** Horizontal kanban pipeline with horizontal scroll, composed of fixed 280px-wide order cards spaced by `space-md` (16px).
- **Handheld / Mobile Terminal:** Single-column vertical stack with persistent bottom-anchored summary sheet (64px collapsed trigger, expanding to full modal).

### Touch Targets
- Interactive targets must never scale below 44×44px. Critical conversion targets (such as *Proses Pembayaran* and *Simpan Pesanan*) use a minimum height of 56px with full component padding (`space-md`).

## Elevation & Depth
Depth in this system avoids heavy, distracting skeuomorphism. Instead, it relies on disciplined structural layering, subtle chromatic rim borders, and low-frequency dark ambient shadows to simulate physical surface levels.

### Depth Tiers
1. **Floor (Level 0 - `#0B0F19`):** Application canvas and system backdrops. Zero elevation.
2. **Structural Panes (Level 1 - `#151D2F`):** Cart pane, persistent side rail, category filter ribbons. Defined by a 1px right/left border (`rgba(255, 255, 255, 0.05)`).
3. **Floating Cards & KDS Tickets (Level 2 - `#1E293B`):** Menu items, prep tickets, and action cards. Elevated using:
   - Ambient drop shadow: `0 4px 20px -2px rgba(0, 0, 0, 0.5)`.
   - Subtle rim light: `border: 1px solid rgba(255, 255, 255, 0.07)`.
4. **Active Modals & Numpad Overlays (Level 3 - `#1E293B`):**
   - High-spread shadow: `0 20px 40px -8px rgba(0, 0, 0, 0.8)`.
   - Backdrop filter: `blur(8px)` with `background: rgba(11, 15, 25, 0.75)`.
   - Active highlight: Top edge rim light `1px solid rgba(249, 115, 22, 0.3)`.

## Shapes
A roundedness value of `2` provides a sturdy balance between structural utility and modern feel.

### Geometric Rules
- **Base Surfaces & Cards:** Standard `rounded` (0.5rem / 8px). Used for menu items, list rows, and ticket containers.
- **Containers, Drawers & Modals:** `rounded-lg` (1rem / 16px). Softens large block boundaries on high-res displays.
- **Operational Badges & Quantity Selectors:** Strict circular or pill implementations for counters and status tags to contrast against rectangular menu tiles.
- **Input Fields & Keypad Buttons:** `rounded` (8px) to maximize internal surface area for finger strikes.

## Components

### 1. Action Buttons
- **Primary (*Bayar / Simpan*):** Background `#F97316`, label `#F8FAFC` (semibold), 56px height for primary payment triggers. Tap down scales to `0.98` with background `#EA580C`.
- **Secondary (*Cetak Struk / Tambah Catatan*):** Background `#1E293B`, border `1px solid rgba(255, 255, 255, 0.1)`, label `#F8FAFC`.
- **Destructive (*Batal / Void*):** Background `rgba(239, 68, 68, 0.15)`, text `#EF4444`, border `1px solid rgba(239, 68, 68, 0.3)`.

### 2. Product Catalog Card
- Dark elevated background (`#1E293B`) with a 1px border (`rgba(255, 255, 255, 0.05)`).
- Image area: 1:1 aspect ratio with subtle gradient vignette at bottom.
- Content: Title (`headline-sm`, `#F8FAFC`), stock indicator (`body-sm`, `#94A3B8`), and price (`label-numeric-md`, `#F97316`).
- Quick Add badge: High-contrast circular plus icon at bottom-right corner (minimum 36×36px within the 44px hit-box).

### 3. Cart Line Item
- 2-row compact layout: Row 1 features item quantity badge (`Space Mono`, orange tint pill), item name, and line total price. Row 2 holds indented modifiers (*Level Pedas 3*, *Ekstra Sambal*) in `body-sm` (`#94A3B8`).
- Slide-to-reveal or direct-tap trash icon (`#EF4444`) for fast deletion.

### 4. KDS Ticket Card
- Header: Table Number (`headline-md`) + Order Time elapsed indicator (`label-numeric-sm`).
- Dynamic header border: Shifts to `#10B981` (Normal), `#F59E0B` (>10 min), `#EF4444` (>20 min).
- Checklist rows: Striking strikethrough state on tap with dimmed opacity (`0.4`) to mark individual dish completion.
- Footer action: Full-width button *Selesai Masak* in primary flame or success green.

### 5. Quick Numeric Numpad
- 3×4 touch matrix with zero-lag active visual state (`#151D2F` to `#2D3B55`).
- Keys spaced with `space-sm` (8px), typography in `label-numeric-lg` (`Space Mono`). Quick denomination buttons (`+10k`, `+20k`, `+50k`, `Pas`) along the right side.

### 6. Inputs & Search Fields
- Inset dark background (`#0B0F19`), height 48px, leading search icon in `#94A3B8`.
- Focus ring: `2px solid #F97316` without outer blur. Clear ("X") button always visible when content is present.