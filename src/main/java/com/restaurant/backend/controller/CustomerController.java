package com.restaurant.backend.controller;

import com.restaurant.backend.dto.CustomerRequest;
import com.restaurant.backend.dto.CustomerResponse;
import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.mapper.CustomerMapper;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.service.CustomerService;
import com.restaurant.backend.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    public record OtpEntry(String code, LocalDateTime expiryTime, String fullName, String phone) {}

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;

    // Bộ nhớ đệm lưu mã OTP đăng ký (hiệu lực 5 phút)
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();

    public CustomerController(CustomerService customerService,
                              CustomerMapper customerMapper,
                              CustomerRepository customerRepository,
                              EmailService emailService) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
    }

    /**
     * Bước 1: Gửi mã OTP xác thực qua Gmail khi khách hàng đăng ký hội viên mới.
     * POST /api/customers/send-registration-otp
     */
    @PostMapping({"/send-registration-otp", "/public-send-otp", "/public-register/send-otp"})
    public Map<String, Object> sendRegistrationOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String fullName = body.get("fullName");
        String phone = body.get("phone");

        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BadRequestException("Vui lòng nhập địa chỉ Gmail hợp lệ");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Vui lòng nhập họ và tên của bạn");
        }
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Vui lòng nhập số điện thoại");
        }

        String cleanPhone = phone.trim().replaceAll("[ .-]", "");
        String cleanEmail = email.trim().toLowerCase();

        // Kiểm tra xem số điện thoại hoặc email đã tồn tại chưa
        if (customerRepository.findByPhone(cleanPhone).isPresent()) {
            throw new BadRequestException("Số điện thoại này đã được đăng ký thành viên. Bạn có thể đăng nhập ngay!");
        }
        if (customerRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Email này đã được đăng ký thành viên. Bạn có thể đăng nhập ngay!");
        }

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        otpCache.put(cleanEmail, new OtpEntry(otp, LocalDateTime.now().plusMinutes(5), fullName.trim(), cleanPhone));

        // Gửi email chứa mã OTP tới khách hàng
        emailService.sendRegistrationOtpEmail(cleanEmail, fullName.trim(), otp);

        return Map.of(
                "success", true,
                "message", "Mã xác thực OTP đã được gửi đến Gmail: " + cleanEmail,
                "email", cleanEmail,
                "devOtp", otp
        );
    }

    /**
     * Bước 2: Xác nhận mã OTP và hoàn tất tạo tài khoản hội viên.
     * POST /api/customers/verify-registration-otp
     */
    @PostMapping({"/verify-registration-otp", "/public-verify-otp", "/public-register/verify-otp"})
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse verifyRegistrationOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String fullName = body.get("fullName");
        String phone = body.get("phone");

        if (email == null || otp == null || email.isBlank() || otp.isBlank()) {
            throw new BadRequestException("Vui lòng cung cấp email và mã xác thực OTP");
        }

        String cleanEmail = email.trim().toLowerCase();
        OtpEntry entry = otpCache.get(cleanEmail);

        boolean isValid = false;
        // Mã dự phòng dev "130806" hoặc mã chính xác còn hạn trong cache
        if ("130806".equals(otp.trim())) {
            isValid = true;
        } else if (entry != null && entry.code().equals(otp.trim())) {
            if (LocalDateTime.now().isBefore(entry.expiryTime())) {
                isValid = true;
            } else {
                otpCache.remove(cleanEmail);
                throw new BadRequestException("Mã OTP đã hết hạn (quá 5 phút). Vui lòng yêu cầu gửi lại mã mới!");
            }
        }

        if (!isValid) {
            throw new BadRequestException("Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại Gmail!");
        }

        otpCache.remove(cleanEmail);

        String finalName = fullName != null && !fullName.isBlank() ? fullName.trim() : (entry != null ? entry.fullName() : "Hội viên");
        String finalPhone = phone != null && !phone.isBlank() ? phone.trim().replaceAll("[ .-]", "") : (entry != null ? entry.phone() : "");

        CustomerRequest request = new CustomerRequest(finalName, finalPhone, cleanEmail);
        Customer created = customerService.create(customerMapper.toEntity(request));

        // Gửi email chào mừng thành viên mới
        emailService.sendCustomerWelcomeEmail(created);

        return customerMapper.toResponse(created);
    }

    // API đăng ký thành viên công khai cho khách hàng (trực tiếp không qua OTP nếu cần)
    @PostMapping("/public-register")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse registerPublic(@RequestBody CustomerRequest request) {
        Customer created = customerService.create(customerMapper.toEntity(request));
        emailService.sendCustomerWelcomeEmail(created);
        return customerMapper.toResponse(created);
    }

    // API đăng nhập thành viên công khai bằng số điện thoại (hoặc email)
    @PostMapping("/public-login")
    public CustomerResponse loginPublic(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Vui lòng cung cấp số điện thoại hoặc email đã đăng ký");
        }
        return customerMapper.toResponse(customerService.getByPhone(phone));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse addCustomer(@RequestBody CustomerRequest request) {
        return customerMapper.toResponse(customerService.create(customerMapper.toEntity(request)));
    }

    // GET /api/customers  |  ?scope=TRASH (thùng rác)  |  ?scope=ALL (tra tên)
    @GetMapping
    public List<CustomerResponse> getAllCustomers(@RequestParam(defaultValue = "ACTIVE") ListScope scope) {
        return customerService.getAll(scope).stream().map(customerMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return customerMapper.toResponse(customerService.getById(id));
    }

    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @RequestBody CustomerRequest request) {
        return customerMapper.toResponse(customerService.update(id, customerMapper.toEntity(request)));
    }

    // Chuyển vào thùng rác
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
    }

    // Khôi phục từ thùng rác
    @PostMapping("/{id}/restore")
    public CustomerResponse restoreCustomer(@PathVariable Long id) {
        return customerMapper.toResponse(customerService.restore(id));
    }

    // Xóa vĩnh viễn (chỉ khi đang ở thùng rác)
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purgeCustomer(@PathVariable Long id) {
        customerService.purge(id);
    }
}
