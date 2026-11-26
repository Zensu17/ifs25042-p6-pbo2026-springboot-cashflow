package org.delcom.ifs25042springbootcashflow.adapter;

import org.delcom.ifs25042springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25042springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25042springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25042springbootcashflow.adapter.repository.UserRepository;
import org.delcom.ifs25042springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25042springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.Role;
import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void financePresenterFormatsMoneyAndMessages() {
        FinancePresenter presenter = new FinancePresenter();
        LocalDateTime time = LocalDateTime.of(2026, 4, 20, 9, 30);
        Transaction tx = new Transaction(1, 1, "Gaji", 1_000_000L, TransactionType.INCOME, null, time);

        assertThat(presenter.formatAmount(1_000_000L)).isEqualTo("Rp 1,000,000.00");
        assertThat(presenter.formatBalance(4_975_000L)).isEqualTo("Saldo: Rp 4,975,000.00");
        assertThat(presenter.formatType(TransactionType.INCOME)).isEqualTo("Pemasukan");
        assertThat(presenter.formatCreatedAt(tx)).isEqualTo("20/04/2026 09:30");
        assertThat(presenter.format(tx)).isEqualTo("[1] Gaji | Rp 1,000,000.00 | [Pemasukan]");

        assertThat(presenter.showTransactions(List.of())).contains("- Data transaksi belum tersedia!");
        assertThat(presenter.showTransactions(List.of(tx))).contains("[1] Gaji");
        assertThat(presenter.showSearchResults(List.of(), "x")).contains("Hasil Pencarian: \"x\"");
        assertThat(presenter.showAddSuccess(tx)).contains("Berhasil menambah transaksi");
        assertThat(presenter.showUpdateSuccess()).isEqualTo("Berhasil mengubah transaksi.");
        assertThat(presenter.showUpdateFailed(5)).contains("5");
        assertThat(presenter.showRemoveSuccess()).isEqualTo("Berhasil menghapus transaksi.");
        assertThat(presenter.showRemoveFailed(6)).contains("6");
        assertThat(presenter.showValidationError("err")).isEqualTo("[!] err");
    }

    @Test
    void fileStorageStoresAndResolvesFiles() throws IOException {
        FileStorage storage = new FileStorage(tempDir.toString());
        assertThat(storage.root()).isEqualTo(tempDir.toAbsolutePath().normalize());

        String p1 = storage.store("proofs", "Foto.PNG", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));
        assertThat(p1).startsWith("proofs/").endsWith(".png");
        assertThat(Files.exists(storage.resolve(p1))).isTrue();

        String p2 = storage.store("proofs", null, new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)));
        assertThat(p2).endsWith(".bin");

        String p3 = storage.store("proofs", "noextension", new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8)));
        assertThat(p3).endsWith(".bin");

        String p4 = storage.store("proofs", "file.verylongextension", new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8)));
        assertThat(p4).endsWith(".bin");
    }

    @Test
    void inMemoryRepositoriesWorkCorrectly() {
        UserRepository users = new UserRepository();
        User u = users.saveNew("doni", "h", "Doni", Role.USER);
        assertThat(users.findAll()).hasSize(1);
        assertThat(users.findById(u.getId())).isPresent();
        assertThat(users.findById(999)).isEmpty();
        assertThat(users.findByUsername("DONI")).isPresent();
        users.update(u);
        assertThat(users.deleteById(u.getId())).isTrue();
        assertThat(users.deleteById(u.getId())).isFalse();

        TransactionRepository txs = new TransactionRepository();
        Transaction t1 = txs.save(1, "Gaji", 200_000L, TransactionType.INCOME, null);
        Transaction t2 = txs.save(2, "Makan", 50_000L, TransactionType.EXPENSE, "proofs/a.png");
        assertThat(txs.findByUserId(1)).containsExactly(t1);
        assertThat(txs.findById(t2.getId())).isPresent();
        assertThat(txs.findAll()).hasSize(2);
    }

    @Test
    void inMemoryFinanceAiServiceCoversAllPaths() throws DomainException {
        InMemoryFinanceAiService defaultAi = new InMemoryFinanceAiService();
        assertThat(defaultAi.generateTransactions("catatan")).hasSize(2);

        assertThatThrownBy(() -> defaultAi.generateTransactions(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> defaultAi.generateTransactions("  ")).isInstanceOf(ValidationException.class);

        InMemoryFinanceAiService customAi = new InMemoryFinanceAiService(
                List.of(new AiGeneratedTransaction("Bonus", 100_000L, TransactionType.INCOME))
        );
        customAi.addCannedResponse("Snack", 15_000L, TransactionType.EXPENSE);
        assertThat(customAi.generateTransactions("apapun")).hasSize(2);

        InMemoryFinanceAiService nullListAi = new InMemoryFinanceAiService(null);
        nullListAi.setSimulateFailure(true);
        assertThatThrownBy(() -> nullListAi.generateTransactions("test")).isInstanceOf(DomainException.class);
    }
}
