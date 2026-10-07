/**
 * KA POS v3.0 - Supabase Cloud Synchronization & Realtime Engine
 */

class SupabaseService {
  constructor() {
    this.client = null;
    this.status = 'offline'; // 'online', 'syncing', 'offline'
    this.isFlushing = false;
    this.channel = null;
  }

  init() {
    const config = window.State.supabaseConfig;
    const isNetOnline = navigator.onLine !== false;

    if (config && config.url && config.anonKey) {
      try {
        if (window.supabase) {
          this.client = window.supabase.createClient(config.url, config.anonKey, {
            realtime: { params: { eventsPerSecond: 10 } }
          });
          this.testConnection();
          this.subscribeRealtime();
        } else {
          this.setStatus(isNetOnline ? 'online' : 'offline');
        }
      } catch (err) {
        console.error('[Supabase] Init error:', err);
        this.setStatus(isNetOnline ? 'online' : 'offline');
      }
    } else {
      // Standalone / Local PWA mode (Supabase cloud not yet configured)
      // When internet is available, show ONLINE and keep offline banner hidden
      this.setStatus(isNetOnline ? 'online' : 'offline');
    }

    // Network connectivity change listeners
    if (!this._networkListenersAdded) {
      this._onOnline = () => {
        if (this.client) {
          this.testConnection().then(() => this.syncOfflineQueue());
        } else {
          this.setStatus('online');
        }
      };
      this._onOffline = () => this.setStatus('offline');
      window.addEventListener('online', this._onOnline);
      window.addEventListener('offline', this._onOffline);
      this._networkListenersAdded = true;
    }
  }

  setStatus(newStatus) {
    this.status = newStatus;
    const badge = document.getElementById('global-sync-badge');
    const badgeText = document.getElementById('global-sync-text');
    const offlineBanner = document.getElementById('global-offline-banner');

    if (badge) {
      badge.className = `sync-pill ${newStatus}`;
    }
    if (badgeText) {
      if (newStatus === 'online') {
        badgeText.textContent = this.client ? 'CLOUD SYNC' : 'ONLINE';
      } else if (newStatus === 'syncing') {
        badgeText.textContent = 'SYNCING';
      } else {
        badgeText.textContent = 'OFFLINE';
      }
    }

    if (offlineBanner) {
      if (newStatus === 'offline') {
        const queueCount = (window.State.offlineQueue || []).length;
        const bannerTxt = document.getElementById('offline-banner-text');
        if (bannerTxt) {
          bannerTxt.textContent = `Mode Offline — Transaksi tersimpan di penyimpanan lokal (${queueCount} Nota Pending).`;
        }
        offlineBanner.classList.add('active');
      } else {
        offlineBanner.classList.remove('active');
      }
    }

    window.State.emit('sync:status', {
      status: this.status,
      queueCount: (window.State.offlineQueue || []).length
    });
  }

  async testConnection(customUrl, customKey) {
    const url = customUrl || (window.State.supabaseConfig ? window.State.supabaseConfig.url : null);
    const key = customKey || (window.State.supabaseConfig ? window.State.supabaseConfig.anonKey : null);
    const isNetOnline = navigator.onLine !== false;

    if (!url || !key || !window.supabase) {
      this.setStatus(isNetOnline ? 'online' : 'offline');
      return { 
        success: isNetOnline, 
        message: isNetOnline ? 'Koneksi internet aktif (Mode Penyimpanan Lokal Standalone).' : 'Tidak ada koneksi internet.' 
      };
    }

    try {
      this.setStatus('syncing');
      const testClient = window.supabase.createClient(url, key);
      const { data, error } = await testClient.from('categories').select('id').limit(1);

      if (error && error.code !== 'PGRST116') {
        throw error;
      }

      this.client = testClient;
      this.setStatus('online');
      return { success: true, message: 'Berhasil terhubung ke Supabase Cloud!' };
    } catch (err) {
      console.warn('[Supabase] Connection failed:', err);
      // Supabase server connection error, but internet may still be online
      this.setStatus(isNetOnline ? 'online' : 'offline');
      return { success: false, message: `Gagal terhubung ke Cloud: ${err.message || 'Cek URL & Anon Key'}` };
    }
  }

  // Realtime CDC Subscriptions
  subscribeRealtime() {
    if (!this.client) return;

    try {
      if (this.channel) {
        this.client.removeChannel(this.channel);
      }

      this.channel = this.client.channel('schema-db-changes')
        .on('postgres_changes', { event: '*', schema: 'public', table: 'inventory_items' }, payload => {
          this.handleInventoryChange(payload);
        })
        .subscribe((status) => {
          if (status === 'SUBSCRIBED') {
            console.log('[Supabase] Realtime CDC channel active');
          }
        });
    } catch (e) {
      console.warn('[Supabase] Realtime subscription error:', e);
    }
  }

