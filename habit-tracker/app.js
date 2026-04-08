'use strict';
// ═══════════════════════════════════════
// Zenko – AI Habit Tracker
// ═══════════════════════════════════════

class ZenkoApp {
  constructor() {
    // ── Apply saved theme immediately (before any render) ──
    const savedTheme = localStorage.getItem('hf_theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);

    this.user = null;
    this.habits = [];
    this.completions = {};
    this.currentView = 'dashboard';
    this.editingId = null;
    this.deleteTarget = null;
    this.selectedEmoji = '✅';
    this.selectedColor = '#6366f1';
    this.weekOffset = 0;
    this.monthOffset = 0;
    this.filterFreq = 'all';
    this.todayFilter = 'all';
    this.searchQuery = '';
    this.isApiMode = false; // true when backed by Spring Boot API

    this._initAuth();
  }

  // ─────────────────────────────────────
  // AUTH INIT
  // ─────────────────────────────────────
  async _initAuth() {
    // 1. Check if Spring Boot server has an active session (Google user)
    const serverUser = await this._checkServerSession();
    if (serverUser) {
      this.isApiMode = true;
      await this._loginUser(serverUser, true);
      return;
    }
    // 2. Check demo user in localStorage
    try {
      const local = localStorage.getItem('hf_user');
      if (local) {
        const u = JSON.parse(local);
        if (u.provider === 'demo') { this._loginUser(u, false); return; }
      }
    } catch(e) {}
    // 3. Show auth screen & init Google One-Tap
    this._initGoogleOneTap();
    this._bindAuthButtons();
  }

  async _checkServerSession() {
    try {
      const res = await fetch('/api/auth/user', { credentials: 'include' });
      if (res.ok) return await res.json();
    } catch(e) {} // Server not running — that's OK (demo mode)
    return null;
  }

  _initGoogleOneTap() {
    const clientId = window.GOOGLE_CLIENT_ID;
    if (!clientId || clientId === 'YOUR_GOOGLE_CLIENT_ID_HERE') return;
    // Try immediately, then poll — handles both before and after 'load'
    this._renderGoogleButton(clientId);
  }

  _renderGoogleButton(clientId, attempt = 0) {
    const container = document.getElementById('googleOneTapContainer');
    if (!container) return; // Auth screen not visible yet

    if (typeof google !== 'undefined' && google.accounts?.id) {
      try {
        google.accounts.id.initialize({
          client_id: clientId,
          callback: (resp) => this._handleGoogleCredential(resp.credential),
          auto_select: false,
          cancel_on_tap_outside: true,
        });
        google.accounts.id.renderButton(container, {
          theme: 'outline', size: 'large', width: 360, text: 'signin_with'
        });
        container.style.display = ''; // ensure visible
      } catch(e) {}
      return;
    }

    // Google script not yet loaded — retry up to 20 times (5 seconds total)
    if (attempt < 20) {
      setTimeout(() => this._renderGoogleButton(clientId, attempt + 1), 250);
    }
  }

  async _handleGoogleCredential(idToken) {
    this._toast('Signing in with Google…', 'info');
    try {
      const res = await fetch('/api/auth/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: idToken }),
        credentials: 'include'
      });
      if (res.ok) {
        const user = await res.json();
        this.isApiMode = true;
        await this._loginUser(user, true);
      } else {
        this._toast('Google sign-in failed. Is the server running?', 'error');
      }
    } catch(e) {
      this._toast('Cannot connect to server. Try Demo mode.', 'error');
    }
  }

  _bindAuthButtons() {
    const demoBtn = document.getElementById('demoSignInBtn');
    if (demoBtn) demoBtn.addEventListener('click', () => this._demoSignIn());
    // Re-attempt Google button render now that auth screen is in DOM
    const clientId = window.GOOGLE_CLIENT_ID;
    if (clientId && clientId !== 'YOUR_GOOGLE_CLIENT_ID_HERE') {
      this._renderGoogleButton(clientId);
    }
  }

  async _demoSignIn() {
    this._toast('Loading demo...', 'info');
    try {
      const res = await fetch('/api/auth/demo', {
        method: 'POST',
        credentials: 'include'
      });
      if (res.ok) {
        const user = await res.json();
        user.avatar = `https://ui-avatars.com/api/?name=Demo+User&background=8b5cf6&color=fff&size=64`;
        this.isApiMode = true;
        if (this.habits.length === 0) await this._seedDemoData();
        await this._loginUser(user, true);
        return;
      }
    } catch(e) {}
    // Fallback: offline demo
    const user = {
      name: 'Demo User',
      email: 'demo@zenko.app',
      avatar: `https://ui-avatars.com/api/?name=Demo+User&background=8b5cf6&color=fff&size=64`,
      provider: 'demo'
    };
    if (this.habits.length === 0) this._seedDemoData();
    this._loginUser(user, false);
  }

  async _loginUser(user, isApi = false) {
    this.user = user;
    this.isApiMode = isApi;
    if (isApi) {
      // Load data from API
      await this._loadFromAPI();
    } else {
      // Demo: use localStorage
      localStorage.setItem('hf_user', JSON.stringify(user));
      this.habits = JSON.parse(localStorage.getItem('hf_habits')) || [];
      this.completions = JSON.parse(localStorage.getItem('hf_completions')) || {};
    }
    this.habits.forEach(h => h.color = this._getCatColor(h.category));
    this._showApp();
    this._toast('Welcome, ' + user.name + '! 🎉', 'success');
  }

  async _loadFromAPI() {
    try {
      const [hRes, cRes] = await Promise.all([
        fetch('/api/habits', { credentials: 'include' }),
        fetch('/api/completions', { credentials: 'include' })
      ]);
      this.habits = hRes.ok ? await hRes.json() : [];
      const rawComp = cRes.ok ? await cRes.json() : {};
      // Convert string keys back to numbers for consistency
      this.completions = {};
      for (const [k, v] of Object.entries(rawComp)) {
        this.completions[parseInt(k)] = v;
      }
    } catch(e) { this.habits = []; this.completions = {}; }
  }

  _save() {
    if (!this.isApiMode) {
      localStorage.setItem('hf_habits', JSON.stringify(this.habits));
      localStorage.setItem('hf_completions', JSON.stringify(this.completions));
    }
  }

  _signOut() {
    if (this.isApiMode) {
      // Sign out from Spring Boot session
      fetch('/api/auth/logout', { method: 'POST', credentials: 'include' }).catch(() => {});
    }
    localStorage.removeItem('hf_user');
    this.user = null;
    this.habits = [];
    this.completions = {};
    this.isApiMode = false;
    document.getElementById('mainApp').style.display = 'none';
    document.getElementById('authScreen').style.display = 'flex';
    this._initGoogleOneTap();
    this._bindAuthButtons();
    this._toast('Signed out successfully', 'info');
  }

  _showApp() {
    document.getElementById('authScreen').style.display = 'none';
    document.getElementById('mainApp').style.display = 'flex';
    this._populateUser();
    this._bindAll();
    this._setPageDate();
    this._switchView('dashboard');
  }

