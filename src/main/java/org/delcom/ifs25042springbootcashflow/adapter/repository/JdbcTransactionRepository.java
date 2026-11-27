package org.delcom.ifs25042springbootcashflow.adapter.repository;

import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25042springbootcashflow.domain.repository.ITransactionRepository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Repository transaksi keuangan berbasis JDBC (PostgreSQL).
 */
public class JdbcTransactionRepository implements ITransactionRepository {
    private static final String SELECT_COLUMNS =
            "SELECT id, user_id, description, amount, type, proof_path, created_at FROM transactions";
    private final JdbcHelper jdbc;

    public JdbcTransactionRepository(DataSource dataSource) {
        this.jdbc = new JdbcHelper(dataSource);
    }

    @Override
    public List<Transaction> findAll() {
        return jdbc.queryList(
                SELECT_COLUMNS + " ORDER BY created_at DESC, id DESC",
                JdbcTransactionRepository::mapRow,
                "Gagal membaca daftar transaksi"
        );
    }

    @Override
    public List<Transaction> findByUserId(int userId) {
        return jdbc.queryList(
                SELECT_COLUMNS + " WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                statement -> statement.setInt(1, userId),
                JdbcTransactionRepository::mapRow,
                "Gagal membaca transaksi pengguna"
        );
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        return jdbc.queryOptional(
                SELECT_COLUMNS + " WHERE id = ?",
                statement -> statement.setInt(1, id),
                JdbcTransactionRepository::mapRow,
                "Gagal membaca transaksi"
        );
    }

    @Override
    public Transaction save(Transaction entity) {
        return save(entity.getUserId(), entity.getDescription(), entity.getAmount(), entity.getType(), entity.getProofPath());
    }

    @Override
    public Transaction save(int userId, String description, long amount, TransactionType type, String proofPath) {
        String sql = """
                INSERT INTO transactions (user_id, description, amount, type, proof_path, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime createdAt = LocalDateTime.now();
        int id = jdbc.insertReturningId(
                sql,
                statement -> {
                    statement.setInt(1, userId);
                    statement.setString(2, description);
                    statement.setLong(3, amount);
                    statement.setString(4, type.name());
                    setNullableString(statement, 5, proofPath);
                    statement.setTimestamp(6, Timestamp.valueOf(createdAt));
                },
                "Gagal mendapatkan ID transaksi baru",
                "Gagal menyimpan transaksi"
        );
        return new Transaction(id, userId, description, amount, type, proofPath, createdAt);
    }

    @Override
    public boolean deleteById(Integer id) {
        return jdbc.executeUpdate(
                "DELETE FROM transactions WHERE id = ?",
                statement -> statement.setInt(1, id),
                "Gagal menghapus transaksi"
        ) > 0;
    }

    @Override
    public void update(Transaction entity) {
        String sql = """
                UPDATE transactions
                SET description = ?, amount = ?, type = ?, proof_path = ?
                WHERE id = ?
                """;
        jdbc.executeUpdate(
                sql,
                statement -> {
                    statement.setString(1, entity.getDescription());
                    statement.setLong(2, entity.getAmount());
                    statement.setString(3, entity.getType().name());
                    setNullableString(statement, 4, entity.getProofPath());
                    statement.setInt(5, entity.getId());
                },
                "Gagal mengubah transaksi"
        );
    }

    @Override
    public List<Transaction> findBy(Predicate<Transaction> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("description"),
                rs.getLong("amount"),
                TransactionType.valueOf(rs.getString("type")),
                rs.getString("proof_path"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
