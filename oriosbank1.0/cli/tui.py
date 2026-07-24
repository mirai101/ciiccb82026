#!/usr/bin/env python3
"""
OriosBank TUI - Full-Screen Terminal User Interface
A sleek, modern banking experience in the terminal.
"""

import os
import json
import re
from datetime import datetime
from textual.app import App, ComposeResult
from textual.binding import Binding
from textual.containers import (
    Container, Horizontal, Vertical, VerticalScroll
)
from textual.screen import ModalScreen, Screen
from textual.widgets import (
    Static, Button, Input, DataTable
)
from textual import on, work
from textual.reactive import var
from textual.css.query import NoMatches
import requests

# ─── Configuration ───────────────────────────────────────────────────────────

API_BASE_URL = "http://localhost:8080/api"
TOKEN_FILE = os.path.expanduser("~/.orios_token")
USER_DATA_FILE = os.path.expanduser("~/.orios_user")

# Limits
MAX_DEPOSIT = 100_000.0
MAX_WITHDRAWAL = 50_000.0
MAX_TRANSFER = 100_000.0
MAX_LOAN_AMOUNT = 2_000_000.0


# ─── Helpers ─────────────────────────────────────────────────────────────────

def save_token(token, user_data):
    with open(TOKEN_FILE, "w") as f:
        f.write(token)
    with open(USER_DATA_FILE, "w") as f:
        json.dump(user_data, f)


def load_token():
    if os.path.exists(TOKEN_FILE):
        with open(TOKEN_FILE, "r") as f:
            return f.read().strip()
    return None


def load_user_data():
    if os.path.exists(USER_DATA_FILE):
        with open(USER_DATA_FILE, "r") as f:
            return json.load(f)
    return None


def clear_session():
    for path in [TOKEN_FILE, USER_DATA_FILE]:
        if os.path.exists(path):
            os.remove(path)


def get_headers():
    token = load_token()
    if token:
        return {"Authorization": f"Bearer {token}"}
    return {}


def api_get(endpoint, headers=None):
    h = headers or get_headers()
    try:
        r = requests.get(f"{API_BASE_URL}{endpoint}", headers=h, timeout=10)
        if r.status_code in [200, 201]:
            return r.json(), None
        return None, r.json().get("message", f"HTTP {r.status_code}")
    except requests.ConnectionError:
        return None, "Cannot connect to server"
    except Exception as e:
        return None, str(e)


def api_post(endpoint, payload=None, params=None, headers=None):
    h = headers or get_headers()
    try:
        r = requests.post(
            f"{API_BASE_URL}{endpoint}",
            json=payload, params=params, headers=h, timeout=10
        )
        if r.status_code in [200, 201]:
            try:
                return r.json(), None
            except Exception:
                return True, None
        return None, r.json().get("message", f"HTTP {r.status_code}")
    except requests.ConnectionError:
        return None, "Cannot connect to server"
    except Exception as e:
        return None, str(e)


def api_delete(endpoint, headers=None):
    h = headers or get_headers()
    try:
        r = requests.delete(f"{API_BASE_URL}{endpoint}", headers=h, timeout=10)
        if r.status_code in [200, 204]:
            return True, None
        return None, r.json().get("message", f"HTTP {r.status_code}")
    except requests.ConnectionError:
        return None, "Cannot connect to server"
    except Exception as e:
        return None, str(e)


def fmt_money(amount):
    return f"${amount:,.2f}"


def fmt_date(iso_str):
    if not iso_str or iso_str == "N/A":
        return "N/A"
    try:
        return datetime.fromisoformat(iso_str.replace("Z", "+00:00")).strftime("%Y-%m-%d %H:%M")
    except Exception:
        return iso_str


# ─── CSS ─────────────────────────────────────────────────────────────────────

TUI_CSS = """
/* ── Global ────────────────────────────────────────────────────── */
Screen {
    background: $surface;
}

/* ── Login Screen ──────────────────────────────────────────────── */
#login-screen {
    align: center middle;
}

#login-box {
    width: 60;
    height: auto;
    max-height: 36;
    border: thick $primary;
    background: $surface;
    padding: 1 2;
}

#login-title {
    width: 100%;
    content-align: center middle;
    text-style: bold;
    color: $primary;
    margin-bottom: 1;
    height: 3;
}

#login-subtitle {
    width: 100%;
    content-align: center middle;
    color: $text-muted;
    margin-bottom: 1;
}

#login-error {
    width: 100%;
    color: $error;
    text-style: bold;
    display: none;
    height: 1;
    margin-top: 0;
}

#login-form {
    width: 100%;
}

#login-form Input {
    margin-bottom: 1;
}

#login-form Button {
    margin-top: 1;
    width: 100%;
}

#login-links {
    width: 100%;
    height: 1;
    margin-top: 1;
    content-align: center middle;
    color: $text-muted;
}

/* ── Register Screen ───────────────────────────────────────────── */
#register-box {
    width: 60;
    height: auto;
    max-height: 42;
    border: thick $secondary;
    background: $surface;
    padding: 1 2;
}

#register-title {
    width: 100%;
    content-align: center middle;
    text-style: bold;
    color: $secondary;
    margin-bottom: 1;
    height: 3;
}

/* ── Main App ──────────────────────────────────────────────────── */
#app-sidebar {
    width: 28;
    min-width: 28;
    background: $surface-darken-1;
    border-right: tall $primary;
}

.sidebar-title {
    width: 100%;
    text-style: bold;
    color: $primary;
    content-align: center middle;
    height: 3;
    margin-bottom: 1;
}

.sidebar-item {
    width: 100%;
    height: 3;
    padding: 0 1;
    color: $text;
}

.sidebar-item:hover {
    background: $primary 15%;
    color: $primary-lighten-2;
}

.sidebar-item.-active {
    background: $primary 25%;
    color: $primary-lighten-2;
    text-style: bold;
    border-left: tall $primary;
}

.sidebar-divider {
    width: 100%;
    height: 1;
    color: $text-muted;
    margin: 0;
}

.sidebar-user {
    width: 100%;
    padding: 1 1;
    color: $text-muted;
    text-style: italic;
}

#main-content {
    width: 1fr;
}

/* ── Dashboard ─────────────────────────────────────────────────── */
.dashboard-stats {
    height: auto;
    margin: 0 0 1 0;
}

.stat-card {
    height: 7;
    min-width: 20;
    margin: 0 1 0 0;
    padding: 1;
    background: $surface;
    border: tall $primary;
    width: 1fr;
}

.stat-card-title {
    text-style: bold;
    color: $text-muted;
    height: 1;
}

.stat-card-value {
    text-style: bold;
    color: $success;
    height: 2;
    width: 100%;
}

.stat-card-sub {
    color: $text-muted;
    height: 1;
}

/* ── Data Tables ───────────────────────────────────────────────── */
.data-view {
    height: 1fr;
}

.data-view DataTable {
    height: 1fr;
}

/* ── Forms ─────────────────────────────────────────────────────── */
.form-container {
    height: auto;
    max-height: 30;
    border: tall $accent;
    padding: 1 2;
    margin: 1 0;
    width: 100%;
}

.form-title {
    text-style: bold;
    color: $accent;
    height: 2;
    margin-bottom: 1;
}

.form-row {
    height: 3;
    margin-bottom: 0;
}

.form-row Label {
    width: 18;
    height: 3;
    content-align: left middle;
}

.form-row Input {
    width: 1fr;
    height: 3;
}

.form-actions {
    height: 3;
    margin-top: 1;
}

.form-actions Button {
    margin-right: 1;
}

.form-result {
    height: 1;
    margin-top: 0;
    display: none;
}

.form-result.-visible {
    display: block;
}

/* ── Modal Dialogs ─────────────────────────────────────────────── */
.modal-overlay {
    align: center middle;
}

.modal-box {
    width: 60;
    height: auto;
    max-height: 30;
    border: thick $warning;
    background: $surface;
    padding: 1 2;
}

.modal-title {
    text-style: bold;
    color: $warning;
    height: 2;
    margin-bottom: 1;
}

/* ── Status Colors ─────────────────────────────────────────────── */
.status-active { color: $success; }
.status-pending { color: $warning; }
.status-blocked { color: $error; }
.status-paid { color: $success; }
.status-rejected { color: $error; }

/* ── Footer ────────────────────────────────────────────────────── */
Footer {
    dock: bottom;
}

Header {
    dock: top;
}

/* ── Misc ──────────────────────────────────────────────────────── */
.error-msg { color: $error; }
.success-msg { color: $success; }
.info-msg { color: $primary; }

#status-bar {
    dock: bottom;
    height: 1;
    background: $surface-darken-1;
    padding: 0 1;
    color: $text-muted;
}

.toast-notification {
    width: 40;
    height: auto;
    max-height: 5;
    dock: right;
    offset: 0 -1;
    margin: 1;
    border: thick $success;
    background: $surface;
    padding: 0 1;
    layer: overlay;
}
"""


