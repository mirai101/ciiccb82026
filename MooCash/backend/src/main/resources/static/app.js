const API_BASE_URL = '/api';

const state = {
  token: localStorage.getItem('token'),
  user: null,
  isAdmin: false,
  accounts: [],
  totalBalance: 0,
  transactions: [],
  loans: [],
  cards: [],
  admin: { customers: [], accounts: [], transactions: [], cards: [], loans: [] },
  adminLoaded: false,
  activeTab: 'home',
  txLoaded: false
};

const viewAuth = document.getElementById('view-auth');
const appShell = document.getElementById('app-shell');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const loginTab = document.getElementById('login-tab');
const registerTab = document.getElementById('register-tab');
const toastContainer = document.getElementById('toast-container');
const modalOverlay = document.getElementById('modal-overlay');
const modalTitle = document.getElementById('modal-title');
const modalForm = document.getElementById('modal-form');

async function init() {
  bindStaticEvents();
  if (state.token) {
    const ok = await fetchUserDetails();
    if (ok) {
      enterApp();
      return;
    }
  }
  showAuth();
}

function bindStaticEvents() {
  loginTab.addEventListener('click', () => switchAuthTab('login'));
  registerTab.addEventListener('click', () => switchAuthTab('register'));
  document.getElementById('go-register').addEventListener('click', () => switchAuthTab('register'));
  document.getElementById('go-login').addEventListener('click', () => switchAuthTab('login'));

  loginForm.addEventListener('submit', handleLogin);
  registerForm.addEventListener('submit', handleRegister);

  document.getElementById('logout-btn').addEventListener('click', logout);
  document.getElementById('logout-btn-profile').addEventListener('click', logout);

  document.querySelectorAll('[data-tab]').forEach(el => {
    el.addEventListener('click', () => showTab(el.getAttribute('data-tab')));
  });

  document.getElementById('qa-topup').addEventListener('click', () => openDepositModal());
  document.getElementById('send-form').addEventListener('submit', handleSend);
  document.getElementById('send-another-btn').addEventListener('click', resetSendForm);
  document.getElementById('copy-handle-btn').addEventListener('click', copyAccountId);
  document.getElementById('change-password-btn').addEventListener('click', openChangePasswordModal);

  document.getElementById('request-loan-btn').addEventListener('click', openRequestLoanModal);
  document.getElementById('request-card-btn').addEventListener('click', openRequestCardModal);
  document.getElementById('admin-refresh-btn').addEventListener('click', () => loadAdmin(true));

  document.querySelectorAll('.filter-tab[data-filter]').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.filter-tab[data-filter]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      renderTransactionList('full-ledger', filterTransactions(btn.getAttribute('data-filter')));
    });
  });

  document.querySelectorAll('.filter-tab[data-admin-tab]').forEach(btn => {
    btn.addEventListener('click', () => showAdminSubtab(btn.getAttribute('data-admin-tab')));
  });

  modalOverlay.addEventListener('click', (e) => {
    if (e.target === modalOverlay) closeModal();
  });
}

function switchAuthTab(which) {
  const isLogin = which === 'login';
  loginTab.classList.toggle('active', isLogin);
  registerTab.classList.toggle('active', !isLogin);
  loginForm.classList.toggle('hidden', !isLogin);
  registerForm.classList.toggle('hidden', isLogin);
}

function showAuth() {
  appShell.classList.remove('active');
  viewAuth.classList.add('active');
}

function enterApp() {
  viewAuth.classList.remove('active');
  appShell.classList.add('active');
  applyUserToChrome();
  showTab('home');
}

async function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  const btn = loginForm.querySelector('button[type="submit"]');
  setBtnLoading(btn, 'Logging in…');

  try {
    const res = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok) {
      state.token = data.token;
      localStorage.setItem('token', data.token);
      await fetchUserDetails();
      enterApp();
      notify('Welcome back!', 'success');
    } else {
      notify(data.message || 'Login failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  } finally {
    resetBtnLoading(btn, 'Log in <span style="font-family:var(--mono),serif">→</span>');
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const fullName = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const phone = document.getElementById('reg-phone').value.trim();
  const password = document.getElementById('reg-password').value;
  const phoneError = document.getElementById('reg-phone-error');

  if (!/^0[0-9]{10}$/.test(phone)) {
    phoneError.classList.add('show');
    return;
  }
  phoneError.classList.remove('show');

  const btn = registerForm.querySelector('button[type="submit"]');
  setBtnLoading(btn, 'Creating account…');

  try {
    const res = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email, password, phone })
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok) {
      notify('Account created — log in to continue', 'success');
      switchAuthTab('login');
      document.getElementById('login-email').value = email;
      registerForm.reset();
    } else {
      notify(data.message || 'Registration failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  } finally {
    resetBtnLoading(btn, 'Create account');
  }
}

function logout() {
  state.token = null;
  state.user = null;
  state.isAdmin = false;
  state.accounts = [];
  state.transactions = [];
  state.loans = [];
  state.cards = [];
  state.adminLoaded = false;
  localStorage.removeItem('token');
  showAuth();
  notify('Logged out', 'success');
}

async function fetchUserDetails() {
  try {
    const res = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.status === 401) {
      localStorage.removeItem('token');
      state.token = null;
      return false;
    }
    if (res.ok) {
      state.user = await res.json();
      state.isAdmin = !!(state.user && state.user.role && String(state.user.role).toUpperCase() === 'ADMIN');
      return true;
    }
    return false;
  } catch (err) {
    return false;
  }
}

function applyUserToChrome() {
  const name = (state.user && (state.user.fullName || state.user.name)) || 'MooCash user';
  const email = (state.user && state.user.email) || '';
  const first = name.trim().split(' ')[0];
  const parts = name.trim().split(/\s+/);
  let initials = parts[0].charAt(0);
  if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
  initials = initials.toUpperCase();

  ['sidebar-avatar', 'home-avatar', 'activity-avatar', 'profile-avatar'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.textContent = initials;
  });
  document.getElementById('sidebar-name').textContent = name;
  document.getElementById('sidebar-email').textContent = email || '@moocash';
  document.getElementById('home-greeting').textContent = `Hey, ${first} 🐄`;
  document.getElementById('profile-name').textContent = name;
  document.getElementById('profile-email').textContent = email;
  document.getElementById('home-date').textContent = new Date().toLocaleDateString(undefined, {
    weekday: 'long', month: 'long', day: 'numeric'
  });

  document.getElementById('admin-nav-link').classList.toggle('hidden', !state.isAdmin);
  document.getElementById('home-admin-banner').classList.toggle('hidden', !state.isAdmin);
  document.getElementById('profile-admin-btn').classList.toggle('hidden', !state.isAdmin);
}

