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
from rich.prompt import Prompt, IntPrompt, FloatPrompt
from datetime import datetime

# --- Configuration ---
API_BASE_URL = "http://localhost:8080/api"
TOKEN_FILE = os.path.expanduser("~/.orios_token")
USER_DATA_FILE = os.path.expanduser("~/.orios_user")

# --- Limits ---
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

# --- Banner ---

BANNER = """┏━┃┏━┃┛┏━┃┏━┛  ┏━ ┏━┃┏━ ┃ ┃
┃ ┃┏┏┛┃┃ ┃━━┃  ┏━┃┏━┃┃ ┃┏┛
━━┛┛ ┛┛━━┛━━┛  ━━ ┛ ┛┛ ┛┛ ┛"""

TAGLINE = "Premium Digital Banking"

def show_banner():
    """Show centered ASCII banner with gradient colors."""
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

# --- Helper Functions ---

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
    if os.path.exists(TOKEN_FILE):
        os.remove(TOKEN_FILE)
    if os.path.exists(USER_DATA_FILE):
        os.remove(USER_DATA_FILE)

def get_headers():
    token = load_token()
    if token:
        return {"Authorization": f"Bearer {token}"}
    return {}

def handle_response(response, success_msg=None):
    if response.status_code in [200, 201]:
        if success_msg:
            console.print(f"[success]✔ {success_msg}[/success]")
        try:
            return response.json()
        except:
            return None
    else:
        try:
            error_data = response.json()
            msg = error_data.get("message", "Unknown error")
            console.print(f"[error]✘ Error: {msg}[/error]")
        except:
            console.print(f"[error]✘ Error: HTTP {response.status_code}[/error]")
        return None

# --- API Actions ---

def login(email, password):
    payload = {"email": email, "password": password}
    response = requests.post(f"{API_BASE_URL}/auth/login", json=payload)
    data = handle_response(response, "Login successful!")
    if data:
        save_token(data['token'], data)
        show_dashboard()

def register(name, email, phone, password):
    payload = {
        "fullName": name,
        "email": email,
        "phone": phone,
        "password": password
    }
    response = requests.post(f"{API_BASE_URL}/auth/register", json=payload)
    handle_response(response, "Registration successful! You can now login.")

def show_dashboard():
    _, user = get_session()
    if not user:
        console.print("[warning]Please login first.[/warning]")
        return

    console.print(Panel(
        f"Welcome back, [accent]{user.get('fullName', user.get('email'))}[/accent]!\n"
        f"Role: [info]{user.get('role')}[/info]",
        title="[header]Orios Bank Dashboard[/header]",
        border_style="blue"
    ))

    # Fetch Accounts
    response = requests.get(f"{API_BASE_URL}/accounts/my-accounts", headers=get_headers())
    accounts = handle_response(response)
    
    if accounts:
        table = Table(title="Your Accounts", box=None, header_style="bold blue")
        table.add_column("Account Number", style="acc_num")
        table.add_column("Type", style="info")
        table.add_column("Balance", justify="right", style="success")
        table.add_column("Status")
        table.add_column("Visibility")

        total_balance = 0
        for acc in accounts:
            if acc.get('isHidden') and not user.get('showHidden'):
                total_balance += acc['balance'] # Still count towards net worth
                continue
                
            table.add_row(
                acc['accountId'],
                acc['type'],
                f"${acc['balance']:,.2f}",
                f"[bold green]{acc['status']}[/bold green]" if acc['status'] == 'ACTIVE' else f"[bold red]{acc['status']}[/bold red]",
                "Hidden" if acc.get('isHidden') else "Visible"
            )
            total_balance += acc['balance']
        
        console.print(table)
        console.print(f"\n[bold]Total Net Worth: [success]${total_balance:,.2f}[/success][/bold]\n")
    else:
        console.print("[info]No accounts found. Open your first account today![/info]")

