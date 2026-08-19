import requests
import json
import os
import sys
import argparse
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.text import Text
from rich.theme import Theme
from rich.prompt import Prompt, FloatPrompt
from datetime import datetime

API_BASE_URL = "http://localhost:8080/api"
TOKEN_FILE = os.path.expanduser("~/.moocash_token")
USER_DATA_FILE = os.path.expanduser("~/.moocash_user")

MAX_DEPOSIT = 100_000.0
MAX_WITHDRAWAL = 50_000.0
MAX_TRANSFER = 100_000.0
MAX_LOAN_AMOUNT = 2_000_000.0
MAX_ACCOUNTS_PER_TYPE = 2
MAX_TRANSFERS_PER_DAY = 5
MAX_DEPOSITS_PER_DAY = 5
MAX_WITHDRAWALS_PER_DAY = 5
MAX_LOANS_PER_WEEK = 2

custom_theme = Theme({
    "info": "cyan",
    "warning": "yellow",
    "error": "bold red",
    "success": "bold green",
    "accent": "bold magenta",
    "header": "bold blue",
    "acc_num": "bold yellow"
})

console = Console(theme=custom_theme)

BANNER = r"""
███╗   ███╗   ██████╗    ██████╗    ██████╗   █████╗   ███████╗  ██╗  ██╗
████╗ ████║  ██╔═══██╗  ██╔═══██╗  ██╔════╝  ██╔══██╗  ██╔════╝  ██║  ██║
██╔████╔██║  ██║   ██║  ██║   ██║  ██║       ███████║  ███████╗  ███████║
██║╚██╔╝██║  ██║   ██║  ██║   ██║  ██║       ██╔══██║  ╚════██║  ██╔══██║
██║ ╚═╝ ██║  ╚██████╔╝  ╚██████╔╝  ╚██████╗  ██║  ██║  ███████║  ██║  ██║
╚═╝     ╚═╝   ╚═════╝    ╚═════╝    ╚═════╝  ╚═╝  ╚═╝  ╚══════╝  ╚═╝  ╚═╝

                          ^__^
     I can secure U      (oo)\_______
                        (__)\       )\/\
                            / |----w /|
                           /  |     / |
"""

TAGLINE = "Every payments secured by our COWS"