# ─── Screens ─────────────────────────────────────────────────────────────────

class ConfirmModal(ModalScreen[bool]):
    """A modal confirmation dialog."""

    CSS = """
    ConfirmModal {
        align: center middle;
    }
    .confirm-box {
        width: 55;
        height: auto;
        border: thick $warning;
        background: $surface;
        padding: 1 2;
    }
    .confirm-title {
        text-style: bold;
        color: $warning;
        height: 2;
        margin-bottom: 1;
    }
    .confirm-msg {
        height: auto;
        margin-bottom: 1;
    }
    .confirm-actions {
        height: 3;
    }
    .confirm-actions Button {
        margin-right: 1;
    }
    """

    def __init__(self, title: str, message: str):
        super().__init__()
        self.title_text = title
        self.message_text = message

    def compose(self) -> ComposeResult:
        with Container(classes="confirm-box"):
            yield Static(self.title_text, classes="confirm-title")
            yield Static(self.message_text, classes="confirm-msg")
            with Horizontal(classes="confirm-actions"):
                yield Button("Confirm", id="confirm-yes", variant="warning")
                yield Button("Cancel", id="confirm-no", variant="default")

    def on_button_pressed(self, event: Button.Pressed):
        self.dismiss(event.button.id == "confirm-yes")


class LoginScreen(Screen):
    """Login screen with animated styling."""

    CSS = """
    LoginScreen {
        align: center middle;
        background: $surface;
    }
    #login-box {
        width: 60;
        height: auto;
        max-height: 36;
        border: thick $primary;
        background: $surface;
        padding: 1 2;
    }
    #login-title {
        width: 100%;
        content-align: center middle;
        text-style: bold reverse;
        color: $primary;
        margin-bottom: 0;
        height: 4;
        background: $primary 15%;
    }
    #login-subtitle {
        width: 100%;
        content-align: center middle;
        color: $text-muted;
        margin-bottom: 1;
        height: 1;
    }
    #login-error {
        width: 100%;
        color: $error;
        text-style: bold;
        height: auto;
        display: none;
        margin-bottom: 0;
    }
    #login-error.-visible {
        display: block;
    }
    #login-form {
        width: 100%;
    }
    #login-form Input {
        margin-bottom: 0;
    }
    #login-form Button {
        margin-top: 1;
        width: 100%;
    }
    #login-links {
        width: 100%;
        height: 1;
        margin-top: 1;
        content-align: center middle;
        color: $text-muted;
    }
    """

    BINDINGS = [
        Binding("q", "quit", "Quit"),
    ]

    def compose(self) -> ComposeResult:
        with Container(id="login-box"):
            yield Static("  ORIOS BANK  ", id="login-title")
            yield Static("Premium Digital Banking Terminal", id="login-subtitle")
            yield Static("", id="login-error")
            with Vertical(id="login-form"):
                yield Input(placeholder="Email address", id="login-email", type="text")
                yield Input(placeholder="Password", id="login-password", password=True)
                yield Button("Sign In", id="login-btn", variant="primary")
            yield Static("Press [r]Register  |  [q]Quit", id="login-links")

    def on_mount(self):
        self.query_one("#login-email").focus()

    @on(Input.Submitted, "#login-password")
    @on(Button.Pressed, "#login-btn")
    def do_login(self):
        email = self.query_one("#login-email").value.strip()
        password = self.query_one("#login-password").value.strip()
        error_el = self.query_one("#login-error")

        if not email or not password:
            error_el.update("Please fill in all fields")
            error_el.add_class("-visible")
            return

        error_el.remove_class("-visible")
        self._do_login(email, password)

    @work(exclusive=True, group="login")
    async def _do_login(self, email: str, password: str):
        data, err = self._login_request(email, password)
        error_el = self.query_one("#login-error")

        if data:
            save_token(data["token"], data)
            self.app.switch_screen("main")
        else:
            error_el.update(err or "Login failed")
            error_el.add_class("-visible")

    def _login_request(self, email, password):
        try:
            r = requests.post(
                f"{API_BASE_URL}/auth/login",
                json={"email": email, "password": password},
                timeout=10
            )
            if r.status_code in [200, 201]:
                return r.json(), None
            return None, r.json().get("message", f"HTTP {r.status_code}")
        except requests.ConnectionError:
            return None, "Cannot connect to server"
        except Exception as e:
            return None, str(e)

    def on_key(self, event):
        if event.key == "r":
            focused = self.focused
            if focused and isinstance(focused, Input):
                return
            self.app.push_screen(RegisterScreen())


