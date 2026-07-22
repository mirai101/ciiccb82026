const API_BASE_URL = '/api';

// State Management
const state = {
    user: null,
    token: localStorage.getItem('token'),
    accounts: [],
    transactions: [],
    activePage: 'overview'
};

// UI Elements
const authSection = document.getElementById('auth-section');
const dashboardSection = document.getElementById('dashboard-section');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const loginTab = document.getElementById('login-tab');
const registerTab = document.getElementById('register-tab');
const pageContent = document.getElementById('page-content');
const userGreeting = document.getElementById('user-greeting');
const userAvatar = document.getElementById('user-avatar');
const logoutBtn = document.getElementById('logout-btn');
const navLinks = document.querySelectorAll('.nav-links li');
const modalOverlay = document.getElementById('modal-overlay');
const modalTitle = document.getElementById('modal-title');
const modalForm = document.getElementById('modal-form');

// Initialize App
async function init() {
    if (state.token) {
        const success = await fetchUserDetails();
        if (success) {
            showDashboard();
        } else {
            showAuth();
        }
    } else {
        showAuth();
    }
}

// Auth Functions
function showAuth() {
    authSection.classList.remove('hidden');
    dashboardSection.classList.add('hidden');
}

function showDashboard() {
    authSection.classList.add('hidden');
    dashboardSection.classList.remove('hidden');
    loadPage(state.activePage);
}

loginTab.addEventListener('click', () => {
    loginTab.classList.add('active');
    registerTab.classList.remove('active');
    loginForm.classList.remove('hidden');
    registerForm.classList.add('hidden');
});

registerTab.addEventListener('click', () => {
    registerTab.classList.add('active');
    loginTab.classList.remove('active');
    registerForm.classList.remove('hidden');
    loginForm.classList.add('hidden');
});

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();
        if (response.ok) {
            state.token = data.token;
            localStorage.setItem('token', data.token);
            await fetchUserDetails();
            showDashboard();
            notify('Welcome back!', 'success');
        } else {
            notify(data.message || 'Login failed', 'error');
        }
    } catch (error) {
        notify('Server connection error', 'error');
    }
});

registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fullName = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    const phone = document.getElementById('reg-phone').value;

    try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, password, phone })
        });

        const data = await response.json();
        if (response.ok) {
            notify('Registration successful! Please login.', 'success');
            loginTab.click();
        } else {
            notify(data.message || 'Registration failed', 'error');
        }
    } catch (error) {
        notify('Server connection error', 'error');
    }
});

logoutBtn.addEventListener('click', () => {
    state.token = null;
    state.user = null;
    localStorage.removeItem('token');
    showAuth();
    notify('Logged out successfully', 'success');
});

