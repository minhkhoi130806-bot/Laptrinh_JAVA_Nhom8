# HAN SEOUL - HỆ THỐNG QUẢN LÝ NHÀ HÀNG ĐỒ ĂN HÀN QUỐC
> **Đồ án môn học:** Lập trình Java  
> **Giảng viên hướng dẫn:** ThS. Nguyễn Minh Tuấn  
> **Nhóm thực hiện:** Nhóm 08 - Lớp CCQ2411D - Khoa Công nghệ Thông tin - Trường Cao đẳng Công Thương TP.HCM (HITC)

---

## 📌 THÀNH VIÊN NHÓM
1. **Ngô Minh Khôi** - MSSV: 2124110139 *(Trưởng nhóm)*
2. **Mai Chí Cường** - MSSV: 2124110113
3. **Nguyễn Lê Tấn Phúc** - MSSV: 2124110116

---

## 🛠 CÔNG NGHỆ SỬ DỤNG
* **Backend:** Java 17 LTS, Spring Boot 4.1.2 / 3.x, Spring Data JPA, Hibernate, Spring Security, JJWT 0.12.6, Spring Boot Starter Mail, SpringDoc OpenAPI 3.0.1 (Swagger UI).
* **Database:** MySQL 8.0+, Flyway Migration 11.14.1.
* **Frontend:** Next.js 16.4.1, React 19.3.0, Tailwind CSS v4.
* **Tích hợp bên thứ ba:** Gmail SMTP Server (cổng 587 TLS), Cổng thanh toán Sandbox: VNPay (HMAC-SHA512), ZaloPay (HMAC-SHA256), MoMo (HMAC-SHA256).
* **Kiểm thử tự động:** JUnit 5, Mockito (19/19 Unit Tests pass 100%), Postman Collection.

---

## 🚀 HƯỚNG DẪN KHỞI CHẠY HỆ THỐNG

### 1. Chuẩn bị Cơ sở dữ liệu (MySQL)
Tạo cơ sở dữ liệu `nhom8` với mã hóa UTF-8 và import dữ liệu mẫu:
```sql
CREATE DATABASE nhom8 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Import dữ liệu mẫu từ tệp `sample_data.sql` tại thư mục gốc:
```bash
mysql -u root -p nhom8 < sample_data.sql
```
*(Lưu ý: Flyway Migration `V2__operational_support_tables.sql` sẽ tự động kích hoạt khi Backend khởi động để đồng bộ cấu trúc bảng).*

---

### 2. Khởi chạy Backend (Spring Boot API Server)
Tại thư mục gốc dự án (`D:\Laptrinh_JAVA_Nhom8`):
```bash
# Sử dụng Maven Wrapper
./mvnw spring-boot:run
```
* **Backend API Base URL:** `http://localhost:8080`
* **Tài liệu tương tác Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **Kiểm tra sức khỏe hệ thống (Actuator):** `http://localhost:8080/actuator/health`

---

### 3. Khởi chạy Frontend (Next.js & Tailwind CSS)
Mở một cửa sổ Terminal mới và di chuyển vào thư mục `frontend`:
```bash
cd frontend
npm install
npm run dev
```
* **Cổng khách hàng (Client Portal):** `http://localhost:3000`
* **Cổng quản trị (Admin Dashboard):** `http://localhost:3000/admin`

---

### 4. Tài khoản thử nghiệm (Phân quyền 4 vai trò RBAC)
| Vai trò | Tên đăng nhập | Mật khẩu | Quyền hạn chính |
| :--- | :--- | :--- | :--- |
| **Quản lý (MANAGER)** | `quanly` | `123456` | Toàn quyền quản trị thực đơn, kho, nguyên liệu, nhân sự, báo cáo |
| **Thu ngân (CASHIER)** | `thungan1` | `123456` | Tiếp nhận đơn, áp dụng điểm tích lũy, thanh toán hóa đơn |
| **Phục vụ (WAITER)** | `phucvu1` | `123456` | Xem sơ đồ bàn, check-in đón khách, mở đơn và gọi món tại bàn |
| **Đầu bếp (CHEF)** | `bep1` | `123456` | Theo dõi màn hình Bếp KDS, xác nhận hoàn thành món |

---

## 🧪 KIỂM THỬ HỆ THỐNG
* **Chạy kiểm thử tự động (Unit Tests):**
  ```bash
  ./mvnw test
  ```
  *(Toàn bộ 19 bài test về chữ ký điện tử VNPay, ZaloPay, MoMo và luồng gửi Email bất đồng bộ đều đạt 100%).*
* **Kiểm thử API qua Postman:**
  Import tệp `postman/HanSeoul.postman_collection.json` vào Postman để kiểm thử toàn diện 17 nhóm RESTful API.