def open_account(acc_type, initial_deposit=0.0):
    params = {"type": acc_type.upper(), "initialDeposit": initial_deposit}
    response = requests.post(f"{API_BASE_URL}/accounts/open", params=params, headers=get_headers())
    handle_response(response, f"{acc_type.capitalize()} account opened successfully!")

def deposit(acc_id, amount, desc):
    if amount <= 0:
        console.print("[error]✘ Deposit amount must be positive.[/error]")
        return
    if amount > MAX_DEPOSIT:
        console.print(f"[error]✘ Deposit limit exceeded. Maximum per transaction: ${MAX_DEPOSIT:,.2f}[/error]")
        return
    payload = {"accountId": acc_id, "amount": amount, "description": desc}
    response = requests.post(f"{API_BASE_URL}/accounts/deposit", json=payload, headers=get_headers())
    handle_response(response, f"Deposited ${amount:,.2f} to {acc_id}")

def withdraw(acc_id, amount, desc=None):
    if amount <= 0:
        console.print("[error]✘ Withdrawal amount must be positive.[/error]")
        return
    if amount > MAX_WITHDRAWAL:
        console.print(f"[error]✘ Withdrawal limit exceeded. Maximum per transaction: ${MAX_WITHDRAWAL:,.2f}[/error]")
        return
    payload = {"accountId": acc_id, "amount": amount, "description": desc}
    response = requests.post(f"{API_BASE_URL}/accounts/withdraw", json=payload, headers=get_headers())
    handle_response(response, f"Withdrew ${amount:,.2f} from {acc_id}")

def transfer(from_id, to_id, amount, desc):
    if amount <= 0:
        console.print("[error]✘ Transfer amount must be positive.[/error]")
        return
    if amount > MAX_TRANSFER:
        console.print(f"[error]✘ Transfer limit exceeded. Maximum per transaction: ${MAX_TRANSFER:,.2f}[/error]")
        return
    if from_id == to_id:
        console.print("[error]✘ Cannot transfer to the same account.[/error]")
        return
    payload = {
        "fromAccountId": from_id,
        "toAccountId": to_id,
        "amount": amount,
        "description": desc
    }
    response = requests.post(f"{API_BASE_URL}/accounts/transfer", json=payload, headers=get_headers())
    handle_response(response, f"Transferred ${amount:,.2f} from {from_id} to {to_id}")

def toggle_visibility(acc_id):
    response = requests.post(f"{API_BASE_URL}/accounts/{acc_id}/toggle-visibility", headers=get_headers())
    handle_response(response, f"Visibility toggled for account {acc_id}")

# --- Loan Actions ---

def request_loan(amount, interest_rate):
    if amount <= 0:
        console.print("[error]✘ Loan amount must be greater than zero.[/error]")
        return
    if amount > MAX_LOAN_AMOUNT:
        console.print(f"[error]✘ Loan amount exceeded. Maximum loan: ${MAX_LOAN_AMOUNT:,.2f}[/error]")
        return
    if interest_rate < 0:
        console.print("[error]✘ Interest rate cannot be negative.[/error]")
        return
    payload = {"amount": amount, "interestRate": interest_rate}
    response = requests.post(f"{API_BASE_URL}/loans/request", json=payload, headers=get_headers())
    handle_response(response, "Loan request submitted successfully!")

def repay_loan(loan_id, from_acc, amount):
    if amount <= 0:
        console.print("[error]✘ Amount must be greater than zero.[/error]")
        return
    params = {"loanId": loan_id, "fromAccountId": from_acc, "amount": amount}
    response = requests.post(f"{API_BASE_URL}/loans/repay", params=params, headers=get_headers())
    handle_response(response, f"Repayment of ${amount} for loan {loan_id} processed.")

