/**
 * KA POS v3.0 - Authentication & Quick Lock PIN Terminal
 */

class AuthView {
  constructor() {
    this.inactivityTimer = null;
    this.pinBuffer = '';
    this.autoLockMinutes = 15;
  }

  init() {
    this.bindEvents();
    this.resetInactivityTimer();

    // Check if previous session exists
    const savedUser = window.State.load('ka_active_user', null);
    if (savedUser) {
      this.loginSuccess(savedUser);
    } else {
      this.showLogin();
    }
  }

  bindEvents() {
    // Inactivity listener
    ['touchstart', 'mousedown', 'mousemove', 'keydown', 'scroll'].forEach(evt => {
      window.addEventListener(evt, () => this.resetInactivityTimer(), { passive: true });
    });

    // Login Form Submit
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
      loginForm.onsubmit = (e) => {
        e.preventDefault();
        this.handleLogin();
      };
    }

    // Role Quick Selector on Login
    document.querySelectorAll('.login-role-tab').forEach(tab => {
      tab.onclick = () => {
        document.querySelectorAll('.login-role-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        const role = tab.getAttribute('data-role');
        const userInput = document.getElementById('login-username');
        // BUG-06 fix: hanya isi username, user harus input password sendiri
        if (userInput) {
          if (role === 'owner') {
            userInput.value = 'owner';
          } else {
            userInput.value = 'kasir';
          }
          // Fokus ke password field agar user mudah melanjutkan
          const passInput = document.getElementById('login-password');
          if (passInput) {
            passInput.value = '';
            passInput.focus();
          }
        }
      };
    });

    // Keypad PIN buttons (.pin-btn from Stitch design & .keypad-btn)
    document.querySelectorAll('.pin-btn, .keypad-btn').forEach(btn => {
      btn.onclick = () => {
        const val = btn.getAttribute('data-val');
        this.handleKeypadInput(val);
      };
    });

    // Quick Lock button in Header
    const btnLock = document.getElementById('btn-header-lock');
    if (btnLock) {
      btnLock.onclick = () => this.lockScreen();
    }
  }

  showLogin() {
    const loginScreen = document.getElementById('screen-login');
    const appScreen = document.getElementById('screen-app');
    const lockScreen = document.getElementById('screen-lock');

    if (loginScreen) loginScreen.style.display = 'flex';
    if (appScreen) appScreen.style.display = 'none';
    if (lockScreen) lockScreen.style.display = 'none';
  }

  async handleLogin() {
    const userField = document.getElementById('login-username');
    const passField = document.getElementById('login-password');
    const errBox = document.getElementById('login-error-msg');

    if (!userField || !passField) return;

    const username = userField.value.trim().toLowerCase();
    const password = passField.value;

    const userRecord = window.State.staff.find(s => s.user.toLowerCase() === username && s.on !== false);

    if (!userRecord) {
      this.showLoginError('Pengguna tidak ditemukan atau dinonaktifkan.');
      return;
    }

    const hashedInput = await window.State.hashPassword(password);
    const passwordMatch = (userRecord.pw === hashedInput) || (userRecord.pw === password);

    if (!passwordMatch) {
      this.showLoginError('Kata sandi salah. Silakan coba lagi.');
      return;
    }

    if (errBox) errBox.style.display = 'none';
    this.loginSuccess({
      id: userRecord.id,
      nm: userRecord.nm,
      user: userRecord.user,
      role: userRecord.role,
      pin: userRecord.pin || '1234'
    });
  }

  showLoginError(msg) {
    const errBox = document.getElementById('login-error-msg');
    if (errBox) {
      errBox.textContent = msg;
      errBox.style.display = 'block';
    }
    window.State.toast(msg, 'error');
  }

