# OriosBank (Prototype)

A lightweight, file-based banking system built with pure Java. OriosBank provides both a **Command-Line Interface (CLI)** and a **Graphical User Interface (GUI)** for managing customer accounts, deposits, withdrawals, and transfers.

---

## Table of Contents

- [Features](#features)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
  - [Compile](#compile)
  - [Run CLI](#run-cli)
  - [Run GUI](#run-gui)
- [Usage](#usage)
  - [CLI Commands](#cli-commands)
  - [GUI Navigation](#gui-navigation)
- [Account Types](#account-types)
- [Architecture](#architecture)
- [Security](#security)
- [Data Persistence](#data-persistence)


---

## Features

- **Customer Registration & Login** — Secure authentication with SHA-256 password hashing
- **Account Management** — Open Savings or Checking accounts
- **Transactions** — Deposit, Withdraw, and Transfer funds between accounts
- **Transaction History** — View all transactions per account
- **Dual Interface** — Choose between a terminal CLI or a modern Swing GUI
- **File-Based Persistence** — No database required; all data stored in plain text files
- **Input Validation** — Email, password, and amount validation with custom exceptions

---

## Project Structure

```
OriosBank/
│
├── src/
│   ├── app/
│   │   ├── MainCLI.java          # CLI entry point
│   │   └── MainGUI.java          # GUI entry point
│   │
│   ├── model/
│   │   ├── Account.java          # Abstract base account class
│   │   ├── SavingsAccount.java   # Savings account with interest & minimum balance
│   │   ├── CheckingAccount.java  # Checking account with overdraft protection
│   │   ├── Customer.java         # Customer entity with hashed credentials
│   │   └── Transaction.java      # Transaction record
│   │
│   ├── service/
│   │   ├── BankService.java      # Core banking operations (register, login, open account)
│   │   ├── AccountService.java   # Deposit, withdraw, transfer logic
│   │   └── TransactionService.java  # Transaction history queries
│   │
│   ├── repository/
│   │   ├── AccountRepository.java     # Account CRUD via file I/O
│   │   ├── CustomerRepository.java    # Customer CRUD via file I/O
│   │   └── FileRepository.java        # Generic file helper
│   │
│   ├── cli/
│   │   ├── Menu.java             # Base menu with shared utilities
│   │   ├── LoginMenu.java        # Login / Register terminal screen
│   │   └── AccountMenu.java      # Main banking terminal dashboard
│   │
│   ├── gui/
│   │   ├── LoginFrame.java       # Login & registration window
│   │   ├── DashboardFrame.java   # Main dashboard with sidebar navigation
│   │   ├── DepositPanel.java     # Deposit funds panel
│   │   ├── WithdrawPanel.java    # Withdraw funds panel
│   │   └── TransferPanel.java    # Transfer funds panel
│   │
│   ├── util/
│   │   ├── InputValidator.java   # Email, password, amount validation
│   │   ├── PasswordHasher.java   # SHA-256 hashing & verification
│   │   └── FileUtil.java         # File read/write/append utilities
│   │
│   └── exception/
│       ├── InsufficientBalanceException.java
│       └── InvalidAmountException.java
│
└── data/
    ├── accounts.txt              # Persisted account records
    └── customers.txt             # Persisted customer records
```

---

## Requirements

- **Java Development Kit (JDK)** 8 or higher
- No external libraries, frameworks, or build tools required because it's a prototype

---

## Getting Started

### Compile

Navigate to the `src` directory and compile all Java files:

```bash
cd OriosBank/src
javac app/MainCLI.java app/MainGUI.java
```

> **Note:** On Windows, use backslashes (`\`) in paths.

> **Reminder:** Prototype project already compiled!. Skip compiling process.

### Run CLI

```bash
java app.MainCLI
```

### Run GUI

```bash
java app.MainGUI
```

---

## Usage

### CLI Commands

After launching the CLI, you will see the **Login / Register** menu:

| Option | Action |
|--------|--------|
| `1` | Login with existing email and password |
| `2` | Register a new customer account |
| `3` | Exit the application |

Once logged in, the **Dashboard** menu provides:

| Option | Action |
|--------|--------|
| `1` | View all your accounts and total balance |
| `2` | Open a new Savings or Checking account |
| `3` | Deposit funds into an account |
| `4` | Withdraw funds from an account |
| `5` | Transfer funds between accounts |
| `6` | View transaction history for an account |
| `7` | Logout and return to login screen |

### GUI Navigation

The Swing GUI features a **sidebar navigation** with the following sections:

- **Dashboard** — Overview of customer info, total balance, and account count
- **Deposit** — Select an account and deposit funds
- **Withdraw** — Select an account and withdraw funds
- **Transfer** — Transfer funds to another account by ID
- **Accounts** — View all accounts in a table; open new accounts
- **Logout** — Return to the login screen

---

## Account Types

| Feature | Savings Account | Checking Account |
|---------|----------------|------------------|
| **Interest Rate** | 2.5% per annum | 0.5% per annum |
| **Minimum Balance** | $100.00 | None |
| **Overdraft Limit** | None | $500.00 |
| **Withdrawal Rule** | Cannot withdraw below $100 minimum | Can overdraft up to $500 |

---

## Architecture

OriosBank follows a **layered architecture**:

```
┌─────────────────────────────────────┐
│  Presentation Layer (CLI / GUI)     │
├─────────────────────────────────────┤
│  Service Layer (Business Logic)     │
├─────────────────────────────────────┤
│  Repository Layer (Data Access)     │
├─────────────────────────────────────┤
│  Model Layer (Entities)             │
├─────────────────────────────────────┤
│  Utility Layer (Helpers)            │
└─────────────────────────────────────┘
```

- **Presentation Layer** — Handles user interaction (terminal menus or Swing frames)
- **Service Layer** — Contains business rules and orchestrates operations
- **Repository Layer** — Abstracts file I/O for accounts and customers
- **Model Layer** — Defines core entities: `Account`, `Customer`, `Transaction`
- **Utility Layer** — Shared helpers for validation, hashing, and file operations

---

## Security

- **Password Hashing** — All passwords are hashed using **SHA-256** before storage
- **No Plaintext Passwords** — Passwords are never stored or transmitted in plaintext
- **Input Validation** — Email format, password length, and positive amount checks
- **Custom Exceptions** — `InsufficientBalanceException` and `InvalidAmountException` prevent invalid operations

---

## Data Persistence

All data is stored in plain text files under the `data/` directory:

- **`customers.txt`** — One line per customer: `ID,Name,Email,HashedPassword,Phone,RegisteredAt`
- **`accounts.txt`** — One line per account: `ID,CustomerID,Type,Balance,CreatedAt`

> **Warning:** These files are human-readable but should not be manually edited, as this may corrupt the data format.

---


## Author

MJDT
