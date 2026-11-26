package org.delcom.ifs25042springbootcashflow.domain.entity;

/**
 * Jenis transaksi keuangan: Pemasukan atau Pengeluaran.
 */
public enum TransactionType {
    INCOME,
    EXPENSE;

    public String label() {
        return switch (this) {
            case INCOME -> "Pemasukan";
            case EXPENSE -> "Pengeluaran";
        };
    }
}
