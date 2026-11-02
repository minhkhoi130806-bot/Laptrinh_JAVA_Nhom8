package com.restaurant.backend.service;

import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.entity.Menuitem;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.service.Impl.EmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailServiceImplTest {

    static class RecordingMailSender extends JavaMailSenderImpl {
        final List<MimeMessage> sentMessages = new ArrayList<>();

        @Override
        public void send(MimeMessage mimeMessage) {
            sentMessages.add(mimeMessage);
        }
    }

    private MenuItemRepository createFakeMenuItemRepo() {
        return (MenuItemRepository) Proxy.newProxyInstance(
                MenuItemRepository.class.getClassLoader(),
                new Class<?>[]{MenuItemRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName()) && args != null && args.length > 0) {
                        Long id = (Long) args[0];
                        Menuitem mi = new Menuitem();
                        mi.setId(id);
                        mi.setNameVi("Thịt nướng Samgyeopsal");
                        return Optional.of(mi);
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Bỏ qua gửi email khi mailEnabled = false")
    void shouldSkipWhenMailDisabled() {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailServiceImpl emailService = new EmailServiceImpl(createFakeMenuItemRepo());
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        Order order = new Order();
        order.setId(100);
        Invoice invoice = new Invoice();

        assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(order, invoice, List.of()));
        assertEquals(0, mailSender.sentMessages.size());
    }

    @Test
    @DisplayName("Không bắn lỗi khi thiếu cấu hình mail credentials")
    void shouldNotThrowWhenCredentialsMissing() {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailServiceImpl emailService = new EmailServiceImpl(createFakeMenuItemRepo());
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "fromEmail", "");
        ReflectionTestUtils.setField(emailService, "mailPassword", "");

        Order order = new Order();
        order.setId(101);
        order.setCustomerEmail("customer@test.com");
        Invoice invoice = new Invoice();

        assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(order, invoice, List.of()));
        assertEquals(0, mailSender.sentMessages.size());
    }

    @Test
    @DisplayName("Gửi email thành công cho cả khách hàng và quản trị viên khi đã cấu hình")
    void shouldSendEmailToCustomerAndAdmin() {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailServiceImpl emailService = new EmailServiceImpl(createFakeMenuItemRepo());

        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "restaurant@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "someAppPassword123");
        ReflectionTestUtils.setField(emailService, "adminEmail", "admin@gmail.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Han Seoul");

        Order order = new Order();
        order.setId(102);
        order.setCustomerName("Nguyen Van A");
        order.setCustomerPhone("0912345678");
        order.setCustomerEmail("customer@gmail.com");
        order.setOrderType("DELIVERY");
        order.setDeliveryAddress("123 Nguyen Hue, Quan 1, TP HCM");
        order.setCustomerNote("Ít cay, không hành");

        Invoice invoice = new Invoice();
        invoice.setId(50);
        invoice.setOrderId(102);
        invoice.setPaymentMethod("ZALOPAY");
        invoice.setPaidAt(LocalDateTime.of(2026, 11, 2, 12, 30));
        invoice.setSubtotal(BigDecimal.valueOf(250000));
        invoice.setDiscount(BigDecimal.valueOf(25000));
        invoice.setTotal(BigDecimal.valueOf(225000));

        OrderItem item = new OrderItem();
        OrderItemId itemId = new OrderItemId(102, 1);
        item.setId(itemId);
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(125000));
        item.setNote("Giao thêm sốt chấm");

        assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(order, invoice, List.of(item)));

        // Đã gửi tới 2 người: khách hàng (customer@gmail.com) và admin (admin@gmail.com)
        assertEquals(2, mailSender.sentMessages.size());
    }

    @Test
    @DisplayName("Gửi email thành công cho cả khách hàng và NHIỀU quản trị viên qua dấu phẩy")
    void shouldSendToMultipleAdminEmails() {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailServiceImpl emailService = new EmailServiceImpl(createFakeMenuItemRepo());

        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "restaurant@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "someAppPassword123");
        ReflectionTestUtils.setField(emailService, "adminEmail", "minhkhoi130806@gmail.com, second_admin@gmail.com, third@gmail.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Han Seoul");

        Order order = new Order();
        order.setId(103);
        order.setCustomerEmail("customer@gmail.com");
        Invoice invoice = new Invoice();
        invoice.setId(51);

        assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(order, invoice, List.of()));

        // Đã gửi tới 4 người: khách hàng + 3 admin
        assertEquals(4, mailSender.sentMessages.size());
    }

    @Test
    @DisplayName("Gửi email chào mừng khi khách hàng mới đăng ký tài khoản")
    void shouldSendWelcomeEmailOnCustomerRegister() {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailServiceImpl emailService = new EmailServiceImpl(createFakeMenuItemRepo());

        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "minhkhoi130806@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "someAppPassword123");
        ReflectionTestUtils.setField(emailService, "adminEmail", "minhkhoi130806@gmail.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Han Seoul");

        com.restaurant.backend.entity.Customer customer = new com.restaurant.backend.entity.Customer();
        customer.setId(10L);
        customer.setFullName("Nguyen Thi Mai");
        customer.setPhone("0987654321");
        customer.setEmail("mainguyen@gmail.com");
        customer.setLoyaltyPoints(0);

        assertDoesNotThrow(() -> emailService.sendCustomerWelcomeEmail(customer));

        // Đã gửi 2 email: 1 thư chào mừng cho khách (mainguyen@gmail.com) và 1 thông báo cho admin (minhkhoi130806@gmail.com)
        assertEquals(2, mailSender.sentMessages.size());
    }
}
