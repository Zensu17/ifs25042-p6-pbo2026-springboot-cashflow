package org.delcom.ifs25042springbootcashflow.domain;

import org.delcom.ifs25042springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.Role;
import org.delcom.ifs25042springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25042springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainTest {

    @Test
    void passwordHasherHashesMatchesAndRejectsInvalidAlgorithm() {
        String hashed = PasswordHasher.hash("rahasia123");
        assertThat(hashed).hasSize(64);
        assertThat(PasswordHasher.matches("rahasia123", hashed)).isTrue();
        assertThat(PasswordHasher.matches("salah", hashed)).isFalse();

        assertThatThrownBy(() -> PasswordHasher.messageDigest("INVALID-ALGO"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void domainExceptionsExposeMessageCauseAndId() {
        RuntimeException cause = new RuntimeException("root");
        DomainException d1 = new DomainException("msg");
        DomainException d2 = new DomainException("msg2", cause);
        EntityNotFoundException notFound = new EntityNotFoundException(42);
        ValidationException validation = new ValidationException("invalid");

        assertThat(d1.getMessage()).isEqualTo("msg");
        assertThat(d2.getCause()).isSameAs(cause);
        assertThat(notFound.getId()).isEqualTo(42);
        assertThat(notFound.getMessage()).contains("42");
        assertThat(validation.getMessage()).isEqualTo("invalid");
    }

    @Test
    void userEntityGettersMutatorsAndRoleChecks() {
        User user = new User(1, "budi", "hash1", "Budi Santoso", Role.USER, null);
        assertThat(user.getId()).isEqualTo(1);
        assertThat(user.getUsername()).isEqualTo("budi");
        assertThat(user.getPasswordHash()).isEqualTo("hash1");
        assertThat(user.getFullName()).isEqualTo("Budi Santoso");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getPhotoPath()).isNull();
        assertThat(user.isAdmin()).isFalse();

        user.changeFullName("Budi Baru");
        user.changePasswordHash("hash2");
        user.changePhotoPath("profiles/a.png");
        assertThat(user.getFullName()).isEqualTo("Budi Baru");
        assertThat(user.getPasswordHash()).isEqualTo("hash2");
        assertThat(user.getPhotoPath()).isEqualTo("profiles/a.png");

        User admin = new User(2, "admin", "hash", "Admin", Role.ADMIN, "p.png");
        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    void transactionEntityTypeAndFiltersWork() {
        assertThat(TransactionType.INCOME.label()).isEqualTo("Pemasukan");
        assertThat(TransactionType.EXPENSE.label()).isEqualTo("Pengeluaran");

        Transaction income = new Transaction(1, 10, "Gaji", 1_000_000L, TransactionType.INCOME);
        assertThat(income.getId()).isEqualTo(1);
        assertThat(income.getUserId()).isEqualTo(10);
        assertThat(income.getDescription()).isEqualTo("Gaji");
        assertThat(income.getAmount()).isEqualTo(1_000_000L);
        assertThat(income.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(income.getProofPath()).isNull();
        assertThat(income.getCreatedAt()).isNotNull();
        assertThat(income.isIncome()).isTrue();
        assertThat(income.isExpense()).isFalse();
        assertThat(income.signedAmount()).isEqualTo(1_000_000L);

        Transaction nullDesc = new Transaction(2, 10, null, 25_000L, TransactionType.EXPENSE, "proofs/x.png", null);
        assertThat(nullDesc.getDescription()).isEmpty();
        assertThat(nullDesc.isIncome()).isFalse();
        assertThat(nullDesc.isExpense()).isTrue();
        assertThat(nullDesc.signedAmount()).isEqualTo(-25_000L);
        assertThat(nullDesc.getProofPath()).isEqualTo("proofs/x.png");

        nullDesc.changeDescription(null);
        assertThat(nullDesc.getDescription()).isEmpty();
        nullDesc.changeDescription("Kopi");
        nullDesc.changeAmount(30_000L);
        nullDesc.changeType(TransactionType.INCOME);
        nullDesc.changeProofPath(null);
        assertThat(nullDesc.getDescription()).isEqualTo("Kopi");
        assertThat(nullDesc.getAmount()).isEqualTo(30_000L);
        assertThat(nullDesc.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(nullDesc.getProofPath()).isNull();

        assertThat(TransactionTypeFilter.ALL.label()).isEqualTo("Semua tipe");
        assertThat(TransactionTypeFilter.INCOME.label()).isEqualTo("Pemasukan");
        assertThat(TransactionTypeFilter.EXPENSE.label()).isEqualTo("Pengeluaran");

        Transaction exp = new Transaction(3, 10, "Makan", 20_000L, TransactionType.EXPENSE);
        assertThat(TransactionTypeFilter.ALL.matches(income)).isTrue();
        assertThat(TransactionTypeFilter.INCOME.matches(income)).isTrue();
        assertThat(TransactionTypeFilter.INCOME.matches(exp)).isFalse();
        assertThat(TransactionTypeFilter.EXPENSE.matches(exp)).isTrue();
        assertThat(TransactionTypeFilter.EXPENSE.matches(income)).isFalse();
    }

    @Test
    void sortOptionsSortCorrectlyAndExposeLabels() {
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 2, 10, 0);
        Transaction a = new Transaction(1, 1, "Alpha", 200_000L, TransactionType.EXPENSE, null, t1);
        Transaction b = new Transaction(2, 1, "Beta", 500_000L, TransactionType.INCOME, null, t2);
        Transaction c = new Transaction(3, 1, "Charlie", 100_000L, TransactionType.EXPENSE, null, null);

        List<Transaction> items = List.of(a, b, c);
        assertThat(items.stream().sorted(SortOption.NEWEST_FIRST.comparator()).map(Transaction::getId))
                .containsExactly(3, 2, 1);
        assertThat(items.stream().sorted(SortOption.OLDEST_FIRST.comparator()).map(Transaction::getId))
                .containsExactly(1, 2, 3);
        assertThat(items.stream().sorted(SortOption.AMOUNT_DESC.comparator()).map(Transaction::getId))
                .containsExactly(2, 1, 3);
        assertThat(items.stream().sorted(SortOption.AMOUNT_ASC.comparator()).map(Transaction::getId))
                .containsExactly(3, 1, 2);
        assertThat(items.stream().sorted(SortOption.DESCRIPTION_ASC.comparator()).map(Transaction::getId))
                .containsExactly(1, 2, 3);
        assertThat(items.stream().sorted(SortOption.DESCRIPTION_DESC.comparator()).map(Transaction::getId))
                .containsExactly(3, 2, 1);

        assertThat(SortOption.NEWEST_FIRST.label()).isEqualTo("Terbaru");
        assertThat(SortOption.OLDEST_FIRST.label()).isEqualTo("Terlama");
        assertThat(SortOption.AMOUNT_DESC.label()).isEqualTo("Jumlah terbesar");
        assertThat(SortOption.AMOUNT_ASC.label()).isEqualTo("Jumlah terkecil");
        assertThat(SortOption.DESCRIPTION_ASC.label()).isEqualTo("Keterangan A-Z");
        assertThat(SortOption.DESCRIPTION_DESC.label()).isEqualTo("Keterangan Z-A");
    }

    @Test
    void aiGeneratedTransactionValidatesFields() {
        AiGeneratedTransaction tx = new AiGeneratedTransaction("  Bonus  ", 1_000_000L, TransactionType.INCOME);
        assertThat(tx.description()).isEqualTo("Bonus");
        assertThat(tx.amount()).isEqualTo(1_000_000L);
        assertThat(tx.type()).isEqualTo(TransactionType.INCOME);

        assertThatThrownBy(() -> new AiGeneratedTransaction(null, 100L, TransactionType.INCOME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiGeneratedTransaction("   ", 100L, TransactionType.INCOME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiGeneratedTransaction("Gaji", 0L, TransactionType.INCOME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiGeneratedTransaction("Gaji", 100L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