class RegisterScreen(ModalScreen):
    """Registration modal overlay."""

    CSS = """
    RegisterScreen {
        align: center middle;
    }
    #reg-box {
        width: 60;
        height: auto;
        max-height: 44;
        border: thick $secondary;
        background: $surface;
        padding: 1 2;
    }
    #reg-title {
        width: 100%;
        content-align: center middle;
        text-style: bold reverse;
        color: $secondary;
        height: 3;
        margin-bottom: 1;
        background: $secondary 15%;
    }
    #reg-error {
        width: 100%;
        color: $error;
        text-style: bold;
        height: auto;
        display: none;
    }
    #reg-error.-visible {
        display: block;
    }
    #reg-form Input {
        margin-bottom: 0;
    }
    #reg-form Button {
        margin-top: 1;
        width: 100%;
    }
    """

    def compose(self) -> ComposeResult:
        with Container(id="reg-box"):
            yield Static("  CREATE ACCOUNT  ", id="reg-title")
            yield Static("", id="reg-error")
            with Vertical(id="reg-form"):
                yield Input(placeholder="Full Name", id="reg-name")
                yield Input(placeholder="Email address", id="reg-email", type="text")
                yield Input(placeholder="Phone (09123456789)", id="reg-phone", type="text")
                yield Input(placeholder="Password (min 6 chars)", id="reg-password", password=True)
                yield Button("Create Account", id="reg-btn", variant="primary")
                yield Button("Back to Login", id="reg-back", variant="default")

    @on(Button.Pressed, "#reg-btn")
    @work(exclusive=True, group="register")
    async def do_register(self):
        name = self.query_one("#reg-name").value.strip()
        email = self.query_one("#reg-email").value.strip()
        phone = self.query_one("#reg-phone").value.strip()
        password = self.query_one("#reg-password").value.strip()
        error_el = self.query_one("#reg-error")

        if not all([name, email, phone, password]):
            error_el.update("All fields are required")
            error_el.add_class("-visible")
            return
        if not re.match(r"^0[0-9]{10}$", phone):
            error_el.update("Phone must be 11 digits starting with 0")
            error_el.add_class("-visible")
            return
        if len(password) < 6:
            error_el.update("Password must be at least 6 characters")
            error_el.add_class("-visible")
            return

        try:
            r = requests.post(
                f"{API_BASE_URL}/auth/register",
                json={"fullName": name, "email": email, "phone": phone, "password": password},
                timeout=10
            )
            if r.status_code in [200, 201]:
                self.notify("Account created! You can now sign in.", severity="success")
                self.dismiss()
            else:
                msg = r.json().get("message", "Registration failed")
                error_el.update(msg)
                error_el.add_class("-visible")
        except requests.ConnectionError:
            error_el.update("Cannot connect to server")
            error_el.add_class("-visible")
        except Exception as e:
            error_el.update(str(e))
            error_el.add_class("-visible")

    @on(Button.Pressed, "#reg-back")
    def go_back(self):
        self.dismiss()


# ─── Main Banking App Screen ────────────────────────────────────────────────

class MainScreen(Screen):
    """The main banking application screen with sidebar navigation."""

    BINDINGS = [
        Binding("1", "nav_dashboard", "Dashboard"),
        Binding("2", "nav_accounts", "Accounts"),
        Binding("3", "nav_deposit", "Deposit"),
        Binding("4", "nav_withdraw", "Withdraw"),
        Binding("5", "nav_transfer", "Transfer"),
        Binding("6", "nav_loans", "Loans"),
        Binding("7", "nav_transactions", "Transactions"),
        Binding("ctrl+a", "nav_admin", "Admin", show=True, key_display="Ctrl+A"),
        Binding("ctrl+l", "do_logout", "Logout", show=True, key_display="Ctrl+L"),
        Binding("q", "quit", "Quit"),
    ]

    CSS = """
    MainScreen {
        layout: horizontal;
    }
    #app-sidebar {
        width: 28;
        min-width: 28;
        background: $surface-darken-1;
        border-right: tall $primary;
    }
    .sidebar-header {
        width: 100%;
        height: 3;
        text-style: bold;
        color: $primary;
        content-align: center middle;
        background: $primary 10%;
        border-bottom: solid $primary;
    }
    .sidebar-user-info {
        width: 100%;
        height: auto;
        padding: 1;
        color: $text-muted;
        border-bottom: tall gray;
        margin-bottom: 0;
    }
    .sidebar-nav {
        width: 100%;
        height: auto;
    }
    .sidebar-item {
        width: 100%;
        height: 3;
        padding: 0 1;
        color: $text;
    }
    .sidebar-item:hover {
        background: $primary 15%;
    }
    .sidebar-item.-active {
        background: $primary 20%;
        color: $primary-lighten-2;
        text-style: bold;
        border-left: tall $primary;
    }
    .sidebar-divider {
        width: 100%;
        height: 1;
        color: $text-muted;
    }
    .sidebar-footer {
        width: 100%;
        height: auto;
        padding: 1;
        color: $text-muted;
        text-style: italic;
        content-align: center middle;
        border-top: tall gray;
        dock: bottom;
    }
    #main-content {
        width: 1fr;
        height: 1fr;
    }
    #status-bar {
        dock: bottom;
        height: 1;
        background: $surface-darken-1;
        padding: 0 1;
        color: $text-muted;
    }
    """

    active_view = var("dashboard")

    def __init__(self):
        super().__init__()
        self.user = load_user_data()
        self.is_admin = self.user is not None and self.user.get("role") == "ADMIN"

    def compose(self) -> ComposeResult:
        user_name = self.user.get("fullName", "User") if self.user else "User"
        user_role = self.user.get("role", "CUSTOMER") if self.user else "CUSTOMER"
        email = self.user.get("email", "") if self.user else ""

        with Container(id="app-sidebar"):
            yield Static(" ORIOS BANK ", classes="sidebar-header")
            with Vertical(classes="sidebar-user-info"):
                yield Static(f"[bold]{user_name}[/bold]")
                yield Static(f"[dim]{email}[/dim]")
                yield Static(f"[dim]{user_role}[/dim]")
            with Vertical(classes="sidebar-nav"):
                yield Static(" [1] Dashboard", classes="sidebar-item -active", id="nav-dashboard")
                yield Static(" [2] My Accounts", classes="sidebar-item", id="nav-accounts")
                yield Static(" [3] Deposit", classes="sidebar-item", id="nav-deposit")
                yield Static(" [4] Withdraw", classes="sidebar-item", id="nav-withdraw")
                yield Static(" [5] Transfer", classes="sidebar-item", id="nav-transfer")
                yield Static(" [6] Loans", classes="sidebar-item", id="nav-loans")
                yield Static(" [7] History", classes="sidebar-item", id="nav-transactions")
                yield Static(" ---", classes="sidebar-divider")
                if self.is_admin:
                    yield Static(" [Ctrl+A] Admin Panel", classes="sidebar-item", id="nav-admin")
            with Vertical(classes="sidebar-footer"):
                yield Static("[dim]Ctrl+L: Logout  Q: Quit[/dim]")

        with Container(id="main-content"):
            yield Static(f"Orios Bank  |  {user_name}  |  {datetime.now().strftime('%Y-%m-%d %H:%M')}", id="status-bar")
            yield DashboardView(id="view-dashboard")
            yield AccountsView(id="view-accounts")
            yield DepositView(id="view-deposit")
            yield WithdrawView(id="view-withdraw")
            yield TransferView(id="view-transfer")
            yield LoansView(id="view-loans")
            yield TransactionsView(id="view-transactions")
            if self.is_admin:
                yield AdminView(id="view-admin")

    def on_mount(self):
        non_dashboard = ["accounts", "deposit", "withdraw", "transfer", "loans", "transactions"]
        if self.is_admin:
            non_dashboard.append("admin")
        for name in non_dashboard:
            try:
                self.query_one(f"#view-{name}").display = False
            except NoMatches:
                pass

    def _switch_view(self, name: str):
        self.active_view = name
        views = [
            "dashboard", "accounts", "deposit", "withdraw",
            "transfer", "loans", "transactions"
        ]
        if self.is_admin:
            views.append("admin")

        nav_items = {
            "dashboard": "nav-dashboard",
            "accounts": "nav-accounts",
            "deposit": "nav-deposit",
            "withdraw": "nav-withdraw",
            "transfer": "nav-transfer",
            "loans": "nav-loans",
            "transactions": "nav-transactions",
            "admin": "nav-admin",
        }

        for v in views:
            try:
                widget = self.query_one(f"#view-{v}")
                widget.display = v == name
            except NoMatches:
                pass

        for key, nav_id in nav_items.items():
            try:
                el = self.query_one(f"#{nav_id}")
                if key == name:
                    el.add_class("-active")
                else:
                    el.remove_class("-active")
            except NoMatches:
                pass

        # Refresh data on view switch
        view = self.query_one(f"#view-{name}")
        if hasattr(view, "refresh_data"):
            view.refresh_data()

    def action_nav_dashboard(self):
        self._switch_view("dashboard")

    def action_nav_accounts(self):
        self._switch_view("accounts")

    def action_nav_deposit(self):
        self._switch_view("deposit")

    def action_nav_withdraw(self):
        self._switch_view("withdraw")

    def action_nav_transfer(self):
        self._switch_view("transfer")

    def action_nav_loans(self):
        self._switch_view("loans")

    def action_nav_transactions(self):
        self._switch_view("transactions")

    def action_nav_admin(self):
        if self.is_admin:
            self._switch_view("admin")

    def action_do_logout(self):
        clear_session()
        self.app.switch_screen("login")