def show_my_loans():
    response = requests.get(f"{API_BASE_URL}/loans/my-loans", headers=get_headers())
    loans = handle_response(response)
    if loans:
        table = Table(title="Your Loans", box=None, header_style="bold magenta")
        table.add_column("Loan ID", style="info")
        table.add_column("Principal", justify="right")
        table.add_column("Remaining", justify="right", style="error")
        table.add_column("Rate", justify="right")
        table.add_column("Status")
        table.add_column("Auto Debt")

        for l in loans:
            status_color = "green" if l['status'] == 'PAID' else "yellow" if l['status'] == 'PENDING' else "blue"
            table.add_row(
                l['loanId'],
                f"${l['amount']:,.2f}",
                f"${l['remainingBalance']:,.2f}",
                f"{l['interestRate']}%",
                f"[{status_color}]{l['status']}[/{status_color}]",
                "Enabled" if l['autoDebtEnabled'] else "Disabled"
            )
        console.print(table)
    else:
        console.print("[info]No loans found.[/info]")

# --- Admin Loan Actions ---

def show_admin_loans():
    response = requests.get(f"{API_BASE_URL}/loans/admin/all", headers=get_headers())
    loans = handle_response(response)
    if loans:
        table = Table(title="System Loans", header_style="bold magenta")
        table.add_column("Loan ID", style="info")
        table.add_column("Customer")
        table.add_column("Amount", justify="right")
        table.add_column("Remaining", justify="right")
        table.add_column("Status")
        table.add_column("Auto Debt")

        for l in loans:
            table.add_row(
                l['loanId'],
                l['customerName'],
                f"${l['amount']:,.2f}",
                f"${l['remainingBalance']:,.2f}",
                l['status'],
                "YES" if l['autoDebtEnabled'] else "NO"
            )
        console.print(table)
    else:
        console.print("[info]No loans in the system.[/info]")

def approve_loan_admin(loan_id, target_acc):
    params = {"targetAccountId": target_acc}
    response = requests.post(f"{API_BASE_URL}/loans/admin/{loan_id}/approve", params=params, headers=get_headers())
    handle_response(response, f"Loan {loan_id} approved. Funds sent to {target_acc}.")

def reject_loan_admin(loan_id):
    response = requests.post(f"{API_BASE_URL}/loans/admin/{loan_id}/reject", headers=get_headers())
    handle_response(response, f"Loan {loan_id} rejected.")

def toggle_auto_debt_admin(loan_id, enabled):
    params = {"enabled": str(enabled).lower()}
    response = requests.post(f"{API_BASE_URL}/loans/admin/{loan_id}/auto-debt", params=params, headers=get_headers())
    handle_response(response, f"Auto-debt {'enabled' if enabled else 'disabled'} for loan {loan_id}.")

def process_auto_debts_admin():
    response = requests.post(f"{API_BASE_URL}/loans/admin/process-auto-debts", headers=get_headers())
    handle_response(response, "Auto-debt processing triggered for all eligible loans.")

def show_admin_users():
    response = requests.get(f"{API_BASE_URL}/accounts/admin/all-customers", headers=get_headers())
    customers = handle_response(response)
    if customers:
        table = Table(title="All Customers", header_style="bold magenta")
        table.add_column("Customer ID", style="dim")
        table.add_column("Name")
        table.add_column("Email")
        table.add_column("Role")
        table.add_column("Joined")
        for c in customers:
            reg_at = c.get('registeredAt')
            if reg_at:
                try:
                    date_str = datetime.fromisoformat(reg_at.replace('Z', '+00:00')).strftime('%Y-%m-%d %H:%M')
                except:
                    date_str = reg_at
            else:
                date_str = 'N/A'
            table.add_row(c.get('customerId', 'N/A'), c['fullName'], c['email'], c['role'], date_str)
        console.print(table)

