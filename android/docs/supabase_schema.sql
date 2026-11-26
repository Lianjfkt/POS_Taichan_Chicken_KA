-- ==============================================================================
-- TAICHAN & CHICKEN KA - SUPABASE POSTGRESQL SCHEMA & RLS POLICIES
-- ==============================================================================
-- File: docs/supabase_schema.sql
-- Description: Complete schema definition, primary keys, foreign keys, indexes,
--              and Row Level Security (RLS) policies for the Cloud POS integration.
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================================================
-- 2. MENU CATALOG TABLES (Categories & Products)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    emoji TEXT DEFAULT '🍽️',
    color TEXT DEFAULT '#0B5C73',
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS emoji TEXT DEFAULT '🍽️';
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS color TEXT DEFAULT '#0B5C73';
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW());
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW());

CREATE TABLE IF NOT EXISTS public.products (
    id TEXT PRIMARY KEY,
    category_id TEXT REFERENCES public.categories(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    cost_price DOUBLE PRECISION DEFAULT 0.0,
    emoji TEXT DEFAULT '🍢',
    spice_level TEXT DEFAULT '',
    description TEXT DEFAULT '',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

ALTER TABLE public.products ADD COLUMN IF NOT EXISTS category_id TEXT REFERENCES public.categories(id) ON DELETE SET NULL;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS cost_price DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS emoji TEXT DEFAULT '🍢';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS spice_level TEXT DEFAULT '';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW());
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_spice_level BOOLEAN DEFAULT FALSE;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_sambal BOOLEAN DEFAULT FALSE;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_complimentary BOOLEAN DEFAULT FALSE;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_toppings BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_products_category ON public.products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_active ON public.products(is_active);

-- Modifiers Table (Level Pedas, Sambal, Complimentary, Extra Toppings, Packaging)
CREATE TABLE IF NOT EXISTS public.modifiers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    price DOUBLE PRECISION DEFAULT 0.0,
    category TEXT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

ALTER TABLE public.modifiers ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for modifiers" ON public.modifiers;
CREATE POLICY "Allow all for modifiers" ON public.modifiers
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

-- ==============================================================================
-- 3. TRANSACTIONS & SALES TABLES (Orders & Order Items)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.orders (
    id TEXT PRIMARY KEY,
    order_no TEXT NOT NULL DEFAULT '',
    customer_name TEXT,
    customer_phone TEXT,
    order_type TEXT NOT NULL DEFAULT 'dine-in', -- dine-in, takeaway, online
    table_no TEXT,
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount DOUBLE PRECISION DEFAULT 0.0,
    total DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    payment_method TEXT NOT NULL DEFAULT 'cash', -- cash, qris, debit, transfer
    status TEXT NOT NULL DEFAULT 'completed', -- completed, void, pending
    cashier_id TEXT DEFAULT 'Kasir',
    notes TEXT,
    void_reason TEXT,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

-- Migrasi aman jika tabel orders sudah ada sebelumnya di Supabase
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS order_no TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS customer_name TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS customer_phone TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS order_type TEXT DEFAULT 'dine-in';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS table_no TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS subtotal DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS discount DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS total DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_method TEXT DEFAULT 'cash';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'completed';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS cashier_id TEXT DEFAULT 'Kasir';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS void_reason TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW());

CREATE INDEX IF NOT EXISTS idx_orders_created_at ON public.orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_customer_phone ON public.orders(customer_phone);
CREATE INDEX IF NOT EXISTS idx_orders_status ON public.orders(status);

CREATE TABLE IF NOT EXISTS public.order_items (
    id TEXT PRIMARY KEY,
    order_id TEXT NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    product_id TEXT,
    product_name TEXT NOT NULL DEFAULT '',
    price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    quantity INT NOT NULL DEFAULT 1,
    spice_level TEXT DEFAULT '',
    notes TEXT DEFAULT '',
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

-- Migrasi aman jika tabel order_items sudah ada sebelumnya di Supabase
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS product_id TEXT;
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS product_name TEXT DEFAULT '';
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS price DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS quantity INT DEFAULT 1;
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS spice_level TEXT DEFAULT '';
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS notes TEXT DEFAULT '';
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS subtotal DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW());

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON public.order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON public.order_items(product_id);

-- ==============================================================================
-- 4. INVENTORY & RAW MATERIALS (BOM & Expiry Tracking)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.raw_materials (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    sku TEXT,
    unit TEXT NOT NULL, -- gr, ml, pcs, kg
    current_stock DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    minimum_stock DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    cost_per_unit DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE TABLE IF NOT EXISTS public.material_batches (
    id TEXT PRIMARY KEY,
    material_id TEXT NOT NULL REFERENCES public.raw_materials(id) ON DELETE CASCADE,
    batch_number TEXT NOT NULL,
    initial_quantity DOUBLE PRECISION NOT NULL,
    remaining_quantity DOUBLE PRECISION NOT NULL,
    cost_per_unit DOUBLE PRECISION NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    expiry_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE INDEX IF NOT EXISTS idx_batches_material ON public.material_batches(material_id);
CREATE INDEX IF NOT EXISTS idx_batches_expiry ON public.material_batches(expiry_date);

-- ==============================================================================
-- 5. CUSTOMERS & LOYALTY
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.customers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL UNIQUE,
    email TEXT,
    points INT DEFAULT 0,
    total_spent DOUBLE PRECISION DEFAULT 0.0,
    visit_count INT DEFAULT 0,
    last_visit TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE INDEX IF NOT EXISTS idx_customers_phone ON public.customers(phone);

-- ==============================================================================
-- 6. CASHIER SHIFTS
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.shifts (
    id TEXT PRIMARY KEY,
    cashier_name TEXT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    starting_cash DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    expected_cash DOUBLE PRECISION,
    actual_cash DOUBLE PRECISION,
    cash_difference DOUBLE PRECISION,
    total_transactions INT DEFAULT 0,
    notes TEXT,
    is_open BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

ALTER TABLE public.shifts ADD COLUMN IF NOT EXISTS is_open BOOLEAN DEFAULT TRUE;

-- Cash Movements (Petty Cash: Kas Masuk / Kas Keluar untuk Audit Fraud)
CREATE TABLE IF NOT EXISTS public.cash_movements (
    id TEXT PRIMARY KEY,
    shift_id TEXT REFERENCES public.shifts(id) ON DELETE CASCADE,
    type TEXT NOT NULL, -- CASH_IN, CASH_OUT
    category TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE INDEX IF NOT EXISTS idx_cash_movements_shift ON public.cash_movements(shift_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_created ON public.cash_movements(created_at DESC);

-- Recipes & BOM (Bill of Materials)
CREATE TABLE IF NOT EXISTS public.recipes (
    id TEXT PRIMARY KEY,
    product_id TEXT REFERENCES public.products(id) ON DELETE CASCADE,
    product_name TEXT NOT NULL,
    yield_qty INT NOT NULL DEFAULT 1,
    instructions TEXT DEFAULT '',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE TABLE IF NOT EXISTS public.recipe_items (
    id TEXT PRIMARY KEY,
    recipe_id TEXT NOT NULL REFERENCES public.recipes(id) ON DELETE CASCADE,
    raw_material_id TEXT NOT NULL REFERENCES public.raw_materials(id) ON DELETE RESTRICT,
    quantity_required DOUBLE PRECISION NOT NULL,
    unit TEXT NOT NULL,
    notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE INDEX IF NOT EXISTS idx_recipe_items_recipe ON public.recipe_items(recipe_id);

-- ==============================================================================
-- 7. ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
-- Keamanan & Fraud Prevention:
-- 1. Read (SELECT) dibuka untuk klien POS yang terkonfigurasi.
-- 2. Insert & Update dibuka untuk sinkronisasi POS offline-first.
-- 3. DELETE DILARANG untuk orders, order_items, shifts, dan cash_movements
--    agar kasir/pihak tidak bertanggung jawab tidak dapat menghapus jejak audit penjualan/kas.

ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.modifiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.raw_materials ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.material_batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.recipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.recipe_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shifts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cash_movements ENABLE ROW LEVEL SECURITY;

-- Categories & Products & Modifiers: Read & Sync
DROP POLICY IF EXISTS "Allow all for categories" ON public.categories;
CREATE POLICY "Allow all for categories" ON public.categories
    FOR ALL TO public USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all for products" ON public.products;
CREATE POLICY "Allow all for products" ON public.products
    FOR ALL TO public USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all for modifiers" ON public.modifiers;
CREATE POLICY "Allow all for modifiers" ON public.modifiers
    FOR ALL TO public USING (true) WITH CHECK (true);

-- Orders: INSERT & SELECT & UPDATE diperbolehkan (untuk sinkronisasi & VOID).
-- DELETE DIBLOKIR TOTAL (Audit Trail Anti-Fraud: Order tidak boleh dihapus fisik).
DROP POLICY IF EXISTS "Allow all for orders" ON public.orders;
CREATE POLICY "Allow select for orders" ON public.orders FOR SELECT TO public USING (true);
CREATE POLICY "Allow insert for orders" ON public.orders FOR INSERT TO public WITH CHECK (true);
CREATE POLICY "Allow update for orders" ON public.orders FOR UPDATE TO public USING (true) WITH CHECK (true);
CREATE POLICY "Disallow delete for orders" ON public.orders FOR DELETE TO public USING (false);

-- Order Items: Sama seperti orders, blokir DELETE
DROP POLICY IF EXISTS "Allow all for order_items" ON public.order_items;
CREATE POLICY "Allow select for order_items" ON public.order_items FOR SELECT TO public USING (true);
CREATE POLICY "Allow insert for order_items" ON public.order_items FOR INSERT TO public WITH CHECK (true);
CREATE POLICY "Allow update for order_items" ON public.order_items FOR UPDATE TO public USING (true) WITH CHECK (true);
CREATE POLICY "Disallow delete for order_items" ON public.order_items FOR DELETE TO public USING (false);

-- Shifts & Cash Movements: Cegah kasir menghapus catatan kas/selisih uang
DROP POLICY IF EXISTS "Allow all for shifts" ON public.shifts;
CREATE POLICY "Allow select for shifts" ON public.shifts FOR SELECT TO public USING (true);
CREATE POLICY "Allow insert for shifts" ON public.shifts FOR INSERT TO public WITH CHECK (true);
CREATE POLICY "Allow update for shifts" ON public.shifts FOR UPDATE TO public USING (true) WITH CHECK (true);
CREATE POLICY "Disallow delete for shifts" ON public.shifts FOR DELETE TO public USING (false);

DROP POLICY IF EXISTS "Allow all for cash_movements" ON public.cash_movements;
CREATE POLICY "Allow select for cash_movements" ON public.cash_movements FOR SELECT TO public USING (true);
CREATE POLICY "Allow insert for cash_movements" ON public.cash_movements FOR INSERT TO public WITH CHECK (true);
CREATE POLICY "Allow update for cash_movements" ON public.cash_movements FOR UPDATE TO public USING (true) WITH CHECK (true);
CREATE POLICY "Disallow delete for cash_movements" ON public.cash_movements FOR DELETE TO public USING (false);

-- Raw materials, Batches & Recipes
DROP POLICY IF EXISTS "Allow all for raw_materials" ON public.raw_materials;
CREATE POLICY "Allow all for raw_materials" ON public.raw_materials FOR ALL TO public USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all for material_batches" ON public.material_batches;
CREATE POLICY "Allow all for material_batches" ON public.material_batches FOR ALL TO public USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all for recipes" ON public.recipes;
CREATE POLICY "Allow all for recipes" ON public.recipes FOR ALL TO public USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all for recipe_items" ON public.recipe_items;
CREATE POLICY "Allow all for recipe_items" ON public.recipe_items FOR ALL TO public USING (true) WITH CHECK (true);

-- Customers
DROP POLICY IF EXISTS "Allow all for customers" ON public.customers;
CREATE POLICY "Allow all for customers" ON public.customers FOR ALL TO public USING (true) WITH CHECK (true);
