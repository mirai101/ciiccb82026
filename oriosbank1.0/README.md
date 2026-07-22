# Orios Bank - Modern Digital Banking Solution

Orios Bank is a sleek, secure, and fully responsive digital banking application. It features a clean light-mode UI with smooth animations, backed by a robust Spring Boot API and MongoDB database.

## Features 🐧 

### Customers Access 🐒
- **Secure Authentication**: JWT-based login and registration with session persistence.
- **Interactive Dashboard**: Real-time overview of total balance and active accounts.
- **Account Management**: Open and manage multiple accounts (Checking & Savings).
- **Financial Operations**: Seamless deposits, withdrawals, and fund transfers between accounts.
- **Loan Management**: Request loans, track remaining balances, and make manual repayments.
- **Credit Cards**: Integrated OriosVISA and OriosMASTER card management with unique branding.
- **Privacy Controls**: Toggle visibility for sensitive information like account and card numbers.

### Administrator Access 🐐
- **System Overview**: Real-time database statistics for customers, accounts, cards, transactions, and loans.
- **Customer Management**: Full oversight of all registered users, including password overrides and deletion.
- **Account Control**: Ability to hold, block, or delete accounts with secure cascading data removal.
- **Loan Oversight**: Approve or reject loan requests, toggle auto-debt, and monitor system-wide debt.
- **Transaction History**: View all system transactions with filtering and detailed breakdowns.

##  Technology 🦜

- **Backend**: Java 17+, Spring Boot 3.2, Spring Security (JWT), Spring Data MongoDB.
- **Database**: MongoDB (local instance on `localhost:27017` 👈).
- **Frontend**: HTML5, CSS3 (Grid, Flexbox, Animations, Gradients), Vanilla JavaScript (ES6+).
- **CLI**: Python 3.x with `requests` and `rich` libraries.
- **Design**: Light mode theme with floating animated blobs, smooth transitions, and responsive layout.

## Start 🐥

### Prerequisites
- **Java Development Kit (JDK)**: version 17 or higher.
- **Maven**: For building and managing dependencies.
- **MongoDB**: A running instance (default: `localhost:27017` 👈).
- **Python 🐍 3**: (optional, for CLI usage).

### Installation & Setup 👨‍💻

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd oriosbank-refactored
   ```

2. **Start MongoDB**:
   ```bash
   mongod
   ```

3. **Build and Run the Backend**:
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

4. **Access the Application**:
   Open your browser and navigate to `http://localhost:8080`. 👈

### Default Credentials

- **Admin Account**:
  - Email: `admin@orios.com`
  - Password: `admin123`
- **User Account**: Register a new account via the UI or CLI.

## Project Structure 🧑‍🔬

```
oriosbank-refactored/
├── backend/                        # Spring Boot application
│   ├── src/main/java/              # Java source code
│   │   └── com/oriosbank/api/
│   │       ├── controller/         # REST API controllers
│   │       ├── service/            # Business logic
│   │       ├── model/              # MongoDB document models
│   │       ├── repository/         # Data access layer
│   │       ├── dto/                # Data transfer objects
│   │       ├── exception/          # Custom exceptions
│   │       └── security/           # JWT authentication
│   ├── src/main/resources/static/  # Frontend assets (HTML/CSS/JS)
│   └── pom.xml                     # Maven configuration
├── frontend/                       # Frontend source (synced with static/)
│   ├── index.html                  # Main HTML page
│   ├── style.css                   # Light mode styles
│   └── app.js                      # Application logic
├── cli/                            # Command-line interface
│   └── orios.py                    # Python CLI with interactive menu
└── README.md                       # This file
```

## API Endpoints 🧑‍🔧

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |
| GET | `/api/auth/me` | Get current user details |

### Accounts
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/accounts/open` | Open a new account |
| GET | `/api/accounts/my-accounts` | List user's accounts |
| GET | `/api/accounts/total-balance` | Get total balance |
| POST | `/api/accounts/deposit` | Deposit funds |
| POST | `/api/accounts/withdraw` | Withdraw funds |
| POST | `/api/accounts/transfer` | Transfer between accounts |
| POST | `/api/accounts/{id}/toggle-visibility` | Toggle account visibility |
| POST | `/api/accounts/{id}/issue-card` | Issue a credit card |

### Loans
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/loans/request` | Request a new loan |
| GET | `/api/loans/my-loans` | List user's loans |
| POST | `/api/loans/repay` | Repay a loan |
| GET | `/api/loans/admin/all` | Admin: list all loans |
| POST | `/api/loans/admin/{id}/approve` | Admin: approve loan |
| POST | `/api/loans/admin/{id}/reject` | Admin: reject loan |
| POST | `/api/loans/admin/{id}/auto-debt` | Admin: toggle auto-debt |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/accounts/admin/all-customers` | List all customers |
| GET | `/api/accounts/admin/all-accounts` | List all accounts |
| GET | `/api/transactions/admin/all` | List all transactions |
| POST | `/api/accounts/admin/accounts/{id}/status` | Update account status |
| DELETE | `/api/accounts/admin/accounts/{id}` | Delete account |
| DELETE | `/api/accounts/admin/customers/{id}` | Delete customer |

## CLI Usage 🐆

The CLI provides both an interactive menu and command-line argument mode.

### Setup
```bash
cd cli
pip install requests rich
python3 orios.py
```

### Interactive Menu
Run without arguments for a full interactive experience:
```bash
python3 orios.py
```

### Command-Line Arguments
```bash
# Login
python3 orios.py auth login user@example.com password123

# Register
python3 orios.py auth register "John Doe" john@example.com 1234567890 password123

# List accounts
python3 orios.py accounts list

# Open account
python3 orios.py accounts open SAVINGS --initial 1000

# Deposit
python3 orios.py tx deposit ACC123 500.0 --desc "Monthly Savings"

# Withdraw
python3 orios.py tx withdraw ACC123 200.0 --desc "ATM Withdrawal"

# Transfer
python3 orios.py tx transfer ACC123 ACC456 300.0 --desc "Rent Payment"

# Request loan
python3 orios.py loans request 5000 --rate 5.0

# Repay loan
python3 orios.py loans repay LOAN123 ACC456 1000.0

# Admin: View all data
python3 orios.py admin users
python3 orios.py admin accounts
python3 orios.py admin txs
python3 orios.py admin db          # Database explorer (all collections)

# Admin: Loan management
python3 orios.py admin loans all
python3 orios.py admin loans approve LOAN123 ACC456
python3 orios.py admin loans reject LOAN123
python3 orios.py admin loans auto-debt LOAN123 true
```

## Testing

Run the unit tests:
```bash
cd backend
mvn test
```

**The test suite covers**:
- Loan service operations (request, approve, reject, repay, auto-debt)
- Customer and admin authorization
- Account validation and edge cases

## Documentation

- **Frontend details**: See [frontend/README.md](frontend/README.md)
- **Features**: Added some ads or maybe a bug, Fix later 🐱 [HERE](https://facebook.com/corei9.9000k) 👈🏼 contact me
- **Fixes report**: See [backend/FIXES_REPORT.md](backend/FIXES_REPORT.md)
- **API docs** (generated): `backend/target/site/apidocs/index.html`
- **Full documentation**: See [OriosBank_Documentation.pdf](OriosBank_Documentation.pdf)