async function fetchUserDetails() {
    try {
        console.log('Fetching user details...');
        const response = await fetch(`${API_BASE_URL}/auth/me`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        
        if (response.status === 401) {
            console.warn('Session expired or invalid token');
            localStorage.removeItem('token');
            state.token = null;
            return false;
        }

        if (response.ok) {
            state.user = await response.json();
            console.log('User details fetched successfully:', state.user.fullName);
            const name = state.user.fullName || state.user.name || 'User';
            const firstName = name.trim().split(' ')[0];
            userGreeting.textContent = `Hello, ${firstName}`;
            
            // Better avatar: first letter of first and last name if available
            const nameParts = name.trim().split(/\s+/);
            let avatarText = nameParts[0].charAt(0);
            if (nameParts.length > 1) {
                avatarText += nameParts[nameParts.length - 1].charAt(0);
            }
            userAvatar.textContent = avatarText.toUpperCase();

            // Admin Access
            const adminNav = document.getElementById('admin-nav');
            if (adminNav) {
                if (state.user.role === 'ADMIN') {
                    adminNav.classList.remove('hidden');
                } else {
                    adminNav.classList.add('hidden');
                }
            }

            return true;
        }
        
        const errorData = await response.json().catch(() => ({}));
        console.error('Fetch user details failed:', response.status, errorData);
        return false;
    } catch (error) {
        console.error('Fetch user details connection error:', error);
        return false;
    }
}

// Navigation
navLinks.forEach(link => {
    link.addEventListener('click', () => {
        navLinks.forEach(l => l.classList.remove('active'));
        link.classList.add('active');
        const page = link.getAttribute('data-page');
        loadPage(page);
    });
});

async function renderLoans() {
    const [loans, accounts] = await Promise.all([
        fetchWithAuth('/loans/my-loans'),
        fetchWithAuth('/accounts/my-accounts')
    ]);

    const loanList = loans || [];
    const accountList = accounts || [];

    let html = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
            <h2>My Loans</h2>
            <button class="btn-primary" style="width:auto; padding: 10px 20px;" onclick="showRequestLoan()">+ Request Loan</button>
        </div>

        ${loanList.length > 0 ? `
            <div class="stats-grid">
                <div class="stat-card">
                    <h3>Total Loans</h3>
                    <div class="value">${loanList.length}</div>
                </div>
                <div class="stat-card">
                    <h3>Total Borrowed</h3>
                    <div class="value">$${loanList.reduce((sum, l) => sum + (l.amount || 0), 0).toLocaleString()}</div>
                </div>
                <div class="stat-card">
                    <h3>Total Remaining</h3>
                    <div class="value">$${loanList.reduce((sum, l) => sum + (l.remainingBalance || 0), 0).toLocaleString()}</div>
                </div>
            </div>

            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Loan ID</th>
                            <th>Amount</th>
                            <th>Remaining</th>
                            <th>Rate</th>
                            <th>Status</th>
                            <th>Auto Debt</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${loanList.map(l => {
                            const statusClass = l.status === 'PAID' ? 'paid' : l.status === 'PENDING' ? 'pending' : l.status === 'APPROVED' ? 'approved' : 'rejected';
                            return `
                            <tr>
                                <td><strong>${l.loanId}</strong></td>
                                <td>$${(l.amount || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</td>
                                <td>$${(l.remainingBalance || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</td>
                                <td>${l.interestRate}%</td>
                                <td><span class="status-${statusClass}">${l.status}</span></td>
                                <td>${l.autoDebtEnabled ? '<span class="badge badge-success">Enabled</span>' : '<span class="badge badge-info">Disabled</span>'}</td>
                                <td>
                                    ${l.status === 'APPROVED' && l.remainingBalance > 0 ? `
                                        <button class="btn-small btn-approve" onclick="showRepayLoan('${l.loanId}')">Repay</button>
                                    ` : ''}
                                </td>
                            </tr>
                        `;}).join('')}
                    </tbody>
                </table>
            </div>
        ` : `
            <div class="empty-state">
                <i class="fas fa-hand-holding-usd"></i>
                <h3>No Loans Yet</h3>
                <p>You haven't applied for any loans. Click the button above to request your first loan.</p>
            </div>
        `}
    `;
    pageContent.innerHTML = html;
}

window.showRequestLoan = () => {
    modalTitle.textContent = 'Request a Loan';
    modalForm.innerHTML = `
        <div class="form-group">
            <label class="form-label">Loan Amount</label>
            <input type="number" id="loan-amount" class="form-input" placeholder="Enter loan amount" min="100" step="0.01" required>
        </div>
        <div class="form-group">
            <label class="form-label">Interest Rate (%)</label>
            <input type="number" id="loan-rate" class="form-input" placeholder="e.g. 5.0" value="5.0" min="0" step="0.1" required>
        </div>
        <button type="submit" class="btn-primary">Submit Request</button>
    `;
    modalForm.onsubmit = async (e) => {
        e.preventDefault();
        const amount = parseFloat(document.getElementById('loan-amount').value);
        const rate = parseFloat(document.getElementById('loan-rate').value);

        if (isNaN(amount) || amount <= 0) {
            notify('Please enter a valid loan amount', 'error');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/loans/request`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${state.token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ amount, interestRate: rate })
            });

            if (response.ok) {
                notify('Loan request submitted successfully!', 'success');
                closeModal();
                loadPage('loans');
            } else {
                const data = await response.json().catch(() => ({}));
                notify(data.message || 'Failed to submit loan request', 'error');
            }
        } catch (error) {
            notify('Connection error', 'error');
        }
    };
    openModal();
};

window.showRepayLoan = (loanId) => {
    if (!state.accounts || state.accounts.length === 0) {
        fetchWithAuth('/accounts/my-accounts').then(accs => {
            state.accounts = accs || [];
            showRepayLoanModal(loanId);
        });
    } else {
        showRepayLoanModal(loanId);
    }
};

function showRepayLoanModal(loanId) {
    modalTitle.textContent = 'Repay Loan';
    modalForm.innerHTML = `
        <div class="form-group">
            <label class="form-label">Loan ID</label>
            <input type="text" class="form-input" value="${loanId}" disabled>
        </div>
        <div class="form-group">
            <label class="form-label">From Account</label>
            <select id="repay-account" class="form-input" required>
                ${state.accounts.length > 0 ?
                    state.accounts.map(acc => `<option value="${acc.accountId}">${acc.type} - ${acc.accountId} ($${acc.balance.toLocaleString()})</option>`).join('') :
                    '<option value="" disabled>No accounts available</option>'
                }
            </select>
        </div>
        <div class="form-group">
            <label class="form-label">Repayment Amount</label>
            <input type="number" id="repay-amount" class="form-input" placeholder="Enter amount" min="0.01" step="0.01" required>
        </div>
        <button type="submit" class="btn-primary" ${state.accounts.length === 0 ? 'disabled' : ''}>Confirm Repayment</button>
    `;
    modalForm.onsubmit = async (e) => {
        e.preventDefault();
        const fromAccountId = document.getElementById('repay-account').value;
        const amount = parseFloat(document.getElementById('repay-amount').value);

        if (!fromAccountId || isNaN(amount) || amount <= 0) {
            notify('Please fill all fields correctly', 'error');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/loans/repay?loanId=${loanId}&fromAccountId=${fromAccountId}&amount=${amount}`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${state.token}` }
            });

            if (response.ok) {
                notify('Loan repayment successful!', 'success');
                closeModal();
                loadPage('loans');
            } else {
                const data = await response.json().catch(() => ({}));
                notify(data.message || 'Repayment failed', 'error');
            }
        } catch (error) {
            notify('Connection error', 'error');
        }
    };
    openModal();
}