async function showTab(name) {
  if (name === 'admin' && !state.isAdmin) return;
  state.activeTab = name;
  document.querySelectorAll('.tab-panel').forEach(p => p.style.display = 'none');
  const panel = document.getElementById('tab-' + name);
  panel.style.display = 'block';
  panel.classList.remove('anim-in');
  void panel.offsetWidth;
  panel.classList.add('anim-in');

  document.querySelectorAll('.side-link, .bn-item').forEach(b => {
    b.classList.toggle('active', b.getAttribute('data-tab') === name);
  });
  window.scrollTo({ top: 0, behavior: 'smooth' });

  if (name === 'home') await loadHome();
  else if (name === 'accounts') await loadAccounts();
  else if (name === 'send') await loadSend();
  else if (name === 'receive') await loadReceive();
  else if (name === 'loans') await loadLoans();
  else if (name === 'cards') await loadCards();
  else if (name === 'activity') await loadActivity();
  else if (name === 'admin') await loadAdmin();
}

async function fetchAccounts(force) {
  if (state.accounts.length && !force) return state.accounts;
  const [accounts, totalData] = await Promise.all([
    fetchWithAuth('/accounts/my-accounts'),
    fetchWithAuth('/accounts/total-balance')
  ]);
  state.accounts = accounts || [];
  state.totalBalance = (totalData && typeof totalData.totalBalance === 'number') ? totalData.totalBalance : 0;
  return state.accounts;
}

function primaryAccount() {
  const visible = state.accounts.filter(a => !a.isHidden);
  return visible[0] || state.accounts[0] || null;
}

async function loadHome() {
  showLedgerSkeleton('home-ledger', 3);
  document.getElementById('balance-sub').textContent = 'Loading accounts…';

  await fetchAccounts();
  animateBalance(state.totalBalance);

  const visibleCount = state.accounts.filter(a => !a.isHidden).length;
  document.getElementById('balance-sub').textContent =
    visibleCount > 0 ? `${visibleCount} active account${visibleCount === 1 ? '' : 's'}` : 'No accounts yet';

  if (state.accounts.length === 0) {
    renderEmptyLedger('home-ledger', 'No accounts yet', 'Open your first MooCash account to start sending and receiving.', 'Open account', openAccountModal);
    document.getElementById('herd-list').innerHTML = '';
    return;
  }

  await fetchTransactions();
  renderTransactionList('home-ledger', state.transactions.slice(0, 5));
  renderHerd();
}

function animateBalance(target) {
  const mainEl = document.getElementById('balance-main');
  const centsEl = document.getElementById('balance-cents');
  const start = 0;
  const duration = 700;
  const startTime = performance.now();

  function tick(now) {
    const p = Math.min(1, (now - startTime) / duration);
    const eased = 1 - Math.pow(1 - p, 3);
    const val = start + (target - start) * eased;
    const [whole, cents] = val.toFixed(2).split('.');
    mainEl.textContent = Number(whole).toLocaleString();
    centsEl.textContent = '.' + cents;
    if (p < 1) requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
}

function renderHerd() {
  const counts = {};
  state.transactions.forEach(t => {
    const other = t.toAccount && t.toAccount !== (primaryAccount() || {}).accountId ? t.toAccount : null;
    if (other) counts[other] = (counts[other] || 0) + 1;
  });
  const top = Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 4);
  const list = document.getElementById('herd-list');
  if (top.length === 0) {
    list.innerHTML = '';
    return;
  }
  list.innerHTML = top.map(([acctId]) => {
    const initials = acctId.slice(-2).toUpperCase();
    return `
      <div class="herd-item" data-acct="${acctId}">
        <div class="avatar">${initials}</div>
        ${acctId.slice(-6)}
      </div>`;
  }).join('');
  list.querySelectorAll('.herd-item').forEach(item => {
    item.addEventListener('click', () => {
      showTab('send');
      setTimeout(() => {
        document.getElementById('to-account').value = item.getAttribute('data-acct');
      }, 50);
    });
  });
}

async function loadAccounts() {
  const grid = document.getElementById('accounts-grid');
  grid.innerHTML = `
    <div class="skel" style="height:190px; border-radius:20px;"></div>
    <div class="skel" style="height:190px; border-radius:20px;"></div>`;
  await fetchAccounts(true);
  renderAccountsGrid();
}

function renderAccountsGrid() {
  const grid = document.getElementById('accounts-grid');
  const tiles = state.accounts.map((a, i) => {
    const type = (a.type || 'CHECKING').toUpperCase();
    const isSavings = type.includes('SAVING');
    return `
      <div class="acct-tile ${isSavings ? 'savings' : ''}" style="animation-delay:${i * 0.04}s">
        <div class="acct-tile-top">
          <div class="acct-chip"></div>
          <span class="acct-type-tag">${type}</span>
        </div>
        <div class="acct-number">${a.accountId}</div>
        <div class="acct-tile-bottom">
          <div><small>Balance</small><div class="bal">$${(a.balance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div></div>
          <span class="badge ${badgeClassForStatus(a.status)}">${a.status || (a.isHidden ? 'Hidden' : 'Active')}</span>
        </div>
        <div class="acct-actions">
          <button data-act="deposit" data-id="${a.accountId}">Deposit</button>
          <button data-act="withdraw" data-id="${a.accountId}">Withdraw</button>
          <button data-act="card" data-id="${a.accountId}">Get card</button>
          <button data-act="visibility" data-id="${a.accountId}">${a.isHidden ? 'Unhide' : 'Hide'}</button>
        </div>
      </div>`;
  }).join('');

  grid.innerHTML = tiles + `
    <div class="add-acct-tile" id="add-account-tile">
      <div class="plus">+</div>
      <div>Open a new account</div>
    </div>`;

  grid.querySelectorAll('button[data-act]').forEach(btn => {
    const id = btn.getAttribute('data-id');
    const act = btn.getAttribute('data-act');
    btn.addEventListener('click', () => {
      if (act === 'deposit') openDepositModal(id);
      else if (act === 'withdraw') openWithdrawModal(id);
      else if (act === 'card') openRequestCardModal(id);
      else if (act === 'visibility') toggleAccountVisibility(id);
    });
  });
  document.getElementById('add-account-tile').addEventListener('click', openAccountModal);
}

