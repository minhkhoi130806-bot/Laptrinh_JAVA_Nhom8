package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.entity.Menuitem;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Triển khai dịch vụ gửi email thông báo thanh toán thành công qua Gmail SMTP.
 * Chạy bất đồng bộ (@Async) để không làm chậm luồng xử lý thanh toán và hóa đơn.
 */
@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final Locale VI_LOCALE = new Locale("vi", "VN");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final MenuItemRepository menuItemRepository;
    private final CustomerRepository customerRepository;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.admin-email:}")
    private String adminEmail;

    @Value("${app.mail.from-name:Nhà hàng Hàn Quốc Han Seoul}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailServiceImpl(MenuItemRepository menuItemRepository) {
        this(menuItemRepository, null);
    }

    public EmailServiceImpl(MenuItemRepository menuItemRepository, CustomerRepository customerRepository) {
        this.menuItemRepository = menuItemRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Async
    public void sendPaymentSuccessEmail(Order order, Invoice invoice, List<OrderItem> items) {
        if (!mailEnabled) {
            log.info("Tính năng gửi email đang bị tắt (app.mail.enabled=false). Bỏ qua gửi email đơn #{}", order.getId());
            return;
        }

        if (mailSender == null || fromEmail == null || fromEmail.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.warn("Gmail SMTP chưa được cấu hình (thiếu MAIL_USERNAME hoặc MAIL_PASSWORD trong application.properties/env). "
                    + "Đơn hàng #{} đã thanh toán thành công nhưng chưa thể gửi email thực tế.", order.getId());
            return;
        }

        // Nếu đơn hàng chưa có email khách, tự tra cứu từ hồ sơ hội viên theo số điện thoại
        if ((order.getCustomerEmail() == null || order.getCustomerEmail().isBlank())
                && order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()
                && customerRepository != null) {
            try {
                customerRepository.findByPhone(order.getCustomerPhone().trim())
                        .ifPresent(c -> {
                            if (c.getEmail() != null && !c.getEmail().isBlank()) {
                                order.setCustomerEmail(c.getEmail().trim());
                            }
                        });
            } catch (Exception ex) {
                log.debug("Tra cứu email hội viên theo số điện thoại không thành công: {}", ex.getMessage());
            }
        }

        // Tập hợp danh sách người nhận (email của khách + các email quản trị viên nếu có)
        Set<String> recipients = new LinkedHashSet<>();
        if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank() && isValidEmail(order.getCustomerEmail())) {
            recipients.add(order.getCustomerEmail().trim());
        }
        if (adminEmail != null && !adminEmail.isBlank()) {
            for (String e : adminEmail.split("[,;]")) {
                String trimmed = e.trim();
                if (isValidEmail(trimmed)) {
                    recipients.add(trimmed);
                }
            }
        }

        if (recipients.isEmpty()) {
            log.info("Đơn hàng #{} không có email khách hàng hoặc admin để gửi thông báo", order.getId());
            return;
        }

        String subject = "[Han Seoul] Xác nhận thanh toán thành công đơn hàng #" + order.getId();
        String htmlContent = buildHtmlContent(order, invoice, items);

        for (String recipient : recipients) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

                helper.setFrom(fromEmail, fromName);
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("ĐÃ GỬI EMAIL xác nhận thanh toán đơn #{} tới địa chỉ: {}", order.getId(), recipient);
            } catch (Exception e) {
                log.error("Không thể gửi email cho đơn #{} tới {}: {}", order.getId(), recipient, e.getMessage());
            }
        }
    }

    private String buildHtmlContent(Order order, Invoice invoice, List<OrderItem> items) {
        String paymentLabel = mapPaymentMethod(invoice.getPaymentMethod());
        String orderTypeLabel = mapOrderType(order.getOrderType());
        String paidTime = invoice.getPaidAt() != null ? invoice.getPaidAt().format(DATE_FORMAT) : LocalDateTime.now().format(DATE_FORMAT);

        // Tạo danh sách các dòng món ăn
        StringBuilder itemRows = new StringBuilder();
        if (items != null) {
            for (OrderItem item : items) {
                String itemName = "Món ăn #" + (item.getId() != null ? item.getId().getMenuItemId() : "");
                if (item.getId() != null && item.getId().getMenuItemId() != null) {
                    Optional<Menuitem> m = menuItemRepository.findById(Long.valueOf(item.getId().getMenuItemId()));
                    if (m.isPresent()) {
                        Menuitem mi = m.get();
                        if (mi.getNameVi() != null && !mi.getNameVi().isBlank()) {
                            itemName = mi.getNameVi();
                        } else if (mi.getNameKo() != null && !mi.getNameKo().isBlank()) {
                            itemName = mi.getNameKo();
                        }
                    }
                }
                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                String noteHtml = (item.getNote() != null && !item.getNote().isBlank())
                        ? "<br/><small style='color: #718096; font-style: italic;'>Ghi chú: " + escapeHtml(item.getNote()) + "</small>"
                        : "";

                itemRows.append("<tr>")
                        .append("<td style='padding: 12px 10px; border-bottom: 1px solid #edf2f7; color: #2d3748;'>")
                        .append("<strong>").append(escapeHtml(itemName)).append("</strong>").append(noteHtml)
                        .append("</td>")
                        .append("<td style='padding: 12px 10px; border-bottom: 1px solid #edf2f7; text-align: center; color: #4a5568;'>")
                        .append(quantity)
                        .append("</td>")
                        .append("<td style='padding: 12px 10px; border-bottom: 1px solid #edf2f7; text-align: right; color: #4a5568;'>")
                        .append(formatVnd(unitPrice))
                        .append("</td>")
                        .append("<td style='padding: 12px 10px; border-bottom: 1px solid #edf2f7; text-align: right; color: #2d3748; font-weight: 600;'>")
                        .append(formatVnd(lineTotal))
                        .append("</td>")
                        .append("</tr>");
            }
        }

        String deliveryAddressHtml = "";
        if ("DELIVERY".equalsIgnoreCase(order.getOrderType()) && order.getDeliveryAddress() != null && !order.getDeliveryAddress().isBlank()) {
            deliveryAddressHtml = "<tr><td style='padding: 6px 0; color: #718096;'>Địa chỉ giao:</td><td style='padding: 6px 0; font-weight: 600; color: #2d3748;'>" + escapeHtml(order.getDeliveryAddress()) + "</td></tr>";
        }

        String customerNoteHtml = "";
        if (order.getCustomerNote() != null && !order.getCustomerNote().isBlank()) {
            customerNoteHtml = "<tr><td style='padding: 6px 0; color: #718096;'>Ghi chú đơn:</td><td style='padding: 6px 0; color: #e53e3e; font-style: italic;'>" + escapeHtml(order.getCustomerNote()) + "</td></tr>";
        }

        String discountHtml = "";
        if (invoice.getDiscount() != null && invoice.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discountHtml = "<tr><td style='padding: 6px 0; color: #718096;'>Giảm giá:</td><td style='padding: 6px 0; text-align: right; color: #e53e3e; font-weight: 600;'>- " + formatVnd(invoice.getDiscount()) + "</td></tr>";
        }

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Hóa đơn thanh toán Han Seoul</title>
            </head>
            <body style="margin: 0; padding: 20px 0; background-color: #f7fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6;">
              <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.06); border: 1px solid #e2e8f0;">
                
                <!-- HEADER -->
                <div style="background: linear-gradient(135deg, #742a2a 0%%, #9b2c2c 100%%); padding: 28px 24px; text-align: center; color: #ffffff;">
                  <h1 style="margin: 0; font-size: 22px; letter-spacing: 1px; text-transform: uppercase;">HAN SEOUL RESTAURANT</h1>
                  <p style="margin: 6px 0 0; font-size: 14px; opacity: 0.9;">Ẩm Thực Truyền Thống Hàn Quốc</p>
                </div>

                <!-- BODY -->
                <div style="padding: 28px 24px;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <span style="display: inline-block; background-color: #c6f6d5; color: #22543d; font-weight: 700; font-size: 13px; padding: 6px 14px; border-radius: 20px; text-transform: uppercase;">
                      ✓ Thanh toán thành công
                    </span>
                    <h2 style="margin: 14px 0 6px; font-size: 20px; color: #1a202c;">HÓA ĐƠN ĐƠN HÀNG #%d</h2>
                    <p style="margin: 0; color: #718096; font-size: 14px;">Thời gian: %s</p>
                  </div>

                  <!-- THÔNG TIN KHÁCH & GIAO HÀNG -->
                  <div style="background-color: #f7fafc; border-radius: 8px; padding: 16px; margin-bottom: 24px; border: 1px solid #edf2f7; font-size: 14px;">
                    <table style="width: 100%%; border-collapse: collapse;">
                      <tr><td style="width: 120px; padding: 6px 0; color: #718096;">Người nhận:</td><td style="padding: 6px 0; font-weight: 600; color: #2d3748;">%s</td></tr>
                      <tr><td style="padding: 6px 0; color: #718096;">Số điện thoại:</td><td style="padding: 6px 0; font-weight: 600; color: #2d3748;">%s</td></tr>
                      <tr><td style="padding: 6px 0; color: #718096;">Hình thức:</td><td style="padding: 6px 0; font-weight: 600; color: #2d3748;">%s</td></tr>
                      %s
                      <tr><td style="padding: 6px 0; color: #718096;">Phương thức:</td><td style="padding: 6px 0; font-weight: 600; color: #9b2c2c;">%s</td></tr>
                      %s
                    </table>
                  </div>

                  <!-- CHI TIẾT MÓN ĂN -->
                  <h3 style="margin: 0 0 12px; font-size: 16px; color: #2d3748; border-bottom: 2px solid #edf2f7; padding-bottom: 8px;">Chi tiết món đã đặt</h3>
                  <table style="width: 100%%; border-collapse: collapse; font-size: 14px; margin-bottom: 20px;">
                    <thead>
                      <tr style="background-color: #edf2f7; color: #4a5568; font-size: 13px;">
                        <th style="padding: 10px; text-align: left; border-radius: 4px 0 0 4px;">MÓN ĂN</th>
                        <th style="padding: 10px; text-align: center; width: 60px;">SL</th>
                        <th style="padding: 10px; text-align: right; width: 100px;">ĐƠN GIÁ</th>
                        <th style="padding: 10px; text-align: right; width: 110px; border-radius: 0 4px 4px 0;">THÀNH TIỀN</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>

                  <!-- TỔNG CỘNG -->
                  <div style="background-color: #fffaf0; border: 1px solid #feebc8; border-radius: 8px; padding: 16px; font-size: 14px; margin-bottom: 24px;">
                    <table style="width: 100%%; border-collapse: collapse;">
                      <tr><td style="padding: 6px 0; color: #718096;">Tạm tính:</td><td style="padding: 6px 0; text-align: right; font-weight: 600; color: #4a5568;">%s</td></tr>
                      %s
                      <tr style="border-top: 1px dashed #cbd5e0; font-size: 16px;">
                        <td style="padding: 12px 0 4px; font-weight: 700; color: #1a202c;">TỔNG THANH TOÁN:</td>
                        <td style="padding: 12px 0 4px; text-align: right; font-weight: 700; color: #9b2c2c; font-size: 18px;">%s</td>
                      </tr>
                    </table>
                  </div>

                  <!-- LỜI CẢM ƠN -->
                  <div style="text-align: center; font-size: 14px; color: #4a5568; padding-top: 10px; border-top: 1px solid #edf2f7;">
                    <p style="margin: 0 0 6px;">Cảm ơn quý khách đã tin tưởng và thưởng thức món ăn tại <strong>Han Seoul</strong>!</p>
                    <p style="margin: 0; color: #718096; font-size: 13px;">Bếp nhà hàng đang chuẩn bị món ăn để giao đến quý khách sớm nhất.</p>
                  </div>
                </div>

                <!-- FOOTER -->
                <div style="background-color: #2d3748; padding: 18px 24px; text-align: center; color: #a0aec0; font-size: 12px;">
                  <p style="margin: 0 0 4px; color: #edf2f7; font-weight: 600;">NHÀ HÀNG HÀN QUỐC HAN SEOUL</p>
                  <p style="margin: 0 0 4px;">Hotline hỗ trợ: 090 123 4567 · Email: support@hanseoul.vn</p>
                  <p style="margin: 0;">Địa chỉ: TP. Hồ Chí Minh, Việt Nam</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                order.getId(),
                paidTime,
                escapeHtml(order.getCustomerName() != null ? order.getCustomerName() : "Quý khách"),
                escapeHtml(order.getCustomerPhone() != null ? order.getCustomerPhone() : "—"),
                orderTypeLabel,
                deliveryAddressHtml,
                paymentLabel,
                customerNoteHtml,
                itemRows.toString(),
                formatVnd(invoice.getSubtotal()),
                discountHtml,
                formatVnd(invoice.getTotal())
        );
    }

    private static String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return NumberFormat.getInstance(VI_LOCALE).format(amount) + " ₫";
    }

    private static String mapPaymentMethod(String method) {
        if (method == null) return "Trực tuyến";
        return switch (method.toUpperCase()) {
            case "ZALOPAY" -> "Ví ZaloPay";
            case "VNPAY" -> "Cổng thanh toán VNPay";
            case "MOMO" -> "Ví điện tử MoMo";
            case "BANKING", "TRANSFER" -> "Chuyển khoản ngân hàng";
            case "CASH" -> "Tiền mặt";
            default -> method;
        };
    }

    private static String mapOrderType(String type) {
        if (type == null) return "Tại nhà hàng";
        return switch (type.toUpperCase()) {
            case "TAKEAWAY" -> "Đến lấy tại quán";
            case "DELIVERY" -> "Giao hàng tận nơi";
            case "DINE_IN" -> "Dùng tại quán";
            default -> type;
        };
    }

    @Override
    public String getDefaultAdminEmail() {
        return adminEmail != null ? adminEmail.trim() : "";
    }

    @Override
    public Map<String, Object> testSendEmail(String toEmail) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!mailEnabled) {
            result.put("success", false);
            result.put("message", "Tính năng gửi email đang bị tắt (app.mail.enabled=false).");
            return result;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            result.put("success", false);
            result.put("message", "Chưa cấu hình spring.mail.username (hoặc biến MAIL_USERNAME) trong application.properties.");
            return result;
        }
        if (mailPassword == null || mailPassword.isBlank()) {
            result.put("success", false);
            result.put("message", "Chưa cấu hình spring.mail.password (hoặc biến MAIL_PASSWORD) trong application.properties. Bạn cần tạo Mật khẩu ứng dụng 16 ký tự tại https://myaccount.google.com/apppasswords");
            return result;
        }
        List<String> targetList = new ArrayList<>();
        if (toEmail != null) {
            for (String t : toEmail.split("[,;]")) {
                String trimmed = t.trim();
                if (isValidEmail(trimmed)) {
                    targetList.add(trimmed);
                }
            }
        }
        if (targetList.isEmpty()) {
            result.put("success", false);
            result.put("message", "Địa chỉ email nhận không hợp lệ: " + toEmail);
            return result;
        }

        try {
            if (mailSender == null) {
                result.put("success", false);
                result.put("message", "mailSender chưa được khởi tạo. Hãy đảm bảo Spring Boot Mail Starter đã được nạp.");
                return result;
            }
            for (String recipient : targetList) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
                helper.setFrom(fromEmail, fromName);
                helper.setTo(recipient);
                helper.setSubject("[Han Seoul] Thử nghiệm gửi email hệ thống");
                helper.setText("<h2>Xin chào!</h2><p>Hệ thống gửi email của nhà hàng Hàn Quốc <strong>Han Seoul</strong> đã hoạt động chính xác.</p><p>Thời gian thử nghiệm: " + LocalDateTime.now().format(DATE_FORMAT) + "</p>", true);

                mailSender.send(message);
            }
            result.put("success", true);
            result.put("message", "Đã gửi email thử nghiệm thành công tới: " + String.join(", ", targetList) + "! Hãy kiểm tra Hộp thư đến (Inbox) hoặc Thư rác (Spam).");
            result.put("from", fromEmail);
            result.put("to", targetList);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thử nghiệm: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("535")) {
                result.put("hint", "Lỗi xác thực Google (535): Bạn đang dùng mật khẩu Gmail thông thường. Bắt buộc phải tạo 'Mật khẩu ứng dụng' (16 ký tự) tại https://myaccount.google.com/apppasswords");
            } else if (e.getMessage() != null && (e.getMessage().contains("Connection timed out") || e.getMessage().contains("ConnectException"))) {
                result.put("hint", "Không thể kết nối tới smtp.gmail.com:587. Kiểm tra kết nối Internet hoặc tường lửa chặn cổng 587.");
            }
        }
        return result;
    }

    @Override
    @Async
    public void sendCustomerWelcomeEmail(Customer customer) {
        if (!mailEnabled || customer == null) {
            return;
        }

        if (mailSender == null || fromEmail == null || fromEmail.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.warn("Gmail SMTP chưa được cấu hình. Bỏ qua gửi email chào mừng hội viên {}.", customer.getFullName());
            return;
        }

        String customerEmail = customer.getEmail();
        if (customerEmail == null || customerEmail.isBlank() || !isValidEmail(customerEmail)) {
            log.info("Khách hàng {} không có email hợp lệ để gửi thư chào mừng", customer.getFullName());
            return;
        }

        String welcomeSubject = "[Han Seoul] Chào mừng bạn gia nhập thành viên Nhà hàng Hàn Quốc Han Seoul!";
        String welcomeHtml = buildWelcomeHtmlContent(customer);

        // 1. Gửi email chào mừng tới khách hàng
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail, fromName);
            helper.setTo(customerEmail.trim());
            helper.setSubject(welcomeSubject);
            helper.setText(welcomeHtml, true);
            mailSender.send(message);
            log.info("ĐÃ GỬI EMAIL CHÀO MỪNG tới khách hàng: {} ({})", customer.getFullName(), customerEmail);
        } catch (Exception e) {
            log.error("Không thể gửi email chào mừng tới {}: {}", customerEmail, e.getMessage());
        }

        // 2. Gửi thông báo cho quản trị viên nhà hàng
        if (adminEmail != null && !adminEmail.isBlank()) {
            String adminSubject = "[Han Seoul] Khách hàng mới đăng ký tài khoản: " + customer.getFullName();
            String adminNotificationHtml = buildAdminCustomerNotificationHtml(customer);
            for (String adm : adminEmail.split("[,;]")) {
                String trimmed = adm.trim();
                if (isValidEmail(trimmed)) {
                    try {
                        MimeMessage admMsg = mailSender.createMimeMessage();
                        MimeMessageHelper admHelper = new MimeMessageHelper(admMsg, StandardCharsets.UTF_8.name());
                        admHelper.setFrom(fromEmail, fromName);
                        admHelper.setTo(trimmed);
                        admHelper.setSubject(adminSubject);
                        admHelper.setText(adminNotificationHtml, true);
                        mailSender.send(admMsg);
                        log.info("ĐÃ GỬI THÔNG BÁO hội viên mới tới admin: {}", trimmed);
                    } catch (Exception e) {
                        log.error("Không thể gửi thông báo admin tới {}: {}", trimmed, e.getMessage());
                    }
                }
            }
        }
    }

    private String buildWelcomeHtmlContent(Customer customer) {
        String name = customer.getFullName() != null ? escapeHtml(customer.getFullName()) : "Quý khách";
        String phone = customer.getPhone() != null ? escapeHtml(customer.getPhone()) : "—";
        String email = customer.getEmail() != null ? escapeHtml(customer.getEmail()) : "—";
        int points = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;

        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Chào mừng thành viên Han Seoul</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f7fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #2d3748; line-height: 1.6;">
              <div style="max-width: 620px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05); border: 1px solid #edf2f7;">
                
                <!-- Header -->
                <div style="background: linear-gradient(135deg, #8B2500 0%, #b83a1b 100%); color: #ffffff; padding: 32px 24px; text-align: center;">
                  <span style="display: inline-block; background: rgba(255,255,255,0.2); padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; margin-bottom: 8px;">
                    HAN SEOUL MEMBERSHIP
                  </span>
                  <h1 style="margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">CHÀO MỪNG BẠN GIA NHẬP!</h1>
                  <p style="margin: 8px 0 0; font-size: 14px; opacity: 0.9;">Nhà hàng Ẩm thực Hàn Quốc Han Seoul xin chào quý khách</p>
                </div>

                <!-- Body -->
                <div style="padding: 28px 24px;">
                  <p style="font-size: 16px; margin: 0 0 16px; color: #1a202c;">
                    Xin chào <strong>{{NAME}}</strong>,
                  </p>
                  <p style="font-size: 14px; color: #4a5568; margin: 0 0 20px;">
                    Cảm ơn bạn đã đăng ký tài khoản thành viên tại <strong>Han Seoul</strong>. Từ nay, bạn sẽ nhận được các đặc quyền hấp dẫn cùng trải nghiệm ẩm thực chuẩn vị Hàn Quốc:
                  </p>

                  <!-- Đặc quyền -->
                  <div style="background-color: #fffaf0; border-left: 4px solid #dd6b20; padding: 14px 16px; border-radius: 4px; margin-bottom: 24px;">
                    <ul style="margin: 0; padding-left: 18px; font-size: 13.5px; color: #7b341e;">
                      <li style="margin-bottom: 6px;"><strong>Tích điểm tự động:</strong> Mỗi 10.000 VNĐ thanh toán được tích 1 điểm thưởng.</li>
                      <li style="margin-bottom: 6px;"><strong>Hóa đơn điện tử qua Gmail:</strong> Tự động nhận hóa đơn chi tiết mỗi khi bạn thanh toán (ZaloPay, VNPay, MoMo, Tiền mặt).</li>
                      <li><strong>Ưu đãi thành viên:</strong> Nhận quà tặng và voucher giảm giá trong dịp sinh nhật & lễ hội ẩm thực.</li>
                    </ul>
                  </div>

                  <!-- Thông tin tài khoản -->
                  <h3 style="font-size: 14px; text-transform: uppercase; letter-spacing: 1px; color: #718096; margin: 0 0 12px; border-bottom: 2px solid #edf2f7; padding-bottom: 6px;">
                    Thông tin tài khoản thành viên
                  </h3>
                  <table style="width: 100%; font-size: 14px; margin-bottom: 24px; border-collapse: collapse;">
                    <tr>
                      <td style="padding: 6px 0; color: #718096; width: 35%;">Họ và tên:</td>
                      <td style="padding: 6px 0; font-weight: 600; color: #2d3748;">{{NAME}}</td>
                    </tr>
                    <tr>
                      <td style="padding: 6px 0; color: #718096;">Số điện thoại:</td>
                      <td style="padding: 6px 0; font-weight: 600; color: #2d3748;">{{PHONE}}</td>
                    </tr>
                    <tr>
                      <td style="padding: 6px 0; color: #718096;">Email nhận tin:</td>
                      <td style="padding: 6px 0; font-weight: 600; color: #2d3748;">{{EMAIL}}</td>
                    </tr>
                    <tr>
                      <td style="padding: 6px 0; color: #718096;">Điểm khởi đầu:</td>
                      <td style="padding: 6px 0; font-weight: 700; color: #c53030;">{{POINTS}} điểm</td>
                    </tr>
                  </table>

                  <!-- CTA Button -->
                  <div style="text-align: center; margin: 30px 0 15px;">
                    <a href="http://localhost:3000/thuc-don" style="background-color: #8B2500; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-size: 14px; font-weight: 600; display: inline-block;">
                      Khám phá thực đơn & Đặt bàn ngay &rarr;
                    </a>
                  </div>
                </div>

                <!-- Footer -->
                <div style="background-color: #edf2f7; padding: 18px 24px; text-align: center; font-size: 12px; color: #718096;">
                  <p style="margin: 0 0 4px; font-weight: 600; color: #4a5568;">Nhà hàng Hàn Quốc Han Seoul</p>
                  <p style="margin: 0 0 4px;">Ẩm thực chuẩn vị truyền thống Hàn Quốc</p>
                  <p style="margin: 0; font-size: 11px; color: #a0aec0;">Email này được gửi tự động từ hệ thống thành viên Han Seoul.</p>
                </div>

              </div>
            </body>
            </html>
            """;

        return template.replace("{{NAME}}", name)
                .replace("{{PHONE}}", phone)
                .replace("{{EMAIL}}", email)
                .replace("{{POINTS}}", String.valueOf(points));
    }

    private String buildAdminCustomerNotificationHtml(Customer customer) {
        String name = customer.getFullName() != null ? escapeHtml(customer.getFullName()) : "Khách hàng";
        String phone = customer.getPhone() != null ? escapeHtml(customer.getPhone()) : "—";
        String email = customer.getEmail() != null ? escapeHtml(customer.getEmail()) : "—";
        String time = LocalDateTime.now().format(DATE_FORMAT);

        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #2d3748; padding: 20px;">
              <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; background: #ffffff;">
                <h3 style="color: #8B2500; margin-top: 0;">[THÔNG BÁO] Khách hàng mới đăng ký thành viên</h3>
                <p>Hệ thống vừa ghi nhận một khách hàng mới đăng ký tài khoản thành công:</p>
                <ul style="line-height: 1.8;">
                  <li><strong>Họ tên:</strong> {{NAME}}</li>
                  <li><strong>Số điện thoại:</strong> {{PHONE}}</li>
                  <li><strong>Email:</strong> {{EMAIL}}</li>
                  <li><strong>Thời gian:</strong> {{TIME}}</li>
                </ul>
                <p style="font-size: 12px; color: #718096;">Thông báo tự động từ hệ thống quản trị Han Seoul.</p>
              </div>
            </body>
            </html>
            """;

        return template.replace("{{NAME}}", name)
                .replace("{{PHONE}}", phone)
                .replace("{{EMAIL}}", email)
                .replace("{{TIME}}", time);
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