# ─── View Components ─────────────────────────────────────────────────────────

class DashboardView(VerticalScroll):
    """Main dashboard with stats and overview."""

    CSS = """
    DashboardView {
        padding: 1 2;
    }
    .dash-title {
        text-style: bold;
        color: $primary;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .stats-row {
        height: 8;
        margin-bottom: 1;
        width: 100%;
    }
    .stat-card {
        height: 7;
        min-width: 20;
        margin: 0 1 0 0;
        padding: 1;
        background: $surface;
        border: tall $primary;
        width: 1fr;
    }
    .stat-card-title {
        text-style: bold;
        color: $text-muted;
        height: 1;
    }
    .stat-card-value {
        text-style: bold;
        height: 2;
        width: 100%;
    }
    .stat-card-sub {
        color: $text-muted;
        height: 1;
    }
    .dash-section {
        height: auto;
        margin-top: 1;
    }
    .dash-section-title {
        text-style: bold;
        color: $primary;
        height: 1;
        margin-bottom: 0;
    }
    .dash-table-wrap {
        height: auto;
        max-height: 15;
        width: 100%;
        margin-bottom: 1;
    }
    .dash-table-wrap DataTable {
        height: 100%;
    }
    """

    def compose(self) -> ComposeResult:
        yield Static(" DASHBOARD ", classes="dash-title")
        with Horizontal(classes="stats-row", id="stats-row"):
            yield Static("Loading...", classes="stat-card", id="stat-total")
            yield Static("Loading...", classes="stat-card", id="stat-accounts")
            yield Static("Loading...", classes="stat-card", id="stat-loans")
        yield Static("Recent Activity", classes="dash-section-title")
        with Container(classes="dash-table-wrap"):
            yield DataTable(id="dash-activity-table")

    def on_mount(self):
        table = self.query_one("#dash-activity-table")
        table.add_columns("Date", "Type", "Amount", "Description")
        table.cursor_type = "row"
        self.refresh_data()

    @work(exclusive=True, group="dashboard-refresh")
    async def refresh_data(self):
        # Total balance
        data, err = api_get("/accounts/my-accounts")
        total = 0
        accounts = data if data else []
        acc_count = len(accounts)
        for a in accounts:
            total += a.get("balance", 0)

        self.query_one("#stat-total").update(
            f"[bold]Total Net Worth[/bold]\n"
            f"[bold green]{fmt_money(total)}[/bold green]\n"
            f"[dim]Across all accounts[/dim]"
        )
        self.query_one("#stat-accounts").update(
            f"[bold]Accounts[/bold]\n"
            f"[bold cyan]{acc_count}[/bold cyan]\n"
            f"[dim]Active accounts[/dim]"
        )

        # Loans
        loan_data, _ = api_get("/loans/my-loans")
        loans = loan_data if loan_data else []
        active = sum(1 for l in loans if l.get("status") == "APPROVED")
        remaining = sum(l.get("remainingBalance", 0) for l in loans if l.get("status") != "PAID")

        self.query_one("#stat-loans").update(
            f"[bold]Loans[/bold]\n"
            f"[bold yellow]{len(loans)}[/bold yellow] ({active} active)\n"
            f"[dim]Remaining: {fmt_money(remaining)}[/dim]"
        )

        # Recent transactions
        tx_data, _ = api_get("/transactions/my-transactions")
        txs = tx_data if tx_data else []
        table = self.query_one("#dash-activity-table")
        table.clear()
        for t in txs[-10:]:
            amt = t.get("amount", 0)
            tx_type = t.get("type", "")
            color = "green" if tx_type in ["DEPOSIT", "TRANSFER_IN"] else "red"
            ts = fmt_date(t.get("timestamp", ""))
            table.add_row(
                ts,
                tx_type,
                f"[{color}]{fmt_money(amt)}[/{color}]",
                t.get("description", "")[:40] if t.get("description") else "-"
            )


