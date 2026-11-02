package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.Reservation;
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

    @Autowired
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

        // Kiểm tra xem đơn hàng có phải thanh toán tại quầy / dùng tại chỗ không
        String paymentMethod = (invoice != null && invoice.getPaymentMethod() != null)
                ? invoice.getPaymentMethod().toUpperCase()
                : (order.getPaymentPreference() != null ? order.getPaymentPreference().toUpperCase() : "");
        String orderType = order.getOrderType() != null ? order.getOrderType().toUpperCase() : "";

        boolean isCounterCashOrDineIn = "CASH".equals(paymentMethod) || "DINE_IN".equals(orderType);
        boolean isWalkInGuest = (order.getCustomerId() == null || order.getCustomerId() <= 0)
                && (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank());

        // Quy định: Không gửi email cho khách thanh toán tại quầy, dùng tại chỗ, hoặc khách vãng lai không đăng ký tài khoản
        if (isCounterCashOrDineIn || isWalkInGuest) {
            log.info("Đơn hàng #{} thanh toán tại quầy/dùng tại chỗ hoặc khách vãng lai ({} - {}). Bỏ qua gửi email theo cấu hình.",
                    order.getId(), paymentMethod, orderType);
            return;
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
            <body style="margin: 0; padding: 20px 0; background-color: #f7fafc; font-family: 'Segoe UI', Arial, Tahoma, Helvetica, sans-serif; line-height: 1.6;">
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
            <body style="margin: 0; padding: 0; background-color: #f7fafc; font-family: 'Segoe UI', Arial, Tahoma, Helvetica, sans-serif; color: #2d3748; line-height: 1.6;">
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
            <body style="font-family: 'Segoe UI', Arial, Tahoma, Helvetica, sans-serif; color: #2d3748; padding: 20px; line-height: 1.6;">
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

    @Override
    @Async
    public void sendReservationConfirmationEmail(Reservation reservation) {
        if (!mailEnabled) {
            log.info("Tính năng gửi email đang bị tắt (app.mail.enabled=false). Bỏ qua gửi email đặt bàn #{}", reservation.getId());
            return;
        }

        if (mailSender == null || fromEmail == null || fromEmail.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.warn("Gmail SMTP chưa được cấu hình. Lượt đặt bàn #{} đã tạo thành công nhưng chưa thể gửi email thực tế.", reservation.getId());
            return;
        }

        Customer customer = reservation.getCustomer();
        String customerEmail = customer != null ? customer.getEmail() : null;
        String customerName = customer != null && customer.getFullName() != null ? customer.getFullName() : "Quý khách";
        String customerPhone = customer != null && customer.getPhone() != null ? customer.getPhone() : "—";
        DiningTable table = reservation.getTable();
        String tableCode = table != null && table.getTableCode() != null ? table.getTableCode() : "Bàn đã chọn";
        String zone = table != null && table.getZone() != null ? table.getZone() : "Khu vực chung";
        Integer capacity = table != null ? table.getCapacity() : (reservation.getGuestCount() != null ? reservation.getGuestCount() : 2);
        String formattedTime = reservation.getReservedAt() != null ? reservation.getReservedAt().format(DATE_FORMAT) : "Theo thỏa thuận";
        int guestCount = reservation.getGuestCount() != null ? reservation.getGuestCount() : 1;
        String note = reservation.getNote() != null && !reservation.getNote().isBlank() ? reservation.getNote().trim() : "Không có";

        // Gửi email xác nhận chi tiết tới Gmail của khách hàng
        if (customerEmail != null && !customerEmail.isBlank() && isValidEmail(customerEmail.trim())) {
            String trimmedEmail = customerEmail.trim();
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, StandardCharsets.UTF_8.name());
                helper.setFrom(fromEmail, fromName);
                helper.setTo(trimmedEmail);
                helper.setSubject("[Han Seoul] Xác nhận đặt bàn thành công #" + reservation.getId() + " - " + tableCode);
                helper.setText(buildReservationConfirmationHtml(reservation.getId(), customerName, customerPhone, tableCode, zone, capacity, formattedTime, guestCount, note), true);
                mailSender.send(msg);
                log.info("ĐÃ GỬI EMAIL XÁC NHẬN ĐẶT BÀN #{} tới khách hàng: {} ({})", reservation.getId(), customerName, trimmedEmail);
            } catch (Exception ex) {
                log.error("Lỗi gửi email xác nhận đặt bàn #{} tới khách {}: {}", reservation.getId(), trimmedEmail, ex.getMessage());
            }
        } else {
            log.info("Lượt đặt bàn #{} không có email khách hàng hợp lệ, bỏ qua gửi email cho khách.", reservation.getId());
        }

        // Đồng thời gửi thông báo tới email quản trị viên nhà hàng
        if (adminEmail != null && !adminEmail.isBlank()) {
            String adminSubject = "[Han Seoul] Khách đặt bàn mới #" + reservation.getId() + " - " + customerName + " (" + formattedTime + ")";
            String adminHtml = buildAdminReservationNotificationHtml(reservation.getId(), customerName, customerPhone, customerEmail, tableCode, zone, formattedTime, guestCount, note);
            for (String adm : adminEmail.split("[,;]")) {
                String trimmed = adm.trim();
                if (isValidEmail(trimmed)) {
                    try {
                        MimeMessage admMsg = mailSender.createMimeMessage();
                        MimeMessageHelper admHelper = new MimeMessageHelper(admMsg, StandardCharsets.UTF_8.name());
                        admHelper.setFrom(fromEmail, fromName);
                        admHelper.setTo(trimmed);
                        admHelper.setSubject(adminSubject);
                        admHelper.setText(adminHtml, true);
                        mailSender.send(admMsg);
                        log.info("ĐÃ GỬI THÔNG BÁO đặt bàn mới #{} tới admin: {}", reservation.getId(), trimmed);
                    } catch (Exception e) {
                        log.error("Không thể gửi thông báo đặt bàn admin tới {}: {}", trimmed, e.getMessage());
                    }
                }
            }
        }
    }

    private String buildReservationConfirmationHtml(Long resId, String name, String phone, String tableCode, String zone, Integer capacity, String time, int guests, String note) {
        String safeName = escapeHtml(name);
        String safePhone = escapeHtml(phone);
        String safeTable = escapeHtml(tableCode);
        String safeZone = escapeHtml(zone);
        String safeNote = escapeHtml(note);

        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Xác nhận đặt bàn Han Seoul</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f7fafc; font-family: 'Segoe UI', Arial, Tahoma, Helvetica, sans-serif; color: #2d3748; line-height: 1.6;">
              <div style="max-width: 620px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05); border: 1px solid #edf2f7;">
                
                <!-- HEADER BANNER -->
                <div style="background: linear-gradient(135deg, #173c32 0%, #255246 100%); color: #ffffff; padding: 32px 24px; text-align: center;">
                  <span style="display: inline-block; background: rgba(226, 200, 155, 0.25); color: #e2c89b; padding: 4px 14px; border-radius: 20px; font-size: 11px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; margin-bottom: 8px;">
                    HAN SEOUL KOREAN DINING
                  </span>
                  <h1 style="margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">XÁC NHẬN ĐẶT BÀN THÀNH CÔNG</h1>
                  <p style="margin: 8px 0 0; font-size: 14px; color: #e2c89b; opacity: 0.95;">Bàn ăn của bạn đã sẵn sàng tại nhà hàng</p>
                </div>

                <!-- BODY -->
                <div style="padding: 28px 24px;">
                  <div style="text-align: center; margin-bottom: 22px;">
                    <span style="display: inline-block; background-color: #c6f6d5; color: #22543d; font-weight: 700; font-size: 13px; padding: 6px 16px; border-radius: 20px; text-transform: uppercase;">
                      ✓ ĐÃ XÁC NHẬN GIỮ CHỖ
                    </span>
                    <h2 style="margin: 14px 0 6px; font-size: 20px; color: #1a202c;">MÃ ĐẶT BÀN: #{{ID}}</h2>
                    <p style="margin: 0; color: #718096; font-size: 14px;">Thời gian đến: <strong style="color: #8B2500;">{{TIME}}</strong></p>
                  </div>

                  <p style="font-size: 15px; margin: 0 0 16px; color: #2d3748;">
                    Kính gửi <strong>{{NAME}}</strong>,
                  </p>
                  <p style="font-size: 14px; color: #4a5568; margin: 0 0 20px; line-height: 1.6;">
                    Cảm ơn bạn đã lựa chọn dùng bữa tại <strong>Han Seoul</strong>. Nhà hàng đã tiếp nhận và sắp xếp bàn ăn chu đáo theo thông tin đặt hẹn của bạn:
                  </p>

                  <!-- CHI TIẾT ĐẶT BÀN -->
                  <div style="background-color: #fcfbf9; border: 1px solid #ede6db; border-radius: 10px; padding: 18px 20px; margin-bottom: 24px; font-size: 14px;">
                    <table style="width: 100%; border-collapse: collapse;">
                      <tr>
                        <td style="padding: 8px 0; color: #718096; width: 38%;">Mã đặt bàn:</td>
                        <td style="padding: 8px 0; font-weight: 700; color: #8B2500; font-size: 15px;">#{{ID}}</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; color: #718096;">Bàn phục vụ:</td>
                        <td style="padding: 8px 0; font-weight: 700; color: #173c32;">{{TABLE}} ({{ZONE}} - {{CAPACITY}} chỗ)</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; color: #718096;">Thời gian dùng bữa:</td>
                        <td style="padding: 8px 0; font-weight: 700; color: #2d3748;">{{TIME}}</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; color: #718096;">Số lượng khách:</td>
                        <td style="padding: 8px 0; font-weight: 600; color: #2d3748;">{{GUESTS}} người</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; color: #718096;">Người đặt:</td>
                        <td style="padding: 8px 0; font-weight: 600; color: #2d3748;">{{NAME}}</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; color: #718096;">Số điện thoại:</td>
                        <td style="padding: 8px 0; font-weight: 600; color: #2d3748;">{{PHONE}}</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; color: #718096;">Ghi chú đặc biệt:</td>
                        <td style="padding: 8px 0; font-style: italic; color: #4a5568;">{{NOTE}}</td>
                      </tr>
                    </table>
                  </div>

                  <!-- ƯU ĐÃI ĐẶT BÀN ONLINE -->
                  <div style="background: linear-gradient(135deg, #fffaf0 0%, #feebc8 100%); border: 1px solid #fbd38d; border-radius: 10px; padding: 14px 18px; margin-bottom: 24px;">
                    <div style="display: flex; align-items: center; gap: 10px;">
                      <span style="font-size: 24px;">🍧</span>
                      <div>
                        <strong style="color: #744210; font-size: 13.5px;">ƯU ĐÃI DÀNH CHO BẠN KHI ĐẾN DÙNG BỮA</strong>
                        <p style="margin: 2px 0 0; font-size: 12.5px; color: #975a16; line-height: 1.4;">
                          Tặng ngay <b>01 phần Bingsu kem tuyết Hàn Quốc</b> hoặc <b>giảm 10%</b> khi thanh toán tại nhà hàng!
                        </p>
                      </div>
                    </div>
                  </div>

                  <!-- LƯU Ý QUAN TRỌNG -->
                  <div style="background-color: #ebf8ff; border-left: 4px solid #3182ce; padding: 12px 16px; border-radius: 4px; margin-bottom: 24px; font-size: 13px; color: #2b6cb0;">
                    <p style="margin: 0 0 6px; font-weight: 700;">✦ Quy định giữ bàn:</p>
                    <p style="margin: 0; line-height: 1.5;">
                      Nhà hàng sẽ giữ bàn trong vòng <strong>15 phút</strong> so với giờ hẹn. Trường hợp quý khách đến trễ hoặc có thay đổi số lượng người, xin vui lòng gọi hotline <strong>090 123 4567</strong> để được hỗ trợ chu đáo nhất.
                    </p>
                  </div>

                  <!-- CTA BUTTONS -->
                  <div style="text-align: center; margin: 28px 0 10px;">
                    <a href="http://localhost:3000/thuc-don" style="background-color: #8B2500; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-size: 13.5px; font-weight: 600; display: inline-block; margin-right: 8px;">
                      Xem Thực Đơn &amp; Món Ăn &rarr;
                    </a>
                    <a href="http://localhost:3000/tra-cuu-dat-ban" style="background-color: #173c32; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-size: 13.5px; font-weight: 600; display: inline-block;">
                      Tra Cứu Lịch Hẹn &rarr;
                    </a>
                  </div>
                </div>

                <!-- FOOTER -->
                <div style="background-color: #2d3748; padding: 20px 24px; text-align: center; color: #a0aec0; font-size: 12px;">
                  <p style="margin: 0 0 4px; color: #edf2f7; font-weight: 600;">NHÀ HÀNG HÀN QUỐC HAN SEOUL</p>
                  <p style="margin: 0 0 4px;">Hotline hỗ trợ: 090 123 4567 · Email: support@hanseoul.vn</p>
                  <p style="margin: 0;">Địa chỉ: TP. Hồ Chí Minh, Việt Nam</p>
                </div>

              </div>
            </body>
            </html>
            """;

        return template.replace("{{ID}}", String.valueOf(resId))
                .replace("{{NAME}}", safeName)
                .replace("{{PHONE}}", safePhone)
                .replace("{{TABLE}}", safeTable)
                .replace("{{ZONE}}", safeZone)
                .replace("{{CAPACITY}}", String.valueOf(capacity))
                .replace("{{TIME}}", time)
                .replace("{{GUESTS}}", String.valueOf(guests))
                .replace("{{NOTE}}", safeNote);
    }

    private String buildAdminReservationNotificationHtml(Long resId, String name, String phone, String email, String tableCode, String zone, String time, int guests, String note) {
        String safeName = escapeHtml(name);
        String safePhone = escapeHtml(phone);
        String safeEmail = escapeHtml(email != null && !email.isBlank() ? email : "Không có");
        String safeTable = escapeHtml(tableCode);
        String safeZone = escapeHtml(zone);
        String safeNote = escapeHtml(note);

        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <body style="font-family: 'Segoe UI', Arial, Tahoma, Helvetica, sans-serif; color: #2d3748; padding: 20px; line-height: 1.6;">
              <div style="max-width: 520px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; background: #ffffff;">
                <h3 style="color: #173c32; margin-top: 0;">[THÔNG BÁO] Khách hàng mới đặt bàn #{{ID}}</h3>
                <p>Hệ thống vừa ghi nhận một lượt đặt bàn mới cần chuẩn bị:</p>
                <ul style="line-height: 1.8;">
                  <li><strong>Mã đặt bàn:</strong> #{{ID}}</li>
                  <li><strong>Khách hàng:</strong> {{NAME}}</li>
                  <li><strong>Số điện thoại:</strong> {{PHONE}}</li>
                  <li><strong>Email khách:</strong> {{EMAIL}}</li>
                  <li><strong>Thời gian hẹn:</strong> <span style="color: #8B2500; font-weight: 700;">{{TIME}}</span></li>
                  <li><strong>Bàn sắp xếp:</strong> {{TABLE}} (Khu vực: {{ZONE}})</li>
                  <li><strong>Số lượng khách:</strong> {{GUESTS}} người</li>
                  <li><strong>Ghi chú:</strong> <em>{{NOTE}}</em></li>
                </ul>
                <div style="margin-top: 16px; padding: 10px; background: #f7fafc; border-radius: 6px; font-size: 13px;">
                  Vui lòng kiểm tra trên trang quản trị hoặc chuẩn bị bàn trước giờ hẹn 15-30 phút.
                </div>
              </div>
            </body>
            </html>
            """;

        return template.replace("{{ID}}", String.valueOf(resId))
                .replace("{{NAME}}", safeName)
                .replace("{{PHONE}}", safePhone)
                .replace("{{EMAIL}}", safeEmail)
                .replace("{{TABLE}}", safeTable)
                .replace("{{ZONE}}", safeZone)
                .replace("{{TIME}}", time)
                .replace("{{GUESTS}}", String.valueOf(guests))
                .replace("{{NOTE}}", safeNote);
    }

    @Override
    @Async
    public void sendRegistrationOtpEmail(String toEmail, String fullName, String otpCode) {
        if (!mailEnabled) {
            log.info("Tính năng gửi email đang bị tắt (app.mail.enabled=false). Bỏ qua gửi email OTP tới {}", toEmail);
            return;
        }

        if (mailSender == null || fromEmail == null || fromEmail.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.warn("Gmail SMTP chưa được cấu hình. Bỏ qua gửi email OTP tới {}. Mã OTP dev: {}", toEmail, otpCode);
            return;
        }

        if (toEmail == null || !isValidEmail(toEmail.trim())) {
            log.warn("Địa chỉ email không hợp lệ: {}. Bỏ qua gửi email OTP.", toEmail);
            return;
        }

        String subject = "[Han Seoul] " + otpCode + " là mã xác thực đăng ký hội viên của bạn";
        String html = buildOtpEmailHtml(fullName, otpCode);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("ĐÃ GỬI MÃ OTP XÁC THỰC ({}) tới Gmail: {}", otpCode, toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP tới {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String fullName, String otpCode) {
        String safeName = fullName != null && !fullName.isBlank() ? escapeHtml(fullName.trim()) : "Quý khách";
        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Mã xác thực đăng ký hội viên</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f7fafc; font-family: 'Segoe UI', Arial, Tahoma, Helvetica, sans-serif; color: #2d3748; line-height: 1.6;">
              <div style="max-width: 580px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05); border: 1px solid #edf2f7;">
                
                <!-- Header -->
                <div style="background: linear-gradient(135deg, #8B2500 0%, #b83a1b 100%); color: #ffffff; padding: 28px 24px; text-align: center;">
                  <span style="display: inline-block; background: rgba(255,255,255,0.2); padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; margin-bottom: 8px;">
                    HAN SEOUL VERIFICATION
                  </span>
                  <h1 style="margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;">XÁC THỰC ĐĂNG KÝ HỘI VIÊN</h1>
                  <p style="margin: 6px 0 0; font-size: 13.5px; opacity: 0.9;">Nhà hàng Ẩm thực Hàn Quốc Han Seoul</p>
                </div>

                <!-- Body -->
                <div style="padding: 28px 24px;">
                  <p style="font-size: 16px; margin: 0 0 14px; color: #1a202c;">
                    Xin chào <strong>{{NAME}}</strong>,
                  </p>
                  <p style="font-size: 14px; color: #4a5568; margin: 0 0 20px;">
                    Cảm ơn bạn đã lựa chọn đăng ký tài khoản thành viên tại <strong>Han Seoul</strong>. Để kích hoạt tài khoản và nhận ngay <strong>50 điểm thưởng & ưu đãi giảm 10%</strong>, vui lòng sử dụng mã xác thực bên dưới:
                  </p>

                  <!-- OTP BOX -->
                  <div style="background: #fffaf0; border: 2px dashed #dd6b20; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0;">
                    <div style="font-size: 12px; font-weight: 700; color: #7b341e; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 8px;">
                      MÃ XÁC THỰC OTP CỦA BẠN
                    </div>
                    <div style="font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #8B2500; font-family: monospace; padding: 6px 0;">
                      {{OTP}}
                    </div>
                    <div style="font-size: 12.5px; color: #718096; margin-top: 8px;">
                      ⏰ Mã có hiệu lực trong vòng <strong>5 phút</strong>
                    </div>
                  </div>

                  <!-- Cảnh báo an toàn -->
                  <div style="background-color: #ebf8ff; border-left: 4px solid #3182ce; padding: 12px 16px; border-radius: 4px; margin-bottom: 20px; font-size: 13px; color: #2b6cb0;">
                    ✦ <strong>Lưu ý bảo mật:</strong> Vui lòng không chia sẻ mã xác thực này cho bất kỳ ai. Nhân viên Han Seoul sẽ không bao giờ hỏi mã OTP của bạn.
                  </div>

                  <p style="font-size: 13px; color: #718096; margin: 0;">
                    Nếu bạn không yêu cầu đăng ký tài khoản tại Han Seoul, xin vui lòng bỏ qua email này.
                  </p>
                </div>

                <!-- Footer -->
                <div style="background-color: #edf2f7; padding: 16px 24px; text-align: center; font-size: 12px; color: #718096;">
                  <p style="margin: 0 0 4px; font-weight: 600; color: #4a5568;">Nhà hàng Hàn Quốc Han Seoul</p>
                  <p style="margin: 0 0 4px;">Ẩm thực chuẩn vị truyền thống Hàn Quốc · Hotline: 0900 000 888</p>
                  <p style="margin: 0; font-size: 11px; color: #a0aec0;">Email này được gửi tự động, vui lòng không phản hồi trực tiếp.</p>
                </div>

              </div>
            </body>
            </html>
            """ ;

        return template.replace("{{NAME}}", safeName).replace("{{OTP}}", otpCode);
    }
}
