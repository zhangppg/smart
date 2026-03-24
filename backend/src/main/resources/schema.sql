CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password_hash` VARCHAR(64) NOT NULL COMMENT '密码哈希',
  `role_code` INT NOT NULL DEFAULT 0 COMMENT '角色代码',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则代码',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_rule_user_rule_code` (`user_id`, `rule_code`),
  CONSTRAINT `fk_user_rule_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `file_url` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `photo_id` VARCHAR(64) NOT NULL COMMENT '照片业务ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `file_url` VARCHAR(512) NOT NULL COMMENT '文件URL',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `original_filename` VARCHAR(255) NULL COMMENT '原始文件名',
  `content_type` VARCHAR(100) NULL COMMENT '内容类型',
  `file_size` BIGINT NULL COMMENT '文件大小(字节)',
  `storage_filename` VARCHAR(255) NULL COMMENT '存储文件名',
  `title` VARCHAR(255) NULL COMMENT '标题',
  `category` VARCHAR(100) NULL COMMENT '分类',
  `tags` VARCHAR(500) NULL COMMENT '标签(逗号分隔)',
  `source_file_url` VARCHAR(512) NULL COMMENT '源文件URL',
  `source_content_type` VARCHAR(100) NULL COMMENT '源内容类型',
  `source_file_size` BIGINT NULL COMMENT '源文件大小(字节)',
  `source_storage_filename` VARCHAR(255) NULL COMMENT '源存储文件名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_url_photo_id` (`photo_id`),
  KEY `idx_file_url_user_id` (`user_id`),
  CONSTRAINT `fk_file_url_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ensure column comments are applied even when tables already exist.
ALTER TABLE `user`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  MODIFY COLUMN `password_hash` VARCHAR(64) NOT NULL COMMENT '密码哈希',
  MODIFY COLUMN `role_code` INT NOT NULL DEFAULT 0 COMMENT '角色代码',
  MODIFY COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE `user_rule`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `rule_code` VARCHAR(64) NOT NULL COMMENT '规则代码',
  MODIFY COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE `file_url`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `photo_id` VARCHAR(64) NOT NULL COMMENT '照片业务ID',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `file_url` VARCHAR(512) NOT NULL COMMENT '文件URL',
  MODIFY COLUMN `original_filename` VARCHAR(255) NULL COMMENT '原始文件名',
  MODIFY COLUMN `content_type` VARCHAR(100) NULL COMMENT '内容类型',
  MODIFY COLUMN `file_size` BIGINT NULL COMMENT '文件大小(字节)',
  MODIFY COLUMN `storage_filename` VARCHAR(255) NULL COMMENT '存储文件名',
  MODIFY COLUMN `title` VARCHAR(255) NULL COMMENT '标题',
  MODIFY COLUMN `category` VARCHAR(100) NULL COMMENT '分类',
  MODIFY COLUMN `tags` VARCHAR(500) NULL COMMENT '标签(逗号分隔)',
  MODIFY COLUMN `source_file_url` VARCHAR(512) NULL COMMENT '源文件URL',
  MODIFY COLUMN `source_content_type` VARCHAR(100) NULL COMMENT '源内容类型',
  MODIFY COLUMN `source_file_size` BIGINT NULL COMMENT '源文件大小(字节)',
  MODIFY COLUMN `source_storage_filename` VARCHAR(255) NULL COMMENT '源存储文件名',
  MODIFY COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- Ensure indexes/constraints exist even when tables already exist.
SET @idx_user_id_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'file_url'
    AND INDEX_NAME = 'idx_file_url_user_id'
);
SET @sql := IF(@idx_user_id_exists = 0,
  'CREATE INDEX `idx_file_url_user_id` ON `file_url` (`user_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uk_photo_id_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'file_url'
    AND INDEX_NAME = 'uk_file_url_photo_id'
);
SET @sql := IF(@uk_photo_id_exists = 0,
  'ALTER TABLE `file_url` ADD UNIQUE KEY `uk_file_url_photo_id` (`photo_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_user_id_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'file_url'
    AND CONSTRAINT_NAME = 'fk_file_url_user_id'
);
SET @sql := IF(@fk_user_id_exists = 0,
  'ALTER TABLE `file_url` ADD CONSTRAINT `fk_file_url_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