class AccountsView(VerticalScroll):
    """View and manage bank accounts."""

    CSS = """
    AccountsView {
        padding: 1 2;
    }
    .acc-title {
        text-style: bold;
        color: $primary;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .acc-table-wrap {
        height: 1fr;
        width: 100%;
        max-height: 20;
    }
    .acc-table-wrap DataTable {
        height: 100%;
    }
    .acc-actions {
        height: 3;
        margin-top: 1;
    }
    .acc-actions Button {
        margin-right: 1;
    }
    """

    def compose(self) -> ComposeResult:
        yield Static(" MY ACCOUNTS ", classes="acc-title")
        with Container(classes="acc-table-wrap"):
            yield DataTable(id="accounts-table")
        with Horizontal(classes="acc-actions"):
            yield Button("Open Checking", id="btn-open-checking", variant="primary")
            yield Button("Open Savings", id="btn-open-savings", variant="success")
            yield Button("Refresh", id="btn-refresh-accounts", variant="default")

    def on_mount(self):
        table = self.query_one("#accounts-table")
        table.add_columns("Account #", "Type", "Balance", "Status", "Visibility")
        table.cursor_type = "row"
        self.refresh_data()

    @work(exclusive=True, group="accounts-refresh")
    async def refresh_data(self):
        data, _ = api_get("/accounts/my-accounts")
        table = self.query_one("#accounts-table")
        table.clear()
        user = load_user_data() or {}
        for acc in (data or []):
            if acc.get("isHidden") and not user.get("showHidden"):
                continue
            bal = acc.get("balance", 0)
            status = acc.get("status", "")
            status_color = "green" if status == "ACTIVE" else "red"
            vis = "Hidden" if acc.get("isHidden") else "Visible"
            table.add_row(
                acc.get("accountId", "?"),
                acc.get("type", "?"),
                f"[bold]{fmt_money(bal)}[/bold]",
                f"[{status_color}]{status}[/{status_color}]",
                vis,
            )

    @on(Button.Pressed, "#btn-open-checking")
    def open_checking(self):
        self.app.push_screen(OpenAccountModal("CHECKING"))

    @on(Button.Pressed, "#btn-open-savings")
    def open_savings(self):
        self.app.push_screen(OpenAccountModal("SAVINGS"))

    @on(Button.Pressed, "#btn-refresh-accounts")
    def do_refresh(self):
        self.refresh_data()


class OpenAccountModal(ModalScreen):
    """Modal to open a new account."""

    def __init__(self, acc_type: str):
        super().__init__()
        self.acc_type = acc_type

    def compose(self) -> ComposeResult:
        with Container(classes="modal-box"):
            yield Static(f"Open {self.acc_type} Account", classes="modal-title")
            yield Input(placeholder="Initial deposit (0.00)", id="open-deposit", type="text")
            yield Static("", id="open-msg")
            with Horizontal():
                yield Button("Confirm", id="open-confirm", variant="primary")
                yield Button("Cancel", id="open-cancel", variant="default")

    @on(Button.Pressed, "#open-confirm")
    @work(exclusive=True, group="open-account")
    async def confirm(self):
        val = self.query_one("#open-deposit").value.strip()
        try:
            amount = float(val) if val else 0.0
        except ValueError:
            self.query_one("#open-msg").update("[red]Invalid amount[/red]")
            return
        if amount < 0:
            self.query_one("#open-msg").update("[red]Amount cannot be negative[/red]")
            return
        data, err = api_post("/accounts/open", params={"type": self.acc_type, "initialDeposit": amount})
        if data:
            self.notify(f"{self.acc_type} account opened!", severity="success")
            self.dismiss(True)
        else:
            self.query_one("#open-msg").update(f"[red]{err}[/red]")

    @on(Button.Pressed, "#open-cancel")
    def cancel(self):
        self.dismiss(False)


class DepositView(VerticalScroll):
    """Deposit funds into an account."""

    CSS = """
    DepositView {
        padding: 1 2;
    }
    .dep-title {
        text-style: bold;
        color: $success;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .dep-form {
        height: auto;
        max-height: 22;
        border: tall $success;
        padding: 1 2;
        width: 60;
    }
    .dep-form Input {
        margin-bottom: 0;
    }
    .dep-form Button {
        margin-top: 1;
        width: 100%;
    }
    .dep-msg {
        height: 1;
        margin-top: 0;
    }
    """

    def compose(self) -> ComposeResult:
        yield Static(" DEPOSIT FUNDS ", classes="dep-title")
        with Container(classes="dep-form"):
            yield Input(placeholder="Account Number", id="dep-account")
            yield Input(placeholder="Amount", id="dep-amount", type="text")
            yield Input(placeholder="Description (optional)", id="dep-desc", value="Deposit")
            yield Static("", id="dep-msg")
            yield Button("Deposit", id="dep-btn", variant="success")

    @on(Button.Pressed, "#dep-btn")
    @work(exclusive=True, group="deposit")
    async def do_deposit(self):
        acc = self.query_one("#dep-account").value.strip()
        amt_str = self.query_one("#dep-amount").value.strip()
        desc = self.query_one("#dep-desc").value.strip() or "Deposit"
        msg = self.query_one("#dep-msg")

        if not acc or not amt_str:
            msg.update("[red]Account and amount are required[/red]")
            return
        try:
            amount = float(amt_str)
        except ValueError:
            msg.update("[red]Invalid amount[/red]")
            return
        if amount <= 0:
            msg.update("[red]Amount must be positive[/red]")
            return
        if amount > MAX_DEPOSIT:
            msg.update(f"[red]Max deposit: {fmt_money(MAX_DEPOSIT)}[/red]")
            return

        data, err = api_post("/accounts/deposit", payload={"accountId": acc, "amount": amount, "description": desc})
        if data is not None:
            msg.update(f"[green]Deposited {fmt_money(amount)} successfully![/green]")
            self.query_one("#dep-amount").value = ""
        else:
            msg.update(f"[red]{err}[/red]")


