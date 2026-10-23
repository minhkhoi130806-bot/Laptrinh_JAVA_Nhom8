-- =====================================================================
--  DỮ LIỆU MẪU - NHÀ HÀNG HÀN QUỐC (Laptrinh_JAVA_Nhom8)
--  Database: nhom8   |   MySQL 8+
--
--  CÁCH DÙNG
--   1. Chạy backend Spring Boot MỘT LẦN để Hibernate tự tạo bảng
--      (spring.jpa.hibernate.ddl-auto=update), rồi tắt backend.
--   2. Chạy file này:
--        mysql -u root -p nhom8 < sample_data.sql
--      hoặc mở trong MySQL Workbench / phpMyAdmin (tab Import) và chạy.
--   3. Chạy lại backend và mở http://localhost:3000/admin
--
--  LƯU Ý
--   - Script dùng ID cố định nên chỉ chạy được trên bảng RỖNG.
--     Muốn chạy lại, bỏ dấu "-- " ở khối "XÓA DỮ LIỆU CŨ" bên dưới.
--   - Ngày giờ dùng NOW() nên các lượt đặt bàn luôn ở tương lai.
--   - Mật khẩu nhân viên đang lưu dạng chữ thường (123456) vì backend
--     chưa mã hóa BCrypt. Khi thêm Spring Security, hãy đổi lại.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS nhom8 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nhom8;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
--  XÓA DỮ LIỆU CŨ (bỏ comment nếu muốn nạp lại từ đầu)
-- ---------------------------------------------------------------------
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE receipt_detail;
-- TRUNCATE TABLE purchase_receipt;
-- TRUNCATE TABLE recipe;
-- TRUNCATE TABLE invoice;
-- TRUNCATE TABLE order_item;
-- TRUNCATE TABLE orders;
-- TRUNCATE TABLE reservation;
-- TRUNCATE TABLE menu_item;
-- TRUNCATE TABLE category;
-- TRUNCATE TABLE ingredient;
-- TRUNCATE TABLE supplier;
-- TRUNCATE TABLE dining_table;
-- TRUNCATE TABLE customer;
-- TRUNCATE TABLE employee;
-- SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
--  1. NHÂN VIÊN  (role: MANAGER, CASHIER, WAITER, CHEF)
-- ---------------------------------------------------------------------
INSERT INTO employee (id, username, full_name, password, role, phone, salary) VALUES
(1, 'quanly',   'Nguyễn Văn Hùng',   '123456', 'MANAGER', '0901000001', 25000000.00),
(2, 'thungan1', 'Trần Thị Mai',      '123456', 'CASHIER', '0901000002', 10000000.00),
(3, 'phucvu1',  'Lê Minh Khôi',      '123456', 'WAITER',  '0901000003',  8000000.00),
(4, 'phucvu2',  'Phạm Thu Hà',       '123456', 'WAITER',  '0901000004',  8000000.00),
(5, 'phucvu3',  'Võ Quốc Bảo',       '123456', 'WAITER',  '0901000005',  7500000.00),
(6, 'bep1',     'Đặng Tấn Phúc',     '123456', 'CHEF',    '0901000006', 15000000.00),
(7, 'bep2',     'Mai Chí Cường',     '123456', 'CHEF',    '0901000007', 12000000.00);

-- ---------------------------------------------------------------------
--  2. KHÁCH HÀNG
-- ---------------------------------------------------------------------
INSERT INTO customer (id, full_name, phone, email, loyalty_points) VALUES
(1,  'Nguyễn Thị Lan',    '0912000001', 'lan.nguyen@example.com',   120),
(2,  'Trần Quốc Anh',     '0912000002', 'anh.tran@example.com',      80),
(3,  'Lê Hoàng Nam',      '0912000003', 'nam.le@example.com',        45),
(4,  'Phạm Ngọc Diệp',    '0912000004', 'diep.pham@example.com',     60),
(5,  'Hoàng Gia Huy',     '0912000005', 'huy.hoang@example.com',    210),
(6,  'Vũ Thanh Tùng',     '0912000006', NULL,                        30),
(7,  'Đỗ Mỹ Linh',        '0912000007', 'linh.do@example.com',       15),
(8,  'Bùi Anh Tuấn',      '0912000008', 'tuan.bui@example.com',      95),
(9,  'Ngô Thị Thảo',      '0912000009', 'thao.ngo@example.com',       0),
(10, 'Dương Minh Châu',   '0912000010', 'chau.duong@example.com',    25);

