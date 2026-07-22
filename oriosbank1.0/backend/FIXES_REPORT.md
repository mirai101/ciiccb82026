# OriosBank API - Fixes and Improvements Report

## Date: 2026-07-20

### 1. Overview
This document summarizes the changes made to the OriosBank API to resolve compilation errors, fix syntax issues, and enhance the security and robustness of the application.

### 2. Compilation and Build Fixes
- **Lombok & Java 26 Compatibility**: Updated Lombok to version `1.18.46` and configured the `maven-compiler-plugin` to explicitly support the Java 26 environment. This resolved issues where the compiler could not find the Lombok annotation processor.
- **Syntax Fixes**: Corrected illegal escape characters in regex patterns within `CustomerDto.java`. Patterns like `\s` and `\+` were properly escaped as `\\s` and `\\+` to comply with Java String requirements.
- **Redundant Constructors**: Removed `@AllArgsConstructor` from `CheckingAccount` and `SavingsAccount`. Since these classes only contained static final constants and no instance fields, the annotation was causing compilation errors.

### 3. Security Enhancements
- **Account Ownership Validation**: Updated `AccountService` and `AccountController` to ensure that users can only withdraw from or transfer between accounts they actually own. The `Authentication` context is now used to verify the `customerId`.
- **Secure Customer Details Endpoint**: Refactored the `/me/{customerId}` endpoint to a more secure `/me` endpoint. It now automatically retrieves the details of the currently authenticated user, preventing unauthorized access to other customers' data.

### 4. Logic and Validation Improvements
- **Account Type Safety**: Added explicit validation in `AccountService.createAccount` to handle invalid account types gracefully.
- **Amount Validation**: Updated DTOs (`DepositRequestDto`, `TransferRequestDto`) to use `@Positive` instead of `@Min(0)`, ensuring that transactions only involve positive amounts.
- **Informative Error Messages**: Enhanced balance check logic to include specific limit/minimum balance details in the error messages.

### 5. Database Migration (MongoDB)
- **Dependency Update**: Replaced `spring-boot-starter-data-jpa` and `postgresql` with `spring-boot-starter-data-mongodb`.
- **Configuration**: Created `application.properties` with MongoDB URI and database settings.
- **Model Refactoring**: Migrated all entities (`Customer`, `Account`, `Transaction`) to MongoDB documents using `@Document`, `@Id`, and `@DBRef`.
- **Repository Refactoring**: Converted all repositories to extend `MongoRepository`. Specifically, the JPQL total balance query in `AccountRepository` was refactored into a MongoDB Aggregation pipeline.
- **Inheritance Support**: Leveraged MongoDB's natural support for inheritance for `CheckingAccount` and `SavingsAccount` within the `accounts` collection.

### 6. Verification (Build Success)
The project has been verified to build successfully with the new MongoDB-based architecture.

```text
[INFO] Scanning for projects...
[INFO] --------------------< com.oriosbank:oriosbank-api >---------------------
[INFO] Building OriosBank API 1.0.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] --- clean:3.3.2:clean (default-clean) @ oriosbank-api ---
[INFO] --- resources:3.3.1:resources (default-resources) @ oriosbank-api ---
[INFO] --- compiler:3.11.0:compile (default-compile) @ oriosbank-api ---
[INFO] Compiling 31 source files with javac [debug release 17] to target/classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 8. Recent Fixes (2026-07-20)
- **Loan Service Compilation**: Resolved a "variable must be final" error in `LoanService.java` by using a final variable within the auto-debt lambda expression.
- **Lombok Builder Warnings**: Added `@Builder.Default` to the `Loan` model to ensure default field values are correctly handled by the Lombok builder and to suppress compilation warnings.
- **Account Opening Parameters**: Explicitly named `@RequestParam` bindings in `AccountController.java` to ensure robust communication between the Web UI/CLI and the backend.
- **Frontend Synchronization**: Synchronized `app.js` between the `frontend` source and the `backend` static resources to ensure consistent behavior across all deployment methods.
