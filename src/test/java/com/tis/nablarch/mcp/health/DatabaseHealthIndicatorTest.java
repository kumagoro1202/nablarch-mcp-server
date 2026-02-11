package com.tis.nablarch.mcp.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Test
    void DB接続成功の場合UPを返す() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/nablarch_mcp");

        var indicator = new DatabaseHealthIndicator(dataSource);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("PostgreSQL", health.getDetails().get("database"));
    }

    @Test
    void DB接続失敗の場合DOWNを返す() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        var indicator = new DatabaseHealthIndicator(dataSource);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("DB接続不可", health.getDetails().get("reason"));
    }

    @Test
    void DB接続は成功するがvalidation失敗の場合DOWNを返す() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(false);

        var indicator = new DatabaseHealthIndicator(dataSource);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }
}