-- ---------------------------------------------------------------------
--  3. BÀN ĂN  (status: AVAILABLE, RESERVED, OCCUPIED)
-- ---------------------------------------------------------------------
INSERT INTO dining_table (id, table_code, capacity, zone, status) VALUES
(1,  'A01',  2, 'Tầng 1', 'AVAILABLE'),
(2,  'A02',  2, 'Tầng 1', 'AVAILABLE'),
(3,  'A03',  4, 'Tầng 1', 'AVAILABLE'),
(4,  'A04',  4, 'Tầng 1', 'OCCUPIED'),
(5,  'A05',  6, 'Tầng 1', 'AVAILABLE'),
(6,  'A06',  6, 'Tầng 1', 'OCCUPIED'),
(7,  'B01',  4, 'Tầng 2', 'AVAILABLE'),
(8,  'B02',  4, 'Tầng 2', 'AVAILABLE'),
(9,  'B03',  8, 'Tầng 2', 'OCCUPIED'),
(10, 'B04',  8, 'Tầng 2', 'RESERVED'),
(11, 'V01', 10, 'VIP',    'RESERVED'),
(12, 'V02', 12, 'VIP',    'AVAILABLE');

-- ---------------------------------------------------------------------
--  4. DANH MỤC MÓN
-- ---------------------------------------------------------------------
INSERT INTO category (id, name, description) VALUES
(1, 'Khai vị',       'Các món ăn nhẹ dùng trước bữa chính'),
(2, 'Món chính',     'Cơm, mì và các món nấu'),
(3, 'Nướng & Lẩu',   'Món nướng tại bàn và lẩu'),
(4, 'Đồ uống',       'Rượu, trà và nước giải khát'),
(5, 'Tráng miệng',   'Món ngọt cuối bữa');

-- ---------------------------------------------------------------------
--  5. THỰC ĐƠN  (spice_level: NONE, MILD, MEDIUM, HOT, EXTRA_HOT)
-- ---------------------------------------------------------------------
INSERT INTO menu_item (id, category_id, name_vi, name_ko, description, price, image_url, spice_level, available) VALUES
(1,  1, 'Kim chi cải thảo',        '김치',        'Cải thảo muối chua cay truyền thống',           45000.00, NULL, 'MEDIUM', 1),
(2,  1, 'Bánh xèo Hàn Quốc',       '파전',        'Bánh hành hải sản chiên giòn',                  95000.00, NULL, 'NONE',   1),
(3,  1, 'Gimbap truyền thống',     '김밥',        'Cơm cuộn rong biển nhân rau củ và trứng',       65000.00, NULL, 'NONE',   1),
(4,  1, 'Gà rán sốt cay',          '양념치킨',    'Gà rán giòn phủ sốt gochujang',                135000.00, NULL, 'HOT',    1),
(5,  2, 'Cơm trộn Bibimbap',       '비빔밥',      'Cơm trộn rau, thịt bò và trứng ốp la',          98000.00, NULL, 'MILD',   1),
(6,  2, 'Tokbokki phô mai',        '치즈떡볶이',  'Bánh gạo cay phủ phô mai kéo sợi',              85000.00, NULL, 'HOT',    1),
(7,  2, 'Mì lạnh Naengmyeon',      '냉면',        'Mì lạnh nước dùng thanh mát',                   92000.00, NULL, 'NONE',   1),
(8,  2, 'Canh kim chi',            '김치찌개',    'Canh kim chi nấu thịt ba chỉ',                  99000.00, NULL, 'HOT',    1),
(9,  2, 'Mì tương đen',            '짜장면',      'Mì sốt tương đen Jajangmyeon',                  89000.00, NULL, 'NONE',   0),
(10, 3, 'Ba chỉ nướng',            '삼겹살',      'Thịt ba chỉ heo nướng, ăn kèm rau cuốn',       249000.00, NULL, 'NONE',   1),
(11, 3, 'Bò nướng Bulgogi',        '불고기',      'Thịt bò ướp tẩm nướng than',                   285000.00, NULL, 'NONE',   1),
(12, 3, 'Lẩu quân đội',            '부대찌개',    'Lẩu xúc xích, mì và kim chi',                  399000.00, NULL, 'HOT',    1),
(13, 4, 'Soju truyền thống',       '소주',        'Rượu soju chai 360ml',                          85000.00, NULL, 'NONE',   1),
(14, 4, 'Trà đào',                 '복숭아차',    'Trà đào mát lạnh',                              45000.00, NULL, 'NONE',   1),
(15, 4, 'Nước ngọt',               '음료수',      'Coca, Sprite, Fanta',                           25000.00, NULL, 'NONE',   1),
(16, 5, 'Bingsu xoài',             '망고빙수',    'Đá bào sữa topping xoài tươi',                 119000.00, NULL, 'NONE',   1);

