/**
 * KA POS v3.0 - Application Bootstrap & View Router
 */

class AppRouter {
  constructor() {
    this.currentView = 'pos';
    this.views = ['pos', 'shift', 'kds', 'inventory', 'reports', 'settings'];
  }

  init() {
    this.bindNavigation();
    
    // Initialize all modular controllers
    if (window.AuthView) window.AuthView.init();
    if (window.POSView) window.POSView.init();
    if (window.PaymentView) window.PaymentView.init();
    if (window.KDSView) window.KDSView.init();
    if (window.ShiftView) window.ShiftView.init();
    if (window.InventoryView) window.InventoryView.init();
    if (window.ReportsView) window.ReportsView.init();
    if (window.SettingsView) window.SettingsView.init();

    // Setup global modal backdrop closers
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
          overlay.classList.remove('open');
        }
      });
    });

    // Close buttons inside modals
    document.querySelectorAll('.modal-close, .btn-modal-cancel').forEach(btn => {
      btn.onclick = () => {
        const overlay = btn.closest('.modal-overlay');
        if (overlay) overlay.classList.remove('open');
      };
    });

    // Header logout trigger
    const btnLogout = document.getElementById('header-btn-logout');
    if (btnLogout) {
      btnLogout.onclick = () => {
        if (confirm('Yakin ingin keluar dari akun kasir/owner?')) {
          if (window.AuthView) window.AuthView.logout();
        }
      };
    }
  }

  bindNavigation() {
    // Desktop Sidebar Links
    document.querySelectorAll('.nav-item').forEach(item => {
      item.onclick = (e) => {
        e.preventDefault();
        const targetView = item.getAttribute('data-view');
        if (targetView) this.navigate(targetView);
      };
    });

    // Mobile Dock Navigation Links
    document.querySelectorAll('.mobile-nav-item').forEach(item => {
      item.onclick = (e) => {
        e.preventDefault();
        const targetView = item.getAttribute('data-view');
        if (targetView) this.navigate(targetView);
      };
    });
  }

  navigate(viewName) {
    if (!this.views.includes(viewName)) return;

    // Check RBAC permissions for Owner views
    const ownerOnlyViews = ['inventory', 'reports', 'settings'];
    const activeUser = window.State.currentUser;

    if (ownerOnlyViews.includes(viewName)) {
      if (!activeUser || activeUser.role !== 'owner') {
        window.State.toast('Akses terbatas khusus akun Owner!', 'error');
        return;
      }
    }

    this.currentView = viewName;
    window.State.activeView = viewName;

    // 1. Switch View Panes
    document.querySelectorAll('.view-pane').forEach(pane => {
      pane.classList.remove('active');
    });
    const targetPane = document.getElementById(`view-${viewName}`);
    if (targetPane) targetPane.classList.add('active');

    // 2. Update Desktop Sidebar Active Tab
    document.querySelectorAll('.nav-item').forEach(item => {
      const v = item.getAttribute('data-view');
      item.classList.toggle('active', v === viewName);
    });

    // 3. Update Mobile Bottom Nav Active Tab
    document.querySelectorAll('.mobile-nav-item').forEach(item => {
      const v = item.getAttribute('data-view');
      item.classList.toggle('active', v === viewName);
    });

    // 4. Trigger target view renders
    if (viewName === 'pos' && window.POSView) {
      window.POSView.renderProducts();
      window.POSView.renderCart();
    } else if (viewName === 'kds' && window.KDSView) {
      window.KDSView.render();
    } else if (viewName === 'shift' && window.ShiftView) {
      window.ShiftView.render();
    } else if (viewName === 'inventory' && window.InventoryView) {
      window.InventoryView.render();
    } else if (viewName === 'reports' && window.ReportsView) {
      window.ReportsView.render();
    } else if (viewName === 'settings' && window.SettingsView) {
      window.SettingsView.render();
    }
  }
}

// Bootstrap on DOM Ready
window.Router = new AppRouter();

window.addEventListener('DOMContentLoaded', () => {
  window.Router.init();
});
