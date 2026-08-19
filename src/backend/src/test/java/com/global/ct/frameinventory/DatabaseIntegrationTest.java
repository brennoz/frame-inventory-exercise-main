package com.global.ct.frameinventory;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;

public abstract class DatabaseIntegrationTest {

    @ServiceConnection
    static final MariaDBContainer<?> MARIA_DB = startMariaDb();

    private static MariaDBContainer<?> startMariaDb() {
        MariaDBContainer<?> container = new MariaDBContainer<>("mariadb:11.4");
        container.start();
        return container;
    }
}