-- ---------------------------------------------------------------------
--  6. NGUYÊN LIỆU
-- ---------------------------------------------------------------------
INSERT INTO ingredient (id, name, unit, stock_qty, min_stock) VALUES
(1,  'Thịt ba chỉ heo',     'kg',   40.000, 10.000),
(2,  'Thịt bò',             'kg',   25.000,  8.000),
(3,  'Cải thảo',            'kg',   30.000, 10.000),
(4,  'Gạo',                 'kg',  100.000, 30.000),
(5,  'Bột ớt Gochugaru',    'kg',    8.000,  2.000),
(6,  'Tương ớt Gochujang',  'kg',   12.000,  3.000),
(7,  'Bánh gạo Tteok',      'kg',   20.000,  5.000),
(8,  'Phô mai Mozzarella',  'kg',    2.000,  3.000),
(9,  'Trứng gà',            'quả', 200.000, 50.000),
(10, 'Đùi gà',              'kg',   30.000, 10.000),
(11, 'Mì lạnh',             'kg',   15.000,  5.000),
(12, 'Rượu Soju',           'chai',120.000, 30.000);

-- ---------------------------------------------------------------------
--  7. CÔNG THỨC (món - nguyên liệu)
-- ---------------------------------------------------------------------
INSERT INTO recipe (menu_item_id, ingredient_id, quantity_needed) VALUES
(1,  3,  0.150), (1,  5,  0.010),
(4,  10, 0.500), (4,  6,  0.030),
(5,  4,  0.150), (5,  2,  0.050), (5,  9,  1.000), (5,  6,  0.020),
(6,  7,  0.200), (6,  6,  0.030), (6,  8,  0.080),
(7,  11, 0.200),
(8,  3,  0.150), (8,  1,  0.100), (8,  5,  0.010),
(10, 1,  0.250),
(11, 2,  0.200),
(13, 12, 1.000);

-- ---------------------------------------------------------------------
--  8. NHÀ CUNG CẤP
-- ---------------------------------------------------------------------
INSERT INTO supplier (id, name, phone, email, address) VALUES
(1, 'Công ty Thực phẩm Hàn Việt',        '0283000001', 'sales@hanviet.example.com',   '12 Nguyễn Huệ, Quận 1, TP.HCM'),
(2, 'Vựa Thịt Sạch Tân Bình',            '0283000002', 'thitsach@example.com',        '45 Cộng Hòa, Tân Bình, TP.HCM'),
(3, 'Nông sản Đà Lạt Xanh',              '0263000003', 'dalatxanh@example.com',       '8 Phan Đình Phùng, Đà Lạt'),
(4, 'Đại lý Rượu & Đồ uống Sài Gòn',     '0283000004', 'douong.sg@example.com',       '99 Lý Thường Kiệt, Quận 10, TP.HCM');

