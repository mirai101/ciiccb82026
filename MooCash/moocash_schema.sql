-- Import this file in phpMyAdmin: Databases -> Create "moocash"
-- -> select it -> Import -> choose this file -> Go

CREATE DATABASE IF NOT EXISTS moocash
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moocash;

SET FOREIGN_KEY_CHECKS = 0;

-- customers
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
  customer_id     VARCHAR(36)  NOT NULL,
  full_name       VARCHAR(255),
  email           VARCHAR(255) NOT NULL,
  hashed_password VARCHAR(255),
  phone           VARCHAR(20),
  role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
  token_version   BIGINT       NOT NULL DEFAULT 0,
  registered_at   DATETIME,
  version         BIGINT,
  PRIMARY KEY (customer_id),
  UNIQUE KEY uk_customers_email (email)
) ENGINE=InnoDB;

-- accounts (base table — JOINED inheritance)
DROP TABLE IF EXISTS accounts;
CREATE TABLE accounts (
  account_id  VARCHAR(36) NOT NULL,
  customer_id VARCHAR(36),
  balance     DOUBLE,
  status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  is_hidden   TINYINT(1)  NOT NULL DEFAULT 0,
  created_at  DATETIME,
  version     BIGINT,
  PRIMARY KEY (account_id),
  KEY idx_accounts_customer (customer_id),
  CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id)
      REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- checking_accounts (subtype of accounts)
DROP TABLE IF EXISTS checking_accounts;
CREATE TABLE checking_accounts (
  account_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (account_id),
  CONSTRAINT fk_checking_account FOREIGN KEY (account_id)
      REFERENCES accounts (account_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- savings_accounts (subtype of accounts)
DROP TABLE IF EXISTS savings_accounts;
CREATE TABLE savings_accounts (
  account_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (account_id),
  CONSTRAINT fk_savings_account FOREIGN KEY (account_id)
      REFERENCES accounts (account_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- cards
DROP TABLE IF EXISTS cards;
CREATE TABLE cards (
  card_id        VARCHAR(36) NOT NULL,
  card_number    VARCHAR(50),
  card_holder_name VARCHAR(255),
  expiry_date    VARCHAR(10),
  cvv            VARCHAR(10),
  card_type      VARCHAR(30),
  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  account_id     VARCHAR(36),
  customer_id    VARCHAR(36),
  created_at     DATETIME,
  PRIMARY KEY (card_id),
  KEY idx_cards_account (account_id),
  KEY idx_cards_customer (customer_id),
  CONSTRAINT fk_cards_account FOREIGN KEY (account_id)
      REFERENCES accounts (account_id) ON DELETE CASCADE,
  CONSTRAINT fk_cards_customer FOREIGN KEY (customer_id)
      REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- loans
DROP TABLE IF EXISTS loans;
CREATE TABLE loans (
  loan_id            VARCHAR(36) NOT NULL,
  customer_id        VARCHAR(36),
  amount             DOUBLE,
  remaining_balance  DOUBLE,
  interest_rate      DOUBLE,
  status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  auto_debt_enabled  TINYINT(1)  NOT NULL DEFAULT 0,
  created_at         DATETIME,
  paid_at            DATETIME,
  PRIMARY KEY (loan_id),
  KEY idx_loans_customer (customer_id),
  CONSTRAINT fk_loans_customer FOREIGN KEY (customer_id)
      REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- transactions
DROP TABLE IF EXISTS transactions;
CREATE TABLE transactions (
  transaction_id VARCHAR(36) NOT NULL,
  type           VARCHAR(30),
  amount         DOUBLE,
  timestamp      DATETIME,
  account_id     VARCHAR(36),
  from_account   VARCHAR(36),
  to_account     VARCHAR(36),
  description    VARCHAR(500),
  PRIMARY KEY (transaction_id),
  KEY idx_tx_account (account_id),
  KEY idx_tx_from_to (from_account, to_account),
  KEY idx_tx_type_time (type, timestamp),
  CONSTRAINT fk_tx_account FOREIGN KEY (account_id)
      REFERENCES accounts (account_id) ON DELETE SET NULL
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
