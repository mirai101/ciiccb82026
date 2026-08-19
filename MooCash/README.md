# 🐮 MooCash — Modern Digital Banking System

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2%2B-green.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-MySQL%20%2F%20phpMyAdmin-orange.svg)](https://www.mysql.com/)
[![Security](https://img.shields.io/badge/Security-JWT%20%26%20BCrypt-red.svg)](https://jwt.io/)
[![CLI](https://img.shields.io/badge/CLI-Python%20%2F%20Rich-yellow.svg)](https://python.org)
[![Build](https://img.shields.io/badge/Build-Maven-brightgreen.svg)](https://maven.apache.org/)

**MooCash** is a full-featured, multi-tiered digital banking application built with Java 17, Spring Boot, Spring Security (JWT), MySQL, and Spring Data JPA. It provides comprehensive financial management capabilities including customer authentication, tiered account management (Savings/Checking), real-time fund transfers, instant deposits/withdrawals, credit/debit card issuing, loan life-cycle management with automated auto-debt processing, and complete administrator management tools.

The ecosystem includes a modern vanilla HTML5/CSS3/JS Web Interface, a rich interactive Python Terminal UI / CLI client, and operational Spring Boot Actuator endpoints.

---

## 👨‍💻 Developer Information

* **Developer:** Mark Jeferson D. Tumolva
* **Tech Stack:** Java 17 | Spring Boot | MySQL | JWT & BCrypt | HTML5/CSS/JS | Python (Rich/Requests)
* **Maven Artifact:** `com.moocash:moocash-api:1.0.0`

---

## 🏛 System Architecture & Design

MooCash follows a clean, decoupled layered architecture:

```text
 Client Layer               Security & Controller                Service Layer               Persistence Layer
+-------------------+       +-----------------------+           +------------------+         +------------------+
|  Web Frontend     | ----> |  JwtAuthentication    | --------> |  AccountService  | -------> | AccountRepo      |
|  (HTML/CSS/JS)    |       |  Filter               |           |  AuthService     |         | CustomerRepo     |
+-------------------+       +-----------------------+           |  LoanService     |         | TransactionRepo  |
                            |                       |           |  CardService     |         | LoanRepo         |
+-------------------+       |  Spring Security      |           +------------------+         +------------------+
|  Python CLI / TUI | ----> |  @PreAuthorize (RBAC) |                    |                            |
|  (Rich / Request) |       +-----------------------+                    v                            v
+-------------------+       |  REST Controllers     |           +------------------+         +------------------+
                            |  - AuthController     |           | Scheduled Loan   |         | MySQL Database   |
                            |  - AccountController  |           | Auto-Debt Task   |         | (phpMyAdmin)     |
                            |  - LoanController     |           +------------------+         +------------------+
                            |  - TxController       |
                            +-----------------------+
```

### Architectural Highlights
1. **Separation of Concerns:** Controllers handle REST requests/responses via explicit DTOs. Business rules, financial consistency, and constraints reside exclusively within Service implementations.
2. **State & Transaction Integrity:** Financial movements (e.g., transfers) operate within `@Transactional` boundaries, ensuring atomic double-entry bookkeeping (debit source + credit destination + transaction log generation).
3. **Multi-Channel Access:** Provides a dual user interface — a lightweight web dashboard served via Spring Boot static resources and an interactive Python terminal application (`moocash.py`).

---

## 🛠 Tech Stack & Dependencies

| Component | Technology | Version / Tool | Description |
| :--- | :--- | :--- | :--- |
| **Language Target** | Java | `17+` | Modern JDK features and syntax |
| **Framework** | Spring Boot | `3.2.x` / `3.5.x` | Core application container & auto-configuration |
| **Security** | Spring Security | `6.x` | Stateless Security Filter Chain & RBAC |
| **Authentication** | JJWT | `0.13.0` | Secure JWT token issuance & signature validation |
| **Password Hashing** | BCrypt | Standard | Adaptive salted password hashing (`PasswordHasher`) |
| **Persistence** | Spring Data JPA | Hibernate | ORM mapping and repository abstractions |
| **Database** | MySQL | `8.0+` | Relational database engine (phpMyAdmin interface) |
| **Caching** | Spring Cache | ConcurrentMap / Simple | In-memory caching for frequently accessed metrics |
| **Monitoring** | Spring Boot Actuator | Native | Operational metrics, health checks, and info endpoints |
| **Code Generation** | Lombok | Latest | Reduction of boilerplate getters/setters/builders |
| **Testing** | JUnit 5 & Mockito | Starter Test | Unit and integration test suites |
| **Terminal Client** | Python 3 | `rich`, `requests` | CLI / TUI interface with styled tables and prompts |

---

## 📂 Complete Project Folder Structure

```text
└── MooCash
    ├── backend
    │   ├── pom.xml
    │   └── src
    │       ├── main
    │       │   ├── java
    │       │   │   └── com
    │       │   │       └── moocash
    │       │   │           └── api
    │       │   │               ├── controller
    │       │   │               │   ├── AccountController.java
    │       │   │               │   ├── AuthController.java
    │       │   │               │   ├── LoanController.java
    │       │   │               │   └── TransactionController.java
    │       │   │               ├── dto
    │       │   │               │   ├── AccountDto.java
    │       │   │               │   ├── AuthResponseDto.java
    │       │   │               │   ├── CardDto.java
    │       │   │               │   ├── CustomerDto.java
    │       │   │               │   ├── DepositRequestDto.java
    │       │   │               │   ├── LoanDto.java
    │       │   │               │   ├── LoanRequestDto.java
    │       │   │               │   ├── LoginRequestDto.java
    │       │   │               │   ├── PasswordChangeDto.java
    │       │   │               │   ├── TransactionDto.java
    │       │   │               │   ├── TransferRequestDto.java
    │       │   │               │   └── WithdrawRequestDto.java
    │       │   │               ├── exception
    │       │   │               │   ├── GlobalExceptionHandler.java
    │       │   │               │   ├── InsufficientBalanceException.java
    │       │   │               │   ├── InvalidAmountException.java
    │       │   │               │   ├── ResourceNotFoundException.java
    │       │   │               │   └── UnauthorizedAccessException.java
    │       │   │               ├── model
    │       │   │               │   ├── Account.java
    │       │   │               │   ├── Card.java
    │       │   │               │   ├── CheckingAccount.java
    │       │   │               │   ├── Customer.java
    │       │   │               │   ├── Loan.java
    │       │   │               │   ├── SavingsAccount.java
    │       │   │               │   └── Transaction.java
    │       │   │               ├── MooCashApiApplication.java
    │       │   │               ├── repository
    │       │   │               │   ├── AccountRepository.java
    │       │   │               │   ├── CardRepository.java
    │       │   │               │   ├── CustomerRepository.java
    │       │   │               │   ├── LoanRepository.java
    │       │   │               │   └── TransactionRepository.java
    │       │   │               ├── scheduler
    │       │   │               │   └── LoanScheduler.java
    │       │   │               ├── security
    │       │   │               │   ├── JwtAuthFilter.java
    │       │   │               │   ├── JwtUtil.java
    │       │   │               │   ├── PasswordHasher.java
    │       │   │               │   └── SecurityConfig.java
    │       │   │               └── service
    │       │   │                   ├── AccountService.java
    │       │   │                   ├── AuthService.java
    │       │   │                   ├── LoanService.java
    │       │   │                   └── TransactionService.java
    │       │   └── resources
    │       │       ├── application.properties
    │       │       ├── static
    │       │       │   ├── app.js
    │       │       │   └── index.html
    │       │       └── static1.zip
    │       └── test
    │           └── java
    │               └── com
    │                   └── moocash
    │                       └── api
    │                           └── service
    │                               └── AccountServiceTest.java
    ├── cli
    │   ├── moocash.py
    │   ├── requirements.txt
    │   └── tui.py
    └── moocash_schema.sql
```

---

## 🗄 Database Model & Relationships

The database is built on MySQL (`moocash_schema.sql`). The relational mappings enforce strict data integrity across customers, bank accounts, cards, loans, and audit trails.

```text
[ CUSTOMERS ] (1) <---+--- (N) [ ACCOUNTS ] (1) <---+--- (N) [ TRANSACTIONS ]
                     |                            |
                     +--- (N) [ LOANS ]           +--- (N) [ CARDS ]
```

### Table Definitions & Key Fields

1. **`customers`**
   * `customer_id` (`VARCHAR(36)`, PK) - UUID.
   * `first_name`, `last_name`, `full_name` (`VARCHAR(255)`).
   * `email` (`VARCHAR(255)`, UNIQUE) - Principal identity for auth.
   * `hashed_password` (`VARCHAR(255)`) - Salted BCrypt hash.
   * `phone` (`VARCHAR(255)`).
   * `role` (`VARCHAR(255)`) - `CUSTOMER` or `ADMIN`.
   * `token_version` (`BIGINT`) - Used to invalidate active sessions.
   * `registered_at` (`DATETIME`).

2. **`accounts`**
   * `account_id` (`VARCHAR(36)`, PK) - Generated bank account number.
   * `customer_id` (`VARCHAR(36)`, FK -> `customers.customer_id`).
   * `balance` (`DOUBLE`) - Account net balance.
   * `status` (`VARCHAR(255)`) - `ACTIVE`, `HOLD`, `BLOCKED`.
   * `is_hidden` (`BOOLEAN`) - Privacy mask toggle in UI.
   * `created_at` (`DATETIME`).

3. **`transactions`**
   * `transaction_id` (`VARCHAR(36)`, PK) - Immutable record ID.
   * `type` (`VARCHAR(255)`) - `INITIAL_DEPOSIT`, `DEPOSIT`, `WITHDRAW`, `TRANSFER_IN`, `TRANSFER_OUT`.
   * `amount` (`DOUBLE`).
   * `timestamp` (`DATETIME`).
   * `account_id` (`VARCHAR(36)`, FK -> `accounts.account_id`).
   * `from_account`, `to_account` (`VARCHAR(255)`).
   * `description` (`VARCHAR(255)`).

4. **`loans`**
   * `loan_id` (`VARCHAR(36)`, PK).
   * `customer_id` (`VARCHAR(36)`, FK).
   * `principal_amount` (`DOUBLE`).
   * `remaining_balance` (`DOUBLE`).
   * `interest_rate` (`DOUBLE`).
   * `status` (`VARCHAR(255)`) - `PENDING`, `APPROVED`, `REJECTED`, `PAID`.
   * `auto_debt` (`BOOLEAN`) - Toggle for automated repayment deduction.

5. **`cards`**
   * `card_id` (`VARCHAR(36)`, PK).
   * `account_id` (`VARCHAR(36)`, FK).
   * `card_number` (`VARCHAR(16)`).
   * `cvv` (`VARCHAR(3)`).
   * `expiration_date` (`VARCHAR(7)`).

---

## ⚡ API Surface Reference

All secure endpoints require `Authorization: Bearer <JWT_TOKEN>` header.

### 🔐 Authentication (`/api/auth`)
* `POST /api/auth/register` — Register a new customer account.
* `POST /api/auth/login` — Authenticate credentials, returns JWT token.
* `GET /api/auth/me` — Get profile information of logged-in user.
* `POST /api/auth/change-password` — Change password.

### 💳 Accounts & Banking (`/api/accounts`)
* `POST /api/accounts/open` — Open a new `CHECKING` or `SAVINGS` account.
* `GET /api/accounts/my-accounts` — List all accounts belonging to authenticated customer.
* `GET /api/accounts/total-balance` — Compute total aggregate balance.
* `POST /api/accounts/deposit` — Deposit funds into an account.
* `POST /api/accounts/withdraw` — Withdraw funds from an account.
* `POST /api/accounts/transfer` — Transfer funds between accounts.
* `POST /api/accounts/visibility` — Toggle privacy mask visibility for account.

### 🏦 Loan Management (`/api/loans`)
* `POST /api/loans/request` — Apply for a new loan.
* `GET /api/loans/my-loans` — View current loans and outstanding principal.
* `POST /api/loans/repay` — Execute loan repayment.
* `POST /api/loans/{id}/approve` — *(ADMIN)* Approve pending loan request.
* `POST /api/loans/{id}/reject` — *(ADMIN)* Reject loan request.
* `POST /api/loans/{id}/toggle-auto-debt` — Enable/Disable scheduled auto-deduction.

### 📜 Transactions (`/api/transactions`)
* `GET /api/transactions/account/{accountId}` — List transaction history for specific account.
* `GET /api/transactions/my-transactions` — View personal history across all accounts.
* `GET /api/transactions/all` — *(ADMIN)* View global transaction log.

---

## 🔒 Security Implementation

1. **Stateless JWT Flow:**
   * Upon successful authentication via `AuthController`, the client receives an encrypted JWT.
   * `JwtAuthenticationFilter` intercepts every incoming HTTP request, validates the signature, checks token expiration, and populates `SecurityContextHolder`.
2. **Password Protection:**
   * Passwords are salted and hashed using `BCryptPasswordEncoder` before database persistence.
3. **Role-Based Access Control (RBAC):**
   * Endpoint protection is enforced at controller method level via `@PreAuthorize("hasRole('ADMIN')")`.
   * Standard customers (`ROLE_CUSTOMER`) cannot access administrative routes or other customers' accounts.
4. **Pre-Seeded Administrator Account:**
   * **Email:** `moocash@admin.com`
   * **Password:** `admin123`

---

## 💻 Python CLI / TUI Client

MooCash includes an interactive terminal user interface (`moocash.py`) that connects to the backend REST API using Python's `requests` and `rich` libraries.

### Features
* Interactive menus with formatted ASCII headers and colored status badges.
* Customer dashboard: Net worth calculation, account balance listing, loan tracker.
* Instant deposit, withdrawal, and transfer Wizards.
* Full Administrative Panel: User list inspection, database explorer, manual loan approvals, password override tool.

---

## 🚀 Getting Started & Setup Guide

### Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher
* **Apache Maven:** Version 3.8+ (or use included `./mvnw`)
* **MySQL Server:** Version 8.0+ & phpMyAdmin
* **Python:** Version 3.8+ (Required for CLI)

---

### Step 1: Database Setup
1. Start your MySQL Server (e.g., via XAMPP, Docker, or native service).
2. Create a database named `moocash`.
3. Import `moocash_schema.sql` into MySQL / phpMyAdmin:
   ```bash
   mysql -u root -p moocash < moocash_schema.sql
   ```

---

### Step 2: Configure Spring Boot
Edit `src/main/resources/application.properties` with your database credentials:

```properties
server.port=8080

# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/moocash?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT Configuration
jwt.secret=9a2f8c7e1d4b6a8f0c2e4a6b8d0e2f4a6b8c0d2e4f6a8b0c2d4e6f8a0b2c4d6e
jwt.expiration=86400000

# Spring Cache Configuration
spring.cache.type=simple
```

---

### Step 3: Build & Run the Backend
Using Maven Wrapper:

```bash
# Clean and compile
./mvnw clean package -DskipTests

# Run unit test suite
./mvnw test

# Start the Spring Boot Application
./mvnw spring-boot:run
```
The REST API will be accessible at `http://localhost:8080`.

---

### Step 4: Run the Web Dashboard
Open your browser and navigate to:
```text
http://localhost:8080/index.html
```

---

### Step 5: Run the Python CLI Client
1. Install Python dependencies:
   ```bash
   pip install requests rich
   ```
2. Run the terminal application:
   ```bash
   python3 moocash.py
   ```

---

## 🧪 Unit & Integration Testing

The project contains unit tests verifying account operations, loan processing, and transfer integrity:

```bash
./mvnw test
```

**Test Coverage Highlights (`com.moocash.api.service`):**
* `AccountServiceTest` — Opening accounts, deposit validation, insufficient funds handling, fund transfers.
* `AuthServiceTest` — Registration, authentication, JWT creation, password hashing validation.
* `LoanServiceTest` — Loan applications, repayment calculations, auto-debt scheduler routines.

---

## 📄 License & Attribution

Developed by **Mark Jeferson D. Tumolva** as a complete banking platform demonstration.
All rights reserved.
