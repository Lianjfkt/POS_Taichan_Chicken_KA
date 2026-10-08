/**
 * KA POS v3.0 - Application Bootstrap & View Router
 */

class AppRouter {
  constructor() {
    this.currentView = 'pos';
    this.views = ['pos', 'orders', 'shift', 'customers', 'inventory', 'menu', 'reports', 'settings'];
  }

  init() {
    this.bindNavigation();
    
    // Initialize all modular controllers
    if (window.AuthView) window.AuthView.init();
    if (window.POSView) window.POSView.init();
    if (window.PaymentView) window.PaymentView.init();
    if (window.OrdersView) window.OrdersView.init();
    if (window.ShiftView) window.ShiftView.init();
    if (window.CustomersView) window.CustomersView.init();
    if (window.InventoryView) window.InventoryView.init();
    if (window.StockTracker) window.StockTracker.init();
    if (window.MenuView) window.MenuView.init();
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

    // Mobile Header Expand Panel toggle
    const btnMobileExpand = document.getElementById('btn-mobile-header-expand');
    const mobileQuickPanel = document.getElementById('mobile-header-quick-panel');
    if (btnMobileExpand && mobileQuickPanel) {
      btnMobileExpand.onclick = () => {
        mobileQuickPanel.classList.toggle('open');
      };
    }

    // Mobile quick logout trigger
    const btnQuickLogout = document.getElementById('btn-mobile-quick-logout');
    if (btnQuickLogout) {
      btnQuickLogout.onclick = () => {
        if (confirm('Yakin ingin keluar dari akun kasir/owner?')) {
          if (window.AuthView) window.AuthView.logout();
        }
      };
    }

    // Mobile More Menu Sheet triggers
    window.toggleMobileMoreSheet = (force) => {
      const sheet = document.getElementById('mobile-more-sheet');
      if (!sheet) return;
      if (typeof force === 'boolean') {
        sheet.classList.toggle('open', force);
      } else {
        sheet.classList.toggle('open');
      }
    };

    const moreSheet = document.getElementById('mobile-more-sheet');
    if (moreSheet) {
      moreSheet.addEventListener('click', (e) => {
        const item = e.target.closest('.mobile-sheet-btn[data-view]');
        if (item) {
          e.preventDefault();
          const targetView = item.getAttribute('data-view');
          if (targetView) {
            window.toggleMobileMoreSheet(false);
            this.navigate(targetView);
          }
        }
      });
    }
  }

  bindNavigation() {
    // Use event delegation so owner-only items revealed post-login are handled
    const sidebarNav = document.querySelector('.sidebar-nav');
    if (sidebarNav) {
      sidebarNav.addEventListener('click', (e) => {
        const item = e.target.closest('.nav-item[data-view]');
        if (item) {
          e.preventDefault();
          const targetView = item.getAttribute('data-view');
          if (targetView) this.navigate(targetView);
        }
      });
    }

    const mobileDock = document.querySelector('.mobile-nav-dock');
    if (mobileDock) {
      mobileDock.addEventListener('click', (e) => {
        const item = e.target.closest('.mobile-nav-item[data-view]');
        if (item) {
          e.preventDefault();
          const targetView = item.getAttribute('data-view');
          if (targetView) this.navigate(targetView);
        }
      });
    }
  }

  navigate(viewName) {
    if (!this.views.includes(viewName)) return;

    // Check RBAC permissions for Owner views
    const ownerOnlyViews = ['menu', 'reports', 'settings'];
    const activeUser = window.State.currentUser;

    if (ownerOnlyViews.includes(viewName)) {
      if (!activeUser || activeUser.role !== 'owner') {
        window.State.toast('Akses terbatas khusus akun Owner!', 'error');
        return;
      }
    }

    this.currentView = viewName;
    window.State.activeView = viewName;

    // Close mobile panels upon navigation
    const mobileQuickPanel = document.getElementById('mobile-header-quick-panel');
    if (mobileQuickPanel) mobileQuickPanel.classList.remove('open');
    if (window.toggleMobileMoreSheet) window.toggleMobileMoreSheet(false);

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

    // 3. Update Mobile Bottom Nav Active Tab (3 Tabs)
    const isMoreSubView = ['shift', 'customers', 'inventory', 'menu', 'reports', 'settings'].includes(viewName);
    document.querySelectorAll('.mobile-nav-item').forEach(item => {
      const v = item.getAttribute('data-view');
      if (item.id === 'btn-mobile-more-menu') {
        item.classList.toggle('active', isMoreSubView);
      } else {
        item.classList.toggle('active', v === viewName);
      }
    });

    // 4. Trigger target view renders
    if (viewName === 'pos' && window.POSView) {
      window.POSView.renderProducts();
      window.POSView.renderCart();
    } else if (viewName === 'orders' && window.OrdersView) {
      window.OrdersView.render();
    } else if (viewName === 'shift' && window.ShiftView) {
      window.ShiftView.render();
    } else if (viewName === 'inventory' && window.InventoryView) {
      window.InventoryView.render();
    } else if (viewName === 'menu' && window.MenuView) {
      window.MenuView.render();
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
