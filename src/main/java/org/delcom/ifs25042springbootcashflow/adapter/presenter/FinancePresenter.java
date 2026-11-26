package org.delcom.ifs25042springbootcashflow.adapter.presenter;

import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Presenter yang memformat data transaksi keuangan menjadi teks siap tampil.
 */
public class FinancePresenter {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Memformat nominal rupiah, mis. Rp 1,000,000.00. */
    public String formatAmount(long amount) {
        return String.format(Locale.US, "Rp %,.2f", (double) amount);
    }

    /** Memformat ringkasan saldo, mis. Saldo: Rp 4,975,000.00. */
    public String formatBalance(long balance) {
        return "Saldo: " + formatAmount(balance);
    }

    /** Memformat label tipe transaksi. */
    public String formatType(TransactionType type) {
        return type.label();
    }

    /** Memformat waktu transaksi. */
    public String formatCreatedAt(Transaction transaction) {
        return transaction.getCreatedAt().format(DATE_TIME_FORMAT);
    }

    /** Memformat satu transaksi menjadi baris teks untuk ditampilkan. */
    public String format(Transaction transaction) {
        return String.format(
                "[%d] %s | %s | [%s]",
                transaction.getId(),
                transaction.getDescription(),
                formatAmount(transaction.getAmount()),
                formatType(transaction.getType())
        );
    }

    /** Memformat daftar transaksi menjadi teks multi-baris. */
    public String formatList(List<Transaction> transactions, String header, String emptyMessage) {
        StringBuilder builder = new StringBuilder(header);
        if (transactions.isEmpty()) {
            builder.append('\n').append(emptyMessage);
            return builder.toString();
        }
        String body = transactions.stream()
                .map(this::format)
                .collect(Collectors.joining("\n"));
        builder.append('\n').append(body);
        return builder.toString();
    }

    public String showTransactions(List<Transaction> transactions) {
        return formatList(transactions, "Daftar Transaksi:", "- Data transaksi belum tersedia!");
    }

    public String showSearchResults(List<Transaction> transactions, String keyword) {
        return formatList(transactions, "Hasil Pencarian: \"" + keyword + "\"", "- Transaksi tidak ditemukan!");
    }

    public String showAddSuccess(Transaction transaction) {
        return "Berhasil menambah transaksi: " + format(transaction);
    }

    public String showUpdateSuccess() {
        return "Berhasil mengubah transaksi.";
    }

    public String showUpdateFailed(int id) {
        return String.format("[!] Gagal mengubah transaksi dengan ID: %d.", id);
    }

    public String showRemoveSuccess() {
        return "Berhasil menghapus transaksi.";
    }

    public String showRemoveFailed(int id) {
        return String.format("[!] Gagal menghapus transaksi dengan ID: %d.", id);
    }

    public String showValidationError(String message) {
        return "[!] " + message;
    }
}
