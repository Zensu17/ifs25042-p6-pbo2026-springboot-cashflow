package org.delcom.ifs25042springbootcashflow.usecase;

import org.delcom.ifs25042springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25042springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25042springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Use case yang menangani logika bisnis pencatatan keuangan (Cashflow).
 * Tidak melakukan I/O UI — hanya memproses aturan bisnis dan melempar checked exception domain.
 */
public class FinanceUseCase {
    private static final String MSG_AMOUNT_POSITIVE = "Jumlah transaksi harus lebih dari 0";
    private static final String MSG_DESC_REQUIRED = "Keterangan transaksi tidak boleh kosong";

    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Mengambil semua transaksi milik satu pengguna, terbaru di atas. */
    public List<Transaction> getTransactionsByUser(int userId) {
        return listTransactions(userId, null, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST);
    }

    /**
     * Mengambil transaksi pengguna dengan pencarian keterangan, filter tipe, dan urutan.
     */
    public List<Transaction> listTransactions(
            int userId,
            String keyword,
            TransactionTypeFilter typeFilter,
            SortOption sortOption
    ) {
        TransactionTypeFilter filter = typeFilter == null ? TransactionTypeFilter.ALL : typeFilter;
        SortOption sort = sortOption == null ? SortOption.NEWEST_FIRST : sortOption;

        Stream<Transaction> stream = transactionRepository.findByUserId(userId).stream()
                .filter(filter::matches);

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.trim().toLowerCase();
            stream = stream.filter(tx -> tx.getDescription().toLowerCase().contains(lowerKeyword));
        }

        return stream.sorted(sort.comparator()).toList();
    }

    /** Mengambil semua transaksi yang tersedia (admin). */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /** Mengurai dan memvalidasi teks jumlah nominal transaksi. */
    public long parseAmount(String rawAmount) throws ValidationException {
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            throw new ValidationException("Jumlah transaksi tidak boleh kosong");
        }
        String trimmed = rawAmount.trim();
        long parsed;
        try {
            parsed = Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Jumlah transaksi harus berupa angka");
        }
        if (parsed <= 0) {
            throw new ValidationException(MSG_AMOUNT_POSITIVE);
        }
        return parsed;
    }

    public Transaction addIncome(int userId, String description, long amount, String proofPath)
            throws ValidationException {
        return addTransaction(userId, description, amount, TransactionType.INCOME, proofPath);
    }

    public Transaction addExpense(int userId, String description, long amount, String proofPath)
            throws ValidationException {
        return addTransaction(userId, description, amount, TransactionType.EXPENSE, proofPath);
    }

    public Transaction addTransaction(
            int userId,
            String description,
            long amount,
            TransactionType type,
            String proofPath
    ) throws ValidationException {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException(MSG_DESC_REQUIRED);
        }
        if (amount <= 0) {
            throw new ValidationException(MSG_AMOUNT_POSITIVE);
        }
        if (type == null) {
            throw new ValidationException("Tipe transaksi tidak boleh kosong");
        }
        return transactionRepository.save(userId, description.trim(), amount, type, proofPath);
    }

    public void removeTransaction(int id) throws EntityNotFoundException, ValidationException {
        if (id <= 0) {
            throw new ValidationException("ID transaksi tidak valid");
        }
        boolean deleted = transactionRepository.deleteById(id);
        if (!deleted) {
            throw new EntityNotFoundException(id);
        }
    }

    public void removeTransactionForUser(int userId, int id)
            throws EntityNotFoundException, ValidationException {
        Transaction tx = requireOwnedTransaction(userId, id);
        transactionRepository.deleteById(tx.getId());
    }

    public void updateTransactionForUser(
            int userId,
            int id,
            String description,
            Long amount,
            TransactionType type,
            String proofPath,
            boolean clearProof
    ) throws EntityNotFoundException, ValidationException {
        Transaction tx = requireOwnedTransaction(userId, id);
        applyUpdates(tx, description, amount, type, proofPath, clearProof);
        transactionRepository.update(tx);
    }

    public long getTotalIncome(int userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(Transaction::isIncome)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getTotalExpense(int userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(Transaction::isExpense)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getBalance(int userId) {
        return getTotalIncome(userId) - getTotalExpense(userId);
    }

    public List<Transaction> searchTransactions(int userId, String keyword) {
        return listTransactions(userId, keyword, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST);
    }

    public List<Transaction> sortTransactions(int userId, SortOption option) {
        return listTransactions(userId, null, TransactionTypeFilter.ALL, option);
    }

    private Transaction requireOwnedTransaction(int userId, int id)
            throws EntityNotFoundException, ValidationException {
        if (id <= 0) {
            throw new ValidationException("ID transaksi tidak valid");
        }
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException(id);
        }
        Transaction tx = found.get();
        if (tx.getUserId() != userId) {
            throw new ValidationException("Transaksi tidak milik pengguna ini");
        }
        return tx;
    }

    private void applyUpdates(
            Transaction tx,
            String description,
            Long amount,
            TransactionType type,
            String proofPath,
            boolean clearProof
    ) throws ValidationException {
        if (description != null) {
            if (description.trim().isEmpty()) {
                throw new ValidationException(MSG_DESC_REQUIRED);
            }
            tx.changeDescription(description.trim());
        }
        if (amount != null) {
            if (amount <= 0) {
                throw new ValidationException(MSG_AMOUNT_POSITIVE);
            }
            tx.changeAmount(amount);
        }
        if (type != null) {
            tx.changeType(type);
        }
        if (clearProof) {
            tx.changeProofPath(null);
        } else if (proofPath != null) {
            tx.changeProofPath(proofPath);
        }
    }
}
