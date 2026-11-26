package org.delcom.ifs25042springbootcashflow.domain.entity;

import java.util.Comparator;

/**
 * Kriteria pengurutan transaksi keuangan.
 */
public enum SortOption {
    NEWEST_FIRST(Comparator
            .comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Transaction::getId)
            .reversed()),

    OLDEST_FIRST(Comparator
            .comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Transaction::getId)),

    AMOUNT_DESC(Comparator
            .comparingLong(Transaction::getAmount)
            .reversed()
            .thenComparing(Transaction::getId, Comparator.reverseOrder())),

    AMOUNT_ASC(Comparator
            .comparingLong(Transaction::getAmount)
            .thenComparing(Transaction::getId)),

    DESCRIPTION_ASC(Comparator
            .comparing(Transaction::getDescription, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Transaction::getId)),

    DESCRIPTION_DESC(Comparator
            .comparing(Transaction::getDescription, String.CASE_INSENSITIVE_ORDER)
            .reversed()
            .thenComparing(Transaction::getId, Comparator.reverseOrder()));

    private final Comparator<Transaction> comparator;

    SortOption(Comparator<Transaction> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Transaction> comparator() {
        return comparator;
    }

    public String label() {
        return switch (this) {
            case NEWEST_FIRST -> "Terbaru";
            case OLDEST_FIRST -> "Terlama";
            case AMOUNT_DESC -> "Jumlah terbesar";
            case AMOUNT_ASC -> "Jumlah terkecil";
            case DESCRIPTION_ASC -> "Keterangan A-Z";
            case DESCRIPTION_DESC -> "Keterangan Z-A";
        };
    }
}