  _populateUser() {
    if (!this.user) return;
    ['sidebarAvatar','topbarAvatarImg'].forEach(id => {
      const el = document.getElementById(id);
      if (el) { el.src = this.user.avatar; el.onerror = () => el.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(this.user.name)}&background=6366f1&color=fff&size=64`; }
    });
    const nm = document.getElementById('sidebarName');
    const em = document.getElementById('sidebarEmail');
    if (nm) nm.textContent = this.user.name;
    if (em) em.textContent = this.user.email;
  }

  // ─────────────────────────────────────
  // SEED DEMO DATA
  // ─────────────────────────────────────
  _seedDemoData() {
    const today = this._dateStr(new Date());
    const seeds = [
      { name:'Morning Run', emoji:'🏃', category:'health', frequency:'daily', color:'#6366f1', desc:'30 min jog every morning' },
      { name:'Read 20 Pages', emoji:'📚', category:'learn', frequency:'daily', color:'#8b5cf6', desc:'Read before bed' },
      { name:'Meditate', emoji:'🧘', category:'mind', frequency:'daily', color:'#14b8a6', desc:'10 min mindfulness' },
      { name:'Weekly Review', emoji:'📊', category:'work', frequency:'weekly', color:'#f97316', desc:'Review goals and progress' },
      { name:'Save Money', emoji:'💰', category:'finance', frequency:'monthly', color:'#22c55e', desc:'Transfer to savings' },
      { name:'Drink 8 Glasses', emoji:'💧', category:'health', frequency:'daily', color:'#3b82f6', desc:'Stay hydrated' },
    ];
    seeds.forEach((s, i) => {
      const id = Date.now() + i;
      const habit = { id, ...s, time: '', createdAt: today };
      this.habits.push(habit);
      // Simulate past completions for demo
      this.completions[id] = {};
      for (let d = 0; d < 14; d++) {
        const dt = new Date(); dt.setDate(dt.getDate() - d);
        if (Math.random() > 0.3) this.completions[id][this._dateStr(dt)] = true;
      }
    });
    this._save();
  }

  // ─────────────────────────────────────
  // BIND ALL EVENTS
  // ─────────────────────────────────────
  _bindAll() {
    // Navigation
    document.querySelectorAll('.nav-btn[data-view]').forEach(btn => {
      btn.addEventListener('click', () => {
        this._switchView(btn.dataset.view);
        this._closeSidebar();
      });
    });
    // Sidebar toggle
    document.getElementById('menuBtn').addEventListener('click', () => this._openSidebar());
    document.getElementById('sidebarClose').addEventListener('click', () => this._closeSidebar());
    // Theme
    document.getElementById('themeToggle').addEventListener('click', () => this._toggleTheme());
    // Add habit buttons
    ['addHabitTopBtn','addFromDash','fabBtn','addFromHabits'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.addEventListener('click', () => this._openModal());
    });
    // Social dashboard button
    const socialFromDash = document.getElementById('socialFromDash');
    if (socialFromDash) socialFromDash.addEventListener('click', () => this._switchView('social'));
    // Modal
    document.getElementById('modalClose').addEventListener('click', () => this._closeModal());
    document.getElementById('cancelHabit').addEventListener('click', () => this._closeModal());
    document.getElementById('habitModalOverlay').addEventListener('click', e => {
      if (e.target === document.getElementById('habitModalOverlay')) this._closeModal();
    });
    document.getElementById('habitForm').addEventListener('submit', e => { e.preventDefault(); this._saveHabit(); });
    // Delete
    document.getElementById('cancelDelete').addEventListener('click', () => this._closeDeleteModal());
    document.getElementById('confirmDelete').addEventListener('click', () => this._confirmDelete());
    document.getElementById('deleteModalOverlay').addEventListener('click', e => {
      if (e.target === document.getElementById('deleteModalOverlay')) this._closeDeleteModal();
    });
    // Color picker
    document.querySelectorAll('.color-dot').forEach(dot => {
      dot.addEventListener('click', () => {
        document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('selected'));
        dot.classList.add('selected');
        this.selectedColor = dot.dataset.color;
      });
    });
    // Emoji picker
    this._initEmojiPicker();
    // Week/Month nav
    document.getElementById('prevWeek').addEventListener('click', () => { this.weekOffset--; this._renderWeekly(); });
    document.getElementById('nextWeek').addEventListener('click', () => { this.weekOffset++; this._renderWeekly(); });
    document.getElementById('prevMonth').addEventListener('click', () => { this.monthOffset--; this._renderMonthly(); });
    document.getElementById('nextMonth').addEventListener('click', () => { this.monthOffset++; this._renderMonthly(); });
    document.getElementById('monthHabitSelect').addEventListener('change', () => this._renderMonthly());
    // AI
    document.getElementById('refreshAI').addEventListener('click', () => this._renderAI());
    document.getElementById('chatSend').addEventListener('click', () => this._sendChat());
    document.getElementById('chatInput').addEventListener('keypress', e => { if (e.key === 'Enter') this._sendChat(); });
    // Filter tabs (today view)
    document.querySelectorAll('#todayView .filter-tab').forEach(tab => {
      tab.addEventListener('click', () => {
        document.querySelectorAll('#todayView .filter-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        this.todayFilter = tab.dataset.filter;
        this._renderToday();
      });
    });
    // Filter tabs (habits view)
    document.querySelectorAll('#habitsView .filter-tab').forEach(tab => {
      tab.addEventListener('click', () => {
        document.querySelectorAll('#habitsView .filter-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        this.filterFreq = tab.dataset.freq;
        this._renderAllHabits();
      });
    });
    // Search
    document.getElementById('habitSearch').addEventListener('input', e => {
      this.searchQuery = e.target.value.toLowerCase();
      this._renderAllHabits();
    });
    // Load theme
    const saved = localStorage.getItem('hf_theme') || 'light';
    document.documentElement.setAttribute('data-theme', saved);
    this._syncThemeIcon(saved);
    // Keyboard
    document.addEventListener('keydown', e => { if (e.key === 'Escape') { this._closeModal(); this._closeDeleteModal(); } });
    // AI Coach new features
    const runAutopsy = document.getElementById('runAutopsyBtn');
    if (runAutopsy) runAutopsy.addEventListener('click', () => window.aiCoachModule && window.aiCoachModule.runAutopsy());
    const runDNA = document.getElementById('runDNABtn');
    if (runDNA) runDNA.addEventListener('click', () => window.aiCoachModule && window.aiCoachModule.runDNA());
    const runAlert = document.getElementById('runAlertBtn');
    if (runAlert) runAlert.addEventListener('click', () => window.aiCoachModule && window.aiCoachModule.runAlert());
    // Sign out
    const soBtn = document.getElementById('signOutBtn');
    if (soBtn) soBtn.addEventListener('click', () => this._signOut());
  }

  // ─────────────────────────────────────
  // EMOJI PICKER
  // ─────────────────────────────────────
  _initEmojiPicker() {
    const emojis = ['✅','🏃','📚','🧘','💼','💰','😴','💧','🎯','🔥','⚡','🌟','🎸','🏋️','🧗','🚴','🍎','🧠','✍️','🎨','🌅','🌙','☕','🚶','🎵','🏊','🤸','🧘','💪','🎯','📝','🌿'];
    const grid = document.getElementById('emojiGrid');
    grid.innerHTML = emojis.map(e => `<div class="emoji-opt" data-emoji="${e}">${e}</div>`).join('');
    grid.querySelectorAll('.emoji-opt').forEach(opt => {
      opt.addEventListener('click', () => {
        this.selectedEmoji = opt.dataset.emoji;
        document.getElementById('emojiBtn').textContent = this.selectedEmoji;
        grid.style.display = 'none';
      });
    });
    document.getElementById('emojiBtn').addEventListener('click', e => {
      e.stopPropagation();
      grid.style.display = grid.style.display === 'none' ? 'grid' : 'none';
    });
    document.addEventListener('click', () => { grid.style.display = 'none'; });
  }

  // ─────────────────────────────────────
  // SIDEBAR & THEME
  // ─────────────────────────────────────
  _openSidebar() { document.getElementById('sidebar').classList.add('open'); }
  _closeSidebar() { document.getElementById('sidebar').classList.remove('open'); }

  _toggleTheme() {
    const cur = document.documentElement.getAttribute('data-theme');
    const next = cur === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('hf_theme', next);
    this._syncThemeIcon(next);
  }

  _syncThemeIcon(theme) {
    const sun = document.querySelector('.sun-icon');
    const moon = document.querySelector('.moon-icon');
    if (sun && moon) {
      sun.style.display = theme === 'dark' ? 'none' : 'block';
      moon.style.display = theme === 'dark' ? 'block' : 'none';
    }
  }

  // ─────────────────────────────────────
  // NAVIGATION
  // ─────────────────────────────────────
  _switchView(view) {
    this.currentView = view;
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    const el = document.getElementById(view + 'View');
    if (el) el.classList.add('active');
    const navId = 'nav' + view.charAt(0).toUpperCase() + view.slice(1);
    const nav = document.getElementById(navId);
    if (nav) nav.classList.add('active');
    const titles = {
      dashboard: 'Dashboard',
      today: "Today's Habits",
      weekly: 'Weekly View',
      monthly: 'Monthly View',
      ai: 'AI Coach',
      social: 'Social Hub',
      game: 'Game Center',
      habits: 'All Habits'
    };
    document.getElementById('pageTitle').textContent = titles[view] || view;
    this._renderView(view);
  }

  _renderView(view) {
    switch(view) {
      case 'dashboard': this._renderDashboard(); break;
      case 'today': this._renderToday(); break;
      case 'weekly': this._renderWeekly(); break;
      case 'monthly': this._renderMonthly(); break;
      case 'ai': this._renderAI(); break;
      case 'social': window.socialModule && window.socialModule.init(); break;
      case 'game': window.gameModule && window.gameModule.init(); break;
      case 'habits': this._renderAllHabits(); break;
    }
  }

  // ─────────────────────────────────────
  // DATE HELPERS
  // ─────────────────────────────────────
  _dateStr(d) { return d.toISOString().split('T')[0]; }
  _today() { return this._dateStr(new Date()); }

  _isCompleted(habitId, dateStr) {
    return !!(this.completions[habitId] && this.completions[habitId][dateStr]);
  }

  async _toggleCompletion(habitId, dateStr, checkbox) {
    if (!this.completions[habitId]) this.completions[habitId] = {};
    const wasDone = this._isCompleted(habitId, dateStr);
    // Optimistic update UI
    if (wasDone) { delete this.completions[habitId][dateStr]; if (checkbox) checkbox.classList.remove('done','checked'); }
    else { this.completions[habitId][dateStr] = true; if (checkbox) { checkbox.classList.add('done','checked'); this._confetti(checkbox); } }

    if (this.isApiMode) {
      try {
        await fetch(`/api/habits/${habitId}/complete`, {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ date: dateStr }), credentials: 'include'
        });
      } catch(e) { /* silently fail, keep optimistic UI */ }
    } else {
      this._save();
    }
    this._updateBadge();
    if (this.currentView === 'dashboard') this._renderDashboard();
    if (this.currentView === 'today') { const pct = this._todayPct(); this._updateScoreRing(pct); }
  }

  _getStreak(habitId) {
    let streak = 0;
    const today = new Date();
    for (let i = 0; i < 365; i++) {
      const d = new Date(today); d.setDate(d.getDate() - i);
      if (this._isCompleted(habitId, this._dateStr(d))) streak++;
      else if (i > 0) break;
    }
    return streak;
  }

  _getDisciplinePct(habitId) {
    const habit = this.habits.find(h => h.id === habitId);
    if (!habit) return 0;
    const days = habit.frequency === 'daily' ? 30 : habit.frequency === 'weekly' ? 12 : 3;
    let done = 0, total = 0;
    for (let i = 0; i < days; i++) {
      const d = new Date(); d.setDate(d.getDate() - i);
      const ds = this._dateStr(d);
      const shouldDo = habit.frequency === 'daily' ? true
        : habit.frequency === 'weekly' ? (d.getDay() === 1)
        : (d.getDate() === 1);
      if (shouldDo) { total++; if (this._isCompleted(habitId, ds)) done++; }
    }
    return total === 0 ? 0 : Math.round((done / total) * 100);
  }

  _overallDiscipline() {
    if (this.habits.length === 0) return 0;
    const dailies = this.habits.filter(h => h.frequency === 'daily');
    if (dailies.length === 0) return 0;
    let done = 0;
    const today = this._today();
    dailies.forEach(h => { if (this._isCompleted(h.id, today)) done++; });
    return Math.round((done / dailies.length) * 100);
  }

  _todayPct() {
    const daily = this.habits.filter(h => h.frequency === 'daily');
    if (daily.length === 0) return 0;
    const today = this._today();
    const done = daily.filter(h => this._isCompleted(h.id, today)).length;
    return Math.round((done / daily.length) * 100);
  }

  _bestStreak() {
    return Math.max(0, ...this.habits.map(h => this._getStreak(h.id)));
  }

  _setPageDate() {
    const el = document.getElementById('pageDate');
    if (el) el.textContent = new Date().toLocaleDateString('en-US', { weekday:'long', month:'long', day:'numeric', year:'numeric' });
  }

  _updateBadge() {
    const today = this._today();
    const daily = this.habits.filter(h => h.frequency === 'daily');
    const pending = daily.filter(h => !this._isCompleted(h.id, today)).length;
    const todayBadge = document.getElementById('todayBadge');
    if (todayBadge) { todayBadge.textContent = pending; todayBadge.style.display = pending > 0 ? 'inline' : 'none'; }

    // Social badge: show sum of active duels + bonds
    const duels = window.app?.socialDuels?.length || 0;
    const bonds = window.app?.socialBonds?.length || 0;
    const socialCount = duels + bonds;
    const socialBadge = document.getElementById('socialBadge');
    if (socialBadge) {
      socialBadge.textContent = socialCount;
      socialBadge.style.display = socialCount > 0 ? 'inline' : 'none';
    }
  }

  // ─────────────────────────────────────
  // DASHBOARD RENDER
  // ─────────────────────────────────────
  _renderDashboard() {
    const today = this._today();
    const total = this.habits.length;
    const dailyDone = this.habits.filter(h => h.frequency === 'daily' && this._isCompleted(h.id, today)).length;
    const bestStreak = this._bestStreak();
    const discipline = this._overallDiscipline();

    this._animNum('statTotalVal', total);
    this._animNum('statTodayVal', dailyDone);
    this._animNum('statStreakVal', bestStreak);

    const dEl = document.getElementById('statDisciplineVal');
    if (dEl) dEl.textContent = discipline + '%';

    // Progress
    const pct = this._todayPct();
    const bar = document.getElementById('progressBarFill');
    if (bar) bar.style.width = pct + '%';
    const pctEl = document.getElementById('progressPct');
    if (pctEl) pctEl.textContent = pct + '%';
    const sub = document.getElementById('progressSubtitle');
    if (sub) {
      sub.textContent = pct === 100 ? '🎉 All done! Legendary discipline!' 
        : pct >= 75 ? '🔥 Almost there, keep pushing!'
        : pct >= 50 ? '💪 Great momentum, halfway there!'
        : pct > 0 ? '✅ Good start! Keep going!'
        : 'Complete your habits to build momentum';
    }

    // Dots
    const daily = this.habits.filter(h => h.frequency === 'daily');
    const dots = document.getElementById('progressDots');
    if (dots) {
      dots.innerHTML = daily.map(h => {
        const done = this._isCompleted(h.id, today);
        return `<div class="p-dot ${done ? 'done' : ''}" style="${done ? 'background:' + h.color : ''}" title="${h.name}"></div>`;
      }).join('');
    }

    // Dash habit list
    this._renderDashHabits();
    // Heatmap
    this._renderHeatmap();
    // Streaks
    this._renderStreakList();
    // Social stats
    this._loadDashboardSocial();

    this._updateBadge();
  }

  _renderDashHabits() {
    const today = this._today();
    const list = document.getElementById('dashHabitsList');
    if (!list) return;
    const daily = this.habits.filter(h => h.frequency === 'daily');
    if (daily.length === 0) {
      list.innerHTML = `<div class="empty-state"><div class="empty-icon">📋</div><p>No daily habits yet. Add your first!</p></div>`;
      return;
    }
    list.innerHTML = daily.map(h => {
      const done = this._isCompleted(h.id, today);
      return `<div class="dash-habit-item ${done ? 'done' : ''}" data-id="${h.id}">
        <div class="dh-check ${done ? 'checked' : ''}" style="${done ? 'background:'+h.color+';border-color:'+h.color : 'border-color:'+h.color}"></div>
        <span class="dh-name">${h.emoji || '✅'} ${h.name}</span>
        <span class="dh-freq">${h.frequency}</span>
      </div>`;
    }).join('');
    list.querySelectorAll('.dash-habit-item').forEach(item => {
      item.addEventListener('click', () => {
        const id = parseInt(item.dataset.id);
        const check = item.querySelector('.dh-check');
        this._toggleCompletion(id, today, check);
        item.classList.toggle('done', check.classList.contains('checked'));
      });
    });
  }

  _renderHeatmap() {
    const hm = document.getElementById('weeklyHeatmap');
    if (!hm) return;
    const days = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
    const today = new Date(); const dow = today.getDay();
    const monday = new Date(today); monday.setDate(today.getDate() - (dow === 0 ? 6 : dow - 1));

    hm.innerHTML = days.map((day, i) => {
      const d = new Date(monday); d.setDate(monday.getDate() + i);
      const ds = this._dateStr(d);
      const isFuture = d > today;
      const allHabits = this.habits.filter(h => h.frequency === 'daily');
      const doneCount = allHabits.filter(h => this._isCompleted(h.id, ds)).length;
      const level = isFuture ? 0 : allHabits.length === 0 ? 0 : Math.ceil((doneCount / allHabits.length) * 4);
      const lvlClass = ['','l1','l2','l3','l4'][Math.min(level, 4)];
      return `<div class="hm-col">
        <div class="hm-day-label">${day}</div>
        <div class="hm-cell ${lvlClass}" title="${day}: ${doneCount} done"></div>
      </div>`;
    }).join('');
  }

  _renderStreakList() {
    const list = document.getElementById('streakList');
    if (!list) return;
    if (this.habits.length === 0) {
      list.innerHTML = '<div class="empty-state" style="padding:20px"><p>Add habits to see your streaks!</p></div>';
      return;
    }
    const sorted = [...this.habits].map(h => ({ ...h, streak: this._getStreak(h.id) })).sort((a,b) => b.streak - a.streak).slice(0,5);
    const max = sorted[0]?.streak || 1;
    const medals = ['🥇','🥈','🥉','4️⃣','5️⃣'];
    list.innerHTML = sorted.map((h, i) => `
      <div class="streak-item">
        <div class="streak-rank">${medals[i]}</div>
        <div style="width:36px;height:36px;border-radius:10px;background:${h.color}20;display:flex;align-items:center;justify-content:center;font-size:18px;">${h.emoji||'✅'}</div>
        <div class="streak-name">${h.name}</div>
        <div class="streak-bar-wrap"><div class="streak-bar-fill" style="width:${(h.streak/max)*100}%;background:${h.color}"></div></div>
        <div class="streak-count">🔥 ${h.streak}d</div>
      </div>`).join('');
  }

  async _loadDashboardSocial() {
    const card = document.getElementById('socialDashCard');
    const feedContainer = document.getElementById('dashSocialFeed');
    if (!card || !feedContainer) return;

    try {
      // Use preloaded data if available (set by the hook)
      let duels, bonds, feed;
      if (window.app?.socialPreloaded) {
        duels = window.app.socialDuels || [];
        bonds = window.app.socialBonds || [];
        feed = window.app.socialFeed || [];
      } else {
        // Fetch from API
        const [duelsRes, bondsRes, feedRes] = await Promise.all([
          fetch('/api/social/duels', { credentials: 'include' }),
          fetch('/api/social/bonds', { credentials: 'include' }),
          fetch('/api/social/feed', { credentials: 'include' })
        ]);
        duels = duelsRes.ok ? await duelsRes.json() : [];
        bonds = bondsRes.ok ? await bondsRes.json() : [];
        feed = feedRes.ok ? await feedRes.json() : [];
        // Store for badge and future use
        window.app.socialDuels = duels;
        window.app.socialBonds = bonds;
        window.app.socialFeed = feed;
      }

      // Update counts
      document.getElementById('dashDuelsCount').textContent = duels.filter(d => d.status === 'active').length;
      document.getElementById('dashBondsCount').textContent = bonds.filter(b => b.status === 'active').length;
      document.getElementById('dashFeedCount').textContent = feed.length;

      // Show recent feed items (last 3)
      if (feed.length > 0) {
        const recentFeed = feed.slice(0, 3);
        feedContainer.innerHTML = recentFeed.map(e => {
          const ago = e.minutesAgo ? `${e.minutesAgo}m ago` : 'just now';
          const streakBadge = e.streak > 1 ? `<span style="font-size:10px;color:var(--orange);font-weight:600">🔥 ${e.streak}d</span>` : '';
          return `<div class="dash-feed-item">
            <div class="dash-feed-emoji">${e.emoji || '✅'}</div>
            <div class="dash-feed-info">
              <div class="dash-feed-main">${e.habit} <span style="color:var(--text3)">in</span> ${e.city}</div>
              <div class="dash-feed-sub">${e.isReal ? '🟢 Real user' : '🌐 Global'} · ${streakBadge}</div>
            </div>
            <div class="dash-feed-time">${ago}</div>
          </div>`;
        }).join('');
      } else {
        feedContainer.innerHTML = '<div class="empty-state" style="padding:12px;text-align:center"><p>No recent activity. Be the first! 🌟</p></div>';
      }

      // Show the social card if there's any activity or if user has duels/bonds
      if (duels.length > 0 || bonds.length > 0 || feed.length > 0) {
        card.style.display = 'block';
      }

      // Update nav badge
      this._updateBadge();

    } catch (e) {
      console.error('Dashboard social load error:', e);
      if (feedContainer) feedContainer.innerHTML = '<div class="feed-error">Unable to load social updates</div>';
    }
  }

  // ─────────────────────────────────────
  // TODAY VIEW
  // ─────────────────────────────────────
  _renderToday() {
    const hour = new Date().getHours();
    const greeting = hour < 12 ? 'Good Morning! ☀️' : hour < 17 ? 'Good Afternoon! 👋' : 'Good Evening! 🌙';
    const el = document.getElementById('todayGreeting');
    if (el) el.textContent = greeting + ' ' + (this.user?.name?.split(' ')[0] || '');

    const pct = this._todayPct();
    this._updateScoreRing(pct);

    const today = this._today();
    let habits = [...this.habits];
    if (this.todayFilter !== 'all') habits = habits.filter(h => h.frequency === this.todayFilter);

    const list = document.getElementById('todayHabitsList');
    if (!list) return;
    if (habits.length === 0) {
      list.innerHTML = `<div class="empty-state"><div class="empty-icon">🎯</div><p>No habits for this filter. Add some!</p></div>`;
      return;
    }

    const done = habits.filter(h => this._isCompleted(h.id, today));
    const pending = habits.filter(h => !this._isCompleted(h.id, today));
    const ordered = [...pending, ...done];

    list.innerHTML = ordered.map(h => {
      const completed = this._isCompleted(h.id, today);
      const streak = this._getStreak(h.id);
      const disc = this._getDisciplinePct(h.id);
      return `<div class="habit-check-card ${completed ? 'completed' : ''}" data-id="${h.id}">
        <div class="hcc-checkbox ${completed ? 'done' : ''}" data-id="${h.id}" style="${completed ? 'background:'+h.color+';border-color:'+h.color : 'border-color:'+h.color}"></div>
        <div class="hcc-emoji">${h.emoji || '✅'}</div>
        <div class="hcc-info">
          <div class="hcc-name ${completed ? 'striked' : ''}">${h.name}</div>
          <div class="hcc-meta">
            <span style="color:${h.color};font-weight:600">${h.frequency}</span>
            ${h.time ? '<span>⏰ ' + h.time + '</span>' : ''}
            ${streak > 0 ? '<span class="hcc-streak">🔥 ' + streak + ' day streak</span>' : ''}
          </div>
        </div>
        <div class="hcc-pct" style="color:${h.color}">${disc}%</div>
      </div>`;
    }).join('');

    list.querySelectorAll('.hcc-checkbox').forEach(cb => {
      cb.addEventListener('click', e => {
        e.stopPropagation();
        const id = parseInt(cb.dataset.id);
        this._toggleCompletion(id, today, cb);
        const card = list.querySelector(`.habit-check-card[data-id="${id}"]`);
        if (card) {
          const isNowDone = cb.classList.contains('done');
          card.classList.toggle('completed', isNowDone);
          const nm = card.querySelector('.hcc-name');
          if (nm) nm.classList.toggle('striked', isNowDone);
        }
        const pct2 = this._todayPct();
        this._updateScoreRing(pct2);
      });
    });
    // Click card also toggles
    list.querySelectorAll('.habit-check-card').forEach(card => {
      card.addEventListener('click', () => {
        const cb = card.querySelector('.hcc-checkbox');
        if (cb) cb.click();
      });
    });
  }

  _updateScoreRing(pct) {
    const scoreText = document.getElementById('scoreText');
    const scoreCircle = document.getElementById('scoreCircle');
    if (scoreText) scoreText.textContent = pct + '%';
    if (scoreCircle) {
      const circumference = 201;
      const offset = circumference - (pct / 100) * circumference;
      scoreCircle.style.strokeDashoffset = offset;
    }
  }

  // ─────────────────────────────────────
  // WEEKLY VIEW
  // ─────────────────────────────────────
  _renderWeekly() {
    const today = new Date();
    const base = new Date(today); base.setDate(today.getDate() + this.weekOffset * 7);
    const dow = base.getDay();
    const monday = new Date(base); monday.setDate(base.getDate() - (dow === 0 ? 6 : dow - 1));
    const weekDates = Array.from({length:7}, (_, i) => { const d = new Date(monday); d.setDate(monday.getDate() + i); return d; });

    const startLabel = weekDates[0].toLocaleDateString('en-US',{month:'short',day:'numeric'});
    const endLabel = weekDates[6].toLocaleDateString('en-US',{month:'short',day:'numeric',year:'numeric'});
    document.getElementById('weekLabel').textContent = `${startLabel} – ${endLabel}`;

    const grid = document.getElementById('weekGrid');
    if (!grid) return;
    if (this.habits.length === 0) {
      grid.innerHTML = '<div class="empty-state" style="padding:40px"><div class="empty-icon">📅</div><p>No habits to show.</p></div>';
      return;
    }
    const dayNames = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
    const cols = 1 + 7;
    const colTemplate = `200px repeat(7, 1fr)`;

    let html = `<div class="wg-header" style="display:grid;grid-template-columns:${colTemplate};gap:2px;margin-bottom:8px;">
      <div class="wg-habit-col">Habit</div>
      ${weekDates.map((d,i) => {
        const isToday = this._dateStr(d) === this._today();
        return `<div class="wg-day-col ${isToday ? 'today' : ''}">${dayNames[i]}<br><small>${d.getDate()}</small></div>`;
      }).join('')}
    </div>`;

    html += this.habits.map(h => {
      const cells = weekDates.map(d => {
        const ds = this._dateStr(d);
        const isFuture = d > today;
        const done = this._isCompleted(h.id, ds);
        const isToday = ds === this._dateStr(new Date());
        return `<div class="wg-cell">
          <div class="wg-tick ${done ? 'checked' : ''} ${isFuture ? 'future' : ''}" 
            data-habit="${h.id}" data-date="${ds}" 
            style="${done ? 'background:'+h.color+';border-color:'+h.color : (isToday ? 'border:2px dashed '+h.color+';' : '')}"></div>
        </div>`;
      }).join('');
      return `<div class="wg-row" style="display:grid;grid-template-columns:${colTemplate};gap:2px;margin-bottom:3px;">
        <div class="wg-habit-name">
          <div style="width:10px;height:10px;border-radius:50%;background:${h.color};flex-shrink:0"></div>
          <span>${h.emoji||'✅'} ${h.name}</span>
        </div>
        ${cells}
      </div>`;
    }).join('');

    grid.innerHTML = html;

    grid.querySelectorAll('.wg-tick:not(.future)').forEach(tick => {
      tick.addEventListener('click', () => {
        const habitId = parseInt(tick.dataset.habit);
        const dateStr = tick.dataset.date;
        this._toggleCompletion(habitId, dateStr, null);
        const done = this._isCompleted(habitId, dateStr);
        const habit = this.habits.find(h => h.id === habitId);
        const isToday = dateStr === this._dateStr(new Date());
        tick.classList.toggle('checked', done);
        tick.style.background = done ? (habit?.color || '') : '';
        tick.style.border = done ? `2px solid ${habit?.color || ''}` : (isToday ? `2px dashed ${habit?.color || ''}` : '');
      });
    });
  }

  // ─────────────────────────────────────
  // MONTHLY VIEW
  // ─────────────────────────────────────
  _renderMonthly() {
    const base = new Date();
    base.setMonth(base.getMonth() + this.monthOffset, 1);
    document.getElementById('monthLabel').textContent = base.toLocaleDateString('en-US',{month:'long',year:'numeric'});

    // Populate habit select
    const sel = document.getElementById('monthHabitSelect');
    const curVal = sel.value;
    sel.innerHTML = '<option value="">Select a habit…</option>' + this.habits.map(h => `<option value="${h.id}">${h.emoji||'✅'} ${h.name}</option>`).join('');
    if (curVal) sel.value = curVal;

    const habitId = parseInt(sel.value);
    const habit = this.habits.find(h => h.id === habitId);

    // Calendar
    const cal = document.getElementById('monthCalendar');
    const year = base.getFullYear(), month = base.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const today = new Date();
    const dayNames = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
    let html = dayNames.map(d => `<div class="mc-header">${d}</div>`).join('');
    const blanks = firstDay;
    for (let b = 0; b < blanks; b++) html += '<div class="mc-day empty"></div>';
    for (let d = 1; d <= daysInMonth; d++) {
      const dt = new Date(year, month, d);
      const ds = this._dateStr(dt);
      const isToday = ds === this._today();
      const isFuture = dt > today;
      const done = habit ? this._isCompleted(habitId, ds) : false;
      html += `<div class="mc-day ${isToday ? 'today' : ''} ${isFuture ? 'future' : ''} ${done ? 'completed' : ''}"
        ${!isFuture && habit ? `data-date="${ds}" data-habit="${habitId}"` : ''} style="${done ? 'background:'+habit?.color : ''}">
        ${d}
      </div>`;
    }
    cal.innerHTML = html;

    if (habit) {
      cal.querySelectorAll('.mc-day:not(.empty):not(.future)').forEach(cell => {
        if (cell.dataset.date) {
          cell.addEventListener('click', () => {
            const ds = cell.dataset.date;
            this._toggleCompletion(habitId, ds, null);
            const done = this._isCompleted(habitId, ds);
            cell.classList.toggle('completed', done);
            cell.style.background = done ? habit.color : '';
            this._renderMonthStats(habitId, year, month, daysInMonth, today);
          });
        }
      });
      this._renderMonthStats(habitId, year, month, daysInMonth, today);
    } else {
      document.getElementById('monthStatsRow').innerHTML = '';
    }
  }

  _renderMonthStats(habitId, year, month, daysInMonth, today) {
    let done = 0, total = 0;
    for (let d = 1; d <= daysInMonth; d++) {
      const dt = new Date(year, month, d);
      if (dt > today) continue;
      total++;
      if (this._isCompleted(habitId, this._dateStr(dt))) done++;
    }
    const pct = total === 0 ? 0 : Math.round((done / total) * 100);
    const streak = this._getStreak(habitId);
    const row = document.getElementById('monthStatsRow');
    row.innerHTML = `
      <div class="month-stat card"><div class="month-stat-val">${done}</div><div class="month-stat-label">Days Completed</div></div>
      <div class="month-stat card"><div class="month-stat-val">${pct}%</div><div class="month-stat-label">Completion Rate</div></div>
      <div class="month-stat card"><div class="month-stat-val">🔥 ${streak}</div><div class="month-stat-label">Current Streak</div></div>
    `;
  }

  // ─────────────────────────────────────
  // AI INSIGHTS
  // ─────────────────────────────────────
  _renderAI() {
    const grid = document.getElementById('aiInsightsGrid');
    if (!grid) return;
    grid.innerHTML = '<div class="ai-loading card"><div class="spin"></div> <span class="typewriter">Analyzing your habits…</span></div>';
    setTimeout(() => { grid.innerHTML = this._generateInsights().map(c => this._insightCard(c)).join(''); }, 800);
  }

  _generateInsights() {
    const today = this._today();
    const pct = this._todayPct();
    const bestStreak = this._bestStreak();
    const discipline = this._overallDiscipline();
    const totalHabits = this.habits.length;
    const daily = this.habits.filter(h => h.frequency === 'daily');
    const doneTodayCount = daily.filter(h => this._isCompleted(h.id, today)).length;
    const insights = [];

    if (totalHabits === 0) {
      insights.push({ icon:'🚀', type:'tip', title:'Get Started!', body:"You haven't added any habits yet. Start with 2-3 simple daily habits to build your foundation." });
    } else {
      if (pct === 100) insights.push({ icon:'🏆', type:'success', title:'Perfect Day!', body:`Incredible! You completed all ${doneTodayCount} habits today. This consistent behavior is the cornerstone of lasting change. Keep this streak going!` });
      else if (pct >= 75) insights.push({ icon:'🔥', type:'success', title:'Crushing It!', body:`You've completed ${doneTodayCount} out of ${daily.length} habits today (${pct}%). You're in the top performer zone. Just a little more!` });
      else if (pct >= 50) insights.push({ icon:'💪', type:'tip', title:'Good Momentum', body:`${pct}% completion today. Research shows completing 50%+ habits daily leads to positive habit formation within 21 days. Keep going!` });
      else if (pct > 0) insights.push({ icon:'⚡', type:'warning', title:'Momentum Needed', body:`Only ${pct}% today. Try habit stacking — attach each habit to an existing routine to make them automatic.` });
      else insights.push({ icon:'📋', type:'warning', title:'Start Your Streak!', body:"Today's habits are waiting. Even completing one habit builds momentum. Start with the easiest one right now!" });

      if (bestStreak >= 7) insights.push({ icon:'🌟', type:'success', title:`${bestStreak}-Day Streak!`, body:`You've maintained a ${bestStreak}-day streak. Neuroscience shows habit loops solidify at 21 days. You're building a lasting change!` });
      else if (bestStreak >= 3) insights.push({ icon:'📈', type:'insight', title:'Growing Streak', body:`${bestStreak} days strong! The hardest part is behind you. Studies show 66 days to form an automatic habit — you're making solid progress.` });

      if (discipline >= 80) insights.push({ icon:'🎯', type:'success', title:'High Discipline Score', body:`Your discipline score of ${discipline}% is exceptional. You're in the top 10% of habit trackers. This level of consistency leads to extraordinary results.` });
      else if (discipline < 40 && totalHabits > 0) insights.push({ icon:'🔄', type:'warning', title:'Discipline Boost Needed', body:`Your discipline score is ${discipline}%. Try reducing your habit count — focus on 3 core habits and master them before adding more.` });

      if (totalHabits > 8) insights.push({ icon:'✂️', type:'tip', title:'Simplify Your Stack', body:`You have ${totalHabits} habits. Research by James Clear shows focusing on 3-5 keystone habits produces better results than tracking many. Consider prioritizing.` });

      const weeklyHabits = this.habits.filter(h => h.frequency === 'weekly');
      if (weeklyHabits.length > 0) insights.push({ icon:'📅', type:'insight', title:'Weekly Habits', body:`You have ${weeklyHabits.length} weekly habit(s). Schedule them on specific days to improve completion rates by up to 40%.` });

      insights.push({ icon:'🧠', type:'tip', title:'AI Recommendation', body:this._aiRecommendation() });
    }
    return insights.slice(0, 6);
  }

  _aiRecommendation() {
    const hour = new Date().getHours();
    const pct = this._todayPct();
    if (pct === 100) return "You've mastered today. Try adding one new challenging habit tomorrow to keep growing!";
    if (hour < 10) return "Mornings are gold for habits. Complete your morning habits first — they set the tone for the entire day.";
    if (hour > 20) return "Evening wind-down: complete remaining habits now. Even late completion builds the neural pathways you need!";
    if (pct < 30) return "Try the 2-minute rule: if a habit takes less than 2 minutes, do it immediately when you think of it.";
    return "You're building a system. Track 3 consecutive perfect days and reward yourself — positive reinforcement accelerates habit formation by 3x.";
  }

  _insightCard(c) {
    return `<div class="ai-insight-card">
      <div class="aic-header">
        <div class="aic-icon">${c.icon}</div>
        <span class="aic-type ${c.type}">${c.type.toUpperCase()}</span>
      </div>
      <div class="aic-title">${c.title}</div>
      <div class="aic-body">${c.body}</div>
    </div>`;
  }

  // AI CHAT
  async _sendChat() {
    const input = document.getElementById('chatInput');
    const msg = input.value.trim();
    if (!msg) return;
    input.value = '';
    input.disabled = true;
    this._appendChat(msg, 'user');

    // Show typing indicator
    const typingId = 'typing-' + Date.now();
    this._appendChatTyping(typingId);

    try {
      // Build habit context to send to OpenRouter
      const today = this._today();
      const habitCtx = this.habits.map(h => {
        const streak = this._getStreak(h.id);
        const disc = this._getDisciplinePct(h.id);
        const done = this._isCompleted(h.id, today);
        return `${h.name}(${h.frequency}, streak:${streak}, discipline:${disc}%, today:${done?'done':'pending'})`;
      }).join('; ') || 'No habits tracked yet';

      const r = await fetch('/api/social/ai/chat', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: msg,
          habitContext: habitCtx,
          userName: this.user?.name || 'User'
        })
      });

