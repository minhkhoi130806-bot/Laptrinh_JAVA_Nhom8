-- Bổ sung các bảng nghiệp vụ, không xóa hoặc đổi tên bảng cũ.
-- Dùng InnoDB để hỗ trợ transaction và khóa ngoại khi cần nối vào nghiệp vụ.
CREATE TABLE IF NOT EXISTS refresh_token (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(120) NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  replaced_by_hash CHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_refresh_token_username (username),
  INDEX idx_refresh_token_expiry (expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS login_attempt (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username_key VARCHAR(190) NOT NULL,
  ip_address VARCHAR(45) NOT NULL,
  succeeded BOOLEAN NOT NULL DEFAULT FALSE,
  attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_login_attempt_subject_time (username_key, attempted_at),
  INDEX idx_login_attempt_ip_time (ip_address, attempted_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  actor_username VARCHAR(120) NULL,
  actor_role VARCHAR(40) NULL,
  action VARCHAR(100) NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  resource_id VARCHAR(100) NULL,
  ip_address VARCHAR(45) NULL,
  details JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_actor_time (actor_username, created_at),
  INDEX idx_audit_resource (resource_type, resource_id),
  INDEX idx_audit_action_time (action, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS contact_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(190) NOT NULL,
  phone VARCHAR(25) NULL,
  subject VARCHAR(180) NOT NULL,
  message TEXT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'NEW',
  assigned_to VARCHAR(120) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_contact_status_time (status, created_at),
  INDEX idx_contact_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS event_inquiry (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  contact_name VARCHAR(100) NOT NULL,
  phone VARCHAR(25) NOT NULL,
  email VARCHAR(190) NULL,
  event_type VARCHAR(60) NOT NULL,
  event_at DATETIME NULL,
  guest_count INT NOT NULL,
  budget VARCHAR(60) NULL,
  notes TEXT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'NEW',
  quote_amount DECIMAL(14,2) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_event_status_date (status, event_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS voucher (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL,
  discount_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(12,2) NOT NULL,
  min_order_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  max_discount_amount DECIMAL(14,2) NULL,
  usage_limit INT NULL,
  used_count INT NOT NULL DEFAULT 0,
  starts_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_voucher_active_dates (active, starts_at, expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS voucher_usage (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  voucher_id BIGINT NOT NULL,
  customer_id BIGINT NULL,
  order_id BIGINT NULL,
  discount_amount DECIMAL(14,2) NOT NULL,
  used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_voucher_order (voucher_id, order_id),
  INDEX idx_voucher_usage_customer (customer_id, used_at),
  CONSTRAINT fk_voucher_usage_voucher FOREIGN KEY (voucher_id) REFERENCES voucher(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_transaction (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NULL,
  invoice_id BIGINT NULL,
  provider VARCHAR(30) NOT NULL,
  provider_transaction_id VARCHAR(160) NULL,
  idempotency_key VARCHAR(120) NOT NULL UNIQUE,
  amount DECIMAL(14,2) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'VND',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  request_payload JSON NULL,
  response_payload JSON NULL,
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_payment_provider_txn (provider, provider_transaction_id),
  INDEX idx_payment_status_time (status, created_at),
  INDEX idx_payment_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_refund (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  payment_transaction_id BIGINT NOT NULL,
  amount DECIMAL(14,2) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  provider_refund_id VARCHAR(160) NULL,
  requested_by VARCHAR(120) NOT NULL,
  approved_by VARCHAR(120) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_refund_status_time (status, created_at),
  CONSTRAINT fk_refund_payment FOREIGN KEY (payment_transaction_id) REFERENCES payment_transaction(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  recipient_username VARCHAR(120) NULL,
  recipient_customer_id BIGINT NULL,
  notification_type VARCHAR(40) NOT NULL,
  title VARCHAR(180) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  target_url VARCHAR(500) NULL,
  read_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notification_recipient (recipient_username, read_at, created_at),
  INDEX idx_notification_customer (recipient_customer_id, read_at, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS favorite (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_favorite_customer_menu (customer_id, menu_item_id),
  INDEX idx_favorite_menu (menu_item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS review (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  rating TINYINT NOT NULL,
  content VARCHAR(2000) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reply VARCHAR(2000) NULL,
  replied_by VARCHAR(120) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
  INDEX idx_review_menu_status (menu_item_id, status, created_at),
  INDEX idx_review_customer (customer_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS waitlist (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  phone VARCHAR(25) NOT NULL,
  guest_count INT NOT NULL,
  preferred_at DATETIME NOT NULL,
  zone VARCHAR(80) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'WAITING',
  notified_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_waitlist_status_time (status, preferred_at),
  INDEX idx_waitlist_phone (phone)
) ENGINE=InnoDB;
