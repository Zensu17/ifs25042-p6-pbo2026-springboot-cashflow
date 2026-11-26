package org.delcom.ifs25042springbootcashflow.domain.repository;

import org.delcom.ifs25042springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25042springbootcashflow.domain.exception.DomainException;

import java.util.List;

/**
 * Port layanan AI untuk menghasilkan daftar transaksi keuangan dari instruksi bebas.
 */
public interface IFinanceAiService {

    /**
     * Menghasilkan daftar transaksi dari prompt teks bebas menggunakan model AI.
     *
     * @param prompt instruksi pengguna
     * @return daftar transaksi hasil generasi
     * @throws DomainException jika terjadi kegagalan komunikasi AI atau parsing
     */
    List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException;
}