      document.getElementById(typingId)?.remove();

      if (r.ok) {
        const d = await r.json();
        this._appendChat(d.reply, 'ai');
      } else {
        this._appendChat(this._aiReply(msg.toLowerCase()), 'ai');
      }
    } catch(e) {
      document.getElementById(typingId)?.remove();
      this._appendChat(this._aiReply(msg.toLowerCase()), 'ai');
    } finally {
      input.disabled = false;
      input.focus();
    }
  }

  _appendChatTyping(id) {
    const messages = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.id = id;
    div.className = 'chat-msg ai-msg';
    div.innerHTML = `<div class="chat-bubble chat-typing"><span></span><span></span><span></span></div>`;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
  }


  _appendChat(msg, role) {
    const messages = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = `chat-msg ${role === 'ai' ? 'ai-msg' : 'user-msg'}`;
    div.innerHTML = `<div class="chat-bubble">${msg}</div>`;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
  }

  _aiReply(q) {
    const pct = this._todayPct();
    const streak = this._bestStreak();
    const disc = this._overallDiscipline();
    if (q.includes('streak')) return `Your best streak is ${streak} days! 🔥 Keep going daily to build lasting habits. The science: after 21 days, habits become automatic.`;
    if (q.includes('today') || q.includes('progress')) return `Today you've completed ${pct}% of your daily habits! ${pct >= 80 ? '🎉 Outstanding!' : pct >= 50 ? '💪 Great work!' : '⚡ Let\'s go — you got this!'}`;
    if (q.includes('discipline') || q.includes('score')) return `Your discipline score is ${disc}%. ${disc >= 70 ? '🏆 You\'re highly disciplined!' : disc >= 40 ? '📈 Growing steadily — keep pushing!' : '🎯 Focus on 2-3 core habits first to build your foundation.'}`;
    if (q.includes('add') || q.includes('habit') || q.includes('new')) return "To add a new habit, click the '+ Add Habit' button in the header or the ✚ floating button. Choose daily, weekly, or monthly and pick a reminder time!";
    if (q.includes('tips') || q.includes('advice') || q.includes('help')) return "Top 3 habit tips: 1️⃣ Start tiny — 2 min habits build momentum. 2️⃣ Stack habits after existing routines. 3️⃣ Celebrate small wins — dopamine reinforces behavior!";
    if (q.includes('best') || q.includes('recommend')) return "Based on your data, I recommend: morning habits first (highest completion), keeping daily habits under 5 until disciplined, and scheduling weekly habits on Monday for maximum accountability.";
    if (q.includes('miss') || q.includes('fail') || q.includes('broke')) return "Missing a day is fine — it's called a 'lapse', not a failure. The key: never miss twice. Research shows one missed day has minimal impact; missing two starts breaking the chain.";
    if (q.includes('motivat')) return `Motivation follows action — not the other way around! Start the habit for just 2 minutes and motivation will follow. Your ${streak}-day streak is proof you're capable! 💪`;
    return `Great question! Based on your ${this.habits.length} habits and ${disc}% discipline score, my advice: consistency beats perfection. Small daily actions compound into extraordinary results over time. What specific habit would you like help with?`;
  }

  // ─────────────────────────────────────
  // ALL HABITS VIEW
  // ─────────────────────────────────────
  _renderAllHabits() {
    const grid = document.getElementById('allHabitsGrid');
    if (!grid) return;
    let habits = [...this.habits];
    if (this.filterFreq !== 'all') habits = habits.filter(h => h.frequency === this.filterFreq);
    if (this.searchQuery) habits = habits.filter(h => h.name.toLowerCase().includes(this.searchQuery) || (h.desc||'').toLowerCase().includes(this.searchQuery));
    if (habits.length === 0) {
      grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">📝</div><p>No habits found. ${this.searchQuery || this.filterFreq !== 'all' ? 'Try a different filter.' : 'Add your first habit!'}</p>${!this.searchQuery && this.filterFreq === 'all' ? '<button class="btn-primary mt-2" id="addFromHabitsEmpty">+ Add Habit</button>' : ''}</div>`;
      const addBtn = document.getElementById('addFromHabitsEmpty');
      if (addBtn) addBtn.addEventListener('click', () => this._openModal());
      return;
    }
    const today = this._today();
    grid.innerHTML = habits.map(h => {
      const streak = this._getStreak(h.id);
      const disc = this._getDisciplinePct(h.id);
      const done = this._isCompleted(h.id, today);
      const freqClass = { daily:'freq-daily', weekly:'freq-weekly', monthly:'freq-monthly' }[h.frequency] || 'freq-daily';
      return `<div class="habit-card" data-id="${h.id}" style="--card-color:${h.color}">
        <div style="position:absolute;top:0;left:0;right:0;height:4px;background:${h.color};border-radius:16px 16px 0 0"></div>
        <div class="hc-top">
          <div class="hc-emoji" style="background:${h.color}15">${h.emoji||'✅'}</div>
          <div class="hc-info">
            <div class="hc-name">${h.name}</div>
            <div class="hc-category">${this._catLabel(h.category)}</div>
          </div>
          <div class="hc-actions">
            <button class="hc-action edit" data-action="edit" title="Edit"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
            <button class="hc-action del" data-action="delete" title="Delete"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
          </div>
        </div>
        <div class="hc-stats">
          <div><div class="hc-stat-val">${disc}%</div>Discipline</div>
          <div><div class="hc-stat-val">${streak}</div>Streak</div>
          <div><div class="hc-stat-val">${done ? '✅' : '⬜'}</div>Today</div>
        </div>
        <div class="hc-bar-track"><div class="hc-bar-fill" style="width:${disc}%;background:${h.color}"></div></div>
        <div class="hc-footer">
          <span class="hc-freq-badge ${freqClass}">${h.frequency}</span>
          ${streak > 0 ? `<span class="hc-streak">🔥 ${streak} day streak</span>` : '<span style="font-size:12px;color:var(--text3)">No streak yet</span>'}
        </div>
      </div>`;
    }).join('');

    grid.querySelectorAll('.hc-action[data-action="edit"]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const id = parseInt(btn.closest('.habit-card').dataset.id);
        this._openModal(id);
      });
    });
    grid.querySelectorAll('.hc-action[data-action="delete"]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const id = parseInt(btn.closest('.habit-card').dataset.id);
        this._openDeleteModal(id);
      });
    });
    // Click card to toggle today
    grid.querySelectorAll('.habit-card').forEach(card => {
      card.addEventListener('click', e => {
        if (e.target.closest('.hc-action')) return;
        const id = parseInt(card.dataset.id);
        this._toggleCompletion(id, today, null);
        const h = this.habits.find(x => x.id === id);
        const isNowDone = this._isCompleted(id, today);
        const todayCell = card.querySelector('.hc-stats div:last-child .hc-stat-val');
        if (todayCell) todayCell.textContent = isNowDone ? '✅' : '⬜';
        if (isNowDone) this._confetti(card);
      });
    });
  }

  _catLabel(cat) {
    return { health:'🏃 Health', mind:'🧘 Mindfulness', learn:'📚 Learning', work:'💼 Productivity', social:'👥 Social', finance:'💰 Finance', sleep:'😴 Sleep', custom:'⭐ Custom' }[cat] || cat;
  }

  _getCatColor(cat) {
    const p = cat ? cat.toLowerCase() : '';
    if (p.includes('health') || p.includes('sleep')) return '#7F77DD';
    if (p.includes('learn')) return '#378ADD';
    if (p.includes('mind')) return '#D4537E';
    if (p.includes('finance')) return '#1D9E75';
    if (p.includes('work') || p.includes('product')) return '#BA7517';
    return '#7F77DD';
  }

  // ─────────────────────────────────────
  // MODAL
  // ─────────────────────────────────────
  _openModal(id = null) {
    this.editingId = id;
    document.getElementById('modalTitle').textContent = id ? 'Edit Habit' : 'Add New Habit';
    const form = document.getElementById('habitForm');
    form.reset();
    document.getElementById('emojiGrid').style.display = 'none';

    if (id) {
      const h = this.habits.find(x => x.id === id);
      if (h) {
        document.getElementById('habitName').value = h.name;
        document.getElementById('habitCategory').value = h.category;
        document.getElementById('habitDesc').value = h.desc || '';
        document.getElementById('habitTime').value = h.time || '';
        document.querySelector(`input[name="frequency"][value="${h.frequency}"]`).checked = true;
        this.selectedEmoji = h.emoji || '✅';
        this.selectedColor = h.color || '#6366f1';
        document.getElementById('emojiBtn').textContent = this.selectedEmoji;
      }
    } else {
      this.selectedEmoji = '✅';
      this.selectedColor = '#6366f1';
      document.getElementById('emojiBtn').textContent = '✅';
    }

    // Sync color picker
    document.querySelectorAll('.color-dot').forEach(d => {
      d.classList.toggle('selected', d.dataset.color === this.selectedColor);
    });

    document.getElementById('habitModalOverlay').classList.add('open');
    setTimeout(() => document.getElementById('habitName').focus(), 100);
  }

  _closeModal() {
    document.getElementById('habitModalOverlay').classList.remove('open');
    this.editingId = null;
  }

  async _saveHabit() {
    const name = document.getElementById('habitName').value.trim();
    if (!name) { this._toast('Please enter a habit name', 'error'); return; }
    const category = document.getElementById('habitCategory').value;
    const frequency = document.querySelector('input[name="frequency"]:checked')?.value || 'daily';
    const time = document.getElementById('habitTime').value;
    const desc = document.getElementById('habitDesc').value.trim();
    const habit = { name, emoji: this.selectedEmoji, category, frequency, time, description: desc, color: this._getCatColor(category) };

    if (this.isApiMode) {
      try {
        if (this.editingId) {
          const res = await fetch(`/api/habits/${this.editingId}`, {
            method: 'PUT', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(habit), credentials: 'include'
          });
          const updated = await res.json();
          const idx = this.habits.findIndex(h => h.id === this.editingId);
          if (idx !== -1) this.habits[idx] = updated;
          this._toast('Habit updated! ✨', 'success');
        } else {
          const res = await fetch('/api/habits', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(habit), credentials: 'include'
          });
          this.habits.push(await res.json());
          this._toast('Habit added! 🎉', 'success');
        }
      } catch(e) { this._toast('Save failed: ' + e.message, 'error'); return; }
    } else {
      if (this.editingId) {
        const idx = this.habits.findIndex(h => h.id === this.editingId);
        if (idx !== -1) this.habits[idx] = { ...this.habits[idx], ...habit };
        this._toast('Habit updated! ✨', 'success');
      } else {
        habit.id = Date.now(); habit.createdAt = this._today();
        this.habits.push(habit);
        this._toast('Habit added! 🎉', 'success');
      }
      this._save();
    }
    this._closeModal();
    this._renderView(this.currentView);
    this._updateBadge();
  }

  _openDeleteModal(id) {
    this.deleteTarget = id;
    const h = this.habits.find(x => x.id === id);
    document.getElementById('deleteHabitName').textContent = h?.name || 'this habit';
    document.getElementById('deleteModalOverlay').classList.add('open');
  }

  _closeDeleteModal() {
    document.getElementById('deleteModalOverlay').classList.remove('open');
    this.deleteTarget = null;
  }

  async _confirmDelete() {
    if (!this.deleteTarget) return;
    if (this.isApiMode) {
      try {
        await fetch(`/api/habits/${this.deleteTarget}`, { method: 'DELETE', credentials: 'include' });
      } catch(e) { this._toast('Delete failed', 'error'); return; }
    }
    this.habits = this.habits.filter(h => h.id !== this.deleteTarget);
    delete this.completions[this.deleteTarget];
    if (!this.isApiMode) this._save();
    this._closeDeleteModal();
    this._toast('Habit deleted', 'info');
    this._renderView(this.currentView);
    this._updateBadge();
  }

  // ─────────────────────────────────────
  // UTILS
  // ─────────────────────────────────────
  _toast(msg, type = 'success') {
    const icons = { success:'✅', error:'❌', info:'ℹ️' };
    const stack = document.getElementById('toastStack');
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = `<span class="toast-icon">${icons[type]}</span><span>${msg}</span>`;
    stack.appendChild(t);
    setTimeout(() => { t.style.opacity = '0'; t.style.transform = 'translateX(50px)'; t.style.transition = '.3s ease'; setTimeout(() => t.remove(), 300); }, 3000);
  }

  // Public wrapper for other modules
  showToast(msg, type = 'success') {
    this._toast(msg, type);
  }

  _confetti(el) {
    if (!el) return;
    const rect = el.getBoundingClientRect ? el.getBoundingClientRect() : {left:200,top:200,width:20,height:20};
    const cx = rect.left + rect.width / 2, cy = rect.top + rect.height / 2;
    const colors = ['#6366f1','#8b5cf6','#ec4899','#22c55e','#f97316','#eab308'];
    for (let i = 0; i < 10; i++) {
      const p = document.createElement('div');
      const angle = (Math.PI * 2 * i) / 10;
      const vel = 40 + Math.random() * 40;
      p.style.cssText = `position:fixed;width:8px;height:8px;border-radius:50%;background:${colors[i%colors.length]};left:${cx}px;top:${cy}px;pointer-events:none;z-index:9999;`;
      document.body.appendChild(p);
      p.animate([{transform:'translate(0,0) scale(1)',opacity:1},{transform:`translate(${Math.cos(angle)*vel}px,${Math.sin(angle)*vel-20}px) scale(0)`,opacity:0}],{duration:700+Math.random()*300,easing:'ease-out'}).onfinish = () => p.remove();
    }
  }

  _animNum(id, target) {
    const el = document.getElementById(id);
    if (!el) return;
    const cur = parseInt(el.textContent) || 0;
    if (cur === target) return;
    let step = 0, steps = 20;
    const inc = (target - cur) / steps;
    const t = setInterval(() => {
      step++;
      el.textContent = Math.round(cur + inc * step);
      if (step >= steps) { el.textContent = target; clearInterval(t); }
    }, 400 / steps);
  }
}

