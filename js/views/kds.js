/**
 * KA POS v3.0 - Kitchen Display System (KDS) & Expediter Audio Chime
 */

class KDSView {
  constructor() {
    this.selectedStation = 'all'; // 'all', 'bakaran', 'gorengan', 'minuman'
    this.timerInterval = null;
    this.audioContext = null;
  }

  init() {
    this.bindEvents();
    this.render();

    // Start aging timer ticker (every 1s)
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => {
      this.updateAgingTimers();
    }, 1000);

    // Listen to new incoming orders
    window.State.on('kds:new_order', () => {
      this.playChime();
      this.render();
    });

    window.State.on(LS_KEYS.dp, () => {
      this.render();
    });
  }

  bindEvents() {
    // Station tabs
    document.querySelectorAll('.kds-tab').forEach(tab => {
      tab.onclick = () => {
        document.querySelectorAll('.kds-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        this.selectedStation = tab.getAttribute('data-station');
        this.render();
      };
    });

    // Sound test button
    const btnTestAudio = document.getElementById('btn-kds-test-audio');
    if (btnTestAudio) {
      btnTestAudio.onclick = () => this.playChime();
    }
  }

  playChime() {
    try {
      if (!this.audioContext) {
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
      }
      if (this.audioContext.state === 'suspended') {
        this.audioContext.resume();
      }

      // Two-tone bell chime (Ding-Dong)
      const now = this.audioContext.currentTime;

      const osc1 = this.audioContext.createOscillator();
      const gain1 = this.audioContext.createGain();
      osc1.type = 'sine';
      osc1.frequency.setValueAtTime(587.33, now); // D5
      gain1.gain.setValueAtTime(0.3, now);
      gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
      osc1.connect(gain1);
      gain1.connect(this.audioContext.destination);
      osc1.start(now);
      osc1.stop(now + 0.6);

      const osc2 = this.audioContext.createOscillator();
      const gain2 = this.audioContext.createGain();
      osc2.type = 'sine';
      osc2.frequency.setValueAtTime(880, now + 0.15); // A5
      gain2.gain.setValueAtTime(0.35, now + 0.15);
      gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
      osc2.connect(gain2);
      gain2.connect(this.audioContext.destination);
      osc2.start(now + 0.15);
      osc2.stop(now + 0.9);
    } catch (e) {
      console.warn('[KDS] Audio chime error:', e);
    }
  }

  render() {
    const grid = document.getElementById('kds-tickets-grid');
    if (!grid) return;

    let orders = window.State.kitchenOrders.filter(o => o.status !== 'selesai');

    // Filter by cooking station if not 'all'
    if (this.selectedStation !== 'all') {
      orders = orders.filter(order => {
        return (order.items || []).some(item => {
          const cat = (item.kat || '').toLowerCase();
          const nm  = (item.nm || '').toLowerCase();
          if (this.selectedStation === 'bakaran')  return cat.includes('taichan') || nm.includes('taichan');
          if (this.selectedStation === 'gorengan') return cat.includes('chicken') || nm.includes('chicken') || nm.includes('crispy');
          if (this.selectedStation === 'minuman')  return cat.includes('minuman') || nm.includes('es ') || nm.includes('teh');
          return true;
        });
      });
    }

    if (orders.length === 0) {
      grid.innerHTML = `
        <div style="grid-column:1/-1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:64px 20px;color:var(--secondary);opacity:0.7">
          <span class="material-symbols-outlined" style="font-size:56px;margin-bottom:12px;color:var(--tertiary)">soup_kitchen</span>
          <div style="font-size:18px;font-weight:700;color:var(--on-surface)">Semua Pesanan Selesai!</div>
          <div style="font-size:13px;margin-top:4px">Dapur sedang tenang. Pesanan baru akan berbunyi otomatis.</div>
        </div>
      `;
      return;
    }

    grid.innerHTML = orders.map(order => {
      const elapsedMs = Date.now() - (order.waktu || Date.now());
      const elapsedMin = Math.floor(elapsedMs / 60000);
      const elapsedSec = Math.floor((elapsedMs % 60000) / 1000);
      const timeStr = `${String(elapsedMin).padStart(2, '0')}:${String(elapsedSec).padStart(2, '0')}`;

      let urgencyClass = 'green';
      let cardClass = '';
      if (elapsedMin >= 12) {
        urgencyClass = 'red';
        cardClass = 'urgent';
      } else if (elapsedMin >= 5) {
        urgencyClass = 'amber';
        cardClass = 'warning';
      }

      return `
        <div class="kds-ticket ${cardClass}" id="ticket-${order.id}">
          <div class="ticket-head">
            <div>
              <div class="ticket-table">${order.meja ? `MEJA ${order.meja}` : 'TAKE AWAY'}</div>
              <div style="font-size:11px;color:var(--secondary)">#${order.id} • ${order.tipe || 'Dine-In'}</div>
            </div>
            <div class="ticket-timer ${urgencyClass}" data-created="${order.waktu || Date.now()}">
              <span class="material-symbols-outlined" style="font-size:14px">timer</span>
              <span class="timer-text">${timeStr}</span>
            </div>
          </div>
          <div class="ticket-items">
            ${(order.items || []).map(item => `
              <div class="ticket-row">
                <div>
                  <span>${item.nm}</span>
                  ${item.mod ? `<span class="ticket-mod-tag">🔥 ${item.mod}</span>` : ''}
                </div>
                <span class="font-mono" style="font-weight:700;padding-left:8px">${item.qty}x</span>
              </div>
            `).join('')}
          </div>
          <div class="ticket-foot">
            <button class="btn-done-order" onclick="window.KDSView.completeOrder('${order.id}')">
              <span class="material-symbols-outlined">check_circle</span>
              <span>Siap Saji / Selesai</span>
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  updateAgingTimers() {
    document.querySelectorAll('.ticket-timer').forEach(el => {
      const created = Number(el.getAttribute('data-created')) || Date.now();
      const elapsedMs = Date.now() - created;
      const elapsedMin = Math.floor(elapsedMs / 60000);
      const elapsedSec = Math.floor((elapsedMs % 60000) / 1000);
      const textEl = el.querySelector('.timer-text');
      if (textEl) {
        textEl.textContent = `${String(elapsedMin).padStart(2, '0')}:${String(elapsedSec).padStart(2, '0')}`;
      }

      el.className = 'ticket-timer';
      const ticketCard = el.closest('.kds-ticket');

      if (elapsedMin >= 12) {
        el.classList.add('red');
        if (ticketCard && !ticketCard.classList.contains('urgent')) {
          ticketCard.className = 'kds-ticket urgent';
        }
      } else if (elapsedMin >= 5) {
        el.classList.add('amber');
        if (ticketCard && !ticketCard.classList.contains('warning')) {
          ticketCard.className = 'kds-ticket warning';
        }
      } else {
        el.classList.add('green');
      }
    });
  }

  completeOrder(orderId) {
    const order = window.State.kitchenOrders.find(o => o.id === orderId);
    if (!order) return;

    order.status = 'selesai';
    window.State.save(LS_KEYS.dp, window.State.kitchenOrders);
    this.render();
    window.State.toast(`Pesanan #${orderId} selesai dimasak!`, 'success');
  }
}

window.KDSView = new KDSView();