async function renderCards() {
    const cards = await fetchWithAuth('/accounts/my-cards') || [];
    let html = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
            <h2>My Credit Cards</h2>
        </div>
        <div class="card-list">
            ${cards.length > 0 ? cards.map(card => `
                <div class="orios-card ${card.cardType === 'OriosVISA' ? 'visa' : 'master'} fade-in">
                    <div class="card-header">
                        <i class="fas fa-shield-halved" style="font-size: 24px; opacity: 0.8;"></i>
                        <span class="card-type-label">${card.cardType === 'OriosVISA' ? 'VISA' : 'Mastercard'}</span>
                    </div>
                    <div class="card-number-display">${card.cardNumber}</div>
                    <div class="card-details">
                        <div class="card-holder">
                            <small>Card Holder</small>
                            <div>${card.cardHolderName}</div>
                        </div>
                        <div class="card-expiry">
                            <small>Expires</small>
                            <div>${card.expiryDate}</div>
                        </div>
                    </div>
                </div>
            `).join('') : `
                <div class="stat-card" style="grid-column: 1/-1; text-align: center; padding: 40px;">
                    <i class="fas fa-credit-card" style="font-size: 48px; color: var(--text-muted); margin-bottom: 15px;"></i>
                    <p>You don't have any cards yet. Go to Accounts to issue one.</p>
                </div>
            `}
        </div>
    `;
    pageContent.innerHTML = html;
}

async function renderAdmin() {
    const responses = await Promise.all([
        fetchWithAuth('/accounts/admin/all-customers'),
        fetchWithAuth('/accounts/admin/all-accounts'),
        fetchWithAuth('/transactions/admin/all'),
        fetchWithAuth('/accounts/admin/all-cards'),
        fetchWithAuth('/loans/admin/all')
    ]);

    const customers = responses[0] || [];
    const accounts = responses[1] || [];
    const transactions = responses[2] || [];
    const cards = responses[3] || [];
    const loans = responses[4] || [];

    const totalBalance = accounts.reduce((sum, a) => sum + (a.balance || 0), 0);
    const pendingLoans = loans.filter(l => l.status === 'PENDING').length;
    const activeLoans = loans.filter(l => l.status === 'APPROVED').length;

    let html = `
        <div class="admin-grid">
            <div class="admin-card full-width">
                <h2>System Overview & Database</h2>
                <div class="stats-row">
                    <div class="stat-item">
                        <span class="stat-label">Total Customers</span>
                        <span class="stat-value">${customers.length}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Active Accounts</span>
                        <span class="stat-value">${accounts.length}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Total Balance</span>
                        <span class="stat-value">$${totalBalance.toLocaleString()}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Active Cards</span>
                        <span class="stat-value">${cards.length}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Transactions</span>
                        <span class="stat-value">${transactions.length}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Loans</span>
                        <span class="stat-value">${loans.length} <small style="font-size:12px;color:var(--warning)">${pendingLoans} pending</small></span>
                    </div>
                    <div class="stat-item security-item">
                        <span class="stat-label">Security</span>
                        <button class="btn-primary" style="margin-top:10px;padding:8px 16px;font-size:13px;width:auto" onclick="promptSelfChangePassword()">Change Password</button>
                    </div>
                </div>
            </div>

            <div class="admin-card">
                <h2>All Customers</h2>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${customers.map(c => `
                                <tr>
                                    <td><small style="color:var(--text-muted)">${c.customerId ? c.customerId.substring(0, 8) + '...' : 'N/A'}</small></td>
                                    <td><strong>${c.fullName}</strong></td>
                                    <td><small>${c.email}</small></td>
                                    <td><span class="badge ${c.role === 'ADMIN' ? 'badge-warning' : 'badge-info'}">${c.role}</span></td>
                                    <td>
                                        <div class="admin-actions">
                                            <button class="btn-small btn-info" onclick="promptAdminChangePassword('${c.customerId}')" title="Change Password"><i class="fas fa-key"></i></button>
                                            <button class="btn-small btn-delete" onclick="adminDeleteCustomer('${c.customerId}')" ${c.role === 'ADMIN' ? 'disabled title="Cannot delete admin"' : ''} title="Delete"><i class="fas fa-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="admin-card">
                <h2>All Accounts</h2>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Account #</th>
                                <th>Customer</th>
                                <th>Balance</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${accounts.map(acc => `
                                <tr>
                                    <td><strong style="color:var(--primary)">${acc.accountId}</strong></td>
                                    <td><small>${acc.customerId ? acc.customerId.substring(0, 8) + '...' : 'N/A'}</small></td>
                                    <td><strong>$${(acc.balance || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</strong></td>
                                    <td><span class="status-${(acc.status || 'active').toLowerCase()}">${acc.status || 'ACTIVE'}</span></td>
                                    <td>
                                        <div class="admin-actions">
                                            <button class="btn-small btn-block" onclick="adminUpdateStatus('${acc.accountId}', 'BLOCKED')" title="Block"><i class="fas fa-ban"></i></button>
                                            <button class="btn-small" style="background:#64748b;color:white" onclick="adminUpdateStatus('${acc.accountId}', 'HELD')" title="Hold"><i class="fas fa-pause"></i></button>
                                            <button class="btn-small btn-delete" onclick="adminDeleteAccount('${acc.accountId}')" title="Delete"><i class="fas fa-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="admin-card full-width">
                <h2>Loan Management</h2>
                ${loans.length > 0 ? `
                    <div class="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Loan ID</th>
                                    <th>Customer</th>
                                    <th>Amount</th>
                                    <th>Remaining</th>
                                    <th>Rate</th>
                                    <th>Status</th>
                                    <th>Auto Debt</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${loans.map(l => {
                                    const statusClass = l.status === 'PAID' ? 'paid' : l.status === 'PENDING' ? 'pending' : l.status === 'APPROVED' ? 'approved' : 'rejected';
                                    return `
                                    <tr>
                                        <td><strong>${l.loanId}</strong></td>
                                        <td>${l.customerName || 'N/A'}</td>
                                        <td>$${(l.amount || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</td>
                                        <td>$${(l.remainingBalance || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</td>
                                        <td>${l.interestRate}%</td>
                                        <td><span class="status-${statusClass}">${l.status}</span></td>
                                        <td>${l.autoDebtEnabled ? '<span class="badge badge-success">ON</span>' : '<span class="badge badge-info">OFF</span>'}</td>
                                        <td>
                                            <div class="admin-actions">
                                                ${l.status === 'PENDING' ? `
                                                    <button class="btn-small btn-approve" onclick="showApproveLoan('${l.loanId}')" title="Approve"><i class="fas fa-check"></i></button>
                                                    <button class="btn-small btn-reject" onclick="adminRejectLoan('${l.loanId}')" title="Reject"><i class="fas fa-times"></i></button>
                                                ` : ''}
                                                ${l.status === 'APPROVED' ? `
                                                    <button class="btn-small btn-info" onclick="adminToggleAutoDebt('${l.loanId}', ${!l.autoDebtEnabled})" title="${l.autoDebtEnabled ? 'Disable Auto-Debt' : 'Enable Auto-Debt'}">
                                                        <i class="fas fa-${l.autoDebtEnabled ? 'toggle-on' : 'toggle-off'}"></i>
                                                    </button>
                                                ` : ''}
                                            </div>
                                        </td>
                                    </tr>
                                `;}).join('')}
                            </tbody>
                        </table>
                    </div>
                ` : `
                    <div class="empty-state" style="padding:30px">
                        <i class="fas fa-hand-holding-usd" style="font-size:40px"></i>
                        <p>No loans in the system</p>
                    </div>
                `}
            </div>

            <div class="admin-card full-width">
                <h2>Recent Transactions</h2>
                ${transactions.length > 0 ? `
                    <div class="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Type</th>
                                    <th>From</th>
                                    <th>To</th>
                                    <th>Amount</th>
                                    <th>Description</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${transactions.slice(0, 20).map(t => {
                                    const date = t.timestamp ? new Date(t.timestamp) : new Date();
                                    const dateStr = isNaN(date.getTime()) ? 'N/A' : date.toLocaleString(undefined, {
                                        month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                                    });
                                    const isDeposit = (t.type || '').includes('DEPOSIT') || (t.type || '').includes('IN');
                                    return `
                                    <tr>
                                        <td><small>${dateStr}</small></td>
                                        <td><span class="type-badge type-${(t.type || '').toLowerCase().replace('_', '-')}">${t.type || 'UNKNOWN'}</span></td>
                                        <td><small>${t.fromAccount || '-'}</small></td>
                                        <td><small>${t.toAccount || '-'}</small></td>
                                        <td class="${isDeposit ? 'amount-positive' : 'amount-negative'}">
                                            ${isDeposit ? '+' : '-'}$${(t.amount || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}
                                        </td>
                                        <td><small>${t.description || ''}</small></td>
                                    </tr>
                                `;}).join('')}
                            </tbody>
                        </table>
                    </div>
                ` : `
                    <div class="empty-state" style="padding:30px">
                        <i class="fas fa-exchange-alt" style="font-size:40px"></i>
                        <p>No transactions yet</p>
                    </div>
                `}
            </div>
        </div>
    `;
    pageContent.innerHTML = html;
}

window.showApproveLoan = (loanId) => {
    modalTitle.textContent = 'Approve Loan';
    modalForm.innerHTML = `
        <div class="form-group">
            <label class="form-label">Loan ID</label>
            <input type="text" class="form-input" value="${loanId}" disabled>
        </div>
        <div class="form-group">
            <label class="form-label">Target Account ID (where to send funds)</label>
            <input type="text" id="approve-target-account" class="form-input" placeholder="Enter account ID" required>
        </div>
        <button type="submit" class="btn-primary">Approve & Disburse</button>
    `;
    modalForm.onsubmit = async (e) => {
        e.preventDefault();
        const targetAccountId = document.getElementById('approve-target-account').value.trim();
        if (!targetAccountId) {
            notify('Please enter a target account ID', 'error');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/loans/admin/${loanId}/approve?targetAccountId=${targetAccountId}`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${state.token}` }
            });

            if (response.ok) {
                notify('Loan approved and funds disbursed!', 'success');
                closeModal();
                loadPage('admin');
            } else {
                const data = await response.json().catch(() => ({}));
                notify(data.message || 'Approval failed', 'error');
            }
        } catch (error) {
            notify('Connection error', 'error');
        }
    };
    openModal();
};

window.adminRejectLoan = async (loanId) => {
    if (!confirm('Are you sure you want to reject this loan?')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/loans/admin/${loanId}/reject`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify('Loan rejected', 'success');
            loadPage('admin');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Rejection failed', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
};

window.adminToggleAutoDebt = async (loanId, enabled) => {
    try {
        const response = await fetch(`${API_BASE_URL}/loans/admin/${loanId}/auto-debt?enabled=${enabled}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify(`Auto-debt ${enabled ? 'enabled' : 'disabled'}`, 'success');
            loadPage('admin');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Action failed', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
};

async function loadPage(page) {
    if (page === 'admin' && (!state.user || state.user.role !== 'ADMIN')) {
        console.warn('Unauthorized access attempt to admin page');
        notify('Access Denied: Administrator role required', 'error');
        loadPage('overview');
        return;
    }
    state.activePage = page;
    pageContent.innerHTML = '<div class="loader-container"><div class="loader"></div><p>Loading your financial world...</p></div>';

    try {
        switch(page) {
            case 'overview':
                await renderOverview();
                break;
            case 'accounts':
                await renderAccounts();
                break;
            case 'transactions':
                await renderTransactions();
                break;
            case 'transfer':
                await renderTransfer();
                break;
            case 'loans':
                await renderLoans();
                break;
            case 'cards':
                await renderCards();
                break;
            case 'admin':
                await renderAdmin();
                break;
            default:
                await renderOverview();
        }
    } catch (error) {
        console.error(`Error loading page ${page}:`, error);
        pageContent.innerHTML = `
            <div class="error-state">
                <i class="fas fa-exclamation-triangle"></i>
                <h2>Something went wrong</h2>
                <p>We couldn't load this page. Please try again or contact support.</p>
                <button class="btn-primary" onclick="loadPage('${page}')">Retry</button>
            </div>
        `;
    }
}

// Page Renderers
async function renderOverview() {
    const [accounts, totalBalanceData] = await Promise.all([
        fetchWithAuth('/accounts/my-accounts'),
        fetchWithAuth('/accounts/total-balance')
    ]);

    state.accounts = accounts || [];
    const visibleAccounts = state.accounts.filter(acc => !acc.isHidden);
    const totalBalance = (totalBalanceData && totalBalanceData.totalBalance !== undefined) ? totalBalanceData.totalBalance : 0;

    let html = `
        <div class="stats-grid">
            <div class="stat-card">
                <h3>Total Balance</h3>
                <div class="value">$${totalBalance.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}</div>
            </div>
            <div class="stat-card">
                <h3>Active Accounts</h3>
                <div class="value">${visibleAccounts.length}</div>
            </div>
        </div>
        
        <h2>Your Accounts</h2>
        <div class="card-list">
            ${visibleAccounts.length > 0 ? visibleAccounts.map(acc => `
                <div class="bank-card ${acc.type === 'SAVINGS' ? 'savings' : ''}">
                    <div class="card-header">
                        <div class="card-chip"></div>
                        <div class="card-logo"><i class="fas fa-shield-halved"></i></div>
                    </div>
                    <div class="card-number">**** **** **** ${acc.accountId ? acc.accountId.slice(-4) : '****'}</div>
                    <div class="card-footer">
                        <div class="card-holder">
                            <small>Account Type</small>
                            <div>${acc.type || 'UNKNOWN'}</div>
                        </div>
                        <div class="card-balance">
                            <small>Balance</small>
                            <div>$${acc.balance ? acc.balance.toLocaleString(undefined, {minimumFractionDigits: 2}) : '0.00'}</div>
                        </div>
                    </div>
                </div>
            `).join('') : ''}
            <div class="bank-card add-account" onclick="showOpenAccount()" style="background: var(--bg-main); border: 2px dashed var(--border);">
                <div style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; gap:15px; cursor:pointer; color: var(--text-muted);">
                    <i class="fas fa-plus-circle" style="font-size: 40px;"></i>
                    <span>Open New Account</span>
                </div>
            </div>
        </div>
    `;
    pageContent.innerHTML = html;
}

async function renderAccounts() {
    const accounts = await fetchWithAuth('/accounts/my-accounts') || [];
    let html = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
            <h2>Manage Accounts</h2>
            <button class="btn-primary" style="width:auto; padding: 10px 20px;" onclick="showOpenAccount()">+ Open Account</button>
        </div>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Account ID</th>
                        <th>Type</th>
                        <th>Balance</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${accounts.length > 0 ? accounts.map(acc => `
                        <tr>
                            <td>${acc.accountId}</td>
                            <td><span class="type-badge type-transfer">${acc.type}</span></td>
                            <td>$${acc.balance ? acc.balance.toLocaleString(undefined, {minimumFractionDigits: 2}) : '0.00'}</td>
                            <td><span class="status-${(acc.status || 'ACTIVE').toLowerCase()}">${acc.status || 'ACTIVE'}</span></td>
                            <td>
                                <div class="account-actions">
                                    <button class="btn-icon" title="Deposit" onclick="showDeposit('${acc.accountId}')"><i class="fas fa-plus"></i></button>
                                    <button class="btn-icon" title="Withdraw" onclick="showWithdraw('${acc.accountId}')"><i class="fas fa-minus"></i></button>
                                    <button class="btn-icon" title="${acc.isHidden ? 'Show Account' : 'Hide Account'}" onclick="toggleVisibility('${acc.accountId}')">
                                        <i class="fas ${acc.isHidden ? 'fa-eye' : 'fa-eye-slash'}"></i>
                                    </button>
                                    <button class="btn-icon" title="Issue Card" onclick="showIssueCard('${acc.accountId}')"><i class="fas fa-credit-card"></i></button>
                                </div>
                            </td>
                        </tr>
                    `).join('') : '<tr><td colspan="5" style="text-align:center;">No accounts found</td></tr>'}
                </tbody>
            </table>
        </div>
    `;
    pageContent.innerHTML = html;
}

async function renderTransactions() {
    const transactions = await fetchWithAuth('/transactions/my-transactions') || [];
    let html = `
        <h2>Transaction History</h2>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Date & Time</th>
                        <th>Account</th>
                        <th>Type</th>
                        <th>Amount</th>
                        <th>Description</th>
                    </tr>
                </thead>
                <tbody>
                    ${transactions.length > 0 ? transactions.map(t => {
                        const date = t.timestamp ? new Date(t.timestamp) : new Date();
                        const dateStr = isNaN(date.getTime()) ? 'N/A' : date.toLocaleString(undefined, {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                        });
                        return `
                        <tr>
                            <td>${dateStr}</td>
                            <td>${t.accountId ? t.accountId.slice(-8) : 'N/A'}</td>
                            <td><span class="type-badge type-${(t.type || '').toLowerCase()}">${t.type || 'UNKNOWN'}</span></td>
                            <td style="color: ${(t.type || '').includes('DEPOSIT') || (t.type || '').includes('IN') ? 'var(--success)' : 'var(--error)'}">
                                ${(t.type || '').includes('DEPOSIT') || (t.type || '').includes('IN') ? '+' : '-'}$${(t.amount || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}
                            </td>
                            <td>${t.description || 'No description'}</td>
                        </tr>
                    `;}).join('') : '<tr><td colspan="5" style="text-align:center;">No transactions found</td></tr>'}
                </tbody>
            </table>
        </div>
    `;
    pageContent.innerHTML = html;
}

async function renderTransfer() {
    if (!state.accounts || state.accounts.length === 0) {
        const accounts = await fetchWithAuth('/accounts/my-accounts');
        state.accounts = accounts || [];
    }

    let html = `
        <div class="auth-card" style="max-width: 600px; margin: 0 auto;">
            <h2>Transfer Funds</h2>
            <p>Send money to any Orios Bank account instantly</p>
            <form id="transfer-form" onsubmit="handleTransfer(event)">
                <div class="input-group">
                    <label style="display:block; margin-bottom:5px; color:var(--text-muted)">From Account</label>
                    <select id="from-account" required class="form-input">
                        ${state.accounts.length > 0 ? 
                            state.accounts.map(acc => `<option value="${acc.accountId}">${acc.type} - ${acc.accountId} ($${acc.balance.toLocaleString()})</option>`).join('') :
                            '<option value="" disabled>No accounts available</option>'
                        }
                    </select>
                </div>
                <div class="input-group">
                    <i class="fas fa-user-tag"></i>
                    <input type="text" id="to-account" placeholder="Recipient Account ID" required>
                </div>
                <div class="input-group">
                    <i class="fas fa-dollar-sign"></i>
                    <input type="number" id="transfer-amount" placeholder="Amount" step="0.01" min="0.01" required>
                </div>
                <div class="input-group">
                    <i class="fas fa-comment"></i>
                    <input type="text" id="transfer-desc" placeholder="Description (Optional)">
                </div>
                <button type="submit" class="btn-primary" ${state.accounts.length === 0 ? 'disabled' : ''}>Confirm Transfer</button>
            </form>
        </div>
    `;
    pageContent.innerHTML = html;
}

// Global functions for inline event handlers
window.showOpenAccount = () => {
    modalTitle.textContent = 'Open New Account';
    modalForm.innerHTML = `
        <div class="input-group">
            <i class="fas fa-list"></i>
            <select id="acc-type" required>
                <option value="CHECKING">Checking Account</option>
                <option value="SAVINGS">Savings Account</option>
            </select>
        </div>
        <div class="input-group">
            <i class="fas fa-dollar-sign"></i>
            <input type="number" id="acc-deposit" placeholder="Initial Deposit" min="0" step="0.01" value="100" required>
        </div>
        <button type="submit" class="btn-primary">Open Account</button>
    `;
    modalForm.onsubmit = async (e) => {
        e.preventDefault();
        const type = document.getElementById('acc-type').value;
        const amount = document.getElementById('acc-deposit').value;
        const success = await handleOpenAccount(type, parseFloat(amount));
        if (success) closeModal();
    };
    openModal();
};

window.showDeposit = (accountId) => {
    modalTitle.textContent = 'Deposit Funds';
    modalForm.innerHTML = `
        <div class="input-group">
            <i class="fas fa-id-card"></i>
            <input type="text" value="${accountId}" disabled>
        </div>
        <div class="input-group">
            <i class="fas fa-dollar-sign"></i>
            <input type="number" id="dep-amount" placeholder="Deposit Amount" min="0.01" step="0.01" required>
        </div>
        <div class="input-group">
            <i class="fas fa-comment"></i>
            <input type="text" id="dep-desc" placeholder="Description (Optional)">
        </div>
        <button type="submit" class="btn-primary">Deposit</button>
    `;
    modalForm.onsubmit = async (e) => {
        e.preventDefault();
        const amount = document.getElementById('dep-amount').value;
        const desc = document.getElementById('dep-desc').value;
        await handleDeposit(accountId, parseFloat(amount), desc);
        closeModal();
    };
    openModal();
};

window.showWithdraw = (accountId) => {
    modalTitle.textContent = 'Withdraw Funds';
    modalForm.innerHTML = `
        <div class="input-group">
            <i class="fas fa-id-card"></i>
            <input type="text" value="${accountId}" disabled>
        </div>
        <div class="input-group">
            <i class="fas fa-dollar-sign"></i>
            <input type="number" id="wit-amount" placeholder="Withdrawal Amount" min="0.01" step="0.01" required>
        </div>
        <div class="input-group">
            <i class="fas fa-comment"></i>
            <input type="text" id="wit-desc" placeholder="Description (Optional)">
        </div>
        <button type="submit" class="btn-primary">Withdraw</button>
    `;
    modalForm.onsubmit = async (e) => {
        e.preventDefault();
        const amount = document.getElementById('wit-amount').value;
        const desc = document.getElementById('wit-desc').value;
        await handleWithdraw(accountId, parseFloat(amount), desc);
        closeModal();
    };
    openModal();
};

function openModal() {
    modalOverlay.classList.remove('hidden');
}

window.closeModal = () => {
    modalOverlay.classList.add('hidden');
    modalForm.innerHTML = '';
};

// Close modal when clicking outside
window.addEventListener('click', (e) => {
    if (e.target === modalOverlay) closeModal();
});

async function handleIssueCard(accountId, cardType) {
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/${accountId}/issue-card?cardType=${cardType}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify('Card issued successfully', 'success');
            loadPage('cards');
            return true;
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Failed to issue card', 'error');
            return false;
        }
    } catch (error) {
        console.error('Issue card error:', error);
        notify('Connection error', 'error');
        return false;
    }
}

async function toggleVisibility(accountId) {
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/${accountId}/toggle-visibility`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify('Visibility updated', 'success');
            loadPage(state.activePage);
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Failed to update visibility', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
}

function showIssueCard(accountId) {
    modalTitle.textContent = 'Issue New Credit Card';
    modalForm.innerHTML = `
        <input type="hidden" id="card-acc-id" value="${accountId}">
        <div class="input-group">
            <label style="display:block; margin-bottom:5px; color:var(--text-muted)">Card Type</label>
            <select id="card-type" required class="form-input">
                <option value="OriosVISA">OriosVISA (Gold)</option>
                <option value="OriosMASTER">OriosMASTER (Premium Black)</option>
            </select>
        </div>
        <button type="submit" class="btn-primary">Issue Card</button>
    `;
    modalForm.onsubmit = async (e) => {
        e.preventDefault();
        const cardType = document.getElementById('card-type').value;
        const success = await handleIssueCard(accountId, cardType);
        if (success) closeModal();
    };
    openModal();
}

async function adminUpdateStatus(accountId, status) {
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/admin/accounts/${accountId}/status?status=${status}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify(`Account ${status}`, 'success');
            loadPage('admin');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Action failed', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
}

async function adminDeleteAccount(accountId) {
    if (!confirm('Are you sure you want to delete this account? This will also delete all transactions and cards associated with it.')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/admin/accounts/${accountId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify('Account deleted', 'success');
            loadPage('admin');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Delete failed', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
}

async function adminDeleteCard(cardId) {
    if (!confirm('Are you sure you want to revoke this credit card?')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/admin/cards/${cardId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify('Card revoked successfully', 'success');
            loadPage('admin');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Action failed', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
}

async function adminDeleteCustomer(customerId) {
    if (!confirm('Are you sure you want to delete this customer? This will also delete ALL their accounts, transactions, and cards. This action cannot be undone.')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/admin/customers/${customerId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify('Customer deleted successfully', 'success');
            loadPage('admin');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Delete failed', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
}

async function promptAdminChangePassword(customerId) {
    const newPassword = prompt('Enter new password (min 6 chars):');
    if (!newPassword) return;
    if (newPassword.length < 6) {
        notify('Password too short', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/auth/admin/change-password/${customerId}`, {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ newPassword })
        });
        if (response.ok) {
            notify('User password updated', 'success');
            loadPage('admin');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Update failed', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
}

async function promptSelfChangePassword() {
    const currentPassword = prompt('Enter current password:');
    if (!currentPassword) return;
    const newPassword = prompt('Enter new password (min 6 chars):');
    if (!newPassword) return;
    if (newPassword.length < 6) {
        notify('Password too short', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/auth/change-password`, {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ currentPassword, newPassword })
        });
        if (response.ok) {
            notify('Your password has been changed', 'success');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Failed to change password', 'error');
        }
    } catch (error) {
        notify('Connection error', 'error');
    }
}

async function handleOpenAccount(type, amount) {
    if (!type || isNaN(amount) || amount < 0) {
        notify('Please enter a valid amount', 'error');
        return false;
    }
    try {
        console.log(`Opening ${type} account with $${amount}...`);
        const response = await fetch(`${API_BASE_URL}/accounts/open?type=${type}&initialDeposit=${amount}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (response.ok) {
            notify('Account opened successfully', 'success');
            await loadPage('overview');
            return true;
        } else {
            const data = await response.json().catch(() => ({}));
            console.error('Open account failed:', response.status, data);
            notify(data.message || 'Failed to open account', 'error');
            return false;
        }
    } catch (error) {
        console.error('Open account error:', error);
        notify('Connection error', 'error');
        return false;
    }
}

async function handleDeposit(accountId, amount, description) {
    if (isNaN(amount) || amount <= 0) {
        notify('Please enter a positive amount', 'error');
        return;
    }
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/deposit`, {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ accountId, amount, description })
        });
        if (response.ok) {
            notify('Deposit successful', 'success');
            await loadPage('accounts');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Deposit failed', 'error');
        }
    } catch (error) {
        console.error('Deposit error:', error);
        notify('Connection error', 'error');
    }
}

async function handleWithdraw(accountId, amount, description) {
    if (isNaN(amount) || amount <= 0) {
        notify('Please enter a positive amount', 'error');
        return;
    }
    try {
        const response = await fetch(`${API_BASE_URL}/accounts/withdraw`, {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ accountId, amount, description })
        });
        if (response.ok) {
            notify('Withdrawal successful', 'success');
            await loadPage('accounts');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Withdrawal failed', 'error');
        }
    } catch (error) {
        console.error('Withdrawal error:', error);
        notify('Connection error', 'error');
    }
}

window.handleTransfer = async (e) => {
    e.preventDefault();
    const from = document.getElementById('from-account').value;
    const to = document.getElementById('to-account').value;
    const amount = parseFloat(document.getElementById('transfer-amount').value);
    const desc = document.getElementById('transfer-desc').value;

    if (!from || !to || isNaN(amount) || amount <= 0) {
        notify('Please fill all required fields correctly', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/accounts/transfer`, {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                fromAccountId: from, 
                toAccountId: to, 
                amount: amount,
                description: desc
            })
        });

        if (response.ok) {
            notify('Transfer successful', 'success');
            loadPage('overview');
        } else {
            const data = await response.json().catch(() => ({}));
            notify(data.message || 'Transfer failed', 'error');
        }
    } catch (error) {
        console.error('Transfer error:', error);
        notify('Connection error', 'error');
    }
};

// Helpers
async function fetchWithAuth(endpoint) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (response.status === 401) {
        logoutBtn.click();
        return null;
    }
    try {
        return await response.json();
    } catch (e) {
        console.error('JSON parse error for', endpoint, e);
        return null;
    }
}

function notify(message, type) {
    const container = document.getElementById('notification-container');
    const div = document.createElement('div');
    div.className = `notification ${type}`;
    div.innerHTML = `
        <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
        ${message}
    `;
    container.appendChild(div);
    setTimeout(() => {
        div.style.opacity = '0';
        div.style.transform = 'translateX(100%)';
        setTimeout(() => div.remove(), 300);
    }, 3000);
}

// Start
init();
