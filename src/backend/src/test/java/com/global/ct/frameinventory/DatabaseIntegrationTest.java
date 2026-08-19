package com.global.ct.frameinventory;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class DatabaseIntegrationTest {

    @Container
    @ServiceConnection
    static final MariaDBContainer<?> MARIA_DB = new MariaDBContainer<>("mariadb:11.4");
}
