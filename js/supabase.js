/**
 * KA POS v3.0 - Supabase Cloud Synchronization & Realtime Engine
 */

class SupabaseService {
  constructor() {
    this.client = null;
    this.status = 'offline'; // 'online', 'syncing', 'offline'
    this.isFlushing = false;
    this.channel = null;
    this.lastSyncTime = null;
    this._realtimeOrderTimer = null;
    this._realtimeMenuTimer = null;
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
      const testClient = window.supabase.createClient(url, key, {
        realtime: { params: { eventsPerSecond: 10 } }
      });
      const { data, error } = await testClient.from('categories').select('id').limit(1);

      if (error && error.code !== 'PGRST116') {
        throw error;
      }

      if (this.client && this.channel) {
        this.cleanup();
      }
      this.client = testClient;
      this.setStatus('online');
      this.subscribeRealtime();
      return { success: true, message: 'Berhasil terhubung ke Supabase Cloud!' };
    } catch (err) {
      console.warn('[Supabase] Connection failed:', err);
      // Supabase server connection error, but internet may still be online
      this.setStatus(isNetOnline ? 'online' : 'offline');
      return { success: false, message: `Gagal terhubung ke Cloud: ${err.message || 'Cek URL & Anon Key'}` };
    }
  }

  _clearRealtimeTimers() {
    if (this._realtimeOrderTimer) {
      clearTimeout(this._realtimeOrderTimer);
      this._realtimeOrderTimer = null;
    }
    if (this._realtimeMenuTimer) {
      clearTimeout(this._realtimeMenuTimer);
      this._realtimeMenuTimer = null;
    }
  }

  cleanup() {
    this._clearRealtimeTimers();
    if (this.client && this.channel) {
      try {
        this.client.removeChannel(this.channel);
      } catch (e) {
        console.warn('[Supabase] Realtime cleanup error:', e);
      }
      this.channel = null;
    }
  }

  // Realtime CDC Subscriptions
  subscribeRealtime() {
    if (!this.client) return;

    this._clearRealtimeTimers();

    try {
      if (this.channel) {
        this.client.removeChannel(this.channel);
        this.channel = null;
      }

      this.channel = this.client.channel('public-db-changes')
        .on('postgres_changes', { event: '*', schema: 'public', table: 'orders' }, payload => {
          this.handleOrderChange(payload);
        })
        .on('postgres_changes', { event: '*', schema: 'public', table: 'products' }, payload => {
          this.handleProductChange(payload);
        })
        .on('postgres_changes', { event: '*', schema: 'public', table: 'categories' }, payload => {
          this.handleProductChange(payload);
        })
        .subscribe((status) => {
          if (status === 'SUBSCRIBED') {
            console.log('[Supabase] Realtime CDC active for orders, products & categories');
          }
        });
    } catch (e) {
      console.warn('[Supabase] Realtime subscription error:', e);
    }
  }

  _scheduleRealtimeSync(timerKey, syncFn, label) {
    if (this[timerKey]) {
      clearTimeout(this[timerKey]);
    }
    this[timerKey] = setTimeout(() => {
      this[timerKey] = null;
      Promise.resolve(syncFn()).catch(err => {
        console.warn(`[Supabase] Realtime ${label} error:`, err);
      });
    }, 500);
  }

  /**
   * Handle realtime INSERT/UPDATE on orders with debounce so order_items
   * written immediately after the order header are pulled together.
   */
  handleOrderChange(payload) {
    if (!payload || typeof payload !== 'object' || payload.eventType === 'DELETE') return;
    this._scheduleRealtimeSync('_realtimeOrderTimer', () => this.pullOrders(20), 'pullOrders');
  }

  /**
   * Handle realtime INSERT/UPDATE on products/categories with debounce so
   * BOM recipes, tusuk metadata, and category mappings remain consistent.
   */
  handleProductChange(payload) {
    if (!payload || typeof payload !== 'object' || payload.eventType === 'DELETE') return;
    this._scheduleRealtimeSync('_realtimeMenuTimer', () => this.pullCategoriesAndProducts(), 'pullCategoriesAndProducts');
  }

  _toIsoString(val, fallbackIso) {
    if (val !== undefined && val !== null && val !== '') {
      const d = new Date(val);
      if (!Number.isNaN(d.getTime())) return d.toISOString();
    }
    return fallbackIso || new Date().toISOString();
  }

  _formatVoidReason(reason, by) {
    const r = (reason != null ? String(reason) : '').trim();
    const b = (by != null ? String(by) : '').trim();
    if (r && b) return `${r} (oleh ${b})`;
    if (r) return r;
    if (b) return `Void oleh ${b}`;
    return '';
  }

  _buildOrderPayloads(trx) {
    if (!trx || typeof trx !== 'object') {
      throw new Error('Invalid transaction payload');
    }
    const orderId = trx.no || trx.id || trx._tempId || (trx._tempId = `ORD-${Date.now()}`);
    const createdSource = (trx.tgl != null && trx.tgl !== '') ? trx.tgl : trx.created_at;
    const createdIso = this._toIsoString(createdSource);
    const isVoid = trx.status === 'void';

    const orderPayload = {
      id: orderId,
      order_no: trx.no || trx.order_no || orderId,
      customer_name: trx.pelanggan || trx.customerName || trx.customer_name || null,
      customer_phone: trx.customerPhone || trx.customer_phone || null,
      order_type: trx.tipe || trx.order_type || 'dine-in',
      table_no: trx.meja || trx.table_no || '',
      subtotal: Number(trx.subtotal != null ? trx.subtotal : (trx.total || 0)) || 0,
      discount: Number(trx.diskon != null ? trx.diskon : (trx.discount || 0)) || 0,
      total: Number(trx.total || 0) || 0,
      payment_method: trx.metode || trx.payment_method || 'cash',
      status: isVoid ? 'void' : (trx.status || 'completed'),
      cashier_id: trx.kasir || trx.cashier_id || 'Kasir',
      notes: trx.catatan || trx.notes || '',
      void_reason: isVoid ? this._formatVoidReason(trx.voidReason || trx.void_reason, trx.voidBy || trx.void_by) : null,
      paid_at: this._toIsoString(trx.paid_at, createdIso),
      created_at: createdIso
    };

    const items = Array.isArray(trx.items) ? trx.items.filter(it => it && typeof it === 'object') : [];
    const itemPayloads = items.map((it, idx) => {
      const unitPrice = Number(it.hr != null ? it.hr : (it.price || 0)) || 0;
      const qty = Number(it.qty != null ? it.qty : (it.quantity != null ? it.quantity : 1)) || 0;
      return {
        id: `${orderId}_${idx + 1}`,
        order_id: orderId,
        product_id: String(it.id || it.productId || it.product_id || ''),
        product_name: it.nm || it.name || it.product_name || '',
        price: unitPrice,
        quantity: qty,
        spice_level: it.level || it.spiceLevel || it.spice_level || it.spice || '',
        notes: it.catatan || it.notes || it.mod || '',
        subtotal: Number(it.subtotal != null ? it.subtotal : (unitPrice * qty)) || 0,
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
      void_reason: status === 'void'
        ? this._formatVoidReason(voidData.reason, voidData.by)
        : (voidData.reason || null)
    };

    const { data, error } = await this.client
      .from('orders')
      .update(updatePayload)
      .eq('id', orderNo)
      .select('id');
    if (error || !data || data.length === 0) {
      const res2 = await this.client
        .from('orders')
        .update(updatePayload)
        .eq('order_no', orderNo)
        .select('id');
      if (res2.error) throw res2.error;
      if (!res2.data || res2.data.length === 0) {
        throw new Error(`Order ${orderNo} not found on remote`);
      }
    }
  }

  // BUG-03 fix: langsung upsert relasional (orders + order_items) ke Supabase jika online, queue jika offline
  async queueTransaction(trx) {
    if (!trx || typeof trx !== 'object') return;
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
    if (!orderNo) return { success: false };

    // 1. Perbarui transaksi di offlineQueue jika masih antre
    let foundInQueue = false;
    if (window.State.offlineQueue && window.State.offlineQueue.length > 0) {
      window.State.offlineQueue.forEach(item => {
        if (item && (item.no === orderNo || item.id === orderNo || item.order_no === orderNo)) {
          item.status = status;
          item.voidReason = voidData.reason || '';
          item.voidBy = voidData.by || '';
          item.voidAt = voidData.at || Date.now();
          foundInQueue = true;
        }
      });
      if (foundInQueue) {
        window.State.save(window.LS_KEYS.ofq, window.State.offlineQueue);
      }
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
  async syncOfflineQueue(options = {}) {
    const skipStatusUpdate = Boolean(options && options.skipStatusUpdate);
    if (!this.client || this.isFlushing) return false;
    if (!Array.isArray(window.State.offlineQueue) || window.State.offlineQueue.length === 0) {
      return true;
    }

    this.isFlushing = true;
    if (!skipStatusUpdate) {
      this.setStatus('syncing');
    }

    try {
      while (window.State.offlineQueue.length > 0) {
        const item = window.State.offlineQueue[0];
        if (!item || typeof item !== 'object') {
          window.State.offlineQueue.shift();
          window.State.save(window.LS_KEYS.ofq, window.State.offlineQueue);
          continue;
        }

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

      if (!skipStatusUpdate) {
        this.setStatus('online');
      }
      window.State.toast('Semua transaksi offline berhasil disinkronkan ke Cloud!', 'success');
      return true;
    } catch (err) {
      console.warn('[Supabase] Flush error:', err);
      if (!skipStatusUpdate) {
        this.setStatus('offline');
      }
      return false;
    } finally {
      this.isFlushing = false;
    }
  }

  // Push all local products and categories to cloud
  async pushLocalData() {
    if (!this.client) return { success: false, message: 'Supabase belum terhubung!' };

    this.setStatus('syncing');
    try {
      const categories = Array.isArray(window.State && window.State.categories)
        ? window.State.categories.filter(c => c && typeof c === 'object')
        : [];
      const products = Array.isArray(window.State && window.State.products)
        ? window.State.products.filter(p => p && typeof p === 'object')
        : [];

      // 1. Categories
      const catRows = categories.map(c => ({
        id: String(c.id),
        name: c.nm || '',
        emoji: c.emj || '🍽️',
        color: c.col || '#0B5C73',
        sort_order: Number(c.ord) || 0,
        is_active: c.on !== false
      }));
      if (catRows.length > 0) {
        const { error: catErr } = (await this.client
          .from('categories')
          .upsert(catRows, { onConflict: 'id' })) || {};
        if (catErr) throw catErr;
      }

      // 2. Products
      const prodRows = products.map(p => {
        // Cari ID kategori dari c.nm jika p.kat berupa nama kategori; null jika tidak cocok agar FK aman
        const catObj = p.kat != null && p.kat !== ''
          ? categories.find(c => c.nm === p.kat || String(c.id) === String(p.kat))
          : null;
        const catId = catObj && catObj.id != null ? String(catObj.id) : null;
        return {
          id: String(p.id),
          category_id: catId,
          name: p.nm || '',
          price: Number(p.hr) || 0,
          cost_price: Number(p.md) || 0,
          emoji: p.emj || '🍢',
          spice_level: p.lv || '',
          description: p.desc || '',
          is_active: p.on !== false
        };
      });
      if (prodRows.length > 0) {
        const { error: prodErr } = (await this.client
          .from('products')
          .upsert(prodRows, { onConflict: 'id' })) || {};
        if (prodErr) throw prodErr;
      }

      this.setStatus('online');
      return { success: true, message: 'Berhasil mengunggah master data ke Supabase Cloud!' };
    } catch (e) {
      this.setStatus('offline');
      return { success: false, message: `Gagal mengunggah data: ${e && e.message ? e.message : String(e)}` };
    }
  }

  _normalizeId(id) {
    if (id === null || id === undefined || id === '') return id;
    const str = String(id);
    return /^\d+$/.test(str) ? Number(str) : str;
  }

  /**
   * Pull master categories & products from Supabase Cloud (Cloud-Wins),
   * while preserving local BOM recipes and tusuk metadata.
   */
  async pullCategoriesAndProducts() {
    if (!this.client) return false;
    try {
      const { data: cats, error: catErr } = await this.client
        .from('categories')
        .select('*')
        .order('sort_order', { ascending: true });
      if (catErr) throw catErr;

      const { data: prods, error: prodErr } = await this.client
        .from('products')
        .select('*')
        .order('name', { ascending: true });
      if (prodErr) throw prodErr;

      if (Array.isArray(cats) && cats.length > 0) {
        window.State.categories = cats.map(c => ({
          id: c.id,
          nm: c.name,
          emj: c.emoji || '🍽️',
          col: c.color || '#0B5C73',
          ord: Number(c.sort_order) || 0,
          on: c.is_active !== false
        }));
        window.State.save(window.LS_KEYS.kat, window.State.categories);
        window.State.emit('categories:update', window.State.categories);
      }

      if (Array.isArray(prods) && prods.length > 0) {
        const existingProducts = Array.isArray(window.State.products) ? window.State.products : [];
        const categories = Array.isArray(window.State.categories) ? window.State.categories : [];
        window.State.products = prods.map(p => {
          const cat = categories.find(c => c && String(c.id) === String(p.category_id));
          const existing = existingProducts.find(ep => ep && String(ep.id) === String(p.id));
          const mapped = {
            id: existing && existing.id !== undefined ? existing.id : this._normalizeId(p.id),
            nm: p.name,
            kat: cat ? cat.nm : 'Lainnya',
            hr: Number(p.price || 0),
            md: Number(p.cost_price || 0),
            emj: p.emoji || '🍢',
            lv: p.spice_level || '',
            desc: p.description || '',
            on: p.is_active !== false,
            bom: existing && Array.isArray(existing.bom) ? existing.bom : []
          };
          if (existing && existing.tusuk !== undefined) {
            mapped.tusuk = existing.tusuk;
          }
          return mapped;
        });
        window.State.save(window.LS_KEYS.prod, window.State.products);
        window.State.emit('products:update', window.State.products);
      }

      return true;
    } catch (err) {
      console.warn('[Supabase] pullCategoriesAndProducts error:', err);
      return false;
    }
  }

  /**
   * Pull recent orders and order_items from Supabase Cloud, merge with local
   * transactions without duplicating or overwriting pending offline void updates.
   */
  async pullOrders(limit = 200) {
    if (!this.client) return false;
    try {
      const { data: remoteOrders, error: orderErr } = await this.client
        .from('orders')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(limit);
      if (orderErr) throw orderErr;
      if (!remoteOrders || remoteOrders.length === 0) return true;

      const orderIds = remoteOrders.map(o => o.id).filter(Boolean);
      const remoteItems = [];
      const CHUNK_SIZE = 50;
      for (let i = 0; i < orderIds.length; i += CHUNK_SIZE) {
        const chunk = orderIds.slice(i, i + CHUNK_SIZE);
        const { data: itemsData, error: itemErr } = await this.client
          .from('order_items')
          .select('*')
          .in('order_id', chunk);
        if (itemErr) throw itemErr;
        if (Array.isArray(itemsData) && itemsData.length > 0) {
          remoteItems.push(...itemsData);
        }
      }

      const itemsByOrder = {};
      remoteItems.forEach(it => {
        if (!it || !it.order_id) return;
        if (!itemsByOrder[it.order_id]) itemsByOrder[it.order_id] = [];
        const spice = (it.spice_level != null ? String(it.spice_level) : '').trim();
        const notes = (it.notes != null ? String(it.notes) : '').trim();
        const modParts = [];
        if (spice) modParts.push(spice);
        if (notes && notes !== spice) modParts.push(notes);
        itemsByOrder[it.order_id].push({
          id: this._normalizeId(it.product_id),
          nm: it.product_name,
          hr: Number(it.price || 0),
          qty: Number(it.quantity || 1),
          level: it.spice_level || '',
          catatan: it.notes || '',
          mod: modParts.join(', '),
          subtotal: Number(it.subtotal || 0)
        });
      });

      let mergedList = Array.isArray(window.State.transactions) ? [...window.State.transactions] : [];
      const queue = Array.isArray(window.State.offlineQueue) ? window.State.offlineQueue : [];

      remoteOrders.forEach(ro => {
        if (!ro) return;
        const key = ro.order_no || ro.id;
        const matchKeys = [key, ro.id]
          .filter(v => v !== undefined && v !== null && v !== '')
          .map(String);

        const isMatchingLocal = (t) => {
          if (!t || typeof t !== 'object') return false;
          const tNo = t.no !== undefined && t.no !== null && t.no !== '' ? String(t.no) : null;
          const tId = t.id !== undefined && t.id !== null && t.id !== '' ? String(t.id) : null;
          return (tNo !== null && matchKeys.includes(tNo)) || (tId !== null && matchKeys.includes(tId));
        };

        const existingIdx = mergedList.findIndex(isMatchingLocal);
        const existing = existingIdx > -1 ? mergedList[existingIdx] : null;

        const pendingVoid = queue.find(q => {
          if (!q || typeof q !== 'object') return false;
          if (q._action !== 'update_status' && q.status !== 'void') return false;
          const qVals = [q.no, q.id, q.order_no]
            .filter(v => v !== undefined && v !== null && v !== '')
            .map(String);
          return qVals.some(v => matchKeys.includes(v));
        });

        const hasPendingStatusUpdate = Boolean(pendingVoid);
        const resolvedStatus = hasPendingStatusUpdate
          ? (pendingVoid.status || (existing && existing.status) || 'void')
          : (ro.status || 'completed');
        const resolvedVoidReason = hasPendingStatusUpdate
          ? (pendingVoid.voidReason || (existing && existing.voidReason) || ro.void_reason || '')
          : (ro.void_reason || (existing ? existing.voidReason : '') || '');
        const resolvedVoidBy = hasPendingStatusUpdate
          ? (pendingVoid.voidBy || (existing && existing.voidBy) || '')
          : (existing && existing.voidBy ? existing.voidBy : '');
        const resolvedVoidAt = hasPendingStatusUpdate
          ? (pendingVoid.voidAt || (existing ? existing.voidAt : undefined))
          : (existing ? existing.voidAt : undefined);

        const rawItems = itemsByOrder[ro.id];
        const items = rawItems
          ? rawItems.map((it, idx) => {
              const existingItems = existing && Array.isArray(existing.items) ? existing.items : [];
              const existingItem = existingItems[idx] && String(existingItems[idx].id) === String(it.id)
                ? existingItems[idx]
                : existingItems.find(ei => ei && String(ei.id) === String(it.id) && (ei.mod || '') === (it.mod || ''))
                  || existingItems.find(ei => ei && String(ei.id) === String(it.id));
              const mergedItem = { ...it };
              if (existingItem && existingItem.emj) {
                mergedItem.emj = existingItem.emj;
              }
              return mergedItem;
            })
          : (existing && Array.isArray(existing.items) ? existing.items : []);
        const createdMs = ro.created_at ? new Date(ro.created_at).getTime() : NaN;
        const tgl = !Number.isNaN(createdMs)
          ? createdMs
          : (existing && existing.tgl ? existing.tgl : Date.now());

        const mergedTrx = {
          id: ro.id,
          no: key,
          tgl,
          pelanggan: ro.customer_name || 'Pelanggan Umum',
          customerPhone: ro.customer_phone || '',
          tipe: ro.order_type || 'dine-in',
          meja: ro.table_no || '',
          subtotal: Number(ro.subtotal != null ? ro.subtotal : (ro.total || 0)) || 0,
          diskon: Number(ro.discount || 0) || 0,
          total: Number(ro.total || 0) || 0,
          bayar: Number(existing && existing.bayar != null ? existing.bayar : (ro.total || 0)) || 0,
          kembali: Number(existing && existing.kembali != null ? existing.kembali : 0) || 0,
          metode: ro.payment_method || 'cash',
          status: resolvedStatus,
          kasir: ro.cashier_id || 'Kasir',
          catatan: ro.notes || (existing ? existing.catatan : '') || '',
          voidReason: resolvedVoidReason,
          voidBy: resolvedVoidBy,
          items
        };
        if (resolvedVoidAt !== undefined) {
          mergedTrx.voidAt = resolvedVoidAt;
        }
        if (existing && existing.voidNotes) {
          mergedTrx.voidNotes = existing.voidNotes;
        }

        if (existingIdx > -1) {
          mergedList[existingIdx] = mergedTrx;
          mergedList = mergedList.filter((t, idx) => idx === existingIdx || !isMatchingLocal(t));
        } else {
          mergedList.push(mergedTrx);
        }
      });

      const updatedTransactions = mergedList
        .filter(Boolean)
        .sort((a, b) => (b.tgl || 0) - (a.tgl || 0));

      window.State.transactions = updatedTransactions;
      window.State.save(window.LS_KEYS.trx, updatedTransactions);
      window.State.emit('transactions:update', updatedTransactions);
      return true;
    } catch (err) {
      console.warn('[Supabase] pullOrders error:', err);
      return false;
    }
  }

  /**
   * Pull customers from Supabase Cloud and merge into window.State.customers
   * by id (with phone fallback), preserving local-only records.
   */
  async pullCustomers() {
    if (!this.client) return false;
    try {
      const { data: remoteCustomers, error } = await this.client
        .from('customers')
        .select('*')
        .order('created_at', { ascending: false });
      if (error) throw error;
      if (!remoteCustomers || remoteCustomers.length === 0) return true;

      const localCustomers = Array.isArray(window.State.customers) ? window.State.customers : [];
      const merged = localCustomers.map(c => ({ ...c }));

      remoteCustomers.forEach(rc => {
        if (!rc) return;
        const remotePhone = (rc.phone || '').trim();
        const existingIdx = merged.findIndex(lc => {
          if (!lc) return false;
          if (rc.id != null && lc.id != null && String(lc.id) === String(rc.id)) return true;
          if (remotePhone && remotePhone !== '-' && (lc.hp || '').trim() === remotePhone) return true;
          return false;
        });
        const existing = existingIdx > -1 ? merged[existingIdx] : null;
        const createdMs = rc.created_at ? new Date(rc.created_at).getTime() : NaN;

        const mapped = {
          id: rc.id || (existing ? existing.id : `CUST-${Date.now()}`),
          nm: rc.name || (existing ? existing.nm : 'Pelanggan'),
          hp: rc.phone || (existing ? existing.hp : '-'),
          poin: Number(rc.points != null ? rc.points : (existing ? existing.poin : 0)) || 0,
          trxCount: Number(rc.visit_count != null ? rc.visit_count : (existing ? existing.trxCount : 0)) || 0,
          totalSpend: Number(rc.total_spent != null ? rc.total_spent : (existing ? existing.totalSpend : 0)) || 0,
          joined: !Number.isNaN(createdMs) ? createdMs : (existing && existing.joined ? existing.joined : Date.now())
        };

        if (existingIdx > -1) {
          merged[existingIdx] = { ...existing, ...mapped };
        } else {
          merged.push(mapped);
        }
      });

      window.State.customers = merged;
      window.State.save(window.LS_KEYS.cust, merged);
      window.State.emit('customers:updated', merged);
      return true;
    } catch (err) {
      console.warn('[Supabase] pullCustomers error:', err);
      return false;
    }
  }

  /**
   * Orchestrate full 2-way sync: flush pending offline queue, then pull
   * categories/products, orders, and customers from Supabase Cloud.
   */
  async syncAll() {
    if (!this.client) return { success: false, message: 'Supabase belum terhubung.' };
    this.setStatus('syncing');
    try {
      // 1. Push pending offline orders / status updates
      const queueOk = await this.syncOfflineQueue({ skipStatusUpdate: true });
      // 2. Pull latest catalog menu
      const menuOk = await this.pullCategoriesAndProducts();
      // 3. Pull transactions (from Android & Web)
      const ordersOk = await this.pullOrders();
      // 4. Pull customers
      const customersOk = await this.pullCustomers();

      const failed = [];
      if (!queueOk) failed.push('Antrean Offline');
      if (!menuOk) failed.push('Menu');
      if (!ordersOk) failed.push('Transaksi');
      if (!customersOk) failed.push('Pelanggan');

      const isNetOnline = typeof navigator === 'undefined' || navigator.onLine !== false;
      this.setStatus(isNetOnline ? 'online' : 'offline');

      const syncTime = Date.now();
      if (failed.length === 0) {
        this.lastSyncTime = syncTime;
        window.State.emit('sync:complete', { time: syncTime, success: true });
        return { success: true, message: 'Sinkronisasi 2-arah berhasil!' };
      }

      const someSucceeded = queueOk || menuOk || ordersOk || customersOk;
      window.State.emit('sync:complete', { time: syncTime, success: false, failed });
      return {
        success: false,
        message: someSucceeded
          ? `Sinkronisasi sebagian gagal (${failed.join(', ')}).`
          : `Sinkronisasi gagal (${failed.join(', ')}).`
      };
    } catch (err) {
      const isNetOnline = typeof navigator === 'undefined' || navigator.onLine !== false;
      this.setStatus(isNetOnline ? 'online' : 'offline');
      return { success: false, message: `Sinkronisasi gagal: ${err && err.message ? err.message : String(err)}` };
    }
  }
}

window.SupabaseService = new SupabaseService();