// ─────────────────────────────────────
// BOOT
// ─────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  window.app = new ZenkoApp();
});

// ═══════════════════════════════════════════════════════════
// AI COACH MODULE
// ═══════════════════════════════════════════════════════════
window.aiCoachModule = {
  _md(text) {
    // Simple markdown → HTML converter
    return text
      .replace(/### (.+)/g, '<h3>$1</h3>')
      .replace(/## (.+)/g, '<h2>$1</h2>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      .replace(/<li>/g, '<li>').replace(/(<li>.*<\/li>\n?)+/gs, m => `<ul>${m}</ul>`)
      .replace(/\n\n/g, '</p><p>')
      .replace(/^(?!<[hul])/gm, '')
      .trim();
  },
  _show(id, html, isLoading) {
    const el = document.getElementById(id);
    if (!el) return;
    el.style.display = 'block';
    if (isLoading) {
      el.innerHTML = '<div class="ai-result-loading"><div class="spin"></div> Asking AI...</div>';
    } else {
      el.innerHTML = `<div>${html}</div>`;
    }
  },
  _populate(el, habits) {
    if (!el) return;
    el.innerHTML = '<option value="">Select a habit...</option>';
    (habits || []).forEach(h => {
      const o = document.createElement('option');
      o.value = h.id; o.textContent = `${h.emoji || '🎯'} ${h.name}`;
      el.appendChild(o);
    });
  },
  async runAutopsy() {
    const sel = document.getElementById('autopsyHabitSelect');
    const reason = document.getElementById('autopsyReason')?.value || '';
    if (!sel?.value) { window.app?.showToast('Select a habit first', 'error'); return; }
    const habit = (window.app?.habits || []).find(h => h.id == sel.value);
    if (!habit) return;
    this._show('autopsyResult', '', true);
    try {
      const r = await fetch('/api/social/ai/autopsy', {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ habitName: habit.name, streak: habit.currentStreak || 0, reason })
      });
      const d = await r.json();
      this._show('autopsyResult', this._md(d.report));
    } catch { this._show('autopsyResult', '<p>Unable to connect. Please try again.</p>'); }
  },
  async runDNA() {
    this._show('dnaResult', '', true);
    const habits = window.app?.habits || [];
    const summary = habits.map(h => `${h.name}(${h.frequency},streak:${h.currentStreak||0})`).join(', ');
    const userName = window.app?.user?.name || 'User';
    try {
      const r = await fetch('/api/social/ai/dna', {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ summary, userName })
      });
      const d = await r.json();
      this._show('dnaResult', this._md(d.profile));
    } catch { this._show('dnaResult', '<p>Unable to connect. Please try again.</p>'); }
  },
  async runAlert() {
    this._show('alertResult', '', true);
    const habits = window.app?.habits || [];
    if (!habits.length) {
      this._show('alertResult', '<p>Add some habits first to get predictive alerts.</p>');
      return;
    }
    // Find habit with most risk (lowest recent completions)
    const habit = habits[0]; // simplified
    try {
      const r = await fetch('/api/social/ai/alert', {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ habitName: habit.name, missRate: 35, timePattern: 'morning' })
      });
      const d = await r.json();
      this._show('alertResult', `<p>⚡ <strong>${habit.name}</strong> — Risk Alert</p><p>${d.alert}</p>`);
    } catch { this._show('alertResult', '<p>Unable to connect.</p>'); }
  },

  async generateHabits() {
    const persona = document.getElementById('habitPersona')?.value || 'healthy person';
    const count = parseInt(document.getElementById('habitCount')?.value) || 5;
    const context = document.getElementById('habitContext')?.value || '';
    const container = document.getElementById('generatedHabitsList');

    if (!container) return;

    container.innerHTML = '<div class="ai-loading"><div class="spin"></div> Generating habits with AI...</div>';

    try {
      const r = await fetch('/api/social/ai/generate-habits', {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ persona, count, context })
      });
      const d = await r.json();

      if (d.habits && d.habits.length > 0) {
        container.innerHTML = d.habits.map(h => `
          <div class="generated-habit card" style="margin-bottom:8px;padding:14px;display:flex;align-items:center;gap:12px;">
            <div style="font-size:24px">${h.emoji || '🎯'}</div>
            <div style="flex:1">
              <div style="font-weight:700;color:var(--text);margin-bottom:2px">${h.name}</div>
              <div style="font-size:11px;color:var(--text3);margin-bottom:6px">${h.description}</div>
              <div style="display:flex;gap:8px;flex-wrap:wrap">
                <span style="font-size:10px;padding:2px 8px;border-radius:12px;background:var(--primary-light);color:var(--primary);font-weight:600">${h.category}</span>
                <span style="font-size:10px;padding:2px 8px;border-radius:12px;background:var(--bg3);color:var(--text2);font-weight:600">${h.frequency}</span>
              </div>
            </div>
            <button class="btn-primary" style="padding:8px 12px;font-size:12px" onclick="window.app?.addAIGeneratedHabit(${JSON.stringify(h).replace(/"/g, '&quot;')})">+ Add</button>
          </div>
        `).join('');
      } else {
        container.innerHTML = '<p style="color:var(--text3);font-size:13px">Could not generate habits. AI may not be configured. Check server logs.</p>';
      }
    } catch (e) {
      container.innerHTML = '<p style="color:var(--red);font-size:13px">Error generating habits. Please try again.</p>';
    }
  },

  // Helper to add AI-generated habit to the list
  addAIGeneratedHabit(habitData) {
    // Convert to our habit format
    const habit = {
      name: habitData.name,
      emoji: habitData.emoji,
      category: habitData.category,
      frequency: habitData.frequency,
      description: habitData.description,
      color: this.categoryColor(habitData.category),
      time: '09:00', // default
      currentStreak: 0,
      bestStreak: 0
    };
    // Add to habits and save
    if (window.app) {
      window.app.habits.push(habit);
      window.app._save();
      window.app._toast(`Added "${habit.name}" to your habits!`, 'success');
      // Refresh views if needed
      if (window.app.currentView === 'habits') window.app._renderAllHabits();
    }
  },

  categoryColor(cat) {
    const colors = {
      health: '#ef4444', mind: '#6366f1', learn: '#8b5cf6',
      work: '#22c55e', social: '#f97316', finance: '#eab308',
      sleep: '#64748b', custom: '#ec4899'
    };
    return colors[cat] || '#6366f1';
  },

  init(habits) {
    // Populate habit selects
    this._populate(document.getElementById('autopsyHabitSelect'), habits);
    // Check AI status
    fetch('/api/game/profile', {credentials:'include'})
      .then(r => r.json())
      .then(d => {
        const badge = document.getElementById('aiKeyBadge');
        if (badge) {
          if (d.aiConfigured) { badge.textContent = '🟢 OpenRouter AI'; badge.classList.add('live'); }
          else { badge.textContent = '🟡 Demo Mode'; }
        }
      }).catch(() => {});
    // Bind generate habits button
    const genBtn = document.getElementById('generateHabitsBtn');
    if (genBtn) genBtn.onclick = () => this.generateHabits();
  }
};

