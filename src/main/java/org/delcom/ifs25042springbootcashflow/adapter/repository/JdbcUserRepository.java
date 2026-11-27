package org.delcom.ifs25042springbootcashflow.adapter.repository;

import org.delcom.ifs25042springbootcashflow.domain.entity.Role;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.repository.IUserRepository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Repository pengguna berbasis JDBC (PostgreSQL).
 */
public class JdbcUserRepository implements IUserRepository {
    private static final String SELECT_COLUMNS = "SELECT id, username, password_hash, full_name, role, photo_path FROM users";
    private final JdbcHelper jdbc;

    public JdbcUserRepository(DataSource dataSource) {
        this.jdbc = new JdbcHelper(dataSource);
    }

    @Override
    public List<User> findAll() {
        return jdbc.queryList(SELECT_COLUMNS + " ORDER BY id", JdbcUserRepository::mapRow, "Gagal membaca daftar pengguna");
    }

    @Override
    public Optional<User> findById(Integer id) {
        return jdbc.queryOptional(
                SELECT_COLUMNS + " WHERE id = ?",
                statement -> statement.setInt(1, id),
                JdbcUserRepository::mapRow,
                "Gagal membaca pengguna"
        );
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jdbc.queryOptional(
                SELECT_COLUMNS + " WHERE LOWER(username) = LOWER(?)",
                statement -> statement.setString(1, username),
                JdbcUserRepository::mapRow,
                "Gagal mencari pengguna"
        );
    }

    @Override
    public User save(User entity) {
        return saveNew(entity.getUsername(), entity.getPasswordHash(), entity.getFullName(), entity.getRole());
    }

    @Override
    public User saveNew(String username, String passwordHash, String fullName, Role role) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, photo_path) VALUES (?, ?, ?, ?, NULL)";
        int id = jdbc.insertReturningId(
                sql,
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.setString(3, fullName);
                    statement.setString(4, role.name());
                },
                "Gagal mendapatkan ID pengguna baru",
                "Gagal menyimpan pengguna"
        );
        return new User(id, username, passwordHash, fullName, role, null);
    }

    @Override
    public boolean deleteById(Integer id) {
        return jdbc.executeUpdate(
                "DELETE FROM users WHERE id = ?",
                statement -> statement.setInt(1, id),
                "Gagal menghapus pengguna"
        ) > 0;
    }

    @Override
    public void update(User entity) {
        String sql = "UPDATE users SET password_hash = ?, full_name = ?, photo_path = ? WHERE id = ?";
        jdbc.executeUpdate(
                sql,
                statement -> {
                    statement.setString(1, entity.getPasswordHash());
                    statement.setString(2, entity.getFullName());
                    statement.setString(3, entity.getPhotoPath());
                    statement.setInt(4, entity.getId());
                },
                "Gagal mengubah pengguna"
        );
    }

    @Override
    public List<User> findBy(Predicate<User> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                Role.valueOf(rs.getString("role")),
                rs.getString("photo_path")
        );
    }
}
