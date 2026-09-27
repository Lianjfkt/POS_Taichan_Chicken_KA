-- ==============================================================================
-- KA POS v2.0 — Supabase PostgreSQL Database Schema
-- Tailored for F&B: Taichan & Chicken KA
-- Includes: Auth Profiles, Products, Inventory, Recipes (BOM), Shifts, Realtime KDS
-- ==============================================================================

-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- ------------------------------------------------------------------------------
-- 1. ENUMS & TYPES
-- ------------------------------------------------------------------------------
create type order_status as enum ('pending', 'cooking', 'ready', 'completed', 'void');
create type order_type as enum ('dine-in', 'take-away', 'delivery');
create type payment_method as enum ('cash', 'qris', 'transfer');
create type cash_flow_type as enum ('masuk', 'keluar');
create type user_role as enum ('owner', 'kasir', 'dapur');

-- ------------------------------------------------------------------------------
-- 2. USER PROFILES (Integrated with Supabase Auth)
-- ------------------------------------------------------------------------------
create table if not exists public.profiles (
  id uuid references auth.users on delete cascade primary key,
  full_name text not null,
  role user_role not null default 'kasir',
  phone text,
  is_active boolean default true,
  created_at timestamptz default timezone('utc'::text, now())
);

-- ------------------------------------------------------------------------------
-- 3. STORE SETTINGS
-- ------------------------------------------------------------------------------
create table if not exists public.store_settings (
  id int primary key default 1,
  store_name text not null default 'Taichan & Chicken KA',
  address text default 'Bandar Lampung',
  phone text default '08xxxxxxxxxx',
  receipt_footer text default 'Terima kasih sudah mampir! Selamat menikmati 🍢',
  paper_width int default 58,
  bank_name text default 'BCA',
  bank_account_no text default '1234567890',
  bank_account_name text default 'Taichan & Chicken KA',
  updated_at timestamptz default now()
);

-- ------------------------------------------------------------------------------
-- 4. CATEGORIES & PRODUCTS
-- ------------------------------------------------------------------------------
create table if not exists public.categories (
  id text primary key,
  name text not null,
  emoji text default '🍽️',
  color text default '#f97316',
  sort_order int default 0,
  is_active boolean default true,
  created_at timestamptz default now()
);

create table if not exists public.products (
  id text primary key,
  category_id text references public.categories(id) on delete set null,
  name text not null,
  price numeric(12,2) not null check (price >= 0),
  cost_price numeric(12,2) not null default 0,
  emoji text default '🍢',
  spice_level text default '',
  description text default '',
  is_active boolean default true,
  created_at timestamptz default now()
);

-- ------------------------------------------------------------------------------
-- 5. INVENTORY & RECIPES (Bill of Materials / BOM)
-- ------------------------------------------------------------------------------
create table if not exists public.inventory_items (
  id text primary key,
  name text not null,
  category text default 'Bahan Utama',
  unit text not null default 'pcs',
  stock numeric(12,2) not null default 0,
  min_stock numeric(12,2) not null default 0,
  cost_per_unit numeric(12,2) not null default 0,
  emoji text default '📦',
  updated_at timestamptz default now()
);

create table if not exists public.product_recipes (
  id uuid default uuid_generate_v4() primary key,
  product_id text references public.products(id) on delete cascade,
  inventory_item_id text references public.inventory_items(id) on delete cascade,
  quantity_required numeric(12,2) not null check (quantity_required > 0),
  created_at timestamptz default now(),
  unique(product_id, inventory_item_id)
);

create table if not exists public.inventory_mutations (
  id uuid default uuid_generate_v4() primary key,
  inventory_item_id text references public.inventory_items(id) on delete cascade,
  mutation_type text not null, -- 'masuk', 'keluar', 'penyesuaian', 'penjualan'
  quantity numeric(12,2) not null,
  stock_before numeric(12,2) not null,
  stock_after numeric(12,2) not null,
  notes text,
  created_by text default 'System',
  created_at timestamptz default now()
);

-- ------------------------------------------------------------------------------
-- 6. CASHIER SESSIONS (Shift & Cash Drawer Management)
-- ------------------------------------------------------------------------------
create table if not exists public.cashier_shifts (
  id text primary key,
  cashier_name text not null,
  cashier_id uuid references public.profiles(id) on delete set null,
  opened_at timestamptz default now(),
  closed_at timestamptz,
  starting_cash numeric(12,2) not null default 0,
  total_cash_sales numeric(12,2) default 0,
  total_non_cash_sales numeric(12,2) default 0,
  total_cash_in numeric(12,2) default 0,
  total_cash_out numeric(12,2) default 0,
  expected_cash_drawer numeric(12,2) default 0,
  actual_cash_counted numeric(12,2),
  cash_discrepancy numeric(12,2),
  status text not null default 'open' check (status in ('open', 'closed')),
  notes text
);