-- ---------------------------------------------------------------------
--  9. PHIẾU NHẬP KHO + CHI TIẾT
-- ---------------------------------------------------------------------
INSERT INTO purchase_receipt (id, supplier_id, employee_id, receipt_date, total_amount, note) VALUES
(1, 2, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), 13050000.00, 'Nhập thịt đầu tuần'),
(2, 3, 2, DATE_SUB(NOW(), INTERVAL 5 DAY),  3280000.00, 'Rau củ, gạo, trứng'),
(3, 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY),  7510000.00, 'Gia vị và nguyên liệu Hàn Quốc'),
(4, 4, 2, DATE_SUB(NOW(), INTERVAL 2 DAY),  5460000.00, 'Soju và mì lạnh');

INSERT INTO receipt_detail (receipt_id, ingredient_id, quantity, unit_price) VALUES
(1, 1,  30.000, 165000.00), (1, 2,  20.000, 320000.00), (1, 10, 20.000,  85000.00),
(2, 3,  40.000,  22000.00), (2, 4, 100.000,  18000.00), (2, 9, 200.000,   3000.00),
(3, 5,  10.000, 280000.00), (3, 6,  15.000,  95000.00), (3, 7,  25.000,  45000.00), (3, 8, 12.000, 180000.00),
(4, 12,120.000,  38000.00), (4, 11, 15.000,  60000.00);

-- ---------------------------------------------------------------------
--  10. ĐẶT BÀN  (status: PENDING, CONFIRMED, CANCELLED, COMPLETED)
-- ---------------------------------------------------------------------
INSERT INTO reservation (id, customer_id, table_id, reserved_at, guest_count, status) VALUES
(1, 10, 10, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:00:00'),  6, 'CONFIRMED'),
(2,  1, 11, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '18:30:00'), 10, 'PENDING'),
(3,  2,  5, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '19:30:00'),  5, 'CONFIRMED'),
(4,  3, 12, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 5 DAY), '18:00:00'), 12, 'PENDING'),
(5,  4,  3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '12:00:00'),  4, 'COMPLETED'),
(6,  5,  7, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '19:00:00'),  3, 'CANCELLED'),
(7,  6,  1, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:00:00'),  2, 'PENDING'),
(8,  7,  8, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 6 DAY), '20:00:00'),  4, 'CONFIRMED');

-- ---------------------------------------------------------------------
--  11. ĐƠN HÀNG  (status: OPEN, SERVING, PAID, CANCELLED)
--      customer_id có thể NULL = khách vãng lai
-- ---------------------------------------------------------------------
INSERT INTO orders (id, customer_id, table_id, employee_id, created_at, status) VALUES
(1,  1,    1, 3, DATE_SUB(NOW(), INTERVAL 72 HOUR),   'PAID'),
(2,  2,    3, 3, DATE_SUB(NOW(), INTERVAL 50 HOUR),   'PAID'),
(3,  3,    5, 4, DATE_SUB(NOW(), INTERVAL 49 HOUR),   'PAID'),
(4,  4,    2, 3, DATE_SUB(NOW(), INTERVAL 26 HOUR),   'PAID'),
(5,  5,    7, 4, DATE_SUB(NOW(), INTERVAL 24 HOUR),   'PAID'),
(6,  6,    8, 3, DATE_SUB(NOW(), INTERVAL 5 HOUR),    'PAID'),
(7,  7,    4, 4, DATE_SUB(NOW(), INTERVAL 1 HOUR),    'SERVING'),
(8,  8,    6, 3, DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'SERVING'),
(9,  NULL, 9, 4, DATE_SUB(NOW(), INTERVAL 10 MINUTE), 'OPEN'),
(10, 9,    2, 3, DATE_SUB(NOW(), INTERVAL 48 HOUR),   'CANCELLED');

