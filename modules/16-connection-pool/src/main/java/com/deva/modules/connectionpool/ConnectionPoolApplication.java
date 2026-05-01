package com.deva.modules.connectionpool;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zaxxer.hikari.HikariDataSource;

@SpringBootApplication
public class ConnectionPoolApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConnectionPoolApplication.class, args);
    }
}

@RestController
class PoolController {
    private final JdbcTemplate jdbcTemplate;
    private final HikariDataSource hikariDataSource;

    PoolController(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.hikariDataSource = (HikariDataSource) dataSource;
    }

    @GetMapping("/api/query")
    Map<String, Object> query(@RequestParam(defaultValue = "0") long holdMs) throws InterruptedException {
        Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
        if (holdMs > 0) {
            Thread.sleep(Math.min(holdMs, 1_000));
        }
        return Map.of("value", value, "holdMs", holdMs);
    }

    @GetMapping("/api/pool")
    Map<String, Object> pool() {
        return Map.of(
                "maxPoolSize", hikariDataSource.getMaximumPoolSize(),
                "connectionTimeoutMs", hikariDataSource.getConnectionTimeout(),
                "metric", "watch active/idle/pending connections in production");
    }
}

