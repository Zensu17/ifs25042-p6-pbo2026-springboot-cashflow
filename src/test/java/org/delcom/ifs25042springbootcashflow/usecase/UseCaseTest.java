package org.delcom.ifs25042springbootcashflow.usecase;

import org.delcom.ifs25042springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25042springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25042springbootcashflow.adapter.repository.UserRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UseCaseTest {

    @Test
    void authUseCaseRegisterLoginAndRootAdmin() throws ValidationException {
        UserRepository repo = new UserRepository();
        AuthUseCase auth = new AuthUseCase(repo);

        assertThatThrownBy(() -> auth.register(null, "1234", "Nama")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.register("  ", "1234", "Nama")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.register("andi", null, "Nama")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.register("andi", "123", "Nama")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.register("andi", "1234", null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.register("andi", "1234", "  ")).isInstanceOf(ValidationException.class);

        User created = auth.register(" andi ", "secret1", " Andi Wijaya ");
        assertThat(created.getUsername()).isEqualTo("andi");
        assertThat(created.getFullName()).isEqualTo("Andi Wijaya");
        assertThat(created.getRole()).isEqualTo(Role.USER);

        assertThatThrownBy(() -> auth.register("andi", "secret2", "Lain"))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> auth.login(null, "secret1")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.login("  ", "secret1")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.login("andi", null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.login("andi", "")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.login("unknown", "secret1")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.login("andi", "wrong")).isInstanceOf(ValidationException.class);

        User loggedIn = auth.login(" andi ", "secret1");
        assertThat(loggedIn.getId()).isEqualTo(created.getId());

        assertThatThrownBy(() -> auth.ensureRootAdmin(null, "admin")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.ensureRootAdmin("  ", "admin")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.ensureRootAdmin("admin", null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> auth.ensureRootAdmin("admin", "")).isInstanceOf(ValidationException.class);

        User admin1 = auth.ensureRootAdmin(" admin ", "admin123");
        User admin2 = auth.ensureRootAdmin("admin", "admin123");
        assertThat(admin1.isAdmin()).isTrue();
        assertThat(admin2.getId()).isEqualTo(admin1.getId());
    }

    @Test
    void userUseCaseCrudAndValidations() throws Exception {
        UserRepository repo = new UserRepository();
        AuthUseCase auth = new AuthUseCase(repo);
        UserUseCase users = new UserUseCase(repo);

        User u = auth.register("sari", "pass12", "Sari Dewi");
        assertThat(users.listUsers()).hasSize(1);
        assertThat(users.getUser(u.getId()).getUsername()).isEqualTo("sari");
        assertThatThrownBy(() -> users.getUser(999)).isInstanceOf(EntityNotFoundException.class);

        assertThatThrownBy(() -> users.updateProfile(u.getId(), null, null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> users.updateProfile(u.getId(), "  ", null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> users.updateProfile(u.getId(), "Sari Baru", "12"))
                .isInstanceOf(ValidationException.class);

        User updatedNoPass = users.updateProfile(u.getId(), " Sari Baru ", null);
        assertThat(updatedNoPass.getFullName()).isEqualTo("Sari Baru");
        User updatedBlankPass = users.updateProfile(u.getId(), "Sari Lagi", "   ");
        assertThat(updatedBlankPass.getFullName()).isEqualTo("Sari Lagi");
        User updatedWithPass = users.updateProfile(u.getId(), "Sari Final", "newpass9");
        assertThat(auth.login("sari", "newpass9").getId()).isEqualTo(u.getId());

        assertThatThrownBy(() -> users.updatePhoto(u.getId(), null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> users.updatePhoto(u.getId(), "  ")).isInstanceOf(ValidationException.class);
        User updatedPhoto = users.updatePhoto(u.getId(), "profiles/sari.png");
        assertThat(updatedPhoto.getPhotoPath()).isEqualTo("profiles/sari.png");
    }

    @Test
    void financeUseCaseAllOperationsAndValidations() throws Exception {
        TransactionRepository repo = new TransactionRepository();
        FinanceUseCase finance = new FinanceUseCase(repo);

        assertThatThrownBy(() -> finance.parseAmount(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.parseAmount("  ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.parseAmount("abc")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.parseAmount("0")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.parseAmount("-500")).isInstanceOf(ValidationException.class);
        assertThat(finance.parseAmount(" 500000 ")).isEqualTo(500_000L);

        assertThatThrownBy(() -> finance.addTransaction(1, null, 1000L, TransactionType.INCOME, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.addTransaction(1, "  ", 1000L, TransactionType.INCOME, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.addTransaction(1, "Gaji", 0L, TransactionType.INCOME, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.addTransaction(1, "Gaji", 1000L, null, null))
                .isInstanceOf(ValidationException.class);

        Transaction inc = finance.addIncome(1, " Gaji Bulanan ", 5_000_000L, "proofs/gaji.png");
        Transaction exp = finance.addExpense(1, " Beli Kopi ", 25_000L, null);
        finance.addIncome(2, "User Lain", 100_000L, null);

        assertThat(finance.getTotalIncome(1)).isEqualTo(5_000_000L);
        assertThat(finance.getTotalExpense(1)).isEqualTo(25_000L);
        assertThat(finance.getBalance(1)).isEqualTo(4_975_000L);
        assertThat(finance.getAllTransactions()).hasSize(3);
        assertThat(finance.getTransactionsByUser(1)).hasSize(2);
        assertThat(finance.listTransactions(1, null, null, null)).hasSize(2);
        assertThat(finance.listTransactions(1, "   ", TransactionTypeFilter.INCOME, SortOption.AMOUNT_ASC))
                .containsExactly(inc);
        assertThat(finance.searchTransactions(1, "kopi")).containsExactly(exp);
        assertThat(finance.sortTransactions(1, SortOption.AMOUNT_ASC)).containsExactly(exp, inc);

        assertThatThrownBy(() -> finance.updateTransactionForUser(1, 0, "X", 1L, TransactionType.INCOME, null, false))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.updateTransactionForUser(1, 999, "X", 1L, TransactionType.INCOME, null, false))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> finance.updateTransactionForUser(2, exp.getId(), "X", null, null, null, false))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.updateTransactionForUser(1, exp.getId(), "  ", null, null, null, false))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.updateTransactionForUser(1, exp.getId(), null, 0L, null, null, false))
                .isInstanceOf(ValidationException.class);

        finance.updateTransactionForUser(1, exp.getId(), null, null, null, null, false);
        finance.updateTransactionForUser(1, inc.getId(), "Gaji Utama", 6_000_000L, TransactionType.INCOME, null, true);
        assertThat(inc.getDescription()).isEqualTo("Gaji Utama");
        assertThat(inc.getAmount()).isEqualTo(6_000_000L);
        assertThat(inc.getProofPath()).isNull();

        finance.updateTransactionForUser(1, inc.getId(), null, null, TransactionType.EXPENSE, "proofs/new.png", false);
        assertThat(inc.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(inc.getProofPath()).isEqualTo("proofs/new.png");

        assertThatThrownBy(() -> finance.removeTransaction(0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> finance.removeTransaction(999)).isInstanceOf(EntityNotFoundException.class);
        finance.removeTransactionForUser(1, exp.getId());
        finance.removeTransaction(inc.getId());
        assertThat(finance.getTransactionsByUser(1)).isEmpty();
    }

    @Test
    void financeAiUseCaseGeneratesAndSavesTransactions() throws DomainException {
        TransactionRepository txRepo = new TransactionRepository();
        InMemoryFinanceAiService aiService = new InMemoryFinanceAiService();
        FinanceAiUseCase aiUseCase = new FinanceAiUseCase(aiService, txRepo);

        assertThatThrownBy(() -> aiUseCase.generateTransactions(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> aiUseCase.generateTransactions("  ")).isInstanceOf(ValidationException.class);

        List<AiGeneratedTransaction> generated = aiUseCase.generateTransactions("  Gaji dan makan  ");
        assertThat(generated).hasSize(2);

        assertThatThrownBy(() -> aiUseCase.saveGeneratedTransactions(1, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> aiUseCase.saveGeneratedTransactions(1, List.of()))
                .isInstanceOf(ValidationException.class);

        List<Transaction> saved = aiUseCase.saveGeneratedTransactions(1, generated);
        assertThat(saved).hasSize(2);
        assertThat(txRepo.findByUserId(1)).hasSize(2);
    }
}