// ═══════════════════════════════════════════════════════════
// SOCIAL MODULE
// ═══════════════════════════════════════════════════════════
window.socialModule = {
  feedInterval: null,
  duels: [],
  bonds: [],
  feed: [],

  async init(preload = false) {
    this._bindTabs();
    await this._loadFeed();
    await this._loadDuels();
    await this._loadBonds();
    if (!preload && !this.feedInterval) {
      this.feedInterval = setInterval(() => this._loadFeed(), 12000);
    }
    this._bindActions();
  },

  _bindTabs() {
    document.querySelectorAll('.social-tab').forEach(tab => {
      tab.onclick = () => {
        document.querySelectorAll('.social-tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.social-panel').forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        const panel = document.getElementById('stab-' + tab.dataset.stab);
        if (panel) panel.classList.add('active');
      };
    });
  },

  _bindActions() {
    const createDuel = document.getElementById('createDuelBtn');
    if (createDuel) createDuel.onclick = () => this._createDuel();
    const joinDuel = document.getElementById('joinDuelBtn');
    if (joinDuel) joinDuel.onclick = () => this._joinDuel();
    const createBond = document.getElementById('createBondBtn');
    if (createBond) createBond.onclick = () => this._createBond();
  },

  async _loadFeed() {
    const el = document.getElementById('globalFeed');
    if (!el) return;
    try {
      const r = await fetch('/api/social/feed', {credentials:'include'});
      if (!r.ok) {
        console.error('Feed failed:', r.status, await r.text());
        el.innerHTML = '<div class="feed-error">Unable to load feed. Not authenticated?</div>';
        return;
      }
      const events = await r.json();
      this.feed = events; // store for external use
      el.innerHTML = events.map(e => this._feedItemHTML(e)).join('');
    } catch (e) {
      console.error('Feed error:', e);
      el.innerHTML = '<div class="feed-error">Error loading feed</div>';
    }
  },

  _feedItemHTML(e) {
    const ago = e.minutesAgo ? `${e.minutesAgo}m ago` : 'just now';
    const streakBadge = e.streak > 1 ? `<span class="feed-streak">🔥 ${e.streak} days</span>` : '';
    return `<div class="feed-item">
      <div class="feed-emoji">${e.emoji || '✅'}</div>
      <div class="feed-info">
        <div class="feed-main">Someone in <strong>${e.city}</strong> completed <em>${e.habit}</em></div>
        <div class="feed-sub">${e.isReal ? '🟢 Real user' : '🌐 Global'}</div>
      </div>
      ${streakBadge}
      <div class="feed-time">${ago}</div>
    </div>`;
  },

  async _loadDuels() {
    const el = document.getElementById('duelsList');
    if (!el) return;
    try {
      const r = await fetch('/api/social/duels', {credentials:'include'});
      if (!r.ok) {
        console.error('Duels failed:', r.status, await r.text());
        el.innerHTML = '<div class="feed-error">Unable to load duels. Not authenticated?</div>';
        return;
      }
      const duels = await r.json();
      this.duels = duels; // store for external use
      el.innerHTML = duels.map(d => this._duelHTML(d)).join('') || '<div class="feed-empty">No active duels. Create one!</div>';
      // Bind leave buttons
      el.querySelectorAll('.unpair-btn').forEach(btn => {
        btn.onclick = (e) => {
          const duelId = parseInt(e.target.dataset.duelId);
          if (duelId) this._leaveDuel(duelId);
        };
      });
    } catch (e) {
      console.error('Duels error:', e);
      el.innerHTML = '<div class="feed-error">Error loading duels</div>';
    }
  },

  _duelHTML(d) {
    const total = d.daysTotal || 30;
    const challPct = Math.min(100, Math.round((d.challengerScore / total) * 100));
    const isActive = d.status === 'active';
    return `<div class="duel-card" data-duel-id="${d.id}">
      <div class="duel-header">
        <div class="duel-title">⚔️ ${d.habitName}</div>
        <span class="duel-status ${d.status}">${isActive ? '🟢 LIVE' : '⏳ Pending'}</span>
      </div>
      <div class="duel-vs">
        <div class="duel-player"><div class="duel-player-name">${d.challengerName || 'You'}</div><div class="duel-score">${d.challengerScore}</div></div>
        <div class="duel-vs-text">VS</div>
        <div class="duel-player"><div class="duel-player-name">${d.opponentName || 'Opponent'}</div><div class="duel-score">${d.opponentScore}</div></div>
      </div>
      <div class="duel-bar-wrap"><div class="duel-bar-fill" style="width:${challPct}%"></div></div>
      <div class="duel-code">Invite Code: <strong>${d.inviteCode}</strong> · ${total - Math.max(d.challengerScore, d.opponentScore)} days left</div>
      <button class="unpair-btn" data-duel-id="${d.id}" style="margin-top:10px;width:100%;padding:8px;border-radius:8px;background:rgba(239,68,68,0.1);color:var(--red);border:1px solid rgba(239,68,68,0.3);font-size:12px;font-weight:600;cursor:pointer;">Leave Duel</button>
    </div>`;
  },

  async _loadBonds() {
    const el = document.getElementById('bondsList');
    if (!el) return;
    try {
      const r = await fetch('/api/social/bonds', {credentials:'include'});
      if (!r.ok) {
        console.error('Bonds failed:', r.status, await r.text());
        el.innerHTML = '<div class="feed-error">Unable to load bonds. Not authenticated?</div>';
        return;
      }
      const bonds = await r.json();
      this.bonds = bonds; // store for external use
      el.innerHTML = bonds.map(b => this._bondHTML(b)).join('') || '<div class="feed-empty">No bonds yet. Create one!</div>';
      // Bind leave buttons
      el.querySelectorAll('.unpair-btn').forEach(btn => {
        btn.onclick = (e) => {
          const bondId = parseInt(e.target.dataset.bondId);
          if (bondId) this._leaveBond(bondId);
        };
      });
    } catch (e) {
      console.error('Bonds error:', e);
      el.innerHTML = '<div class="feed-error">Error loading bonds</div>';
    }
  },

  _bondHTML(b) {
    return `<div class="bond-card" data-bond-id="${b.id}">
      <div class="bond-title">🤝 ${b.habitName}</div>
      <div class="bond-users">
        <div class="bond-user"><div class="bond-user-name">${b.user1Name}</div><div class="bond-user-streak">${b.user1Streak} 🔥</div></div>
        <div class="bond-plus">+</div>
        <div class="bond-user"><div class="bond-user-name">${b.user2Name}</div><div class="bond-user-streak">${b.user2Streak} 🔥</div></div>
      </div>
      <div class="bond-shared">Shared Streak: ${b.sharedStreak} 🔥 ${b.status === 'pending' ? '· Invite Code: <strong>' + b.inviteCode + '</strong>' : ''}</div>
      <button class="unpair-btn" data-bond-id="${b.id}" style="margin-top:10px;width:100%;padding:8px;border-radius:8px;background:rgba(239,68,68,0.1);color:var(--red);border:1px solid rgba(239,68,68,0.3);font-size:12px;font-weight:600;cursor:pointer;">Dissolve Bond</button>
    </div>`;
  },

  async _createDuel() {
    const habits = window.app?.habits || [];
    if (!habits.length) { window.app?.showToast('Add a habit first', 'error'); return; }
    const habit = habits[0];
    try {
      const r = await fetch('/api/social/duels', {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({
          habitName: habit.name,
          habitId: habit.id,
          challengerName: window.app?.user?.name || 'You'
        })
      });
      const d = await r.json();
      window.app?.showToast(`Duel created! Share code: ${d.inviteCode}`, 'success');
      this._loadDuels();
    } catch { window.app?.showToast('Error creating duel', 'error'); }
  },

  async _joinDuel() {
    const code = document.getElementById('duelCodeInput')?.value?.trim().toUpperCase();
    if (!code) { window.app?.showToast('Enter an invite code', 'error'); return; }
    try {
      const r = await fetch(`/api/social/duels/${code}/join`, {method:'POST', credentials:'include'});
      if (r.ok) { window.app?.showToast('Joined duel! 🎮', 'success'); this._loadDuels(); }
      else { window.app?.showToast('Invalid or already-started duel', 'error'); }
    } catch { window.app?.showToast('Error joining duel', 'error'); }
  },

  async _createBond() {
    const habits = window.app?.habits || [];
    if (!habits.length) { window.app?.showToast('Add a habit first', 'error'); return; }
    const habit = habits[0];
    try {
      const r = await fetch('/api/social/bonds', {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({
          habitName: habit.name,
          habitId: habit.id,
          user1Name: window.app?.user?.name || 'You'
        })
      });
      const d = await r.json();
      window.app?.showToast(`Bond created! Share code: ${d.inviteCode}`, 'success');
      this._loadBonds();
    } catch { window.app?.showToast('Error creating bond', 'error'); }
  },

  async _leaveDuel(duelId) {
    if (!confirm('Leave this duel? The duel will end.')) return;
    try {
      const r = await fetch(`/api/social/duels/${duelId}`, {method: 'DELETE', credentials: 'include'});
      if (r.ok) {
        window.app?.showToast('Duel ended', 'success');
        this._loadDuels();
      } else {
        const err = await r.json();
        window.app?.showToast(err.error || 'Error leaving duel', 'error');
      }
    } catch { window.app?.showToast('Error leaving duel', 'error'); }
  },

  async _leaveBond(bondId) {
    if (!confirm('Dissolve this accountability bond?')) return;
    try {
      const r = await fetch(`/api/social/bonds/${bondId}`, {method: 'DELETE', credentials: 'include'});
      if (r.ok) {
        window.app?.showToast('Bond dissolved', 'success');
        this._loadBonds();
      } else {
        const err = await r.json();
        window.app?.showToast(err.error || 'Error dissolving bond', 'error');
      }
    } catch { window.app?.showToast('Error dissolving bond', 'error'); }
  }
};

