package org.delcom.ifs25042springbootcashflow.adapter;

import org.delcom.ifs25042springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25042springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25042springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25042springbootcashflow.domain.entity.Role;
import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcRepositoryTest {

    private DataSource createH2DataSource(String dbName) {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        SchemaInitializer.initialize(ds);
        return ds;
    }

    @Test
    void jdbcUserAndTransactionRepositoriesPerformFullCrud() {
        DataSource ds = createH2DataSource("cashflow_crud_test");
        JdbcUserRepository userRepo = new JdbcUserRepository(ds);
        JdbcTransactionRepository txRepo = new JdbcTransactionRepository(ds);

        User saved = userRepo.save(new User(0, "rina", "hash1", "Rina", Role.USER, null));
        assertThat(saved.getId()).isPositive();
        assertThat(userRepo.findAll()).hasSize(1);
        assertThat(userRepo.findById(saved.getId())).isPresent();
        assertThat(userRepo.findById(9999)).isEmpty();
        assertThat(userRepo.findByUsername("RINA")).isPresent();
        assertThat(userRepo.findByUsername("nobody")).isEmpty();
        assertThat(userRepo.findBy(u -> u.getRole() == Role.USER)).hasSize(1);

        saved.changeFullName("Rina Updated");
        saved.changePhotoPath("profiles/rina.png");
        userRepo.update(saved);
        assertThat(userRepo.findById(saved.getId())).get()
                .satisfies(u -> {
                    assertThat(u.getFullName()).isEqualTo("Rina Updated");
                    assertThat(u.getPhotoPath()).isEqualTo("profiles/rina.png");
                });

        Transaction t1 = txRepo.save(new Transaction(0, saved.getId(), "Gaji", 2_000_000L, TransactionType.INCOME));
        Transaction t2 = txRepo.save(saved.getId(), "Internet", 300_000L, TransactionType.EXPENSE, "proofs/net.png");

        assertThat(txRepo.findAll()).hasSize(2);
        assertThat(txRepo.findByUserId(saved.getId())).hasSize(2);
        assertThat(txRepo.findById(t1.getId())).isPresent();
        assertThat(txRepo.findById(9999)).isEmpty();
        assertThat(txRepo.findBy(Transaction::isExpense)).hasSize(1);

        t1.changeDescription("Gaji Utama");
        t1.changeAmount(2_500_000L);
        t1.changeType(TransactionType.INCOME);
        t1.changeProofPath("proofs/slip.png");
        txRepo.update(t1);
        assertThat(txRepo.findById(t1.getId())).get()
                .satisfies(t -> assertThat(t.getProofPath()).isEqualTo("proofs/slip.png"));

        t2.changeProofPath(null);
        txRepo.update(t2);
        assertThat(txRepo.findById(t2.getId())).get()
                .satisfies(t -> assertThat(t.getProofPath()).isNull());

        assertThat(txRepo.deleteById(t1.getId())).isTrue();
        assertThat(txRepo.deleteById(t1.getId())).isFalse();
        assertThat(txRepo.deleteById(t2.getId())).isTrue();

        assertThat(userRepo.deleteById(saved.getId())).isTrue();
        assertThat(userRepo.deleteById(saved.getId())).isFalse();
    }

    @Test
    void jdbcRepositoriesAndInitializerWrapSqlExceptionsAndMissingKeys() throws SQLException {
        DataSource failingDs = mock(DataSource.class);
        when(failingDs.getConnection()).thenThrow(new SQLException("db down"));

        assertThatThrownBy(() -> SchemaInitializer.initialize(failingDs))
                .isInstanceOf(IllegalStateException.class);

        JdbcUserRepository failingUserRepo = new JdbcUserRepository(failingDs);
        User dummyUser = new User(1, "u", "p", "n", Role.USER, null);
        assertThatThrownBy(failingUserRepo::findAll).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingUserRepo.findById(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingUserRepo.findByUsername("u")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingUserRepo.save(dummyUser)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingUserRepo.deleteById(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingUserRepo.update(dummyUser)).isInstanceOf(IllegalStateException.class);

        JdbcTransactionRepository failingTxRepo = new JdbcTransactionRepository(failingDs);
        Transaction dummyTx = new Transaction(1, 1, "d", 100L, TransactionType.INCOME);
        assertThatThrownBy(failingTxRepo::findAll).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingTxRepo.findByUserId(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingTxRepo.findById(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingTxRepo.save(dummyTx)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingTxRepo.deleteById(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failingTxRepo.update(dummyTx)).isInstanceOf(IllegalStateException.class);

        DataSource noKeysDs = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet emptyKeys = mock(ResultSet.class);
        when(noKeysDs.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString(), anyInt())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.getGeneratedKeys()).thenReturn(emptyKeys);
        when(emptyKeys.next()).thenReturn(false);

        JdbcUserRepository noKeyUserRepo = new JdbcUserRepository(noKeysDs);
        assertThatThrownBy(() -> noKeyUserRepo.saveNew("a", "b", "c", Role.USER))
                .isInstanceOf(IllegalStateException.class);

        JdbcTransactionRepository noKeyTxRepo = new JdbcTransactionRepository(noKeysDs);
        assertThatThrownBy(() -> noKeyTxRepo.save(1, "desc", 100L, TransactionType.EXPENSE, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