create table if not exists public.cash_logs (
  id uuid default uuid_generate_v4() primary key,
  shift_id text references public.cashier_shifts(id) on delete set null,
  type cash_flow_type not null,
  amount numeric(12,2) not null check (amount > 0),
  notes text not null,
  created_by text not null,
  created_at timestamptz default now()
);

-- ------------------------------------------------------------------------------
-- 7. ORDERS & TRANSACTIONS
-- ------------------------------------------------------------------------------
create table if not exists public.orders (
  id text primary key,
  shift_id text references public.cashier_shifts(id) on delete set null,
  order_type order_type not null default 'dine-in',
  table_number text default '',
  status order_status not null default 'pending',
  total_amount numeric(12,2) not null default 0,
  payment_method payment_method default 'cash',
  amount_received numeric(12,2) default 0,
  change_returned numeric(12,2) default 0,
  cashier_name text default 'Kasir',
  notes text default '',
  void_reason text,
  void_by text,
  void_at timestamptz,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists public.order_items (
  id uuid default uuid_generate_v4() primary key,
  order_id text references public.orders(id) on delete cascade,
  product_id text references public.products(id) on delete set null,
  product_name text not null,
  quantity int not null check (quantity > 0),
  unit_price numeric(12,2) not null,
  unit_cost numeric(12,2) default 0,
  subtotal numeric(12,2) not null,
  notes text default ''
);

-- ------------------------------------------------------------------------------
-- 8. TRIGGER: AUTO DEDUCT INVENTORY ON COMPLETED ORDER
-- ------------------------------------------------------------------------------
create or replace function public.fn_deduct_inventory_on_order()
returns trigger as $$
declare
  item_rec record;
  curr_stock numeric(12,2);
  new_stock numeric(12,2);
begin
  -- Only trigger when order status transitions to 'completed'
  if new.status = 'completed' and (old.status is null or old.status <> 'completed') then
    for item_rec in
      select 
        oi.product_id,
        oi.quantity as ordered_qty,
        pr.inventory_item_id,
        pr.quantity_required,
        (oi.quantity * pr.quantity_required) as total_deduct
      from public.order_items oi
      join public.product_recipes pr on pr.product_id = oi.product_id
      where oi.order_id = new.id
    loop
      -- Fetch current stock
      select stock into curr_stock 
      from public.inventory_items 
      where id = item_rec.inventory_item_id 
      for update;

      if found then
        new_stock := curr_stock - item_rec.total_deduct;
        
        -- Update inventory stock
        update public.inventory_items
        set stock = new_stock, updated_at = now()
        where id = item_rec.inventory_item_id;

        -- Record stock movement audit trail
        insert into public.inventory_mutations (
          inventory_item_id,
          mutation_type,
          quantity,
          stock_before,
          stock_after,
          notes,
          created_by,
          created_at
        ) values (
          item_rec.inventory_item_id,
          'penjualan',
          item_rec.total_deduct,
          curr_stock,
          new_stock,
          'Order #' || new.id,
          coalesce(new.cashier_name, 'System'),
          now()
        );
      end if;
    end loop;
  end if;
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists trg_deduct_inventory on public.orders;
create trigger trg_deduct_inventory
after update or insert on public.orders
for each row execute function public.fn_deduct_inventory_on_order();

-- ------------------------------------------------------------------------------
-- 9. REALTIME PUBLICATION (Kitchen Display System & POS Sync)
-- ------------------------------------------------------------------------------
alter publication supabase_realtime add table public.orders;
alter publication supabase_realtime add table public.order_items;
alter publication supabase_realtime add table public.cashier_shifts;

-- ------------------------------------------------------------------------------
-- 10. ROW LEVEL SECURITY (RLS) POLICIES
-- ------------------------------------------------------------------------------
alter table public.profiles enable row level security;
alter table public.categories enable row level security;
alter table public.products enable row level security;
alter table public.inventory_items enable row level security;
alter table public.product_recipes enable row level security;
alter table public.inventory_mutations enable row level security;
alter table public.cashier_shifts enable row level security;
alter table public.cash_logs enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;
alter table public.store_settings enable row level security;

-- Open policies for POS client (using Anon Key or Authenticated role)
create policy "Allow read access to all" on public.categories for select using (true);
create policy "Allow write access to authenticated and anon" on public.categories for all using (true);

create policy "Allow read access to all" on public.products for select using (true);
create policy "Allow write access to authenticated and anon" on public.products for all using (true);

create policy "Allow read access to all" on public.orders for select using (true);
create policy "Allow write access to authenticated and anon" on public.orders for all using (true);

create policy "Allow read access to all" on public.order_items for select using (true);
create policy "Allow write access to authenticated and anon" on public.order_items for all using (true);

create policy "Allow read access to all" on public.cashier_shifts for select using (true);
create policy "Allow write access to authenticated and anon" on public.cashier_shifts for all using (true);

create policy "Allow read access to all" on public.cash_logs for select using (true);
create policy "Allow write access to authenticated and anon" on public.cash_logs for all using (true);

create policy "Allow read access to all" on public.inventory_items for select using (true);
create policy "Allow write access to authenticated and anon" on public.inventory_items for all using (true);

create policy "Allow read access to all" on public.product_recipes for select using (true);
create policy "Allow write access to authenticated and anon" on public.product_recipes for all using (true);

create policy "Allow read access to all" on public.inventory_mutations for select using (true);
create policy "Allow write access to authenticated and anon" on public.inventory_mutations for all using (true);

create policy "Allow read access to all" on public.store_settings for select using (true);
create policy "Allow write access to authenticated and anon" on public.store_settings for all using (true);

-- ------------------------------------------------------------------------------
-- 11. SEED DATA (KA Taichan & Chicken Default Data)
-- ------------------------------------------------------------------------------
insert into public.store_settings (id, store_name, address, phone, receipt_footer)
values (1, 'Taichan & Chicken KA', 'Bandar Lampung', '081234567890', 'Terima kasih sudah mampir! Selamat menikmati 🍢')
on conflict (id) do nothing;

insert into public.categories (id, name, emoji, color, sort_order) values
('k1', 'Taichan', '🍢', '#f97316', 0),
('k2', 'Chicken', '🍗', '#ef4444', 1),
('k3', 'Minuman', '🧊', '#3b82f6', 2),
('k4', 'Nasi', '🍚', '#22c55e', 3),
('k5', 'Lainnya', '🍴', '#6b7280', 4)
on conflict (id) do nothing;

insert into public.products (id, category_id, name, price, cost_price, emoji, spice_level) values
('1', 'k1', 'Taichan Pedas Lv.3', 18000, 10000, '🍢', 'Lv.3'),
('2', 'k1', 'Taichan Original', 15000, 8500, '🍢', ''),
('3', 'k1', 'Taichan Jumbo Lv.2', 22000, 12000, '🍢', 'Lv.2'),
('4', 'k2', 'Chicken Crispy', 20000, 11000, '🍗', ''),
('5', 'k2', 'Chicken Pedas Lv.1', 20000, 11000, '🍗', 'Lv.1'),
('6', 'k3', 'Es Teh Manis', 5000, 1500, '🧊', ''),
('7', 'k3', 'Es Jeruk', 7000, 2500, '🍊', ''),
('8', 'k3', 'Air Mineral', 4000, 1200, '💧', ''),
('9', 'k4', 'Nasi Putih', 5000, 1800, '🍚', ''),
('10', 'k4', 'Nasi Goreng', 18000, 9000, '🍳', '')
on conflict (id) do nothing;

insert into public.inventory_items (id, name, category, unit, stock, min_stock, cost_per_unit, emoji) values
('1', 'Daging Ayam Fillet', 'Daging', 'kg', 8.5, 3.0, 32000, '🍗'),
('2', 'Bumbu Taichan Khas KA', 'Bumbu', 'pak', 12.0, 5.0, 8000, '🌶'),
('3', 'Minyak Goreng', 'Lainnya', 'liter', 4.0, 3.0, 18000, '🫙'),
('4', 'Cup Gelas Minuman', 'Kemasan', 'pcs', 200.0, 50.0, 500, '🥤'),
('5', 'Es Batu Kristal', 'Minuman', 'kg', 2.0, 5.0, 3000, '🧊')
on conflict (id) do nothing;

-- Sample Recipes (BOM) linking Taichan to raw ingredients:
-- 1 Porsi Taichan Original uses 0.15kg Chicken and 0.1 pak Bumbu
insert into public.product_recipes (product_id, inventory_item_id, quantity_required) values
('2', '1', 0.15),
('2', '2', 0.10),
('1', '1', 0.15),
('1', '2', 0.15),
('6', '4', 1.00),
('6', '5', 0.20)
on conflict do nothing;