def show_admin_transactions():
    response = requests.get(f"{API_BASE_URL}/transactions/admin/all", headers=get_headers())
    txs = handle_response(response)
    if txs:
        table = Table(title="Global Transaction Record", header_style="bold red")
        table.add_column("ID", style="dim")
        table.add_column("Date", style="info")
        table.add_column("From", style="acc_num")
        table.add_column("To", style="acc_num")
        table.add_column("Type")
        table.add_column("Amount", justify="right")
        table.add_column("Description")
        
        for t in txs:
            ts = t.get('timestamp', 'N/A')
            if ts and ts != 'N/A':
                try:
                    ts = datetime.fromisoformat(ts.replace('Z', '+00:00')).strftime('%b %d, %H:%M')
                except: pass
            
            amount_style = "success" if t['type'] in ['DEPOSIT', 'TRANSFER_IN'] else "error"
            table.add_row(
                t.get('id', 'N/A')[:8],
                ts,
                t.get('fromAccount', '-'),
                t.get('toAccount', '-'),
                t['type'],
                f"[{amount_style}]${t['amount']:,.2f}[/{amount_style}]",
                t.get('description', '')
            )
        console.print(table)
    else:
        console.print("[info]No transactions found in the system.[/info]")

def delete_account_admin(acc_id):
    if not Prompt.ask(f"Are you sure you want to [bold red]DELETE[/bold red] account {acc_id}?", choices=["y", "n"], default="n") == "y":
        return
    response = requests.delete(f"{API_BASE_URL}/accounts/admin/accounts/{acc_id}", headers=get_headers())
    handle_response(response, f"Account {acc_id} deleted successfully.")

def delete_customer_admin(cust_id):
    if not Prompt.ask(f"Are you sure you want to [bold red]DELETE[/bold red] customer {cust_id} and ALL their data?", choices=["y", "n"], default="n") == "y":
        return
    response = requests.delete(f"{API_BASE_URL}/accounts/admin/customers/{cust_id}", headers=get_headers())
    handle_response(response, f"Customer {cust_id} and all associated data deleted.")

def admin_change_password(cust_id, new_password=None):
    if not new_password:
        new_password = Prompt.ask(f"Enter new password for customer {cust_id}", password=True)
    
    if len(new_password) < 6:
        console.print("[error]✘ Password must be at least 6 characters.[/error]")
        return

    payload = {"newPassword": new_password}
    response = requests.post(f"{API_BASE_URL}/auth/admin/change-password/{cust_id}", json=payload, headers=get_headers())
    handle_response(response, f"Password for customer {cust_id} updated successfully.")

def show_admin_accounts():
    response = requests.get(f"{API_BASE_URL}/accounts/admin/all-accounts", headers=get_headers())
    accounts = handle_response(response)
    if accounts:
        table = Table(title="All System Accounts", header_style="bold magenta")
        table.add_column("Account #", style="acc_num")
        table.add_column("Customer ID")
        table.add_column("Balance", justify="right")
        table.add_column("Status")
        for acc in accounts:
            table.add_row(acc['accountId'], acc['customerId'], f"${acc['balance']:,.2f}", acc['status'])
        console.print(table)

