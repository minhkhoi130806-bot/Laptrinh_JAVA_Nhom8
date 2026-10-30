package com.restaurant.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    // Khoa chinh cua don hang
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Khach hang tao don
    @Column(name = "customer_id")
    private Integer customerId;

    // Ban duoc su dung
    @Column(name = "table_id")
    private Integer tableId;

    // Nhan vien phu trach don
    @Column(name = "employee_id")
    private Integer employeeId;

    // Thoi gian tao don
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Thông tin liên hệ và giao nhận cho đơn đặt món từ website khách hàng.
    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "order_type", length = 20)
    private String orderType;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "customer_note", length = 1000)
    private String customerNote;

    @Column(name = "payment_preference", length = 30)
    private String paymentPreference;

    // Trạng thái đơn hàng
    @Column(name = "status")
    private String status;

    public Order() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getTableId() {
        return tableId;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { this.customerName = value; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String value) { this.customerPhone = value; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String value) { this.customerEmail = value; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String value) { this.orderType = value; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String value) { this.deliveryAddress = value; }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String value) { this.customerNote = value; }
    public String getPaymentPreference() { return paymentPreference; }
    public void setPaymentPreference(String value) { this.paymentPreference = value; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}