-- ---------------------------------------------------------------------
--  12. CHI TIẾT ĐƠN HÀNG  (khóa chính kép: order_id + menu_item_id)
-- ---------------------------------------------------------------------
INSERT INTO order_item (order_id, menu_item_id, quantity, unit_price, note) VALUES
-- Đơn 1: 628.000
(1, 10, 2, 249000.00, 'Nướng vừa'),
(1, 13, 1,  85000.00, NULL),
(1,  1, 1,  45000.00, NULL),
-- Đơn 2: 514.000
(2, 12, 1, 399000.00, 'Cay vừa'),
(2,  3, 1,  65000.00, NULL),
(2, 15, 2,  25000.00, NULL),
-- Đơn 3: 838.000
(3, 11, 2, 285000.00, NULL),
(3,  5, 1,  98000.00, 'Không trứng'),
(3, 13, 2,  85000.00, NULL),
-- Đơn 4: 331.000
(4,  5, 2,  98000.00, NULL),
(4,  6, 1,  85000.00, 'Thêm phô mai'),
(4, 15, 2,  25000.00, NULL),
-- Đơn 5: 1.171.000
(5, 10, 3, 249000.00, NULL),
(5,  4, 1, 135000.00, NULL),
(5, 13, 2,  85000.00, NULL),
(5, 16, 1, 119000.00, NULL),
-- Đơn 6: 363.000
(6,  7, 2,  92000.00, NULL),
(6,  9, 1,  89000.00, NULL),
(6, 14, 2,  45000.00, 'Ít đá'),
-- Đơn 7 (đang phục vụ): 559.000
(7, 12, 1, 399000.00, 'Cay nhiều'),
(7,  6, 1,  85000.00, NULL),
(7, 15, 3,  25000.00, NULL),
-- Đơn 8 (đang phục vụ): 745.000
(8, 11, 2, 285000.00, NULL),
(8,  1, 2,  45000.00, NULL),
(8, 13, 1,  85000.00, NULL),
-- Đơn 9 (mới mở): 230.000
(9,  4, 1, 135000.00, NULL),
(9,  2, 1,  95000.00, NULL),
-- Đơn 10 (đã hủy)
(10, 8, 1,  99000.00, NULL);

-- ---------------------------------------------------------------------
--  13. HÓA ĐƠN  (chỉ cho các đơn đã thanh toán)
-- ---------------------------------------------------------------------
INSERT INTO invoice (id, order_id, subtotal, discount, total, payment_method, paid_at) VALUES
(1, 1,  628000.00,      0.00,  628000.00, 'CASH',     DATE_SUB(NOW(), INTERVAL 71 HOUR)),
(2, 2,  514000.00,  51400.00,  462600.00, 'CARD',     DATE_SUB(NOW(), INTERVAL 49 HOUR)),
(3, 3,  838000.00,      0.00,  838000.00, 'TRANSFER', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(4, 4,  331000.00,      0.00,  331000.00, 'MOMO',     DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(5, 5, 1171000.00, 100000.00, 1071000.00, 'CARD',     DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(6, 6,  363000.00,      0.00,  363000.00, 'CASH',     DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- ---------------------------------------------------------------------
--  KIỂM TRA NHANH
-- ---------------------------------------------------------------------
SELECT 'employee' AS bang, COUNT(*) AS so_dong FROM employee
UNION ALL SELECT 'customer',        COUNT(*) FROM customer
UNION ALL SELECT 'dining_table',    COUNT(*) FROM dining_table
UNION ALL SELECT 'category',        COUNT(*) FROM category
UNION ALL SELECT 'menu_item',       COUNT(*) FROM menu_item
UNION ALL SELECT 'ingredient',      COUNT(*) FROM ingredient
UNION ALL SELECT 'recipe',          COUNT(*) FROM recipe
UNION ALL SELECT 'supplier',        COUNT(*) FROM supplier
UNION ALL SELECT 'purchase_receipt',COUNT(*) FROM purchase_receipt
UNION ALL SELECT 'receipt_detail',  COUNT(*) FROM receipt_detail
UNION ALL SELECT 'reservation',     COUNT(*) FROM reservation
UNION ALL SELECT 'orders',          COUNT(*) FROM orders
UNION ALL SELECT 'order_item',      COUNT(*) FROM order_item
UNION ALL SELECT 'invoice',         COUNT(*) FROM invoice;