def show_database_view():
    """Admin database view showing all collections"""
    console.print(Panel.fit(
        "[header]Database Explorer[/header]\n[italic]View all system data[/italic]",
        border_style="blue"
    ))

    # Fetch all data
    customers_resp = requests.get(f"{API_BASE_URL}/accounts/admin/all-customers", headers=get_headers())
    accounts_resp = requests.get(f"{API_BASE_URL}/accounts/admin/all-accounts", headers=get_headers())
    txs_resp = requests.get(f"{API_BASE_URL}/transactions/admin/all", headers=get_headers())
    loans_resp = requests.get(f"{API_BASE_URL}/loans/admin/all", headers=get_headers())

    customers = handle_response(customers_resp) or []
    accounts = handle_response(accounts_resp) or []
    txs = handle_response(txs_resp) or []
    loans = handle_response(loans_resp) or []

    # Summary
    total_balance = sum(a.get('balance', 0) for a in accounts)
    total_loan_amount = sum(l.get('amount', 0) for l in loans)
    total_remaining = sum(l.get('remainingBalance', 0) for l in loans)

    summary = Table(title="Database Summary", header_style="bold cyan", box=None)
    summary.add_column("Collection", style="bold")
    summary.add_column("Records", justify="right", style="success")
    summary.add_column("Details")
    summary.add_row("Customers", str(len(customers)), f"Admins: {sum(1 for c in customers if c.get('role') == 'ADMIN')}")
    summary.add_row("Accounts", str(len(accounts)), f"Total Balance: ${total_balance:,.2f}")
    summary.add_row("Transactions", str(len(txs)), f"Deposits: {sum(1 for t in txs if t.get('type') == 'DEPOSIT')}")
    summary.add_row("Loans", str(loans.__len__()), f"Pending: {sum(1 for l in loans if l.get('status') == 'PENDING')}, Active: {sum(1 for l in loans if l.get('status') == 'APPROVED')}")
    console.print(summary)

    # Customers table
    if customers:
        cust_table = Table(title="Customers Collection", header_style="bold blue")
        cust_table.add_column("ID", style="dim")
        cust_table.add_column("Name")
        cust_table.add_column("Email")
        cust_table.add_column("Phone")
        cust_table.add_column("Role")
        cust_table.add_column("Joined")
        for c in customers:
            reg_at = c.get('registeredAt', 'N/A')
            if reg_at and reg_at != 'N/A':
                try:
                    reg_at = datetime.fromisoformat(reg_at.replace('Z', '+00:00')).strftime('%Y-%m-%d')
                except: pass
            cust_table.add_row(
                c.get('customerId', 'N/A')[:12],
                c.get('fullName', 'N/A'),
                c.get('email', 'N/A'),
                c.get('phone', 'N/A'),
                c.get('role', 'N/A'),
                reg_at
            )
        console.print(cust_table)

    # Accounts table
    if accounts:
        acc_table = Table(title="Accounts Collection", header_style="bold green")
        acc_table.add_column("Account #", style="acc_num")
        acc_table.add_column("Customer")
        acc_table.add_column("Type")
        acc_table.add_column("Balance", justify="right")
        acc_table.add_column("Status")
        acc_table.add_column("Created")
        for acc in accounts:
            acc_table.add_row(
                acc.get('accountId', 'N/A'),
                acc.get('customerId', 'N/A')[:12],
                acc.get('type', 'N/A'),
                f"${acc.get('balance', 0):,.2f}",
                acc.get('status', 'N/A'),
                'N/A'
            )
        console.print(acc_table)

    # Loans table
    if loans:
        loan_table = Table(title="Loans Collection", header_style="bold magenta")
        loan_table.add_column("Loan ID", style="info")
        loan_table.add_column("Customer")
        loan_table.add_column("Amount", justify="right")
        loan_table.add_column("Remaining", justify="right")
        loan_table.add_column("Rate", justify="right")
        loan_table.add_column("Status")
        loan_table.add_column("Auto Debt")
        for l in loans:
            status_color = "green" if l.get('status') == 'PAID' else "yellow" if l.get('status') == 'PENDING' else "blue" if l.get('status') == 'APPROVED' else "red"
            loan_table.add_row(
                l.get('loanId', 'N/A'),
                l.get('customerName', 'N/A'),
                f"${l.get('amount', 0):,.2f}",
                f"${l.get('remainingBalance', 0):,.2f}",
                f"{l.get('interestRate', 0)}%",
                f"[{status_color}]{l.get('status', 'N/A')}[/{status_color}]",
                "YES" if l.get('autoDebtEnabled') else "NO"
            )
        console.print(loan_table)

    # Transactions table
    if txs:
        tx_table = Table(title="Transactions Collection", header_style="bold red")
        tx_table.add_column("ID", style="dim")
        tx_table.add_column("Date", style="info")
        tx_table.add_column("Type")
        tx_table.add_column("Amount", justify="right")
        tx_table.add_column("From", style="acc_num")
        tx_table.add_column("To", style="acc_num")
        tx_table.add_column("Description")
        for t in txs[:50]:  # Limit to last 50 transactions
            ts = t.get('timestamp', 'N/A')
            if ts and ts != 'N/A':
                try:
                    ts = datetime.fromisoformat(ts.replace('Z', '+00:00')).strftime('%Y-%m-%d %H:%M')
                except: pass
            amount_style = "success" if t.get('type') in ['DEPOSIT', 'TRANSFER_IN'] else "error"
            tx_table.add_row(
                t.get('id', t.get('transactionId', 'N/A'))[:10],
                ts,
                t.get('type', 'N/A'),
                f"[{amount_style}]${t.get('amount', 0):,.2f}[/{amount_style}]",
                t.get('fromAccount', '-') or '-',
                t.get('toAccount', '-') or '-',
                t.get('description', '') or ''
            )
        console.print(tx_table)

