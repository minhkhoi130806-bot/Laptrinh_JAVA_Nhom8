-- Shared operational registers and reference settings; existing data is retained.
CREATE TABLE IF NOT EXISTS operation_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  record_type VARCHAR(32) NOT NULL,
  singleton_key VARCHAR(50) NULL UNIQUE,
  fields_json TEXT NOT NULL,
  done BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_operation_type_created (record_type, created_at)
) ENGINE=InnoDB;