class WithdrawView(VerticalScroll):
    """Withdraw funds from an account."""

    CSS = """
    WithdrawView {
        padding: 1 2;
    }
    .wd-title {
        text-style: bold;
        color: $warning;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .wd-form {
        height: auto;
        max-height: 22;
        border: tall $warning;
        padding: 1 2;
        width: 60;
    }
    .wd-form Input {
        margin-bottom: 0;
    }
    .wd-form Button {
        margin-top: 1;
        width: 100%;
    }
    .wd-msg {
        height: 1;
        margin-top: 0;
    }
    """

    def compose(self) -> ComposeResult:
        yield Static(" WITHDRAW FUNDS ", classes="wd-title")
        with Container(classes="wd-form"):
            yield Input(placeholder="Account Number", id="wd-account")
            yield Input(placeholder="Amount", id="wd-amount", type="text")
            yield Input(placeholder="Description (optional)", id="wd-desc", value="Withdrawal")
            yield Static("", id="wd-msg")
            yield Button("Withdraw", id="wd-btn", variant="warning")

    @on(Button.Pressed, "#wd-btn")
    @work(exclusive=True, group="withdraw")
    async def do_withdraw(self):
        acc = self.query_one("#wd-account").value.strip()
        amt_str = self.query_one("#wd-amount").value.strip()
        desc = self.query_one("#wd-desc").value.strip() or "Withdrawal"
        msg = self.query_one("#wd-msg")

        if not acc or not amt_str:
            msg.update("[red]Account and amount are required[/red]")
            return
        try:
            amount = float(amt_str)
        except ValueError:
            msg.update("[red]Invalid amount[/red]")
            return
        if amount <= 0:
            msg.update("[red]Amount must be positive[/red]")
            return
        if amount > MAX_WITHDRAWAL:
            msg.update(f"[red]Max withdrawal: {fmt_money(MAX_WITHDRAWAL)}[/red]")
            return

        data, err = api_post("/accounts/withdraw", payload={"accountId": acc, "amount": amount, "description": desc})
        if data is not None:
            msg.update(f"[green]Withdrew {fmt_money(amount)} successfully![/green]")
            self.query_one("#wd-amount").value = ""
        else:
            msg.update(f"[red]{err}[/red]")


class TransferView(VerticalScroll):
    """Transfer funds between accounts."""

    CSS = """
    TransferView {
        padding: 1 2;
    }
    .tr-title {
        text-style: bold;
        color: $accent;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .tr-form {
        height: auto;
        max-height: 28;
        border: tall $accent;
        padding: 1 2;
        width: 60;
    }
    .tr-form Input {
        margin-bottom: 0;
    }
    .tr-form Button {
        margin-top: 1;
        width: 100%;
    }
    .tr-msg {
        height: 1;
        margin-top: 0;
    }
    """

    def compose(self) -> ComposeResult:
        yield Static(" TRANSFER FUNDS ", classes="tr-title")
        with Container(classes="tr-form"):
            yield Input(placeholder="From Account Number", id="tr-from")
            yield Input(placeholder="To Account Number", id="tr-to")
            yield Input(placeholder="Amount", id="tr-amount", type="text")
            yield Input(placeholder="Description (optional)", id="tr-desc", value="Transfer")
            yield Static("", id="tr-msg")
            yield Button("Transfer", id="tr-btn", variant="primary")

    @on(Button.Pressed, "#tr-btn")
    @work(exclusive=True, group="transfer")
    async def do_transfer(self):
        from_acc = self.query_one("#tr-from").value.strip()
        to_acc = self.query_one("#tr-to").value.strip()
        amt_str = self.query_one("#tr-amount").value.strip()
        desc = self.query_one("#tr-desc").value.strip() or "Transfer"
        msg = self.query_one("#tr-msg")

        if not all([from_acc, to_acc, amt_str]):
            msg.update("[red]All fields are required[/red]")
            return
        if from_acc == to_acc:
            msg.update("[red]Cannot transfer to the same account[/red]")
            return
        try:
            amount = float(amt_str)
        except ValueError:
            msg.update("[red]Invalid amount[/red]")
            return
        if amount <= 0:
            msg.update("[red]Amount must be positive[/red]")
            return
        if amount > MAX_TRANSFER:
            msg.update(f"[red]Max transfer: {fmt_money(MAX_TRANSFER)}[/red]")
            return

        data, err = api_post("/accounts/transfer", payload={
            "fromAccountId": from_acc, "toAccountId": to_acc,
            "amount": amount, "description": desc
        })
        if data is not None:
            msg.update(f"[green]Transferred {fmt_money(amount)} successfully![/green]")
            self.query_one("#tr-amount").value = ""
        else:
            msg.update(f"[red]{err}[/red]")