# --- Interactive Mode ---

def get_session():
    token = load_token()
    user = load_user_data()
    
    if token and not user:
        # Auto-login logic: fetch user data if token exists but user data is missing
        response = requests.get(f"{API_BASE_URL}/auth/me", headers={"Authorization": f"Bearer {token}"})
        if response.status_code == 200:
            user = response.json()
            save_token(token, user)
        else:
            clear_session()
            token = None
            user = None
    return token, user

def interactive_menu():
    while True:
        token, user = get_session()
        
        console.clear()
        show_banner()
        
        if not token:
            console.print("1. Login")
            console.print("2. Register")
            console.print("q. Quit")
            choice = Prompt.ask("Choose an option", choices=["1", "2", "q"])
            
            if choice == "1":
                email = Prompt.ask("Email")
                password = Prompt.ask("Password", password=True)
                login(email, password)
                input("\nPress Enter to continue...")
            elif choice == "2":
                name = Prompt.ask("Full Name")
                email = Prompt.ask("Email")
                phone = Prompt.ask("Phone Number")
                password = Prompt.ask("Password", password=True)
                register(name, email, phone, password)
                input("\nPress Enter to continue...")
            else:
                break
        else:
            is_admin = user.get('role') == 'ADMIN'
            console.print(f"Logged in as: [accent]{user['fullName']}[/accent] ([info]{user['role']}[/info])")
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

            choices = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "h", "l", "q"]
            if is_admin: choices += ["a", "b", "t", "db", "da", "dc", "cp", "la", "lap", "lrj", "lad", "lpr"]
            
            choice = Prompt.ask("Choose an option", choices=choices)
            
            if choice == "1":
                show_dashboard()
            elif choice == "2":
                acc_type = Prompt.ask("Account Type", choices=["CHECKING", "SAVINGS"])
                initial = FloatPrompt.ask("Initial Deposit", default=0.0)
                open_account(acc_type, initial)
            elif choice == "3":
                acc_id = Prompt.ask("Account Number")
                amount = FloatPrompt.ask("Amount")
                desc = Prompt.ask("Description", default="Deposit")
                deposit(acc_id, amount, desc)
            elif choice == "4":
                acc_id = Prompt.ask("Account Number")
                amount = FloatPrompt.ask("Amount")
                desc = Prompt.ask("Description", default="Withdrawal")
                withdraw(acc_id, amount, desc)
            elif choice == "5":
                from_id = Prompt.ask("From Account Number")
                to_id = Prompt.ask("To Account Number")
                amount = FloatPrompt.ask("Amount")
                desc = Prompt.ask("Description", default="Transfer")
                transfer(from_id, to_id, amount, desc)
            elif choice == "6":
                acc_id = Prompt.ask("Account Number")
                toggle_visibility(acc_id)
            elif choice == "7":
                show_my_loans()
            elif choice == "8":
                amount = FloatPrompt.ask("Loan Amount")
                rate = FloatPrompt.ask("Interest Rate (%)", default=5.0)
                request_loan(amount, rate)
            elif choice == "9":
                loan_id = Prompt.ask("Loan ID")
                from_acc = Prompt.ask("From Account Number")
                amount = FloatPrompt.ask("Repayment Amount")
                repay_loan(loan_id, from_acc, amount)
            elif choice == "h":
                user['showHidden'] = not user.get('showHidden', False)
                save_token(token, user)
                console.print(f"[info]Show hidden accounts: {'Enabled' if user['showHidden'] else 'Disabled'}[/info]")
            elif choice == "a":
                show_admin_users()
            elif choice == "b":
                show_admin_accounts()
            elif choice == "t":
                show_admin_transactions()
            elif choice == "db":
                show_database_view()
            elif choice == "da":
                acc_id = Prompt.ask("Account Number to Delete")
                delete_account_admin(acc_id)
            elif choice == "dc":
                cust_id = Prompt.ask("Customer ID to Delete")
                delete_customer_admin(cust_id)
            elif choice == "cp":
                cust_id = Prompt.ask("Customer ID to change password")
                admin_change_password(cust_id)
            elif choice == "la":
                show_admin_loans()
            elif choice == "lap":
                loan_id = Prompt.ask("Loan ID to Approve")
                target_acc = Prompt.ask("Target Account Number for Funds")
                approve_loan_admin(loan_id, target_acc)
            elif choice == "lrj":
                loan_id = Prompt.ask("Loan ID to Reject")
                reject_loan_admin(loan_id)
            elif choice == "lad":
                loan_id = Prompt.ask("Loan ID")
                enabled = Prompt.ask("Enable Auto-Debt?", choices=["y", "n"]) == "y"
                toggle_auto_debt_admin(loan_id, enabled)
            elif choice == "lpr":
                process_auto_debts_admin()
            elif choice == "l":
                clear_session()
                console.print("[info]Logged out.[/info]")
            else:
                break
            input("\nPress Enter to continue...")