function badgeClassForStatus(status) {
  const s = (status || 'ACTIVE').toUpperCase();
  if (s.includes('BLOCK')) return 'badge-blocked';
  if (s.includes('HOLD') || s.includes('HELD')) return 'badge-held';
  return 'badge-active';
}

async function toggleAccountVisibility(accountId) {
  try {
    const res = await fetch(`${API_BASE_URL}/accounts/${accountId}/toggle-visibility`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      notify('Visibility updated', 'success');
      await fetchAccounts(true);
      renderAccountsGrid();
    } else {
      const data = await res.json().catch(() => ({}));
      notify(data.message || 'Could not update visibility', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  }
}

async function fetchTransactions(force) {
  if (state.txLoaded && !force) return state.transactions;
  const tx = await fetchWithAuth('/transactions/my-transactions');
  state.transactions = tx || [];
  state.txLoaded = true;
  return state.transactions;
}

function isIncoming(t) {
  const type = (t.type || '').toUpperCase();
  return type.includes('DEPOSIT') || type.includes('IN');
}

function filterTransactions(kind) {
  if (kind === 'all') return state.transactions;
  if (kind === 'in') return state.transactions.filter(isIncoming);
  return state.transactions.filter(t => !isIncoming(t));
}

function renderTransactionList(elId, list) {
  const el = document.getElementById(elId);
  if (!list || list.length === 0) {
    renderEmptyLedger(elId, 'No activity yet', 'Transactions will show up here once money starts moving.');
    return;
  }
  el.innerHTML = list.map((t, i) => {
    const incoming = isIncoming(t);
    const date = t.timestamp ? new Date(t.timestamp) : null;
    const dateStr = date && !isNaN(date.getTime())
      ? date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' · ' + date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
      : '';
    const label = t.description || (t.type || 'Transaction').replace(/_/g, ' ');
    const counterpart = incoming ? (t.fromAccount || '') : (t.toAccount || '');
    return `
      <div class="tx-row" style="animation-delay:${i * 0.03}s">
        <div class="tx-ic ${incoming ? 'in' : 'out'}">${incoming ? '↙' : '↗'}</div>
        <div class="tx-info">
          <div class="tx-name">${escapeHtml(label)}</div>
          <div class="tx-meta">${dateStr}${counterpart ? ' · ' + counterpart.slice(-8) : ''}</div>
        </div>
        <div class="tx-amt ${incoming ? 'in' : 'out'}">${incoming ? '+' : '-'}$${(t.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
      </div>`;
  }).join('');
}

function renderEmptyLedger(elId, title, body, actionLabel, actionFn) {
  const el = document.getElementById(elId);
  el.innerHTML = `
    <div class="empty-row">
      <div style="font-weight:600; color:var(--cream); margin-bottom:6px;">${title}</div>
      <div>${body}</div>
      ${actionLabel ? `<button class="btn btn-primary btn-auto" style="margin-top:14px;" id="empty-action-${elId}">${actionLabel}</button>` : ''}
    </div>`;
  if (actionLabel) {
    document.getElementById(`empty-action-${elId}`).addEventListener('click', actionFn);
  }
}

function showLedgerSkeleton(elId, rows) {
  const el = document.getElementById(elId);
  let html = '';
  for (let i = 0; i < rows; i++) {
    html += `
      <div class="skel-row">
        <div class="skel skel-circle"></div>
        <div style="flex:1;">
          <div class="skel skel-line" style="width:60%; margin-bottom:6px;"></div>
          <div class="skel skel-line" style="width:35%; height:9px;"></div>
        </div>
        <div class="skel skel-line" style="width:50px;"></div>
      </div>`;
  }
  el.innerHTML = html;
}

async function loadActivity() {
  showLedgerSkeleton('full-ledger', 6);
  document.querySelectorAll('.filter-tab[data-filter]').forEach(b => b.classList.remove('active'));
  document.querySelector('.filter-tab[data-filter="all"]').classList.add('active');
  await fetchTransactions();
  renderTransactionList('full-ledger', state.transactions);
}

async function loadSend() {
  document.getElementById('send-success').classList.add('hidden');
  document.getElementById('send-form').classList.remove('hidden');
  await fetchAccounts();
  const select = document.getElementById('from-account');
  if (state.accounts.length === 0) {
    select.innerHTML = '<option value="">No accounts available</option>';
    document.getElementById('send-submit-btn').disabled = true;
    document.getElementById('send-balance-hint').textContent = 'Open an account first to send money.';
    return;
  }
  document.getElementById('send-submit-btn').disabled = false;
  select.innerHTML = state.accounts.map(a =>
    `<option value="${a.accountId}">${a.type} · ${a.accountId.slice(-6)} ($${(a.balance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })})</option>`
  ).join('');
  updateSendHint();
  select.onchange = updateSendHint;
}

function updateSendHint() {
  const select = document.getElementById('from-account');
  const acct = state.accounts.find(a => a.accountId === select.value);
  document.getElementById('send-balance-hint').textContent = acct
    ? `Available: $${(acct.balance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`
    : '';
}

async function handleSend(e) {
  e.preventDefault();
  const fromAccountId = document.getElementById('from-account').value;
  const toAccountId = document.getElementById('to-account').value.trim();
  const amount = parseFloat(document.getElementById('send-amount').value);
  const description = document.getElementById('send-desc').value.trim();

  if (!fromAccountId || !toAccountId || isNaN(amount) || amount <= 0) {
    notify('Fill in every field with a valid amount', 'error');
    return;
  }

  const btn = document.getElementById('send-submit-btn');
  setBtnLoading(btn, 'Sending…');

  try {
    const res = await fetch(`${API_BASE_URL}/accounts/transfer`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${state.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ fromAccountId, toAccountId, amount, description })
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok) {
      document.getElementById('send-form').classList.add('hidden');
      const successPanel = document.getElementById('send-success');
      successPanel.classList.remove('hidden');
      successPanel.classList.remove('anim-in'); void successPanel.offsetWidth; successPanel.classList.add('anim-in');
      document.getElementById('success-amount').textContent = `$${amount.toLocaleString(undefined, { minimumFractionDigits: 2 })} sent`;
      document.getElementById('success-detail').textContent = `to ${toAccountId.slice(-8)}${description ? ' · ' + description : ''}`;
      state.txLoaded = false;
      await fetchAccounts(true);
      notify('Transfer sent', 'success');
    } else {
      notify(data.message || 'Transfer failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  } finally {
    resetBtnLoading(btn, 'Review and send');
  }
}

function resetSendForm() {
  document.getElementById('send-form').reset();
  document.getElementById('send-form').classList.remove('hidden');
  document.getElementById('send-success').classList.add('hidden');
  loadSend();
}

async function loadReceive() {
  await fetchAccounts();
  const visible = state.accounts.filter(a => !a.isHidden);
  const picker = document.getElementById('account-picker-field');
  const select = document.getElementById('receive-account-select');

  if (visible.length === 0) {
    document.getElementById('receive-account-id').textContent = 'No account yet';
    document.getElementById('copy-account-id').textContent = '—';
    buildQR('no-account');
    picker.style.display = 'none';
    return;
  }

  if (visible.length > 1) {
    picker.style.display = 'block';
    select.innerHTML = visible.map(a => `<option value="${a.accountId}">${a.type} · ${a.accountId.slice(-6)}</option>`).join('');
    select.onchange = () => displayReceiveAccount(select.value);
  } else {
    picker.style.display = 'none';
  }
  displayReceiveAccount(visible[0].accountId);
}

function displayReceiveAccount(accountId) {
  document.getElementById('receive-account-id').textContent = accountId;
  document.getElementById('copy-account-id').textContent = accountId;
  buildQR(accountId);
}

function buildQR(seed) {
  const box = document.getElementById('qr-box');
  let hash = 0;
  for (let i = 0; i < seed.length; i++) hash = (hash * 31 + seed.charCodeAt(i)) >>> 0;
  let html = '';
  for (let i = 0; i < 81; i++) {
    hash = (hash * 1103515245 + 12345) >>> 0;
    const on = (hash % 100) > 45;
    html += `<div class="qr-cell ${on ? '' : 'off'}"></div>`;
  }
  box.innerHTML = html;
  box.classList.remove('anim-in'); void box.offsetWidth; box.classList.add('anim-in');
}

function copyAccountId() {
  const id = document.getElementById('copy-account-id').textContent;
  if (!id || id === '—') return;
  navigator.clipboard.writeText(id).catch(() => {});
  const btn = document.getElementById('copy-handle-btn');
  const original = btn.textContent;
  btn.textContent = 'Copied';
  btn.classList.add('copied');
  setTimeout(() => { btn.textContent = original; btn.classList.remove('copied'); }, 1200);
}

async function fetchLoans(force) {
  if (state.loans.length && !force) return state.loans;
  const loans = await fetchWithAuth('/loans/my-loans');
  state.loans = loans || [];
  return state.loans;
}

async function loadLoans() {
  document.getElementById('loan-metrics').innerHTML = `
    <div class="skel" style="height:64px;"></div><div class="skel" style="height:64px;"></div><div class="skel" style="height:64px;"></div>`;
  showLedgerSkeleton('loan-list', 3);
  await Promise.all([fetchAccounts(), fetchLoans(true)]);
  renderLoans();
}

function renderLoans() {
  const total = state.loans.length;
  const outstanding = state.loans.reduce((s, l) => s + (l.remainingBalance || 0), 0);
  const pending = state.loans.filter(l => (l.status || '').toUpperCase() === 'PENDING').length;

  document.getElementById('loan-metrics').innerHTML = `
    <div class="metric-card"><div class="lbl">Total loans</div><div class="val">${total}</div></div>
    <div class="metric-card"><div class="lbl">Outstanding</div><div class="val">$${outstanding.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div></div>
    <div class="metric-card"><div class="lbl">Pending</div><div class="val">${pending}</div></div>`;

  const list = document.getElementById('loan-list');
  if (state.loans.length === 0) {
    list.innerHTML = `
      <div class="ledger"><div class="empty-row">
        <div style="font-weight:600; color:var(--cream); margin-bottom:6px;">No loans yet</div>
        <div>Request a loan and it'll show up here once an administrator reviews it.</div>
      </div></div>`;
    return;
  }

  list.innerHTML = state.loans.map((l, i) => {
    const status = (l.status || 'PENDING').toUpperCase();
    const canRepay = status === 'APPROVED' && (l.remainingBalance || 0) > 0;
    return `
      <div class="loan-card" style="animation-delay:${i * 0.04}s">
        <div class="loan-top">
          <span class="loan-id">${l.loanId || l.id || ''}</span>
          <span class="badge badge-${status.toLowerCase()}">${status}</span>
        </div>
        <div class="loan-grid">
          <div><small>Amount</small><span>$${(l.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span></div>
          <div><small>Remaining</small><span>$${(l.remainingBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span></div>
          <div><small>Interest rate</small><span>${l.interestRate ?? 0}%</span></div>
        </div>
        ${canRepay ? `<button class="btn btn-primary btn-auto repay-btn" data-loan="${l.loanId || l.id}" data-remaining="${l.remainingBalance || 0}">Repay</button>` : ''}
      </div>`;
  }).join('');

  list.querySelectorAll('.repay-btn').forEach(btn => {
    btn.addEventListener('click', () => openRepayLoanModal(btn.getAttribute('data-loan'), parseFloat(btn.getAttribute('data-remaining'))));
  });
}

function openRequestLoanModal() {
  openModal('Request a loan', `
    <div class="field">
      <label>Amount</label>
      <input type="number" id="loan-amount" min="1" step="0.01" placeholder="0.00" required>
    </div>
    <div class="field">
      <label>Proposed interest rate (%)</label>
      <input type="number" id="loan-rate" min="0" step="0.01" value="5" required>
    </div>
    <div class="field-hint">Requests are reviewed by a MooCash administrator before funds are released.</div>
    <button type="submit" class="btn btn-primary" style="margin-top:14px;">Submit request</button>
  `, async (e) => {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('loan-amount').value);
    const interestRate = parseFloat(document.getElementById('loan-rate').value);
    if (isNaN(amount) || amount <= 0) { notify('Enter a valid amount', 'error'); return; }

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Submitting…');
    try {
      const res = await fetch(`${API_BASE_URL}/loans/request`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ amount, interestRate })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Loan request submitted', 'success');
        closeModal();
        await loadLoans();
      } else {
        notify(data.message || 'Request failed', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Submit request');
    }
  });
}

async function openRepayLoanModal(loanId, remaining) {
  await fetchAccounts();
  if (state.accounts.length === 0) { notify('You need an account to repay from', 'error'); return; }
  const options = state.accounts.map(a => `<option value="${a.accountId}">${a.type} · ${a.accountId.slice(-6)} ($${(a.balance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })})</option>`).join('');
  openModal('Repay loan', `
    <div class="field-hint" style="margin-bottom:14px;">Remaining balance: $${remaining.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
    <div class="field">
      <label>Pay from account</label>
      <select id="repay-account">${options}</select>
    </div>
    <div class="field">
      <label>Amount</label>
      <input type="number" id="repay-amount" min="0.01" max="${remaining}" step="0.01" value="${remaining}" required>
    </div>
    <button type="submit" class="btn btn-primary">Confirm repayment</button>
  `, async (e) => {
    e.preventDefault();
    const fromAccountId = document.getElementById('repay-account').value;
    const amount = parseFloat(document.getElementById('repay-amount').value);
    if (isNaN(amount) || amount <= 0) { notify('Enter a valid amount', 'error'); return; }

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Processing…');
    try {
      const res = await fetch(`${API_BASE_URL}/loans/repay?loanId=${encodeURIComponent(loanId)}&fromAccountId=${encodeURIComponent(fromAccountId)}&amount=${amount}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Repayment successful', 'success');
        closeModal();
        await fetchAccounts(true);
        await loadLoans();
      } else {
        notify(data.message || 'Repayment failed', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Confirm repayment');
    }
  });
}

async function fetchCards(force) {
  if (state.cards.length && !force) return state.cards;
  const cards = await fetchWithAuth('/accounts/my-cards');
  state.cards = cards || [];
  return state.cards;
}

async function loadCards() {
  document.getElementById('card-grid').innerHTML = `<div class="skel" style="height:150px; border-radius:20px;"></div>`;
  await fetchCards(true);
  renderCards();
}

function renderCards() {
  const grid = document.getElementById('card-grid');
  if (state.cards.length === 0) {
    grid.innerHTML = `
      <div class="ledger" style="grid-column:1/-1;"><div class="empty-row">
        <div style="font-weight:600; color:var(--cream); margin-bottom:6px;">No cards yet</div>
        <div>Request a MooCash VISA or Mastercard against one of your accounts.</div>
      </div></div>`;
    return;
  }
  grid.innerHTML = state.cards.map((c, i) => {
    const isVisa = (c.cardType || '').toUpperCase().includes('VISA');
    return `
      <div class="credit-card ${isVisa ? 'visa' : 'master'}" style="animation-delay:${i * 0.05}s">
        <div class="cc-top">
          <span class="cc-brand">${isVisa ? 'MOOCASH VISA' : 'MOOCASH MASTER'}</span>
          <span style="font-family:var(--mono),serif; opacity:.5; font-size:11px;">(oo)</span>
        </div>
        <div class="cc-number">${c.cardNumber || '•••• •••• •••• ••••'}</div>
        <div class="cc-bottom">
          <div><small>Card holder</small>${c.cardHolderName || (state.user && state.user.fullName) || ''}</div>
          <div><small>Expires</small>${c.expiryDate || '--/--'}</div>
        </div>
      </div>`;
  }).join('');
}

async function openRequestCardModal(presetAccountId) {
  await fetchAccounts();
  if (state.accounts.length === 0) { notify('You need an account before requesting a card', 'error'); return; }
  const options = state.accounts.map(a => `<option value="${a.accountId}" ${a.accountId === presetAccountId ? 'selected' : ''}>${a.type} · ${a.accountId.slice(-6)}</option>`).join('');
  openModal('Request a card', `
    <div class="field">
      <label>Linked account</label>
      <select id="card-account">${options}</select>
    </div>
    <div class="field">
      <label>Card type</label>
      <select id="card-type">
        <option value="MooCashVISA">MooCash VISA</option>
        <option value="MooCashMASTER">MooCash Mastercard</option>
      </select>
    </div>
    <button type="submit" class="btn btn-primary">Issue card</button>
  `, async (e) => {
    e.preventDefault();
    const accountId = document.getElementById('card-account').value;
    const cardType = document.getElementById('card-type').value;

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Issuing…');
    try {
      const res = await fetch(`${API_BASE_URL}/accounts/${accountId}/issue-card?cardType=${cardType}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Card issued', 'success');
        closeModal();
        await loadCards();
      } else {
        notify(data.message || 'Could not issue card', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Issue card');
    }
  });
}

function openModal(title, formHtml, onSubmit) {
  modalTitle.textContent = title;
  modalForm.innerHTML = formHtml;
  modalForm.onsubmit = onSubmit;
  modalOverlay.classList.add('active');
}

function closeModal() {
  modalOverlay.classList.remove('active');
  modalForm.onsubmit = null;
}

async function openDepositModal(presetAccountId) {
  await fetchAccounts();
  if (state.accounts.length === 0) {
    openAccountModal();
    return;
  }
  const options = state.accounts.map(a => `<option value="${a.accountId}" ${a.accountId === presetAccountId ? 'selected' : ''}>${a.type} · ${a.accountId.slice(-6)}</option>`).join('');
  openModal('Deposit funds', `
    <div class="field">
      <label>Account</label>
      <select id="topup-account">${options}</select>
    </div>
    <div class="field">
      <label>Amount</label>
      <input type="number" id="topup-amount" min="0.01" step="0.01" placeholder="0.00" required>
    </div>
    <div class="field">
      <label>Note (optional)</label>
      <input type="text" id="topup-desc" placeholder="Top up">
    </div>
    <button type="submit" class="btn btn-primary">Confirm deposit</button>
  `, async (e) => {
    e.preventDefault();
    const accountId = document.getElementById('topup-account').value;
    const amount = parseFloat(document.getElementById('topup-amount').value);
    const description = document.getElementById('topup-desc').value.trim() || 'Deposit';
    if (isNaN(amount) || amount <= 0) { notify('Enter a valid amount', 'error'); return; }

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Processing…');
    try {
      const res = await fetch(`${API_BASE_URL}/accounts/deposit`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountId, amount, description })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Deposit successful', 'success');
        closeModal();
        state.txLoaded = false;
        await fetchAccounts(true);
        if (state.activeTab === 'accounts') renderAccountsGrid(); else await loadHome();
      } else {
        notify(data.message || 'Deposit failed', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Confirm deposit');
    }
  });
}

async function openWithdrawModal(accountId) {
  const acct = state.accounts.find(a => a.accountId === accountId);
  openModal('Withdraw funds', `
    <div class="field-hint" style="margin-bottom:14px;">Available: $${((acct && acct.balance) || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
    <div class="field">
      <label>Amount</label>
      <input type="number" id="withdraw-amount" min="0.01" step="0.01" max="50000" placeholder="0.00" required>
      <div class="field-hint">Max per transaction: $50,000</div>
    </div>
    <div class="field">
      <label>Note (optional)</label>
      <input type="text" id="withdraw-desc" placeholder="Withdrawal">
    </div>
    <button type="submit" class="btn btn-primary">Confirm withdrawal</button>
  `, async (e) => {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('withdraw-amount').value);
    const description = document.getElementById('withdraw-desc').value.trim() || 'Withdrawal';
    if (isNaN(amount) || amount <= 0) { notify('Enter a valid amount', 'error'); return; }

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Processing…');
    try {
      const res = await fetch(`${API_BASE_URL}/accounts/withdraw`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountId, amount, description })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Withdrawal successful', 'success');
        closeModal();
        state.txLoaded = false;
        await fetchAccounts(true);
        if (state.activeTab === 'accounts') renderAccountsGrid(); else await loadHome();
      } else {
        notify(data.message || 'Withdrawal failed', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Confirm withdrawal');
    }
  });
}

function openAccountModal() {
  openModal('Open an account', `
    <div class="field">
      <label>Account type</label>
      <select id="open-acc-type">
        <option value="CHECKING">Checking</option>
        <option value="SAVINGS">Savings</option>
      </select>
    </div>
    <div class="field">
      <label>Initial deposit</label>
      <input type="number" id="open-acc-deposit" min="0" step="0.01" value="100" required>
    </div>
    <button type="submit" class="btn btn-primary">Open account</button>
  `, async (e) => {
    e.preventDefault();
    const type = document.getElementById('open-acc-type').value;
    const amount = parseFloat(document.getElementById('open-acc-deposit').value);
    if (isNaN(amount) || amount < 0) { notify('Enter a valid deposit amount', 'error'); return; }

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Opening…');
    try {
      const res = await fetch(`${API_BASE_URL}/accounts/open?type=${type}&initialDeposit=${amount}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Account opened', 'success');
        closeModal();
        await fetchAccounts(true);
        if (state.activeTab === 'accounts') renderAccountsGrid(); else await loadHome();
      } else {
        notify(data.message || 'Could not open account', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Open account');
    }
  });
}

function openChangePasswordModal() {
  openModal('Change password', `
    <div class="field">
      <label>Current password</label>
      <input type="password" id="cp-current" required>
    </div>
    <div class="field">
      <label>New password</label>
      <input type="password" id="cp-new" minlength="6" required>
    </div>
    <button type="submit" class="btn btn-primary">Update password</button>
  `, async (e) => {
    e.preventDefault();
    const currentPassword = document.getElementById('cp-current').value;
    const newPassword = document.getElementById('cp-new').value;
    if (newPassword.length < 6) { notify('New password must be at least 6 characters', 'error'); return; }

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Updating…');
    try {
      const res = await fetch(`${API_BASE_URL}/auth/change-password`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ currentPassword, newPassword })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Password updated', 'success');
        closeModal();
      } else {
        notify(data.message || 'Could not update password', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Update password');
    }
  });
}

async function loadAdmin(force) {
  if (!state.isAdmin) return;
  if (state.adminLoaded && !force) {
    renderAdminAll();
    return;
  }
  ['admin-customers-body', 'admin-accounts-body', 'admin-loans-body', 'admin-cards-body', 'admin-transactions-body'].forEach(id => {
    document.getElementById(id).innerHTML = `<tr><td colspan="8"><div class="skel skel-line" style="width:100%; height:14px;"></div></td></tr>`;
  });

  const [customers, accounts, transactions, cards, loans] = await Promise.all([
    fetchWithAuth('/accounts/admin/all-customers'),
    fetchWithAuth('/accounts/admin/all-accounts'),
    fetchWithAuth('/transactions/admin/all'),
    fetchWithAuth('/accounts/admin/all-cards'),
    fetchWithAuth('/loans/admin/all')
  ]);

  state.admin = {
    customers: customers || [],
    accounts: accounts || [],
    transactions: transactions || [],
    cards: cards || [],
    loans: loans || []
  };
  state.adminLoaded = true;
  renderAdminAll();
  notify('Admin data refreshed', 'success');
}

function showAdminSubtab(name) {
  document.querySelectorAll('.filter-tab[data-admin-tab]').forEach(b => b.classList.toggle('active', b.getAttribute('data-admin-tab') === name));
  document.querySelectorAll('.admin-section').forEach(s => s.classList.remove('active'));
  document.getElementById('admin-' + name).classList.add('active');
}

function customerName(customerId) {
  const c = state.admin.customers.find(x => x.id === customerId || x.customerId === customerId || x._id === customerId);
  return c ? (c.fullName || c.name || c.email) : (customerId || '—');
}

function renderAdminAll() {
  const { customers, accounts, transactions, cards, loans } = state.admin;
  const totalBalance = accounts.reduce((s, a) => s + (a.balance || 0), 0);
  const activeAccounts = accounts.filter(a => (a.status || 'ACTIVE').toUpperCase() === 'ACTIVE').length;
  const pendingLoans = loans.filter(l => (l.status || '').toUpperCase() === 'PENDING').length;
  const activeLoans = loans.filter(l => (l.status || '').toUpperCase() === 'APPROVED').length;

  document.getElementById('admin-metrics').innerHTML = `
    <div class="metric-card"><div class="lbl">Total customers</div><div class="val">${customers.length}</div></div>
    <div class="metric-card"><div class="lbl">Total accounts</div><div class="val">${accounts.length}</div></div>
    <div class="metric-card"><div class="lbl">Active accounts</div><div class="val">${activeAccounts}</div></div>`;
  document.getElementById('admin-metrics-2').innerHTML = `
    <div class="metric-card"><div class="lbl">Total balance</div><div class="val">$${totalBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div></div>
    <div class="metric-card"><div class="lbl">Pending loans</div><div class="val">${pendingLoans}</div></div>
    <div class="metric-card"><div class="lbl">Active loans</div><div class="val">${activeLoans}</div></div>`;

  renderAdminCustomers();
  renderAdminAccounts();
  renderAdminLoans();
  renderAdminCards();
  renderAdminTransactions();
}

function renderAdminCustomers() {
  const body = document.getElementById('admin-customers-body');
  const list = state.admin.customers;
  if (list.length === 0) { body.innerHTML = `<tr><td colspan="5">No customers found.</td></tr>`; return; }
  body.innerHTML = list.map(c => {
    const id = c.id || c.customerId || c._id;
    const role = (c.role || 'USER').toUpperCase();
    return `
      <tr>
        <td>${escapeHtml(c.fullName || c.name || '—')}</td>
        <td class="mono">${escapeHtml(c.email || '—')}</td>
        <td class="mono">${escapeHtml(c.phone || '—')}</td>
        <td><span class="badge ${role === 'ADMIN' ? 'badge-admin' : 'badge-user'}">${role}</span></td>
        <td>
          <div class="row-actions">
            <button class="icon-btn" title="Change password" data-cust-pw="${id}">🔑</button>
            <button class="icon-btn danger" title="Delete customer" data-cust-del="${id}" ${role === 'ADMIN' ? 'disabled' : ''}>🗑</button>
          </div>
        </td>
      </tr>`;
  }).join('');

  body.querySelectorAll('[data-cust-pw]').forEach(btn => btn.addEventListener('click', () => adminChangeCustomerPassword(btn.getAttribute('data-cust-pw'))));
  body.querySelectorAll('[data-cust-del]').forEach(btn => btn.addEventListener('click', () => adminDeleteCustomer(btn.getAttribute('data-cust-del'))));
}

function renderAdminAccounts() {
  const body = document.getElementById('admin-accounts-body');
  const list = state.admin.accounts;
  if (list.length === 0) { body.innerHTML = `<tr><td colspan="6">No accounts found.</td></tr>`; return; }
  body.innerHTML = list.map(a => {
    const status = (a.status || 'ACTIVE').toUpperCase();
    return `
      <tr>
        <td class="mono">${a.accountId.slice(-10)}</td>
        <td>${escapeHtml(customerName(a.customerId || a.ownerId || a.userId))}</td>
        <td>${a.type || '—'}</td>
        <td class="mono">$${(a.balance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
        <td><span class="badge ${badgeClassForStatus(status)}">${status}</span></td>
        <td>
          <div class="row-actions">
            <button class="icon-btn" title="Activate" data-acc-status="${a.accountId}|ACTIVE" ${status === 'ACTIVE' ? 'disabled' : ''}>✓</button>
            <button class="icon-btn" title="Hold" data-acc-status="${a.accountId}|HELD" ${status === 'HELD' ? 'disabled' : ''}>⏸</button>
            <button class="icon-btn" title="Block" data-acc-status="${a.accountId}|BLOCKED" ${status === 'BLOCKED' ? 'disabled' : ''}>⛔</button>
            <button class="icon-btn danger" title="Delete account" data-acc-del="${a.accountId}">🗑</button>
          </div>
        </td>
      </tr>`;
  }).join('');

  body.querySelectorAll('[data-acc-status]').forEach(btn => {
    const [accountId, status] = btn.getAttribute('data-acc-status').split('|');
    btn.addEventListener('click', () => adminUpdateAccountStatus(accountId, status));
  });
  body.querySelectorAll('[data-acc-del]').forEach(btn => btn.addEventListener('click', () => adminDeleteAccount(btn.getAttribute('data-acc-del'))));
}

function renderAdminLoans() {
  const body = document.getElementById('admin-loans-body');
  const list = state.admin.loans;
  if (list.length === 0) { body.innerHTML = `<tr><td colspan="8">No loans found.</td></tr>`; return; }
  body.innerHTML = list.map(l => {
    const id = l.loanId || l.id;
    const status = (l.status || 'PENDING').toUpperCase();
    return `
      <tr>
        <td class="mono">${String(id).slice(-8)}</td>
        <td>${escapeHtml(customerName(l.customerId || l.userId || l.borrowerId))}</td>
        <td class="mono">$${(l.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
        <td class="mono">$${(l.remainingBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
        <td>${l.interestRate ?? 0}%</td>
        <td><span class="badge badge-${status.toLowerCase()}">${status}</span></td>
        <td>${l.autoDebtEnabled ? 'On' : 'Off'}</td>
        <td>
          <div class="row-actions">
            <button class="icon-btn" title="Approve" data-loan-approve="${id}" ${status !== 'PENDING' ? 'disabled' : ''}>✓</button>
            <button class="icon-btn danger" title="Reject" data-loan-reject="${id}" ${status !== 'PENDING' ? 'disabled' : ''}>✕</button>
            <button class="icon-btn" title="Toggle auto-debt" data-loan-autodebt="${id}|${!l.autoDebtEnabled}">↻</button>
          </div>
        </td>
      </tr>`;
  }).join('');

  body.querySelectorAll('[data-loan-approve]').forEach(btn => btn.addEventListener('click', () => adminApproveLoanPrompt(btn.getAttribute('data-loan-approve'))));
  body.querySelectorAll('[data-loan-reject]').forEach(btn => btn.addEventListener('click', () => adminRejectLoan(btn.getAttribute('data-loan-reject'))));
  body.querySelectorAll('[data-loan-autodebt]').forEach(btn => {
    const [loanId, enabled] = btn.getAttribute('data-loan-autodebt').split('|');
    btn.addEventListener('click', () => adminToggleAutoDebt(loanId, enabled === 'true'));
  });
}

function renderAdminCards() {
  const body = document.getElementById('admin-cards-body');
  const list = state.admin.cards;
  if (list.length === 0) { body.innerHTML = `<tr><td colspan="5">No cards found.</td></tr>`; return; }
  body.innerHTML = list.map(c => `
    <tr>
      <td class="mono">${c.cardNumber || '—'}</td>
      <td>${escapeHtml(c.cardHolderName || '—')}</td>
      <td>${(c.cardType || '').includes('VISA') ? 'VISA' : 'Mastercard'}</td>
      <td class="mono">${c.expiryDate || '--/--'}</td>
      <td><button class="icon-btn danger" title="Revoke card" data-card-del="${c.cardId || c.id}">🗑</button></td>
    </tr>`).join('');

  body.querySelectorAll('[data-card-del]').forEach(btn => btn.addEventListener('click', () => adminDeleteCard(btn.getAttribute('data-card-del'))));
}

function renderAdminTransactions() {
  const body = document.getElementById('admin-transactions-body');
  const list = state.admin.transactions.slice(0, 100);
  if (list.length === 0) { body.innerHTML = `<tr><td colspan="5">No transactions found.</td></tr>`; return; }
  body.innerHTML = list.map(t => {
    const date = t.timestamp ? new Date(t.timestamp) : null;
    const dateStr = date && !isNaN(date.getTime()) ? date.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—';
    return `
      <tr>
        <td class="mono">${dateStr}</td>
        <td class="mono">${(t.accountId || '').slice(-10)}</td>
        <td>${(t.type || '').replace(/_/g, ' ')}</td>
        <td class="mono">$${(t.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
        <td>${escapeHtml(t.description || '—')}</td>
      </tr>`;
  }).join('');
}

async function adminUpdateAccountStatus(accountId, status) {
  try {
    const res = await fetch(`${API_BASE_URL}/accounts/admin/accounts/${accountId}/status?status=${status}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      notify(`Account ${status.toLowerCase()}`, 'success');
      await loadAdmin(true);
    } else {
      const data = await res.json().catch(() => ({}));
      notify(data.message || 'Action failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  }
}

async function adminDeleteAccount(accountId) {
  if (!confirm('Delete this account? This also removes its transactions and cards. This cannot be undone.')) return;
  try {
    const res = await fetch(`${API_BASE_URL}/accounts/admin/accounts/${accountId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      notify('Account deleted', 'success');
      await loadAdmin(true);
    } else {
      const data = await res.json().catch(() => ({}));
      notify(data.message || 'Delete failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  }
}

async function adminDeleteCustomer(customerId) {
  if (!confirm('Delete this customer? All of their accounts, cards, and loans will also be removed. This cannot be undone.')) return;
  try {
    const res = await fetch(`${API_BASE_URL}/accounts/admin/customers/${customerId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      notify('Customer deleted', 'success');
      await loadAdmin(true);
    } else {
      const data = await res.json().catch(() => ({}));
      notify(data.message || 'Delete failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  }
}

function adminChangeCustomerPassword(customerId) {
  openModal('Reset customer password', `
    <div class="field">
      <label>New password</label>
      <input type="password" id="admin-new-pw" minlength="6" required>
    </div>
    <button type="submit" class="btn btn-primary">Update password</button>
  `, async (e) => {
    e.preventDefault();
    const newPassword = document.getElementById('admin-new-pw').value;
    if (newPassword.length < 6) { notify('Password must be at least 6 characters', 'error'); return; }

    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Updating…');
    try {
      const res = await fetch(`${API_BASE_URL}/auth/admin/change-password/${customerId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ newPassword })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Password reset', 'success');
        closeModal();
      } else {
        notify(data.message || 'Could not reset password', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Update password');
    }
  });
}

function adminApproveLoanPrompt(loanId) {
  const options = state.admin.accounts.map(a => `<option value="${a.accountId}">${a.accountId.slice(-8)} · ${escapeHtml(customerName(a.customerId || a.ownerId || a.userId))}</option>`).join('');
  openModal('Approve loan', `
    <div class="field">
      <label>Disburse to account</label>
      <select id="approve-target-account">${options}</select>
    </div>
    <button type="submit" class="btn btn-primary">Approve and disburse</button>
  `, async (e) => {
    e.preventDefault();
    const targetAccountId = document.getElementById('approve-target-account').value;
    const btn = modalForm.querySelector('button[type="submit"]');
    setBtnLoading(btn, 'Approving…');
    try {
      const res = await fetch(`${API_BASE_URL}/loans/admin/${loanId}/approve?targetAccountId=${encodeURIComponent(targetAccountId)}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        notify('Loan approved', 'success');
        closeModal();
        await loadAdmin(true);
      } else {
        notify(data.message || 'Approval failed', 'error');
      }
    } catch (err) {
      notify('Server connection error', 'error');
    } finally {
      resetBtnLoading(btn, 'Approve and disburse');
    }
  });
}

async function adminRejectLoan(loanId) {
  if (!confirm('Reject this loan request?')) return;
  try {
    const res = await fetch(`${API_BASE_URL}/loans/admin/${loanId}/reject`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      notify('Loan rejected', 'success');
      await loadAdmin(true);
    } else {
      const data = await res.json().catch(() => ({}));
      notify(data.message || 'Action failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  }
}

async function adminToggleAutoDebt(loanId, enabled) {
  try {
    const res = await fetch(`${API_BASE_URL}/loans/admin/${loanId}/auto-debt?enabled=${enabled}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      notify(`Auto-debt ${enabled ? 'enabled' : 'disabled'}`, 'success');
      await loadAdmin(true);
    } else {
      const data = await res.json().catch(() => ({}));
      notify(data.message || 'Action failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  }
}

async function adminDeleteCard(cardId) {
  if (!confirm('Revoke this card? This cannot be undone.')) return;
  try {
    const res = await fetch(`${API_BASE_URL}/accounts/admin/cards/${cardId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      notify('Card revoked', 'success');
      await loadAdmin(true);
    } else {
      const data = await res.json().catch(() => ({}));
      notify(data.message || 'Revoke failed', 'error');
    }
  } catch (err) {
    notify('Server connection error', 'error');
  }
}

async function fetchWithAuth(endpoint) {
  try {
    const res = await fetch(`${API_BASE_URL}${endpoint}`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.status === 401) {
      logout();
      return null;
    }
    return await res.json();
  } catch (err) {
    return null;
  }
}

function setBtnLoading(btn, label) {
  btn.dataset.original = btn.innerHTML;
  btn.disabled = true;
  btn.innerHTML = `<span class="spinner"></span> ${label}`;
}

function resetBtnLoading(btn, fallbackHtml) {
  btn.disabled = false;
  btn.innerHTML = btn.dataset.original || fallbackHtml;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : String(str);
  return div.innerHTML;
}

function notify(message, type) {
  const el = document.createElement('div');
  el.className = `toast ${type === 'error' ? 'error' : ''}`;
  el.textContent = message;
  toastContainer.appendChild(el);
  setTimeout(() => {
    el.style.opacity = '0';
    el.style.transform = 'translateX(24px)';
    setTimeout(() => el.remove(), 250);
  }, 3200);
}

init();