// ═══════════════════════════════════════════════════════════
// GAME MODULE
// ═══════════════════════════════════════════════════════════
window.gameModule = {
  profile: null,
  levelTitles: ['Habit Novice','Rookie Builder','Habit Seeker','Dedicated Warrior',
                'Streak Master','Elite Performer','Habit Legend','Iron Discipline',
                'Ascended Mind','Zenko Champion'],

  async init() {
    await this._loadProfile();
    this._renderGame();
    await this._loadQuests();
    this._bindActions();
  },

  _bindActions() {
    const useToken = document.getElementById('useTokenBtn');
    if (useToken) useToken.onclick = () => this._useToken();
    const refreshQ = document.getElementById('refreshQuestsBtn');
    if (refreshQ) refreshQ.onclick = () => this._loadQuests(true);
  },

  async _loadProfile() {
    try {
      const r = await fetch('/api/game/profile', {credentials:'include'});
      if (r.ok) this.profile = await r.json();
    } catch {}
  },

  _renderGame() {
    const p = this.profile;
    if (!p) return;
    const level = p.level || 1;
    const title = this.levelTitles[level - 1] || 'Zenko Champion';

    // Level badge
    const badge = document.getElementById('levelBadge');
    if (badge) { badge.textContent = `Lv.${level}`; badge.classList.add('xp-pop'); setTimeout(() => badge.classList.remove('xp-pop'), 400); }
    const titleEl = document.getElementById('levelTitle');
    if (titleEl) titleEl.textContent = title;
    const xpEl = document.getElementById('levelXP');
    if (xpEl) xpEl.textContent = `${p.xp} / ${p.xpForNextLevel} XP`;
    const barEl = document.getElementById('levelBarFill');
    if (barEl) barEl.style.width = (p.levelProgress || 0) + '%';

    // Tokens
    const tokenCount = document.getElementById('tokenCount');
    if (tokenCount) tokenCount.textContent = p.streakTokens || 0;
    const tokenDisplay = document.getElementById('tokenDisplay');
    if (tokenDisplay) {
      tokenDisplay.innerHTML = Array.from({length: 5}, (_, i) =>
        `<span class="token-icon-item ${i >= (p.streakTokens || 0) ? 'used' : ''}">🛡️</span>`
      ).join('');
    }

    // Skill trees
    this._renderSkillTree('health', p.healthLevel, p.healthXp, [
      {lv:2,'u':'Unlocked: Morning Habit Templates 🌅'},
      {lv:3,'u':'Unlocked: Body Scan Tracker 🧘'},
      {lv:4,'u':'Unlocked: Health Quest Pack 💪'},
      {lv:5,'u':'Unlocked: Iron Body Badge 🏋️'}
    ]);
    this._renderSkillTree('mind', p.mindLevel, p.mindXp, [
      {lv:2,'u':'Unlocked: Focus Mode Timer ⏱️'},
      {lv:3,'u':'Unlocked: Meditation Streaks 🧘'},
      {lv:4,'u':'Unlocked: Deep Work Templates 🧠'},
      {lv:5,'u':'Unlocked: Enlightened Mind Badge 🌟'}
    ]);
    this._renderSkillTree('wealth', p.wealthLevel, p.wealthXp, [
      {lv:2,'u':'Unlocked: Finance Habit Pack 💰'},
      {lv:3,'u':'Unlocked: Wealth Score Tracker 📈'},
      {lv:4,'u':'Unlocked: Investment Quests 🚀'},
      {lv:5,'u':'Unlocked: Wealth Architect Badge 👑'}
    ]);
  },

  _renderSkillTree(name, level, xp, unlocks) {
    const lvlEl = document.getElementById(name + 'Level');
    const barEl = document.getElementById(name + 'Bar');
    const unlockEl = document.getElementById(name + 'Unlocks');
    if (lvlEl) lvlEl.textContent = `Level ${level || 1}`;
    // XP progress within level (0-350 range simplified)
    const pct = Math.min(100, Math.round(((xp || 0) % 350) / 3.5));
    if (barEl) barEl.style.width = pct + '%';
    if (unlockEl) {
      const unlocked = unlocks.filter(u => (level || 1) >= u.lv);
      unlockEl.textContent = unlocked.length ? unlocked[unlocked.length-1].u : '🔒 Keep going to unlock';
    }
  },

  async _loadQuests(refresh) {
    const el = document.getElementById('questsGrid');
    if (!el) return;
    el.innerHTML = '<div class="feed-loading"><div class="spin" style="display:inline-block;margin-right:8px"></div> Generating quests...</div>';
    const habits = window.app?.habits || [];
    const summary = habits.map(h => h.name).join(', ');
    try {
      const r = await fetch(`/api/game/quests?habitsSummary=${encodeURIComponent(summary)}`, {credentials:'include'});
      const d = await r.json();
      let quests = [];
      try { quests = JSON.parse(d.quests); } catch { quests = []; }
      const completed = (this.profile?.completedQuestIds || '').split(',').filter(Boolean);
      el.innerHTML = quests.map(q => this._questHTML(q, completed.includes(String(q.id)))).join('');
      // Bind complete buttons
      el.querySelectorAll('.quest-complete-btn:not(:disabled)').forEach(btn => {
        btn.onclick = () => this._completeQuest(btn.dataset.id, parseInt(btn.dataset.xp));
      });
    } catch { el.innerHTML = '<div class="feed-loading">Could not load quests</div>'; }
  },

  _questHTML(q, done) {
    const diffColor = q.difficulty === 'Easy' ? 'var(--green)' : q.difficulty === 'Hard' ? '#ef4444' : 'var(--orange)';
    return `<div class="quest-card ${done ? 'done' : ''}">
      <div class="quest-emoji">${q.emoji || '⚡'}</div>
      <div class="quest-info">
        <div class="quest-title">${q.title}</div>
        <div class="quest-desc">${q.description}</div>
      </div>
      <div class="quest-right">
        <div class="quest-xp">+${q.xpReward} XP</div>
        <div class="quest-difficulty" style="color:${diffColor}">${q.difficulty}</div>
        <button class="quest-complete-btn" data-id="${q.id}" data-xp="${q.xpReward}" ${done ? 'disabled' : ''}>${done ? '✅ Done' : 'Complete'}</button>
      </div>
    </div>`;
  },

  async _completeQuest(id, xp) {
    try {
      const r = await fetch(`/api/game/complete-quest/${id}`, {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ xpReward: xp })
      });
      if (r.ok) {
        const d = await r.json();
        window.app?.showToast(`Quest complete! +${xp} XP 🎉`, 'success');
        if (this.profile) {
          this.profile.xp = d.xp;
          this.profile.level = d.level;
          this.profile.questsCompleted = d.questsCompleted;
          const ids = (this.profile.completedQuestIds || '') + ',' + id;
          this.profile.completedQuestIds = ids;
        }
        await this._loadProfile();
        this._renderGame();
        this._loadQuests();
      }
    } catch { window.app?.showToast('Error completing quest', 'error'); }
  },

  async _useToken() {
    if ((this.profile?.streakTokens || 0) <= 0) {
      window.app?.showToast('No freeze tokens left! Complete quests to earn more.', 'error'); return;
    }
    try {
      const r = await fetch('/api/game/use-token', {method:'POST', credentials:'include'});
      if (r.ok) {
        const d = await r.json();
        window.app?.showToast('🛡️ Streak protected! Token used.', 'success');
        if (this.profile) this.profile.streakTokens = d.streakTokens;
        this._renderGame();
      }
    } catch { window.app?.showToast('Error using token', 'error'); }
  }
};

