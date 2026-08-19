package com.moocash.api.service;

import com.moocash.api.dto.DepositRequestDto;
import com.moocash.api.dto.TransferRequestDto;
import com.moocash.api.dto.WithdrawRequestDto;
import com.moocash.api.exception.InsufficientBalanceException;
import com.moocash.api.exception.InvalidAmountException;
import com.moocash.api.exception.ResourceNotFoundException;
import com.moocash.api.exception.UnauthorizedAccessException;
import com.moocash.api.model.CheckingAccount;
import com.moocash.api.model.Customer;
import com.moocash.api.model.SavingsAccount;
import com.moocash.api.repository.AccountRepository;
import com.moocash.api.repository.CardRepository;
import com.moocash.api.repository.CustomerRepository;
import com.moocash.api.repository.LoanRepository;
import com.moocash.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccountService}.
 *
 * These tests mock the repository layer (no real MySQL required) and focus on the
 * business rules inside the service: deposit/withdraw/transfer validation, balance
 * limits, ownership checks, and the daily transaction caps.
 *
 * Updated to match the BigDecimal-based money handling introduced in the refactor
 * (balances/amounts are no longer double/Double).
 *
 * Run with: mvn test
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private AccountService accountService;

    private Customer customer;
    private CheckingAccount checkingAccount;
    private SavingsAccount savingsAccount;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId("CUST0001")
                .firstName("Juan")
                .lastName("Dela Cruz")
                .email("juan@mail.com")
                .role("USER")
                .build();

        checkingAccount = new CheckingAccount();
        checkingAccount.setAccountId("CHK0001");
        checkingAccount.setCustomer(customer);
        checkingAccount.setBalance(new BigDecimal("1000.00"));
        checkingAccount.setStatus("ACTIVE");
        checkingAccount.setCreatedAt(LocalDateTime.now());

        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountId("SAV0001");
        savingsAccount.setCustomer(customer);
        savingsAccount.setBalance(new BigDecimal("1000.00"));
        savingsAccount.setStatus("ACTIVE");
        savingsAccount.setCreatedAt(LocalDateTime.now());
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
    
    @Nested
    @DisplayName("openAccount")
    class OpenAccount {

        @Test
        @DisplayName("creates a CHECKING account with a zero initial deposit")
        void opensCheckingAccount() {
            when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of());

            var dto = accountService.openAccount("CUST0001", "CHECKING", BigDecimal.ZERO);

            assertEquals("CHECKING", dto.getType());
            assertEquals(0, dto.getBalance().compareTo(BigDecimal.ZERO));
            verify(accountRepository).save(any());
            // No initial deposit -> no transaction record should be created
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates a SAVINGS account and records an INITIAL_DEPOSIT transaction when funded")
        void opensSavingsAccountWithInitialDeposit() {
            when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of());

            var dto = accountService.openAccount("CUST0001", "SAVINGS", money("500.00"));

            assertEquals("SAVINGS", dto.getType());
            assertEquals(0, dto.getBalance().compareTo(money("500.00")));
            verify(transactionRepository).save(argThat(tx -> "INITIAL_DEPOSIT".equals(tx.getType())));
        }

        @Test
        @DisplayName("rejects an unknown account type")
        void rejectsInvalidAccountType() {
            when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of());

            assertThrows(IllegalArgumentException.class,
                    () -> accountService.openAccount("CUST0001", "CRYPTO", BigDecimal.ZERO));
        }

        @Test
        @DisplayName("rejects opening a 3rd account of the same type (max 2 per type)")
        void rejectsExceedingMaxAccountsPerType() {
            when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
            when(accountRepository.findByCustomerCustomerId("CUST0001"))
                    .thenReturn(List.of(checkingAccount, checkingAccount));

            assertThrows(IllegalArgumentException.class,
                    () -> accountService.openAccount("CUST0001", "CHECKING", BigDecimal.ZERO));
        }

        @Test
        @DisplayName("rejects an initial deposit above the account's max balance")
        void rejectsInitialDepositAboveMaxBalance() {
            when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of());

            assertThrows(InvalidAmountException.class,
                    () -> accountService.openAccount("CUST0001", "CHECKING", money("200000000.00")));
        }

        @Test
        @DisplayName("throws when the customer does not exist")
        void throwsWhenCustomerMissing() {
            when(customerRepository.findById("NOPE")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> accountService.openAccount("NOPE", "CHECKING", BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("increases the account balance and records a DEPOSIT transaction")
        void depositsSuccessfully() {
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("DEPOSIT"), any())).thenReturn(0L);
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            DepositRequestDto dto = DepositRequestDto.builder()
                    .accountId("CHK0001").amount(money("250.00")).description("Payday").build();

            accountService.deposit("CUST0001", dto);

            assertEquals(0, checkingAccount.getBalance().compareTo(money("1250.00")));
            verify(transactionRepository).save(argThat(tx ->
                    "DEPOSIT".equals(tx.getType()) && tx.getAmount().compareTo(money("250.00")) == 0));
        }

        @Test
        @DisplayName("rejects a negative or zero amount")
        void rejectsNonPositiveAmount() {
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("DEPOSIT"), any())).thenReturn(0L);

            DepositRequestDto dto = DepositRequestDto.builder().accountId("CHK0001").amount(money("-10.00")).build();

            assertThrows(InvalidAmountException.class, () -> accountService.deposit("CUST0001", dto));
        }

        @Test
        @DisplayName("rejects a deposit above the per-transaction limit ($100,000)")
        void rejectsDepositAboveLimit() {
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("DEPOSIT"), any())).thenReturn(0L);

            DepositRequestDto dto = DepositRequestDto.builder().accountId("CHK0001").amount(money("150000.00")).build();

            assertThrows(InvalidAmountException.class, () -> accountService.deposit("CUST0001", dto));
        }

        @Test
        @DisplayName("rejects the 6th deposit within the same day (daily limit is 5)")
        void rejectsAfterDailyLimitReached() {
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("DEPOSIT"), any())).thenReturn(5L);

            DepositRequestDto dto = DepositRequestDto.builder().accountId("CHK0001").amount(money("10.00")).build();

            assertThrows(InvalidAmountException.class, () -> accountService.deposit("CUST0001", dto));
        }

        @Test
        @DisplayName("rejects a deposit into a non-existent account")
        void rejectsUnknownAccount() {
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("DEPOSIT"), any())).thenReturn(0L);
            when(accountRepository.findById("GHOST")).thenReturn(Optional.empty());

            DepositRequestDto dto = DepositRequestDto.builder().accountId("GHOST").amount(money("10.00")).build();

            assertThrows(ResourceNotFoundException.class, () -> accountService.deposit("CUST0001", dto));
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("decreases the account balance and records a WITHDRAW transaction")
        void withdrawsSuccessfully() {
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("WITHDRAW"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            WithdrawRequestDto dto = WithdrawRequestDto.builder().accountId("CHK0001").amount(money("300.00")).build();

            accountService.withdraw("CUST0001", dto);

            assertEquals(0, checkingAccount.getBalance().compareTo(money("700.00")));
            verify(transactionRepository).save(argThat(tx -> "WITHDRAW".equals(tx.getType())));
        }

        @Test
        @DisplayName("allows a CHECKING withdrawal into the overdraft limit ($500)")
        void allowsOverdraftWithinLimit() {
            checkingAccount.setBalance(money("100.00")); // withdrawing 400 dips 300 into the 500 overdraft
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("WITHDRAW"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            WithdrawRequestDto dto = WithdrawRequestDto.builder().accountId("CHK0001").amount(money("400.00")).build();

            accountService.withdraw("CUST0001", dto);

            assertEquals(0, checkingAccount.getBalance().compareTo(money("-300.00")));
        }

        @Test
        @DisplayName("rejects a CHECKING withdrawal that exceeds the overdraft limit")
        void rejectsBeyondOverdraftLimit() {
            checkingAccount.setBalance(money("100.00"));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("WITHDRAW"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            // balance(100) + overdraft(500) = 600 max; requesting 700 should fail
            WithdrawRequestDto dto = WithdrawRequestDto.builder().accountId("CHK0001").amount(money("700.00")).build();

            assertThrows(InsufficientBalanceException.class, () -> accountService.withdraw("CUST0001", dto));
        }

        @Test
        @DisplayName("rejects a SAVINGS withdrawal that would break the minimum balance ($100)")
        void rejectsBelowSavingsMinimumBalance() {
            savingsAccount.setBalance(money("150.00"));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("WITHDRAW"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(savingsAccount));
            when(accountRepository.findById("SAV0001")).thenReturn(Optional.of(savingsAccount));

            // 150 - 100 = 50, which is below the $100 minimum balance
            WithdrawRequestDto dto = WithdrawRequestDto.builder().accountId("SAV0001").amount(money("100.00")).build();

            assertThrows(InsufficientBalanceException.class, () -> accountService.withdraw("CUST0001", dto));
        }

        @Test
        @DisplayName("rejects withdrawing from an account the customer does not own")
        void rejectsWithdrawFromAccountNotOwned() {
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("WITHDRAW"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("OTHER0001")).thenReturn(List.of());
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            WithdrawRequestDto dto = WithdrawRequestDto.builder().accountId("CHK0001").amount(money("50.00")).build();

            assertThrows(UnauthorizedAccessException.class, () -> accountService.withdraw("OTHER0001", dto));
        }

        @Test
        @DisplayName("rejects a withdrawal above the per-transaction limit ($50,000)")
        void rejectsWithdrawalAboveLimit() {
            WithdrawRequestDto dto = WithdrawRequestDto.builder().accountId("CHK0001").amount(money("60000.00")).build();

            assertThrows(InvalidAmountException.class, () -> accountService.withdraw("CUST0001", dto));
        }
    }

    @Nested
    @DisplayName("transfer")
    class Transfer {

        @Test
        @DisplayName("moves funds between two accounts and records both TRANSFER_OUT and TRANSFER_IN")
        void transfersSuccessfully() {
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("TRANSFER_OUT"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));
            when(accountRepository.findById("SAV0001")).thenReturn(Optional.of(savingsAccount));

            TransferRequestDto dto = TransferRequestDto.builder()
                    .fromAccountId("CHK0001").toAccountId("SAV0001").amount(money("200.00")).build();

            accountService.transfer("CUST0001", dto);

            assertEquals(0, checkingAccount.getBalance().compareTo(money("800.00")));
            assertEquals(0, savingsAccount.getBalance().compareTo(money("1200.00")));
            verify(transactionRepository).save(argThat(tx -> "TRANSFER_OUT".equals(tx.getType())));
            verify(transactionRepository).save(argThat(tx -> "TRANSFER_IN".equals(tx.getType())));
        }

        @Test
        @DisplayName("rejects transferring to the same account")
        void rejectsSelfTransfer() {
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("TRANSFER_OUT"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            TransferRequestDto dto = TransferRequestDto.builder()
                    .fromAccountId("CHK0001").toAccountId("CHK0001").amount(money("100.00")).build();

            assertThrows(InvalidAmountException.class, () -> accountService.transfer("CUST0001", dto));
        }

        @Test
        @DisplayName("rejects transferring from an account the customer does not own")
        void rejectsTransferFromUnownedAccount() {
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("TRANSFER_OUT"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("OTHER0001")).thenReturn(List.of());
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            TransferRequestDto dto = TransferRequestDto.builder()
                    .fromAccountId("CHK0001").toAccountId("SAV0001").amount(money("100.00")).build();

            assertThrows(UnauthorizedAccessException.class, () -> accountService.transfer("OTHER0001", dto));
        }

        @Test
        @DisplayName("rejects the 6th transfer within the same day (daily limit is 5)")
        void rejectsAfterDailyTransferLimitReached() {
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("TRANSFER_OUT"), any())).thenReturn(5L);

            TransferRequestDto dto = TransferRequestDto.builder()
                    .fromAccountId("CHK0001").toAccountId("SAV0001").amount(money("10.00")).build();

            assertThrows(InvalidAmountException.class, () -> accountService.transfer("CUST0001", dto));
        }

        @Test
        @DisplayName("rejects a transfer to a non-existent target account")
        void rejectsUnknownTargetAccount() {
            when(transactionRepository.countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
                    anyList(), eq("TRANSFER_OUT"), any())).thenReturn(0L);
            when(accountRepository.findByCustomerCustomerId("CUST0001")).thenReturn(List.of(checkingAccount));
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));
            when(accountRepository.findById("GHOST")).thenReturn(Optional.empty());

            TransferRequestDto dto = TransferRequestDto.builder()
                    .fromAccountId("CHK0001").toAccountId("GHOST").amount(money("50.00")).build();

            assertThrows(ResourceNotFoundException.class, () -> accountService.transfer("CUST0001", dto));
        }
    }

    @Nested
    @DisplayName("read operations")
    class ReadOperations {

        @Test
        @DisplayName("returns the customer's accounts mapped to DTOs")
        void returnsCustomerAccounts() {
            when(accountRepository.findByCustomerCustomerId("CUST0001"))
                    .thenReturn(List.of(checkingAccount, savingsAccount));

            var accounts = accountService.getCustomerAccounts("CUST0001");

            assertEquals(2, accounts.size());
        }

        @Test
        @DisplayName("returns the summed total balance across accounts")
        void returnsTotalBalance() {
            when(accountRepository.getTotalBalanceByCustomerId("CUST0001")).thenReturn(money("2000.00"));

            assertEquals(0, accountService.getTotalBalance("CUST0001").compareTo(money("2000.00")));
        }
    }
    
    @Nested
    @DisplayName("issueCard")
    class IssueCard {

        @Test
        @DisplayName("card holder name combines first and last name (regression test for the old overwrite bug)")
        void cardHolderNameIncludesFullName() {
            when(accountRepository.findById("CHK0001")).thenReturn(Optional.of(checkingAccount));

            var card = accountService.issueCard("CUST0001", "CHK0001", "DEBIT");

            assertEquals("JUAN DELA CRUZ", card.getCardHolderName());
        }
    }
}