class LoansView(VerticalScroll):
    """View and manage loans."""

    CSS = """
    LoansView {
        padding: 1 2;
    }
    .loan-title {
        text-style: bold;
        color: $warning;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .loan-table-wrap {
        height: auto;
        max-height: 18;
        width: 100%;
        margin-bottom: 1;
    }
    .loan-table-wrap DataTable {
        height: 100%;
    }
    .loan-actions {
        height: 3;
        margin-top: 0;
    }
    .loan-actions Button {
        margin-right: 1;
    }
    .loan-form-area {
        height: auto;
        max-height: 22;
        margin-top: 1;
    }
    .loan-form {
        height: auto;
        max-height: 22;
        border: tall $warning;
        padding: 1 2;
        width: 60;
    }
    .loan-form Input {
        margin-bottom: 0;
    }
    .loan-form Button {
        margin-top: 1;
        width: 100%;
    }
    .loan-msg {
        height: 1;
    }
    """

    def compose(self) -> ComposeResult:
        yield Static(" LOANS ", classes="loan-title")
        with Container(classes="loan-table-wrap"):
            yield DataTable(id="loans-table")
        with Horizontal(classes="loan-actions"):
            yield Button("Request Loan", id="btn-request-loan", variant="warning")
            yield Button("Refresh", id="btn-refresh-loans", variant="default")
        with Container(classes="loan-form-area", id="loan-form-area"):
            with Container(classes="loan-form"):
                yield Input(placeholder="Loan Amount", id="loan-amount", type="text")
                yield Input(placeholder="Interest Rate (%)", id="loan-rate", type="text", value="5.0")
                yield Input(placeholder="Loan ID (for repay)", id="loan-repay-id", type="text")
                yield Input(placeholder="Repay Amount", id="loan-repay-amount", type="text")
                yield Static("", id="loan-msg")
                with Horizontal():
                    yield Button("Submit Loan", id="loan-submit", variant="warning")
                    yield Button("Repay Loan", id="loan-repay", variant="success")
                    yield Button("Close", id="loan-close-form", variant="default")

    def on_mount(self):
        table = self.query_one("#loans-table")
        table.add_columns("Loan ID", "Amount", "Remaining", "Rate", "Status", "Auto-Debt")
        table.cursor_type = "row"
        self.query_one("#loan-form-area").display = False
        self.refresh_data()

    @work(exclusive=True, group="loans-refresh")
    async def refresh_data(self):
        data, _ = api_get("/loans/my-loans")
        table = self.query_one("#loans-table")
        table.clear()
        for l in (data or []):
            status = l.get("status", "")
            s_color = "green" if status == "PAID" else "yellow" if status == "PENDING" else "blue"
            table.add_row(
                l.get("loanId", "?"),
                fmt_money(l.get("amount", 0)),
                fmt_money(l.get("remainingBalance", 0)),
                f"{l.get('interestRate', 0)}%",
                f"[{s_color}]{status}[/{s_color}]",
                "ON" if l.get("autoDebtEnabled") else "OFF",
            )

    @on(Button.Pressed, "#btn-request-loan")
    def toggle_form(self):
        area = self.query_one("#loan-form-area")
        area.display = not area.display

    @on(Button.Pressed, "#btn-refresh-loans")
    def do_refresh(self):
        self.refresh_data()

    @on(Button.Pressed, "#loan-close-form")
    def close_form(self):
        self.query_one("#loan-form-area").display = False

    @on(Button.Pressed, "#loan-submit")
    @work(exclusive=True, group="loan-submit")
    async def submit_loan(self):
        amt_str = self.query_one("#loan-amount").value.strip()
        rate_str = self.query_one("#loan-rate").value.strip()
        msg = self.query_one("#loan-msg")

        try:
            amount = float(amt_str)
            rate = float(rate_str) if rate_str else 5.0
        except ValueError:
            msg.update("[red]Invalid amount or rate[/red]")
            return
        if amount <= 0 or amount > MAX_LOAN_AMOUNT:
            msg.update(f"[red]Amount must be 1-{fmt_money(MAX_LOAN_AMOUNT)}[/red]")
            return

        data, err = api_post("/loans/request", payload={"amount": amount, "interestRate": rate})
        if data is not None:
            msg.update("[green]Loan request submitted![/green]")
            self.query_one("#loan-amount").value = ""
            self.refresh_data()
        else:
            msg.update(f"[red]{err}[/red]")

    @on(Button.Pressed, "#loan-repay")
    @work(exclusive=True, group="loan-repay")
    async def repay_loan(self):
        loan_id = self.query_one("#loan-repay-id").value.strip()
        amt_str = self.query_one("#loan-repay-amount").value.strip()
        msg = self.query_one("#loan-msg")

        if not loan_id or not amt_str:
            msg.update("[red]Loan ID and amount required[/red]")
            return
        try:
            amount = float(amt_str)
        except ValueError:
            msg.update("[red]Invalid amount[/red]")
            return

        data, err = api_post("/loans/repay", params={
            "loanId": loan_id, "amount": amount
        })
        if data is not None:
            msg.update("[green]Repayment successful![/green]")
            self.query_one("#loan-repay-amount").value = ""
            self.query_one("#loan-repay-id").value = ""
            self.refresh_data()
        else:
            msg.update(f"[red]{err}[/red]")


class TransactionsView(VerticalScroll):
    """View transaction history."""

    CSS = """
    TransactionsView {
        padding: 1 2;
    }
    .tx-title {
        text-style: bold;
        color: $primary;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .tx-table-wrap {
        height: 1fr;
        width: 100%;
    }
    .tx-table-wrap DataTable {
        height: 100%;
    }
    """

    def compose(self) -> ComposeResult:
        yield Static(" TRANSACTION HISTORY ", classes="tx-title")
        with Container(classes="tx-table-wrap"):
            yield DataTable(id="tx-table")

    def on_mount(self):
        table = self.query_one("#tx-table")
        table.add_columns("Date", "Type", "From", "To", "Amount", "Description")
        table.cursor_type = "row"
        self.refresh_data()

    @work(exclusive=True, group="tx-refresh")
    async def refresh_data(self):
        data, err = api_get("/transactions/my-transactions")
        table = self.query_one("#tx-table")
        table.clear()
        if data is None:
            table.add_row("-", "-", "-", "-", "-", err or "Failed to load")
            return
        if not data:
            table.add_row("-", "-", "-", "-", "-", "No transactions yet")
            return
        for t in data:
            amt = t.get("amount", 0)
            tx_type = t.get("type", "")
            color = "green" if tx_type in ["DEPOSIT", "TRANSFER_IN"] else "red"
            ts = fmt_date(t.get("timestamp", ""))
            table.add_row(
                ts,
                tx_type,
                t.get("fromAccount", "-") or "-",
                t.get("toAccount", "-") or "-",
                f"[{color}]{fmt_money(amt)}[/{color}]",
                (t.get("description", "") or "-")[:30],
            )