// ═══════════════════════════════════════════════════════════
// Hook: After habits load, init AI coach + award XP for completions
// ═══════════════════════════════════════════════════════════
const _origLoad = ZenkoApp.prototype._loadAll;
ZenkoApp.prototype._loadAll = async function() {
  if (typeof _origLoad === 'function') {
    await _origLoad.call(this);
  }
  // Init AI coach habit selects
  if (window.aiCoachModule) window.aiCoachModule.init(this.habits);
  // Preload social data for dashboard: initialize social module early
  if (window.socialModule) {
    try {
      await window.socialModule.init(true); // preload = true, no feed interval
      // Copy loaded data to window.app for badge and other uses
      window.app.socialDuels = window.socialModule.duels || [];
      window.app.socialBonds = window.socialModule.bonds || [];
      window.app.socialFeed = window.socialModule.feed || [];
      window.app.socialPreloaded = true; // flag for dashboard
    } catch (e) { console.error('Social init error:', e); }
  }
  // Award XP for any habit-tree categories
  this.habits.forEach(h => {
    if (!h.category) return;
    const cat = h.category.toLowerCase();
    const mapped = cat.includes('health') || cat.includes('sleep') ? 'Health' :
                   cat.includes('mind') || cat.includes('learn') ? 'Mind' :
                   cat.includes('finance') || cat.includes('work') ? 'Wealth' : null;
    if (mapped && h.currentStreak > 0) {
      fetch('/api/game/add-xp', {
        method: 'POST', credentials: 'include',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ amount: Math.min(h.currentStreak * 2, 20), category: mapped })
      }).catch(() => {});
    }
  });
};
