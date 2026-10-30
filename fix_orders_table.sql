USE nhom8;

-- 1. Bổ sung các cột còn thiếu vào bảng orders
ALTER TABLE orders 
ADD COLUMN customer_name VARCHAR(100) NULL,
ADD COLUMN customer_phone VARCHAR(20) NULL,
ADD COLUMN customer_email VARCHAR(150) NULL,
ADD COLUMN order_type VARCHAR(20) NULL DEFAULT 'DINE_IN',
ADD COLUMN delivery_address VARCHAR(500) NULL,
ADD COLUMN customer_note VARCHAR(1000) NULL,
ADD COLUMN payment_preference VARCHAR(30) NULL;

-- 2. Đảm bảo bảng payment_transaction đã tồn tại
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