class AdminView(VerticalScroll):
    """Admin panel for system management."""

    CSS = """
    AdminView {
        padding: 1 2;
    }
    .admin-title {
        text-style: bold;
        color: $error;
        height: 2;
        width: 100%;
        margin-bottom: 1;
    }
    .admin-nav {
        height: 3;
        width: 100%;
        margin-bottom: 1;
    }
    .admin-nav Button {
        margin-right: 1;
    }
    .admin-table-wrap {
        height: 1fr;
        width: 100%;
    }
    .admin-table-wrap DataTable {
        height: 100%;
    }
    .admin-actions {
        height: 3;
        margin-top: 1;
    }
    .admin-actions Button {
        margin-right: 1;
    }
    .admin-msg {
        height: 1;
        margin-top: 0;
    }
    """

    admin_section = var("customers")

    def compose(self) -> ComposeResult:
        yield Static(" ADMIN PANEL ", classes="admin-title")
        with Horizontal(classes="admin-nav"):
            yield Button("Customers", id="admin-customers", variant="primary")
            yield Button("Accounts", id="admin-accounts", variant="success")
            yield Button("Loans", id="admin-loans", variant="warning")
            yield Button("Transactions", id="admin-transactions", variant="primary")
            yield Button("DB Explorer", id="admin-db", variant="default")
        with Container(classes="admin-table-wrap"):
            yield DataTable(id="admin-table")
        with Horizontal(classes="admin-actions"):
            yield Button("Refresh", id="admin-refresh", variant="default")
            yield Button("Delete Selected", id="admin-delete", variant="error")
        yield Static("", id="admin-msg")

    def on_mount(self):
        table = self.query_one("#admin-table")
        table.cursor_type = "row"
        self._load_section("customers")

    @work(exclusive=True, group="admin-section")
    async def _load_section(self, section: str):
        self.admin_section = section
        table = self.query_one("#admin-table")
        table.clear(columns=True)

        if section == "customers":
            table.add_columns("ID", "Name", "Email", "Phone", "Role", "Joined")
            data, _ = api_get("/accounts/admin/all-customers")
            for c in (data or []):
                table.add_row(
                    c.get("customerId", "?")[:12],
                    c.get("fullName", "?"),
                    c.get("email", "?"),
                    c.get("phone", "?"),
                    c.get("role", "?"),
                    fmt_date(c.get("registeredAt", "")),
                )

        elif section == "accounts":
            table.add_columns("Account #", "Customer", "Type", "Balance", "Status")
            data, _ = api_get("/accounts/admin/all-accounts")
            for a in (data or []):
                status = a.get("status", "")
                s_color = "green" if status == "ACTIVE" else "red"
                table.add_row(
                    a.get("accountId", "?"),
                    a.get("customerId", "?")[:12],
                    a.get("type", "?"),
                    fmt_money(a.get("balance", 0)),
                    f"[{s_color}]{status}[/{s_color}]",
                )

        elif section == "loans":
            table.add_columns("Loan ID", "Customer", "Amount", "Remaining", "Rate", "Status")
            data, _ = api_get("/loans/admin/all")
            for l in (data or []):
                status = l.get("status", "")
                s_color = "green" if status == "PAID" else "yellow" if status == "PENDING" else "blue"
                table.add_row(
                    l.get("loanId", "?"),
                    l.get("customerName", "?"),
                    fmt_money(l.get("amount", 0)),
                    fmt_money(l.get("remainingBalance", 0)),
                    f"{l.get('interestRate', 0)}%",
                    f"[{s_color}]{status}[/{s_color}]",
                )

        elif section == "transactions":
            table.add_columns("ID", "Date", "Type", "From", "To", "Amount")
            data, _ = api_get("/transactions/admin/all")
            for t in (data or []):
                amt = t.get("amount", 0)
                tx_type = t.get("type", "")
                color = "green" if tx_type in ["DEPOSIT", "TRANSFER_IN"] else "red"
                table.add_row(
                    t.get("transactionId", "?")[:10],
                    fmt_date(t.get("timestamp", "")),
                    tx_type,
                    t.get("fromAccount", "-") or "-",
                    t.get("toAccount", "-") or "-",
                    f"[{color}]{fmt_money(amt)}[/{color}]",
                )

        elif section == "db":
            table.add_columns("Metric", "Value")
            cust_data, _ = api_get("/accounts/admin/all-customers")
            acc_data, _ = api_get("/accounts/admin/all-accounts")
            tx_data, _ = api_get("/transactions/admin/all")
            loan_data, _ = api_get("/loans/admin/all")
            customers = cust_data or []
            accounts = acc_data or []
            txs = tx_data or []
            loans = loan_data or []
            total_bal = sum(a.get("balance", 0) for a in accounts)
            pending_loans = sum(1 for l in loans if l.get("status") == "PENDING")
            total_loan_rem = sum(l.get("remainingBalance", 0) for l in loans)
            table.add_row("Total Customers", str(len(customers)))
            table.add_row("Admins", str(sum(1 for c in customers if c.get("role") == "ADMIN")))
            table.add_row("Total Accounts", str(len(accounts)))
            table.add_row("Total Balance", fmt_money(total_bal))
            table.add_row("Total Transactions", str(len(txs)))
            table.add_row("Total Loans", str(len(loans)))
            table.add_row("Pending Loans", str(pending_loans))
            table.add_row("Total Loan Remaining", fmt_money(total_loan_rem))

    @on(Button.Pressed, "#admin-customers")
    def show_customers(self):
        self._load_section("customers")

    @on(Button.Pressed, "#admin-accounts")
    def show_accounts(self):
        self._load_section("accounts")

    @on(Button.Pressed, "#admin-loans")
    def show_loans(self):
        self._load_section("loans")

    @on(Button.Pressed, "#admin-transactions")
    def show_transactions(self):
        self._load_section("transactions")

    @on(Button.Pressed, "#admin-db")
    def show_db(self):
        self._load_section("db")

    @on(Button.Pressed, "#admin-refresh")
    def do_refresh(self):
        self._load_section(self.admin_section)

    @on(Button.Pressed, "#admin-delete")
    @work(exclusive=True, group="admin-delete")
    async def do_delete(self):
        msg = self.query_one("#admin-msg")
        table = self.query_one("#admin-table")
        if table.cursor_row is None:
            msg.update("[yellow]Select a row first[/yellow]")
            return

        result = await self.app.push_screen_async(ConfirmModal(
            "Confirm Delete",
            "Are you sure you want to delete the selected item? This cannot be undone."
        ))
        if not result:
            msg.update("[dim]Delete cancelled[/dim]")
            return

        row_data = table.get_row_at(table.cursor_row)
        if self.admin_section == "accounts" and row_data:
            acc_id = row_data[0]
            data, err = api_delete(f"/accounts/admin/accounts/{acc_id}")
            if data is not None:
                msg.update(f"[green]Account {acc_id} deleted[/green]")
                self._load_section(self.admin_section)
            else:
                msg.update(f"[red]{err}[/red]")
        elif self.admin_section == "customers" and row_data:
            cust_id = row_data[0]
            data, err = api_delete(f"/accounts/admin/customers/{cust_id}")
            if data is not None:
                msg.update(f"[green]Customer {cust_id} deleted[/green]")
                self._load_section(self.admin_section)
            else:
                msg.update(f"[red]{err}[/red]")
        else:
            msg.update("[yellow]Delete only available for accounts and customers[/yellow]")


# ─── Main App ────────────────────────────────────────────────────────────────

class OriosBankApp(App):
    """OriosBank Terminal UI Application."""

    TITLE = "OriosBank TUI"
    SUB_TITLE = "Premium Digital Banking Terminal"

    CSS = TUI_CSS

    SCREENS = {
        "login": LoginScreen,
        "main": MainScreen,
    }

    BINDINGS = [
        Binding("ctrl+c", "quit", "Quit", show=False),
    ]

    def on_mount(self):
        token = load_token()
        if token:
            # Try to refresh user data
            data, _ = api_get("/auth/me")
            if data:
                save_token(token, data)
                self.push_screen(MainScreen())
            else:
                clear_session()
                self.push_screen(LoginScreen())
        else:
            self.push_screen(LoginScreen())


def main():
    app = OriosBankApp()
    app.run()


if __name__ == "__main__":
    main()
