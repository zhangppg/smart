CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(64) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `rule_code` VARCHAR(64) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_rule_user_rule_code` (`user_id`, `rule_code`),
  CONSTRAINT `fk_user_rule_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `file_url` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `photo_id` VARCHAR(64) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `file_url` VARCHAR(512) NOT NULL,
  `original_filename` VARCHAR(255) NULL,
  `content_type` VARCHAR(100) NULL,
  `file_size` BIGINT NULL,
  `storage_filename` VARCHAR(255) NULL,
  `title` VARCHAR(255) NULL,
  `category` VARCHAR(100) NULL,
  `tags` VARCHAR(500) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_url_photo_id` (`photo_id`),
  KEY `idx_file_url_user_id` (`user_id`),
  CONSTRAINT `fk_file_url_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
