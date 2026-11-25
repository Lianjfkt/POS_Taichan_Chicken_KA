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
        // BUG-A fix: gunakan window.LS_KEYS agar tidak ReferenceError
        window.State.save(window.LS_KEYS.inv, window.State.inventory);
        window.State.emit('inventory:update', window.State.inventory);
      }
    }
  }

  _buildOrderPayloads(trx) {
    const orderId = trx.no || trx.id || trx._tempId || (trx._tempId = `ORD-${Date.now()}`);
    const createdIso = new Date(trx.tgl || trx.created_at || Date.now()).toISOString();
    const nowIso = new Date().toISOString();
    const isVoid = trx.status === 'void';

    const orderPayload = {
      id: orderId,
      order_no: trx.no || trx.order_no || orderId,
      customer_name: trx.pelanggan || trx.customerName || trx.customer_name || null,
      customer_phone: trx.customerPhone || trx.customer_phone || null,
      order_type: trx.tipe || trx.order_type || 'dine-in',
      table_no: trx.meja || trx.table_no || '',
      subtotal: Number(trx.subtotal != null ? trx.subtotal : (trx.total || 0)),
      discount: Number(trx.diskon != null ? trx.diskon : (trx.discount || 0)),
      total: Number(trx.total || 0),
      payment_method: trx.metode || trx.payment_method || 'cash',
      status: isVoid ? 'void' : (trx.status || 'completed'),
      cashier_id: trx.kasir || trx.cashier_id || 'Kasir',
      notes: trx.catatan || trx.notes || '',
      void_reason: isVoid ? (trx.voidReason || trx.void_reason || '') : (trx.voidReason || null),
      void_by: isVoid ? (trx.voidBy || trx.void_by || '') : null,
      void_at: isVoid ? (trx.voidAt ? new Date(trx.voidAt).toISOString() : nowIso) : null,
      paid_at: trx.paid_at ? new Date(trx.paid_at).toISOString() : createdIso,
      created_at: createdIso,
      updated_at: nowIso
    };

    const itemPayloads = (trx.items || []).map((it, idx) => {
      const unitPrice = Number(it.hr != null ? it.hr : (it.price || 0));
      const qty = Number(it.qty != null ? it.qty : (it.quantity != null ? it.quantity : 1));
      return {
        id: `${orderId}_${idx + 1}`,
        order_id: orderId,
        product_id: String(it.id || it.productId || it.product_id || ''),
        product_name: it.nm || it.name || it.product_name || '',
        price: unitPrice,
        quantity: qty,
        spice_level: it.level || it.spiceLevel || it.spice_level || it.spice || '',
        notes: it.catatan || it.notes || it.mod || '',
        subtotal: Number(it.subtotal != null ? it.subtotal : (unitPrice * qty)),
        created_at: createdIso
      };
    });

    return { orderPayload, itemPayloads };
  }

  async _upsertOrderAndItems(trx) {
    const { orderPayload, itemPayloads } = this._buildOrderPayloads(trx);

    const { error: orderErr } = await this.client
      .from('orders')
      .upsert([orderPayload], { onConflict: 'id' });
    if (orderErr) throw orderErr;

    if (itemPayloads.length > 0) {
      const { error: itemsErr } = await this.client
        .from('order_items')
        .upsert(itemPayloads, { onConflict: 'id' });
      if (itemsErr) throw itemsErr;
    }
  }

  async _pushOrderStatusUpdate(orderNo, status, voidData = {}) {
    const updatePayload = {
      status: status,
      void_reason: voidData.reason || '',
      void_by: voidData.by || '',
      void_at: voidData.at ? new Date(voidData.at).toISOString() : new Date().toISOString(),
      updated_at: new Date().toISOString()
    };

    const { error } = await this.client.from('orders').update(updatePayload).eq('id', orderNo);
    if (error) {
      const res2 = await this.client.from('orders').update(updatePayload).eq('order_no', orderNo);
      if (res2.error) throw res2.error;
    }
  }

  // BUG-03 fix: langsung upsert relasional (orders + order_items) ke Supabase jika online, queue jika offline
  async queueTransaction(trx) {
    if (this.status === 'online' && this.client) {
      try {
        await this._upsertOrderAndItems(trx);
        return; // berhasil, tidak perlu masuk queue
      } catch (err) {
        console.warn('[Supabase] Direct insert failed, falling back to queue:', err);
      }
    }
    // Offline atau upsert gagal: masuk ke offline queue
    window.State.offlineQueue.push(trx);
    window.State.save(window.LS_KEYS.ofq, window.State.offlineQueue);
    this.setStatus(this.status);
  }

  // TASK 4: Sinkronisasi Status Void ke Supabase Cloud
  async updateOrderStatus(orderNo, status, voidData = {}) {
    // 1. Perbarui transaksi di offlineQueue jika masih antre
    let foundInQueue = false;
    if (window.State.offlineQueue && window.State.offlineQueue.length > 0) {
      window.State.offlineQueue.forEach(item => {
        if (item.no === orderNo || item.id === orderNo || item.order_no === orderNo) {
          item.status = status;
          item.voidReason = voidData.reason || '';
          item.voidBy = voidData.by || '';
          item.voidAt = voidData.at || Date.now();
          foundInQueue = true;
        }
      });
      window.State.save(window.LS_KEYS.ofq, window.State.offlineQueue);
    }

    // 2. Jika online, langsung update status nota di Supabase orders table
    if (this.status === 'online' && this.client) {
      try {
        await this._pushOrderStatusUpdate(orderNo, status, voidData);
        console.log(`[Supabase] Status nota ${orderNo} berhasil diupdate ke '${status}' di Cloud.`);
        return { success: true };
      } catch (err) {
        console.warn('[Supabase] Gagal update status nota ke cloud, masuk ke antrean offline:', err);
      }
    }

    // 3. Jika offline atau update gagal dan belum ada di queue, masukkan perintah update ke offlineQueue
    if (!foundInQueue) {
      window.State.offlineQueue.push({
        _action: 'update_status',
        no: orderNo,
        status: status,
        voidReason: voidData.reason || '',
        voidBy: voidData.by || '',
        voidAt: voidData.at || Date.now()
      });
      window.State.save(window.LS_KEYS.ofq, window.State.offlineQueue);
    }
    return { success: false, queued: true };
  }

  // Flush queued transactions to Supabase
  async syncOfflineQueue() {
    if (this.isFlushing || !this.client || window.State.offlineQueue.length === 0) return;

    this.isFlushing = true;
    this.setStatus('syncing');

    try {
      while (window.State.offlineQueue.length > 0) {
        const item = window.State.offlineQueue[0];

        // Periksa jika antrean merupakan aksi update status (Void)
        if (item._action === 'update_status') {
          await this._pushOrderStatusUpdate(item.no, item.status || 'void', {
            reason: item.voidReason || '',
            by: item.voidBy || '',
            at: item.voidAt
          });
          window.State.offlineQueue.shift();
          window.State.save(window.LS_KEYS.ofq, window.State.offlineQueue);
          continue;
        }

        await this._upsertOrderAndItems(item);

        // Shift successfully pushed item
        window.State.offlineQueue.shift();
        window.State.save(window.LS_KEYS.ofq, window.State.offlineQueue);
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