  loginSuccess(user) {
    window.State.currentUser = user;
    window.State.save('ka_active_user', user);

    // Update UI User Pill & Permissions
    const userNameEl = document.getElementById('header-user-name');
    const userRoleEl = document.getElementById('header-user-role');
    const userInitialEl = document.getElementById('header-user-initial');

    if (userNameEl) userNameEl.textContent = user.nm;
    if (userRoleEl) userRoleEl.textContent = user.role.toUpperCase();
    if (userInitialEl) userInitialEl.textContent = (user.nm || 'U').charAt(0).toUpperCase();

    // Mobile quick panel user sync
    const mobUserName = document.getElementById('mobile-quick-user-name');
    const mobUserRole = document.getElementById('mobile-quick-user-role');
    const mobUserInit = document.getElementById('mobile-quick-user-initial');
    if (mobUserName) mobUserName.textContent = user.nm;
    if (mobUserRole) mobUserRole.textContent = user.role.toUpperCase();
    if (mobUserInit) mobUserInit.textContent = (user.nm || 'U').charAt(0).toUpperCase();

    // Toggle Owner-only elements
    const isOwner = user.role === 'owner';
    document.querySelectorAll('.owner-only').forEach(el => {
      el.style.display = isOwner ? '' : 'none';
    });

    const loginScreen = document.getElementById('screen-login');
    const appScreen = document.getElementById('screen-app');
    const lockScreen = document.getElementById('screen-lock');

    if (loginScreen) loginScreen.style.display = 'none';
    if (lockScreen) lockScreen.style.display = 'none';
    if (appScreen) appScreen.style.display = 'flex';

    window.State.toast(`Selamat datang, ${user.nm}!`, 'success');

    // Trigger router to default view
    if (window.Router) {
      window.Router.navigate('pos');
    }
    
    // Auto-init Supabase if configured
    if (window.SupabaseService) {
      window.SupabaseService.init();
    }

    // Check for pending alarm if Owner logged in
    if (isOwner && window.AlarmService) {
      try {
        const saved = localStorage.getItem('ka_owner_alarm_alert');
        if (saved) {
          const alertData = JSON.parse(saved);
          if (alertData && !alertData.dismissed) {
            window.AlarmService.handleIncomingAlarm(alertData);
          }
        }
      } catch (e) {}
    }
  }

  logout() {
    // BUG-16 fix: bersihkan inactivity timer agar tidak fire setelah logout
    if (this.inactivityTimer) {
      clearTimeout(this.inactivityTimer);
      this.inactivityTimer = null;
    }
    window.State.currentUser = null;
    window.State.save('ka_active_user', null);
    window.State.clearCart();
    this.showLogin();
    window.State.toast('Anda telah keluar dari aplikasi.', 'warning');
  }

  // --- Quick Lock Terminal Operations ---
  lockScreen() {
    if (!window.State.currentUser) return;
    this.pinBuffer = '';
    this.updatePinDots();

    const lockScreen = document.getElementById('screen-lock');
    if (lockScreen) {
      const lockUserName = document.getElementById('lock-user-name');
      const lockUserRole = document.getElementById('lock-user-role');
      if (lockUserName) lockUserName.textContent = window.State.currentUser.nm;
      if (lockUserRole) lockUserRole.textContent = window.State.currentUser.role.toUpperCase();

      lockScreen.style.display = 'flex';
      window.State.isLocked = true;
    }
  }

  handleKeypadInput(key) {
    if (key === 'clear') {
      this.pinBuffer = '';
    } else if (key === 'backspace') {
      this.pinBuffer = this.pinBuffer.slice(0, -1);
    } else if (this.pinBuffer.length < 4) {
      this.pinBuffer += key;
    }

    this.updatePinDots();

    if (this.pinBuffer.length === 4) {
      this.verifyPin();
    }
  }

  updatePinDots() {
    const dots = document.querySelectorAll('.pin-dot');
    dots.forEach((dot, idx) => {
      if (idx < this.pinBuffer.length) {
        dot.classList.add('filled');
      } else {
        dot.classList.remove('filled');
      }
    });
  }

  verifyPin() {
    const activeUser = window.State.currentUser;
    const targetPin = (activeUser && activeUser.pin) ? activeUser.pin : '1234';

    // BUG-04 fix: hapus master PIN bypass '9999' — celah keamanan
    if (this.pinBuffer === targetPin) {
      // Unlocked
      const lockScreen = document.getElementById('screen-lock');
      if (lockScreen) lockScreen.style.display = 'none';
      window.State.isLocked = false;
      this.pinBuffer = '';
      this.updatePinDots();
      window.State.toast('Layar berhasil dibuka', 'success');
      this.resetInactivityTimer();
    } else {
      // Wrong PIN animation
      window.State.toast('PIN Salah!', 'error');
      const container = document.getElementById('pin-dots-container');
      if (container) {
        container.classList.add('shake');
        setTimeout(() => container.classList.remove('shake'), 400);
      }
      this.pinBuffer = '';
      setTimeout(() => this.updatePinDots(), 300);
    }
  }

  resetInactivityTimer() {
    if (this.inactivityTimer) clearTimeout(this.inactivityTimer);
    if (!window.State.currentUser || window.State.isLocked) return;

    this.inactivityTimer = setTimeout(() => {
      this.lockScreen();
    }, this.autoLockMinutes * 60 * 1000);
  }
}

window.AuthView = new AuthView();