def show_banner():
    width = console.width
    lines = BANNER.splitlines()
    banner_width = max(len(line) for line in lines)

    gradient_colors = ["bold cyan", "bold blue", "bold magenta"]

    for i, line in enumerate(lines):
        padding = max(0, (width - banner_width) // 2)
        styled_line = Text(" " * padding)
        color = gradient_colors[i % len(gradient_colors)]
        styled_line.append(line, style=color)
        console.print(styled_line)

    tagline_padding = max(0, (width - len(TAGLINE)) // 2)
    console.print()
    tagline = Text(" " * tagline_padding)
    tagline.append(TAGLINE, style="dim italic cyan")
    console.print(tagline)


def save_token(token, user_data):
    with open(TOKEN_FILE, "w", encoding="utf-8") as f:
        f.write(token)

    with open(USER_DATA_FILE, "w", encoding="utf-8") as f:
        json.dump(user_data, f)


def load_token():
    if os.path.exists(TOKEN_FILE):
        try:
            with open(TOKEN_FILE, "r", encoding="utf-8") as f:
                return f.read().strip()
        except OSError:
            return None
    return None


def load_user_data():
    if os.path.exists(USER_DATA_FILE):
        try:
            with open(USER_DATA_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except (OSError, json.JSONDecodeError):
            return None
    return None


def clear_session():
    for path in (TOKEN_FILE, USER_DATA_FILE):
        try:
            if os.path.exists(path):
                os.remove(path)
        except OSError:
            pass


def get_headers():
    token = load_token()
    if token:
        return {"Authorization": f"Bearer {token}"}
    return {}


def handle_response(response, success_msg=None):
    if response.status_code in (200, 201, 202, 204):
        if success_msg:
            console.print(f"[success]✔ {success_msg}[/success]")

        if response.status_code == 204:
            return True

        try:
            return response.json()
        except (ValueError, requests.exceptions.JSONDecodeError):
            return True

    try:
        error_data = response.json()

        if isinstance(error_data, dict):
            msg = (
                    error_data.get("message")
                    or error_data.get("error")
                    or error_data.get("detail")
                    or "Unknown error"
            )
        else:
            msg = str(error_data)

        console.print(f"[error]✘ Error: {msg}[/error]")
    except (ValueError, requests.exceptions.JSONDecodeError):
        console.print(
            f"[error]✘ Error: HTTP {response.status_code} "
            f"{response.reason or ''}[/error]"
        )

    return None


def api_request(method, endpoint, **kwargs):
    try:
        return requests.request(
            method,
            f"{API_BASE_URL}{endpoint}",
            timeout=15,
            **kwargs
        )
    except requests.exceptions.Timeout:
        console.print("[error]✘ Request timed out.[/error]")
    except requests.exceptions.ConnectionError:
        console.print(
            "[error]✘ Could not connect to the MooCash API. "
            "Make sure the server is running.[/error]"
        )
    except requests.exceptions.RequestException as exc:
        console.print(f"[error]✘ Request failed: {exc}[/error]")

    return None


def login(email, password):
    payload = {
        "email": email,
        "password": password
    }

    response = api_request(
        "POST",
        "/auth/login",
        json=payload
    )

    if response is None:
        return False

    data = handle_response(response, "Login successful!")

    if not isinstance(data, dict):
        return False

    token = data.get("token")

    if not token:
        console.print("[error]✘ Login response did not contain a token.[/error]")
        return False

    save_token(token, data)
    return True


def register(first_name, last_name, email, phone, password):
    import re

    first_name = first_name.strip()
    last_name = last_name.strip()
    email = email.strip()
    phone = phone.strip()

    if not first_name:
        console.print("[error]✘ First name is required.[/error]")
        return

    if not last_name:
        console.print("[error]✘ Last name is required.[/error]")
        return

    if not email:
        console.print("[error]✘ Email is required.[/error]")
        return

    if not re.match(r"^0[0-9]{10}$", phone):
        console.print(
            "[error]✘ Phone number must be exactly 11 digits "
            "starting with 0 (e.g. 09123456789)[/error]"
        )
        return

    if not password:
        console.print("[error]✘ Password is required.[/error]")
        return

    if len(password) < 6:
        console.print("[error]✘ Password must be at least 6 characters.[/error]")
        return

    payload = {
        "firstName": first_name,
        "lastName": last_name,
        "email": email,
        "phone": phone,
        "password": password
    }

    try:
        response = requests.post(
            f"{API_BASE_URL}/auth/register",
            json=payload,
            timeout=15
        )

        handle_response(
            response,
            "Registration successful! You can now login."
        )

    except requests.RequestException as e:
        console.print(f"[error]✘ Connection error: {e}[/error]")


def get_session():
    token = load_token()
    user = load_user_data()

    if token and not user:
        response = api_request(
            "GET",
            "/auth/me",
            headers={"Authorization": f"Bearer {token}"}
        )

        if response is not None and response.status_code == 200:
            try:
                user = response.json()

                if isinstance(user, dict):
                    save_token(token, user)
                else:
                    clear_session()
                    token = None
                    user = None
            except (ValueError, requests.exceptions.JSONDecodeError):
                clear_session()
                token = None
                user = None
        else:
            clear_session()
            token = None
            user = None

    return token, user


def show_dashboard():
    _, user = get_session()

    if not user:
        console.print("[warning]Please login first.[/warning]")
        return

    first_name = user.get("firstName") or user.get("lastName") or user.get("email", "User")
    role = user.get("role", "CUSTOMER")

    console.print(
        Panel(
            f"Welcome back, [accent]{first_name}[/accent]!\n"
            f"Role: [info]{role}[/info]",
            title="[header]MooCash Dashboard[/header]",
            border_style="blue"
        )
    )

    response = api_request(
        "GET",
        "/accounts/my-accounts",
        headers=get_headers()
    )

    if response is None:
        return

    accounts = handle_response(response)

    if not isinstance(accounts, list):
        console.print("[info]No accounts found.[/info]")
        return

    if not accounts:
        console.print(
            "[info]No accounts found. Open your first account today![/info]"
        )
        return

    table = Table(
        title="Your Accounts",
        box=None,
        header_style="bold blue"
    )

    table.add_column("Account Number", style="acc_num")
    table.add_column("Type", style="info")
    table.add_column("Balance", justify="right", style="success")
    table.add_column("Status")
    table.add_column("Visibility")

    total_balance = 0.0
    show_hidden = user.get("showHidden", False)

    for acc in accounts:
        balance = float(acc.get("balance", 0) or 0)
        total_balance += balance

        if acc.get("isHidden") and not show_hidden:
            continue

        status = acc.get("status", "UNKNOWN")
        status_markup = (
            f"[bold green]{status}[/bold green]"
            if status == "ACTIVE"
            else f"[bold red]{status}[/bold red]"
        )

        table.add_row(
            str(acc.get("accountId", "N/A")),
            str(acc.get("type", "N/A")),
            f"${balance:,.2f}",
            status_markup,
            "Hidden" if acc.get("isHidden") else "Visible"
        )

    console.print(table)
    console.print(
        f"\n[bold]Total Net Worth: "
        f"[success]${total_balance:,.2f}[/success][/bold]\n"
    )


def open_account(acc_type, initial_deposit=0.0):
    acc_type = acc_type.upper()

    if acc_type not in ("CHECKING", "SAVINGS"):
        console.print("[error]✘ Invalid account type.[/error]")
        return

    if initial_deposit < 0:
        console.print(
            "[error]✘ Initial deposit cannot be negative.[/error]"
        )
        return

    if initial_deposit > MAX_DEPOSIT:
        console.print(
            f"[error]✘ Initial deposit limit exceeded. "
            f"Maximum: ${MAX_DEPOSIT:,.2f}[/error]"
        )
        return

    params = {
        "type": acc_type,
        "initialDeposit": initial_deposit
    }

    response = api_request(
        "POST",
        "/accounts/open",
        params=params,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"{acc_type.capitalize()} account opened successfully!"
        )


def deposit(acc_id, amount, desc="Deposit"):
    if amount <= 0:
        console.print("[error]✘ Deposit amount must be positive.[/error]")
        return

    if amount > MAX_DEPOSIT:
        console.print(
            f"[error]✘ Deposit limit exceeded. "
            f"Maximum per transaction: ${MAX_DEPOSIT:,.2f}[/error]"
        )
        return

    payload = {
        "accountId": acc_id,
        "amount": amount,
        "description": desc
    }

    response = api_request(
        "POST",
        "/accounts/deposit",
        json=payload,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Deposited ${amount:,.2f} to {acc_id}"
        )


def withdraw(acc_id, amount, desc="Withdrawal"):
    if amount <= 0:
        console.print(
            "[error]✘ Withdrawal amount must be positive.[/error]"
        )
        return

    if amount > MAX_WITHDRAWAL:
        console.print(
            f"[error]✘ Withdrawal limit exceeded. "
            f"Maximum per transaction: ${MAX_WITHDRAWAL:,.2f}[/error]"
        )
        return

    payload = {
        "accountId": acc_id,
        "amount": amount,
        "description": desc
    }

    response = api_request(
        "POST",
        "/accounts/withdraw",
        json=payload,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Withdrew ${amount:,.2f} from {acc_id}"
        )


def transfer(from_id, to_id, amount, desc="Transfer"):
    if amount <= 0:
        console.print(
            "[error]✘ Transfer amount must be positive.[/error]"
        )
        return

    if amount > MAX_TRANSFER:
        console.print(
            f"[error]✘ Transfer limit exceeded. "
            f"Maximum per transaction: ${MAX_TRANSFER:,.2f}[/error]"
        )
        return

    if from_id == to_id:
        console.print(
            "[error]✘ Cannot transfer to the same account.[/error]"
        )
        return

    payload = {
        "fromAccountId": from_id,
        "toAccountId": to_id,
        "amount": amount,
        "description": desc
    }

    response = api_request(
        "POST",
        "/accounts/transfer",
        json=payload,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Transferred ${amount:,.2f} from {from_id} to {to_id}"
        )


def toggle_visibility(acc_id):
    response = api_request(
        "POST",
        f"/accounts/{acc_id}/toggle-visibility",
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Visibility toggled for account {acc_id}"
        )


def request_loan(amount, interest_rate):
    if amount <= 0:
        console.print(
            "[error]✘ Loan amount must be greater than zero.[/error]"
        )
        return

    if amount > MAX_LOAN_AMOUNT:
        console.print(
            f"[error]✘ Loan amount exceeded. "
            f"Maximum loan: ${MAX_LOAN_AMOUNT:,.2f}[/error]"
        )
        return

    if interest_rate < 0:
        console.print(
            "[error]✘ Interest rate cannot be negative.[/error]"
        )
        return

    payload = {
        "amount": amount,
        "interestRate": interest_rate
    }

    response = api_request(
        "POST",
        "/loans/request",
        json=payload,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            "Loan request submitted successfully!"
        )


def repay_loan(loan_id, from_acc, amount):
    if amount <= 0:
        console.print(
            "[error]✘ Amount must be greater than zero.[/error]"
        )
        return

    params = {
        "loanId": loan_id,
        "fromAccountId": from_acc,
        "amount": amount
    }

    response = api_request(
        "POST",
        "/loans/repay",
        params=params,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Repayment of ${amount:,.2f} for loan {loan_id} processed."
        )


def show_my_loans():
    response = api_request(
        "GET",
        "/loans/my-loans",
        headers=get_headers()
    )

    if response is None:
        return

    loans = handle_response(response)

    if not isinstance(loans, list) or not loans:
        console.print("[info]No loans found.[/info]")
        return

    table = Table(
        title="Your Loans",
        box=None,
        header_style="bold magenta"
    )

    table.add_column("Loan ID", style="info")
    table.add_column("Principal", justify="right")
    table.add_column("Remaining", justify="right", style="error")
    table.add_column("Rate", justify="right")
    table.add_column("Status")
    table.add_column("Auto Debt")

    for loan in loans:
        status = loan.get("status", "UNKNOWN")

        if status == "PAID":
            status_color = "green"
        elif status == "PENDING":
            status_color = "yellow"
        else:
            status_color = "blue"

        table.add_row(
            str(loan.get("loanId", "N/A")),
            f"${float(loan.get('amount', 0) or 0):,.2f}",
            f"${float(loan.get('remainingBalance', 0) or 0):,.2f}",
            f"{loan.get('interestRate', 0)}%",
            f"[{status_color}]{status}[/{status_color}]",
            "Enabled" if loan.get("autoDebtEnabled") else "Disabled"
        )

    console.print(table)


def show_admin_loans():
    response = api_request(
        "GET",
        "/loans/admin/all",
        headers=get_headers()
    )

    if response is None:
        return

    loans = handle_response(response)

    if not isinstance(loans, list) or not loans:
        console.print("[info]No loans in the system.[/info]")
        return

    table = Table(
        title="System Loans",
        header_style="bold magenta"
    )

    table.add_column("Loan ID", style="info")
    table.add_column("Customer")
    table.add_column("Amount", justify="right")
    table.add_column("Remaining", justify="right")
    table.add_column("Status")
    table.add_column("Auto Debt")

    for loan in loans:
        table.add_row(
            str(loan.get("loanId", "N/A")),
            str(loan.get("customerName", "N/A")),
            f"${float(loan.get('amount', 0) or 0):,.2f}",
            f"${float(loan.get('remainingBalance', 0) or 0):,.2f}",
            str(loan.get("status", "N/A")),
            "YES" if loan.get("autoDebtEnabled") else "NO"
        )

    console.print(table)


def approve_loan_admin(loan_id, target_acc):
    params = {
        "targetAccountId": target_acc
    }

    response = api_request(
        "POST",
        f"/loans/admin/{loan_id}/approve",
        params=params,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Loan {loan_id} approved. Funds sent to {target_acc}."
        )


def reject_loan_admin(loan_id):
    response = api_request(
        "POST",
        f"/loans/admin/{loan_id}/reject",
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Loan {loan_id} rejected."
        )


def toggle_auto_debt_admin(loan_id, enabled):
    params = {
        "enabled": str(enabled).lower()
    }

    response = api_request(
        "POST",
        f"/loans/admin/{loan_id}/auto-debt",
        params=params,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Auto-debt {'enabled' if enabled else 'disabled'} "
            f"for loan {loan_id}."
        )


def process_auto_debts_admin():
    response = api_request(
        "POST",
        "/loans/admin/process-auto-debts",
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            "Auto-debt processing triggered for all eligible loans."
        )


def show_admin_users():
    response = api_request(
        "GET",
        "/accounts/admin/all-customers",
        headers=get_headers()
    )

    if response is None:
        return

    customers = handle_response(response)

    if not isinstance(customers, list) or not customers:
        console.print("[info]No customers found.[/info]")
        return

    table = Table(
        title="All Customers",
        header_style="bold magenta"
    )

    table.add_column("Customer ID", style="dim")
    table.add_column("First Name")
    table.add_column("Last Name")
    table.add_column("Email")
    table.add_column("Role")
    table.add_column("Joined")

    for customer in customers:
        reg_at = customer.get("registeredAt")

        if reg_at:
            try:
                date_str = datetime.fromisoformat(
                    reg_at.replace("Z", "+00:00")
                ).strftime("%Y-%m-%d %H:%M")
            except (ValueError, TypeError):
                date_str = str(reg_at)
        else:
            date_str = "N/A"

        table.add_row(
            str(customer.get("customerId", "N/A")),
            str(customer.get("firstName", "N/A")),
            str(customer.get("lastName", "N/A")),
            str(customer.get("email", "N/A")),
            str(customer.get("role", "N/A")),
            date_str
        )

    console.print(table)


def show_admin_transactions():
    response = api_request(
        "GET",
        "/transactions/admin/all",
        headers=get_headers()
    )

    if response is None:
        return

    txs = handle_response(response)

    if not isinstance(txs, list) or not txs:
        console.print(
            "[info]No transactions found in the system.[/info]"
        )
        return

    table = Table(
        title="Global Transaction Record",
        header_style="bold red"
    )

    table.add_column("ID", style="dim")
    table.add_column("Date", style="info")
    table.add_column("From", style="acc_num")
    table.add_column("To", style="acc_num")
    table.add_column("Type")
    table.add_column("Amount", justify="right")
    table.add_column("Description")

    for tx in txs:
        timestamp = tx.get("timestamp", "N/A")

        if timestamp and timestamp != "N/A":
            try:
                timestamp = datetime.fromisoformat(
                    str(timestamp).replace("Z", "+00:00")
                ).strftime("%b %d, %H:%M")
            except (ValueError, TypeError):
                pass

        tx_type = tx.get("type", "UNKNOWN")
        amount_style = (
            "success"
            if tx_type in ("DEPOSIT", "TRANSFER_IN")
            else "error"
        )

        tx_id = str(
            tx.get("id", tx.get("transactionId", "N/A"))
        )

        table.add_row(
            tx_id[:8],
            str(timestamp),
            str(tx.get("fromAccount") or "-"),
            str(tx.get("toAccount") or "-"),
            str(tx_type),
            f"[{amount_style}]"
            f"${float(tx.get('amount', 0) or 0):,.2f}"
            f"[/{amount_style}]",
            str(tx.get("description") or "")
        )

    console.print(table)


def delete_account_admin(acc_id):
    confirmed = Prompt.ask(
        f"Are you sure you want to [bold red]DELETE[/bold red] "
        f"account {acc_id}?",
        choices=["y", "n"],
        default="n"
    )

    if confirmed != "y":
        return

    response = api_request(
        "DELETE",
        f"/accounts/admin/accounts/{acc_id}",
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Account {acc_id} deleted successfully."
        )


def delete_customer_admin(cust_id):
    confirmed = Prompt.ask(
        f"Are you sure you want to [bold red]DELETE[/bold red] "
        f"customer {cust_id} and ALL their data?",
        choices=["y", "n"],
        default="n"
    )

    if confirmed != "y":
        return

    response = api_request(
        "DELETE",
        f"/accounts/admin/customers/{cust_id}",
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Customer {cust_id} and all associated data deleted."
        )


def admin_change_password(cust_id, new_password=None):
    if not new_password:
        new_password = Prompt.ask(
            f"Enter new password for customer {cust_id}",
            password=True
        )

    if len(new_password) < 6:
        console.print(
            "[error]✘ Password must be at least 6 characters.[/error]"
        )
        return

    payload = {
        "newPassword": new_password
    }

    response = api_request(
        "POST",
        f"/auth/admin/change-password/{cust_id}",
        json=payload,
        headers=get_headers()
    )

    if response is not None:
        handle_response(
            response,
            f"Password for customer {cust_id} updated successfully."
        )


def show_admin_accounts():
    response = api_request(
        "GET",
        "/accounts/admin/all-accounts",
        headers=get_headers()
    )

    if response is None:
        return

    accounts = handle_response(response)

    if not isinstance(accounts, list) or not accounts:
        console.print("[info]No accounts found.[/info]")
        return

    table = Table(
        title="All System Accounts",
        header_style="bold magenta"
    )

    table.add_column("Account #", style="acc_num")
    table.add_column("Customer ID")
    table.add_column("Balance", justify="right")
    table.add_column("Status")

    for account in accounts:
        table.add_row(
            str(account.get("accountId", "N/A")),
            str(account.get("customerId", "N/A")),
            f"${float(account.get('balance', 0) or 0):,.2f}",
            str(account.get("status", "N/A"))
        )

    console.print(table)


def show_database_view():
    console.print(
        Panel.fit(
            "[header]Database Explorer[/header]\n"
            "[italic]View all system data[/italic]",
            border_style="blue"
        )
    )

    endpoints = {
        "customers": "/accounts/admin/all-customers",
        "accounts": "/accounts/admin/all-accounts",
        "transactions": "/transactions/admin/all",
        "loans": "/loans/admin/all"
    }

    data = {}

    for key, endpoint in endpoints.items():
        response = api_request(
            "GET",
            endpoint,
            headers=get_headers()
        )

        if response is None:
            data[key] = []
        else:
            result = handle_response(response)
            data[key] = result if isinstance(result, list) else []

    customers = data["customers"]
    accounts = data["accounts"]
    txs = data["transactions"]
    loans = data["loans"]

    total_balance = sum(
        float(account.get("balance", 0) or 0)
        for account in accounts
    )

    total_loan_amount = sum(
        float(loan.get("amount", 0) or 0)
        for loan in loans
    )

    total_remaining = sum(
        float(loan.get("remainingBalance", 0) or 0)
        for loan in loans
    )

    summary = Table(
        title="Database Summary",
        header_style="bold cyan",
        box=None
    )

    summary.add_column("Collection", style="bold")
    summary.add_column("Records", justify="right", style="success")
    summary.add_column("Details")

    summary.add_row(
        "Customers",
        str(len(customers)),
        f"Admins: {sum(1 for c in customers if c.get('role') == 'ADMIN')}"
    )

    summary.add_row(
        "Accounts",
        str(len(accounts)),
        f"Total Balance: ${total_balance:,.2f}"
    )

    summary.add_row(
        "Transactions",
        str(len(txs)),
        f"Deposits: {sum(1 for t in txs if t.get('type') == 'DEPOSIT')}"
    )

    summary.add_row(
        "Loans",
        str(len(loans)),
        f"Total: ${total_loan_amount:,.2f} | "
        f"Remaining: ${total_remaining:,.2f}"
    )

    console.print(summary)

    if customers:
        cust_table = Table(
            title="Customers Collection",
            header_style="bold blue"
        )

        cust_table.add_column("ID", style="dim")
        cust_table.add_column("First Name")
        cust_table.add_column("Last Name")
        cust_table.add_column("Email")
        cust_table.add_column("Phone")
        cust_table.add_column("Role")
        cust_table.add_column("Joined")

        for customer in customers:
            reg_at = customer.get("registeredAt", "N/A")

            if reg_at and reg_at != "N/A":
                try:
                    reg_at = datetime.fromisoformat(
                        str(reg_at).replace("Z", "+00:00")
                    ).strftime("%Y-%m-%d")
                except (ValueError, TypeError):
                    pass

            customer_id = str(
                customer.get("customerId", "N/A")
            )

            cust_table.add_row(
                customer_id[:12],
                str(customer.get("firstName", "N/A")),
                str(customer.get("lastName", "N/A")),
                str(customer.get("email", "N/A")),
                str(customer.get("phone", "N/A")),
                str(customer.get("role", "N/A")),
                str(reg_at)
            )

        console.print(cust_table)

    if accounts:
        acc_table = Table(
            title="Accounts Collection",
            header_style="bold green"
        )

        acc_table.add_column("Account #", style="acc_num")
        acc_table.add_column("Customer")
        acc_table.add_column("Type")
        acc_table.add_column("Balance", justify="right")
        acc_table.add_column("Status")

        for account in accounts:
            customer_id = str(
                account.get("customerId", "N/A")
            )

            acc_table.add_row(
                str(account.get("accountId", "N/A")),
                customer_id[:12],
                str(account.get("type", "N/A")),
                f"${float(account.get('balance', 0) or 0):,.2f}",
                str(account.get("status", "N/A"))
            )

        console.print(acc_table)

    if loans:
        loan_table = Table(
            title="Loans Collection",
            header_style="bold magenta"
        )

        loan_table.add_column("Loan ID", style="info")
        loan_table.add_column("Customer")
        loan_table.add_column("Amount", justify="right")
        loan_table.add_column("Remaining", justify="right")
        loan_table.add_column("Rate", justify="right")
        loan_table.add_column("Status")
        loan_table.add_column("Auto Debt")

        for loan in loans:
            status = loan.get("status", "N/A")

            if status == "PAID":
                status_color = "green"
            elif status == "PENDING":
                status_color = "yellow"
            elif status == "APPROVED":
                status_color = "blue"
            else:
                status_color = "red"

            loan_table.add_row(
                str(loan.get("loanId", "N/A")),
                str(loan.get("customerName", "N/A")),
                f"${float(loan.get('amount', 0) or 0):,.2f}",
                f"${float(loan.get('remainingBalance', 0) or 0):,.2f}",
                f"{loan.get('interestRate', 0)}%",
                f"[{status_color}]{status}[/{status_color}]",
                "YES" if loan.get("autoDebtEnabled") else "NO"
            )

        console.print(loan_table)

    if txs:
        tx_table = Table(
            title="Transactions Collection",
            header_style="bold red"
        )

        tx_table.add_column("ID", style="dim")
        tx_table.add_column("Date", style="info")
        tx_table.add_column("Type")
        tx_table.add_column("Amount", justify="right")
        tx_table.add_column("From", style="acc_num")
        tx_table.add_column("To", style="acc_num")
        tx_table.add_column("Description")

        for tx in txs[:50]:
            timestamp = tx.get("timestamp", "N/A")

            if timestamp and timestamp != "N/A":
                try:
                    timestamp = datetime.fromisoformat(
                        str(timestamp).replace("Z", "+00:00")
                    ).strftime("%Y-%m-%d %H:%M")
                except (ValueError, TypeError):
                    pass

            tx_type = tx.get("type", "N/A")

            amount_style = (
                "success"
                if tx_type in ("DEPOSIT", "TRANSFER_IN")
                else "error"
            )

            tx_id = str(
                tx.get("id", tx.get("transactionId", "N/A"))
            )

            tx_table.add_row(
                tx_id[:10],
                str(timestamp),
                str(tx_type),
                f"[{amount_style}]"
                f"${float(tx.get('amount', 0) or 0):,.2f}"
                f"[/{amount_style}]",
                str(tx.get("fromAccount") or "-"),
                str(tx.get("toAccount") or "-"),
                str(tx.get("description") or "")
            )

        console.print(tx_table)


def interactive_menu():
    while True:
        token, user = get_session()

        console.clear()
        show_banner()

        if not token:
            console.print("1. Login")
            console.print("2. Register")
            console.print("q. Quit")

            choice = Prompt.ask(
                "Choose an option",
                choices=["1", "2", "q"]
            )

            if choice == "1":
                email = Prompt.ask("Email")
                password = Prompt.ask(
                    "Password",
                    password=True
                )

                login(email, password)
                input("\nPress Enter to continue...")

            elif choice == "2":
                first_name = Prompt.ask("First Name")
                last_name = Prompt.ask("Last Name")
                email = Prompt.ask("Email")
                phone = Prompt.ask("Phone Number")
                password = Prompt.ask(
                    "Password",
                    password=True
                )

                register(
                    first_name,
                    last_name,
                    email,
                    phone,
                    password
                )

                input("\nPress Enter to continue...")

            else:
                break

        else:
            is_admin = user.get("role") == "ADMIN"

            console.print(
                f"Logged in as: [accent]"
                f"{user.get('firstName', 'User')}"
                f"[/accent] "
                f"([info]{user.get('role', 'CUSTOMER')}[/info])"
            )

            console.print("\n[bold]User Menu:[/bold]")
            console.print("1. View Dashboard")
            console.print("2. Open New Account")
            console.print("3. Deposit Funds")
            console.print("4. Withdraw Funds")
            console.print("5. Transfer Funds")
            console.print("6. Toggle Account Visibility")
            console.print("7. View My Loans")
            console.print("8. Request Loan")
            console.print("9. Repay Loan")
            console.print("h. Toggle Show Hidden Accounts")

            if is_admin:
                console.print("\n[bold magenta]Admin Menu:[/bold magenta]")
                console.print("a. View All Users")
                console.print("b. View All Accounts")
                console.print("t. View Transaction Record (All)")
                console.print("db. Database Explorer (All Collections)")
                console.print("da. Delete Account")
                console.print("dc. Delete Customer")
                console.print("cp. Change User Password")
                console.print("la. Manage Loans (All)")
                console.print("lap. Approve Loan")
                console.print("lrj. Reject Loan")
                console.print("lad. Toggle Auto-Debt")
                console.print("lpr. Process Auto-Debts (Global)")

            console.print("\nl. Logout")
            console.print("q. Quit")

            choices = [
                "1", "2", "3", "4", "5", "6",
                "7", "8", "9", "h", "l", "q"
            ]

            if is_admin:
                choices += [
                    "a", "b", "t", "db", "da", "dc",
                    "cp", "la", "lap", "lrj", "lad", "lpr"
                ]

            choice = Prompt.ask(
                "Choose an option",
                choices=choices
            )

            if choice == "1":
                show_dashboard()

            elif choice == "2":
                acc_type = Prompt.ask(
                    "Account Type",
                    choices=["CHECKING", "SAVINGS"]
                )

                initial = FloatPrompt.ask(
                    "Initial Deposit",
                    default=0.0
                )

                open_account(acc_type, initial)

            elif choice == "3":
                acc_id = Prompt.ask("Account Number")
                amount = FloatPrompt.ask("Amount")
                desc = Prompt.ask(
                    "Description",
                    default="Deposit"
                )

                deposit(acc_id, amount, desc)

            elif choice == "4":
                acc_id = Prompt.ask("Account Number")
                amount = FloatPrompt.ask("Amount")
                desc = Prompt.ask(
                    "Description",
                    default="Withdrawal"
                )

                withdraw(acc_id, amount, desc)

            elif choice == "5":
                from_id = Prompt.ask("From Account Number")
                to_id = Prompt.ask("To Account Number")
                amount = FloatPrompt.ask("Amount")
                desc = Prompt.ask(
                    "Description",
                    default="Transfer"
                )

                transfer(
                    from_id,
                    to_id,
                    amount,
                    desc
                )

            elif choice == "6":
                acc_id = Prompt.ask("Account Number")
                toggle_visibility(acc_id)

            elif choice == "7":
                show_my_loans()

            elif choice == "8":
                amount = FloatPrompt.ask("Loan Amount")
                rate = FloatPrompt.ask(
                    "Interest Rate (%)",
                    default=5.0
                )

                request_loan(amount, rate)

            elif choice == "9":
                loan_id = Prompt.ask("Loan ID")
                from_acc = Prompt.ask("From Account Number")
                amount = FloatPrompt.ask("Repayment Amount")

                repay_loan(
                    loan_id,
                    from_acc,
                    amount
                )

            elif choice == "h":
                user["showHidden"] = not user.get(
                    "showHidden",
                    False
                )

                save_token(token, user)

                console.print(
                    "[info]Show hidden accounts: "
                    f"{'Enabled' if user['showHidden'] else 'Disabled'}"
                    "[/info]"
                )

            elif choice == "a":
                show_admin_users()

            elif choice == "b":
                show_admin_accounts()

            elif choice == "t":
                show_admin_transactions()

            elif choice == "db":
                show_database_view()

            elif choice == "da":
                acc_id = Prompt.ask(
                    "Account Number to Delete"
                )

                delete_account_admin(acc_id)

            elif choice == "dc":
                cust_id = Prompt.ask(
                    "Customer ID to Delete"
                )

                delete_customer_admin(cust_id)

            elif choice == "cp":
                cust_id = Prompt.ask(
                    "Customer ID to change password"
                )

                admin_change_password(cust_id)

            elif choice == "la":
                show_admin_loans()

            elif choice == "lap":
                loan_id = Prompt.ask(
                    "Loan ID to Approve"
                )

                target_acc = Prompt.ask(
                    "Target Account Number for Funds"
                )

                approve_loan_admin(
                    loan_id,
                    target_acc
                )

            elif choice == "lrj":
                loan_id = Prompt.ask(
                    "Loan ID to Reject"
                )

                reject_loan_admin(loan_id)

            elif choice == "lad":
                loan_id = Prompt.ask("Loan ID")

                enabled = Prompt.ask(
                    "Enable Auto-Debt?",
                    choices=["y", "n"]
                ) == "y"

                toggle_auto_debt_admin(
                    loan_id,
                    enabled
                )

            elif choice == "lpr":
                process_auto_debts_admin()

            elif choice == "l":
                clear_session()
                console.print("[info]Logged out.[/info]")

            elif choice == "q":
                break

            input("\nPress Enter to continue...")


def main():
    parser = argparse.ArgumentParser(
        description="MooCash CLI"
    )

    subparsers = parser.add_subparsers(
        dest="command"
    )

    auth_parser = subparsers.add_parser("auth")
    auth_sub = auth_parser.add_subparsers(
        dest="subcommand"
    )

    login_p = auth_sub.add_parser("login")
    login_p.add_argument("email")
    login_p.add_argument("password")

    reg_p = auth_sub.add_parser("register")
    reg_p.add_argument("first_name")
    reg_p.add_argument("last_name")
    reg_p.add_argument("email")
    reg_p.add_argument("phone")
    reg_p.add_argument("password")

    acc_parser = subparsers.add_parser("accounts")
    acc_sub = acc_parser.add_subparsers(
        dest="subcommand"
    )

    acc_sub.add_parser("list")

    open_p = acc_sub.add_parser("open")
    open_p.add_argument(
        "type",
        choices=["CHECKING", "SAVINGS"]
    )
    open_p.add_argument(
        "--initial",
        type=float,
        default=0.0
    )

    tv_p = acc_sub.add_parser("toggle-visibility")
    tv_p.add_argument("id")

    tx_parser = subparsers.add_parser("tx")
    tx_sub = tx_parser.add_subparsers(
        dest="subcommand"
    )

    dep_p = tx_sub.add_parser("deposit")
    dep_p.add_argument("account")
    dep_p.add_argument("amount", type=float)
    dep_p.add_argument(
        "--desc",
        default="Deposit"
    )

    wd_p = tx_sub.add_parser("withdraw")
    wd_p.add_argument("account")
    wd_p.add_argument("amount", type=float)
    wd_p.add_argument(
        "--desc",
        default="Withdrawal"
    )

    tr_p = tx_sub.add_parser("transfer")
    tr_p.add_argument("from_acc")
    tr_p.add_argument("to_acc")
    tr_p.add_argument("amount", type=float)
    tr_p.add_argument(
        "--desc",
        default="Transfer"
    )

    admin_parser = subparsers.add_parser("admin")
    admin_sub = admin_parser.add_subparsers(
        dest="subcommand"
    )

    admin_sub.add_parser("users")
    admin_sub.add_parser("accounts")
    admin_sub.add_parser("txs")
    admin_sub.add_parser("db")

    del_acc = admin_sub.add_parser(
        "delete-account"
    )
    del_acc.add_argument("id")

    del_cust = admin_sub.add_parser(
        "delete-customer"
    )
    del_cust.add_argument("id")

    admin_cp = admin_sub.add_parser("cp")
    admin_cp.add_argument("id")
    admin_cp.add_argument("password")

    al_parser = admin_sub.add_parser("loans")
    al_sub = al_parser.add_subparsers(
        dest="al_subcommand"
    )

    al_sub.add_parser("all")

    al_app = al_sub.add_parser("approve")
    al_app.add_argument("id")
    al_app.add_argument("target")

    al_rej = al_sub.add_parser("reject")
    al_rej.add_argument("id")

    al_ad = al_sub.add_parser("auto-debt")
    al_ad.add_argument("id")
    al_ad.add_argument(
        "enabled",
        choices=["true", "false"]
    )

    admin_sub.add_parser(
        "process-auto-debts"
    )

    loan_parser = subparsers.add_parser("loans")
    loan_sub = loan_parser.add_subparsers(
        dest="subcommand"
    )

    loan_sub.add_parser("my")

    l_req = loan_sub.add_parser("request")
    l_req.add_argument(
        "amount",
        type=float
    )
    l_req.add_argument(
        "--rate",
        type=float,
        default=5.0
    )

    l_rep = loan_sub.add_parser("repay")
    l_rep.add_argument("loan_id")
    l_rep.add_argument("account")
    l_rep.add_argument(
        "amount",
        type=float
    )

    args = parser.parse_args()

    if not args.command:
        interactive_menu()
        return

    if args.command == "auth":
        if args.subcommand == "login":
            login(
                args.email,
                args.password
            )

        elif args.subcommand == "register":
            register(
                args.first_name,
                args.last_name,
                args.email,
                args.phone,
                args.password
            )

        return

    if args.command == "accounts":
        if args.subcommand == "list":
            show_dashboard()

        elif args.subcommand == "open":
            open_account(
                args.type,
                args.initial
            )

        elif args.subcommand == "toggle-visibility":
            toggle_visibility(args.id)

        return

    if args.command == "tx":
        if args.subcommand == "deposit":
            deposit(
                args.account,
                args.amount,
                args.desc
            )

        elif args.subcommand == "withdraw":
            withdraw(
                args.account,
                args.amount,
                args.desc
            )

        elif args.subcommand == "transfer":
            transfer(
                args.from_acc,
                args.to_acc,
                args.amount,
                args.desc
            )

        return

    if args.command == "admin":
        if args.subcommand == "users":
            show_admin_users()

        elif args.subcommand == "accounts":
            show_admin_accounts()

        elif args.subcommand == "txs":
            show_admin_transactions()

        elif args.subcommand == "db":
            show_database_view()

        elif args.subcommand == "delete-account":
            delete_account_admin(args.id)

        elif args.subcommand == "delete-customer":
            delete_customer_admin(args.id)

        elif args.subcommand == "cp":
            admin_change_password(
                args.id,
                args.password
            )

        elif args.subcommand == "loans":
            if args.al_subcommand == "all":
                show_admin_loans()

            elif args.al_subcommand == "approve":
                approve_loan_admin(
                    args.id,
                    args.target
                )

            elif args.al_subcommand == "reject":
                reject_loan_admin(args.id)

            elif args.al_subcommand == "auto-debt":
                toggle_auto_debt_admin(
                    args.id,
                    args.enabled == "true"
                )

        elif args.subcommand == "process-auto-debts":
            process_auto_debts_admin()

        return

    if args.command == "loans":
        if args.subcommand == "my":
            show_my_loans()

        elif args.subcommand == "request":
            request_loan(
                args.amount,
                args.rate
            )

        elif args.subcommand == "repay":
            repay_loan(
                args.loan_id,
                args.account,
                args.amount
            )


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        console.print(
            "\n[warning]Exiting...[/warning]"
        )
        sys.exit(0)