# --- CLI Argument Parsing ---

def main():
    parser = argparse.ArgumentParser(description="Orios Bank CLI")
    subparsers = parser.add_subparsers(dest="command")

    # Auth
    auth_parser = subparsers.add_parser("auth")
    auth_sub = auth_parser.add_subparsers(dest="subcommand")
    
    login_p = auth_sub.add_parser("login")
    login_p.add_argument("email")
    login_p.add_argument("password")
    
    reg_p = auth_sub.add_parser("register")
    reg_p.add_argument("name")
    reg_p.add_argument("email")
    reg_p.add_argument("phone")
    reg_p.add_argument("password")

    # Accounts
    acc_parser = subparsers.add_parser("accounts")
    acc_sub = acc_parser.add_subparsers(dest="subcommand")
    acc_sub.add_parser("list")
    open_p = acc_sub.add_parser("open")
    open_p.add_argument("type", choices=["CHECKING", "SAVINGS"])
    open_p.add_argument("--initial", type=float, default=0.0)

    tv_p = acc_sub.add_parser("toggle-visibility")
    tv_p.add_argument("id")

    # Transactions
    tx_parser = subparsers.add_parser("tx")
    tx_sub = tx_parser.add_subparsers(dest="subcommand")
    
    dep_p = tx_sub.add_parser("deposit")
    dep_p.add_argument("account")
    dep_p.add_argument("amount", type=float)
    dep_p.add_argument("--desc", default="Deposit")
    
    wd_p = tx_sub.add_parser("withdraw")
    wd_p.add_argument("account")
    wd_p.add_argument("amount", type=float)
    wd_p.add_argument("--desc", default="Withdrawal")
    
    tr_p = tx_sub.add_parser("transfer")
    tr_p.add_argument("from_acc")
    tr_p.add_argument("to_acc")
    tr_p.add_argument("amount", type=float)
    tr_p.add_argument("--desc", default="Transfer")

    # Admin
    admin_parser = subparsers.add_parser("admin")
    admin_sub = admin_parser.add_subparsers(dest="subcommand")
    admin_sub.add_parser("users")
    admin_sub.add_parser("accounts")
    admin_sub.add_parser("txs")
    admin_sub.add_parser("db")

    del_acc = admin_sub.add_parser("delete-account")
    del_acc.add_argument("id")
    
    del_cust = admin_sub.add_parser("delete-customer")
    del_cust.add_argument("id")

    admin_cp = admin_sub.add_parser("cp")
    admin_cp.add_argument("id")
    admin_cp.add_argument("password")

    # Loans
    loan_parser = subparsers.add_parser("loans")
    loan_sub = loan_parser.add_subparsers(dest="subcommand")
    loan_sub.add_parser("my")
    
    l_req = loan_sub.add_parser("request")
    l_req.add_argument("amount", type=float)
    l_req.add_argument("--rate", type=float, default=5.0)
    
    l_rep = loan_sub.add_parser("repay")
    l_rep.add_argument("loan_id")
    l_rep.add_argument("account")
    l_rep.add_argument("amount", type=float)

    # Admin Loans
    al_parser = admin_sub.add_parser("loans")
    al_sub = al_parser.add_subparsers(dest="al_subcommand")
    al_sub.add_parser("all")
    
    al_app = al_sub.add_parser("approve")
    al_app.add_argument("id")
    al_app.add_argument("target")
    
    al_rej = al_sub.add_parser("reject")
    al_rej.add_argument("id")
    
    al_ad = al_sub.add_parser("auto-debt")
    al_ad.add_argument("id")
    al_ad.add_argument("enabled", choices=["true", "false"])
    
    admin_sub.add_parser("process-auto-debts")

    args = parser.parse_args()

    if not args.command:
        interactive_menu()
    elif args.command == "auth":
        if args.subcommand == "login": login(args.email, args.password)
        elif args.subcommand == "register": register(args.name, args.email, args.phone, args.password)
    elif args.command == "accounts":
        if args.subcommand == "list": show_dashboard()
        elif args.subcommand == "open": open_account(args.type, args.initial)
        elif args.subcommand == "toggle-visibility": toggle_visibility(args.id)
    elif args.command == "tx":
        if args.subcommand == "deposit": deposit(args.account, args.amount, args.desc)
        elif args.subcommand == "withdraw": withdraw(args.account, args.amount, args.desc)
        elif args.subcommand == "transfer": transfer(args.from_acc, args.to_acc, args.amount, args.desc)
    elif args.command == "admin":
        if args.subcommand == "users": show_admin_users()
        elif args.subcommand == "accounts": show_admin_accounts()
        elif args.subcommand == "txs": show_admin_transactions()
        elif args.subcommand == "db": show_database_view()
        elif args.subcommand == "delete-account": delete_account_admin(args.id)
        elif args.subcommand == "delete-customer": delete_customer_admin(args.id)
        elif args.subcommand == "cp": admin_change_password(args.id, args.password)
        elif args.subcommand == "loans":
            if args.al_subcommand == "all": show_admin_loans()
            elif args.al_subcommand == "approve": approve_loan_admin(args.id, args.target)
            elif args.al_subcommand == "reject": reject_loan_admin(args.id)
            elif args.al_subcommand == "auto-debt": toggle_auto_debt_admin(args.id, args.enabled == "true")
        elif args.subcommand == "process-auto-debts": process_auto_debts_admin()
    elif args.command == "loans":
        if args.subcommand == "my": show_my_loans()
        elif args.subcommand == "request": request_loan(args.amount, args.rate)
        elif args.subcommand == "repay": repay_loan(args.loan_id, args.account, args.amount)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        console.print("\n[warning]Exiting...[/warning]")
        sys.exit(0)