  handleInventoryChange(payload) {
    if (payload.eventType === 'UPDATE' && payload.new) {
      const idx = window.State.inventory.findIndex(i => i.id === payload.new.id);
      if (idx > -1) {
        window.State.inventory[idx].stok = payload.new.current_stock;
        window.State.save(LS_KEYS.inv, window.State.inventory);
        window.State.emit('inventory:update', window.State.inventory);
      }
    }
  }

  // BUG-03 fix: langsung insert ke Supabase jika online, hanya queue jika offline
  async queueTransaction(trx) {
    if (this.status === 'online' && this.client) {
      try {
        const payload = {
          order_no: trx.no || `ORD-${Date.now()}`,
          order_type: trx.tipe || 'dine-in',
          table_no: '',
          cashier_id: trx.kasir || 'Kasir',
          payment_method: trx.metode || 'cash',
          subtotal: trx.subtotal || trx.total,
          discount_amount: trx.diskon || 0,
          total_amount: trx.total || 0,
          paid_amount: trx.bayar || trx.total,
          change_amount: trx.kembali || 0,
          status: 'completed',
          items: trx.items || [],
          created_at: new Date(trx.tgl || Date.now()).toISOString()
        };
        const { error } = await this.client.from('orders').insert([payload]);
        if (error) throw error;
        return; // berhasil, tidak perlu masuk queue
      } catch (err) {
        console.warn('[Supabase] Direct insert failed, falling back to queue:', err);
      }
    }
    // Offline atau insert gagal: masuk ke offline queue
    window.State.offlineQueue.push(trx);
    window.State.save(LS_KEYS.ofq, window.State.offlineQueue);
    this.setStatus(this.status);
  }

  // Flush queued transactions to Supabase
  async syncOfflineQueue() {
    if (this.isFlushing || !this.client || window.State.offlineQueue.length === 0) return;

    this.isFlushing = true;
    this.setStatus('syncing');

    try {
      while (window.State.offlineQueue.length > 0) {
        const item = window.State.offlineQueue[0];
        const payload = {
          order_no: item.no || `ORD-${Date.now()}`,
          order_type: item.tipe || 'dine-in',
          table_no: '',
          cashier_id: item.kasir || 'Kasir',
          payment_method: item.metode || 'cash',
          subtotal: item.subtotal || item.total,
          discount_amount: item.diskon || 0,
          total_amount: item.total || 0,
          paid_amount: item.bayar || item.total,
          change_amount: item.kembali || 0,
          status: 'completed',
          items: item.items || [],
          created_at: new Date(item.tgl || Date.now()).toISOString()
        };

        const { error } = await this.client.from('orders').insert([payload]);
        if (error) throw error;

        // Shift successfully pushed item
        window.State.offlineQueue.shift();
        window.State.save(LS_KEYS.ofq, window.State.offlineQueue);
      }

      this.setStatus('online');
      window.State.toast('Semua transaksi offline berhasil disinkronkan ke Cloud!', 'success');
    } catch (err) {
      console.warn('[Supabase] Flush error:', err);
      this.setStatus('offline');
    } finally {
      this.isFlushing = false;
    }
  }

  // Push all local products, categories, and inventory to cloud
  async pushLocalData() {
    if (!this.client) return { success: false, message: 'Supabase belum terhubung!' };

    this.setStatus('syncing');
    try {
      // 1. Categories
      const catRows = window.State.categories.map(c => ({
        id: c.id,
        name: c.nm,
        icon: c.emj,
        color: c.col,
        sort_order: c.ord || 0,
        is_active: c.on !== false
      }));
      await this.client.from('categories').upsert(catRows);

      // 2. Products
      const prodRows = window.State.products.map(p => ({
        id: p.id,
        name: p.nm,
        category: p.kat,
        price: p.hr,
        cost_price: p.md || 0,
        image_emoji: p.emj || '🍢',
        spice_level: p.lv || '',
        is_active: p.on !== false
      }));
      await this.client.from('products').upsert(prodRows);

      this.setStatus('online');
      return { success: true, message: 'Berhasil mengunggah master data ke Supabase Cloud!' };
    } catch (e) {
      this.setStatus('offline');
      return { success: false, message: `Gagal mengunggah data: ${e.message}` };
    }
  }
}

window.SupabaseService = new SupabaseService();
