package com.example.photomanager.storage.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FileUrlSchemaInitializer implements ApplicationRunner {
    private static final String SCHEMA = "photo_manager";
    private static final String TABLE = "file_url";

    private final JdbcTemplate jdbcTemplate;

    public FileUrlSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("original_filename", "ALTER TABLE file_url ADD COLUMN original_filename VARCHAR(255) NULL");
        ensureColumn("content_type", "ALTER TABLE file_url ADD COLUMN content_type VARCHAR(100) NULL");
        ensureColumn("file_size", "ALTER TABLE file_url ADD COLUMN file_size BIGINT NULL");
        ensureColumn("storage_filename", "ALTER TABLE file_url ADD COLUMN storage_filename VARCHAR(255) NULL");
        ensureColumn("title", "ALTER TABLE file_url ADD COLUMN title VARCHAR(255) NULL");
        ensureColumn("category", "ALTER TABLE file_url ADD COLUMN category VARCHAR(100) NULL");
        ensureColumn("tags", "ALTER TABLE file_url ADD COLUMN tags VARCHAR(500) NULL");
    }

    private void ensureColumn(String column, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND COLUMN_NAME=?",
                Integer.class,
                SCHEMA,
                TABLE,
                column
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
