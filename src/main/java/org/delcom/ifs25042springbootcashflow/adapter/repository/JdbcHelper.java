package org.delcom.ifs25042springbootcashflow.adapter.repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper JDBC bersama untuk mengurangi boilerplate koneksi dan penanganan SQLException pada repository.
 */
final class JdbcHelper {
    private final DataSource dataSource;

    JdbcHelper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @FunctionalInterface
    interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    <T> List<T> queryList(String sql, RowMapper<T> mapper, String errorMessage) {
        return queryList(sql, statement -> {
        }, mapper, errorMessage);
    }

    <T> List<T> queryList(String sql, SqlBinder binder, RowMapper<T> mapper, String errorMessage) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    <T> Optional<T> queryOptional(String sql, SqlBinder binder, RowMapper<T> mapper, String errorMessage) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    int insertReturningId(String sql, SqlBinder binder, String missingKeyMessage, String errorMessage) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException(missingKeyMessage);
                }
                return keys.getInt(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    int executeUpdate(String sql, SqlBinder binder, String errorMessage) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
    }
}
