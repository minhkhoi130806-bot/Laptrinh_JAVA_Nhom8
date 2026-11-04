package com.restaurant.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.backend.dto.OperationRequest;
import com.restaurant.backend.dto.OperationResponse;
import com.restaurant.backend.entity.OperationRecord;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.OperationRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Transactional
public class OperationService {
    private static final Map<String, List<String>> FIELDS = Map.of(
            "waitlist", List.of("customer", "phone", "partySize", "arrival", "area", "note"),
            "feedback", List.of("customer", "phone", "type", "priority", "detail", "assignee", "resolution"),
            "marketing", List.of("name", "code", "kind", "value", "startDate", "endDate", "condition", "limit"),
            "shifts", List.of("employee", "role", "date", "shift", "start", "end", "handover"),
            "cleaning", List.of("task", "area", "frequency", "assignee", "due", "note"),
            "maintenance", List.of("equipment", "area", "severity", "reportedAt", "assignee", "description", "cost"),
            "settings", List.of("name", "phone", "address", "opening", "closing", "serviceCharge", "vat", "reservationWindow", "receiptFooter"));
    private static final Map<String, List<String>> REQUIRED = Map.of(
            "waitlist", List.of("customer", "phone", "partySize", "arrival"),
            "feedback", List.of("customer", "type", "priority", "detail"),
            "marketing", List.of("name", "code", "kind", "value", "startDate", "endDate"),
            "shifts", List.of("employee", "role", "date", "shift", "start", "end"),
            "cleaning", List.of("task", "area", "frequency"),
            "maintenance", List.of("equipment", "area", "severity", "reportedAt", "description"));
    private final OperationRecordRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public OperationService(OperationRecordRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<OperationResponse> list(String type) {
        authorize(type);
        return repository.findByRecordTypeOrderByCreatedAtDescIdDesc(type).stream().map(this::response).toList();
    }

    public OperationResponse create(String type, OperationRequest request) {
        authorize(type);
        if ("settings".equals(type)) throw new BadRequestException("Hãy sử dụng trang cấu hình nhà hàng");
        OperationRecord record = new OperationRecord();
        record.setRecordType(type);
        record.setFieldsJson(encode(validate(type, request)));
        record.setDone(false);
        return response(repository.saveAndFlush(record));
    }

    public OperationResponse update(String type, Long id, OperationRequest request) {
        authorize(type);
        OperationRecord record = find(type, id);
        checkVersion(record, request == null ? null : request.version());
        record.setFieldsJson(encode(validate(type, request)));
        if (request.done() != null) record.setDone(request.done());
        return response(repository.saveAndFlush(record));
    }

    public void delete(String type, Long id, Long version) {
        authorize(type);
        OperationRecord record = find(type, id);
        checkVersion(record, version);
        repository.delete(record);
        repository.flush();
    }

    @Transactional(readOnly = true)
    public OperationResponse settings() {
        authorize("settings");
        return repository.findBySingletonKey("restaurant").map(this::response)
                .orElseGet(() -> new OperationResponse(null, "settings", Map.of(), false, null, null, null));
    }

    public OperationResponse saveSettings(OperationRequest request) {
        authorize("settings");
        Map<String, String> fields = validate("settings", request);
        OperationRecord record = repository.findBySingletonKey("restaurant").orElse(null);
        if (record == null) {
            if (request.version() != null) throw conflict(0L);
            record = new OperationRecord();
            record.setRecordType("settings");
            record.setSingletonKey("restaurant");
        } else checkVersion(record, request.version());
        record.setFieldsJson(encode(fields));
        try { return response(repository.saveAndFlush(record)); }
        catch (DataIntegrityViolationException exception) { throw conflict(record.getId()); }
    }

    public void resetSettings(Long version) {
        authorize("settings");
        OperationRecord record = repository.findBySingletonKey("restaurant")
                .orElseThrow(() -> new NotFoundException("Cấu hình đã được đặt lại. Hãy tải lại trang."));
        checkVersion(record, version);
        repository.delete(record);
        repository.flush();
    }

    private void authorize(String type) {
        if (!FIELDS.containsKey(type)) throw new BadRequestException("Loại sổ vận hành không hợp lệ");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean manager = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        boolean staff = auth != null && auth.getAuthorities().stream().anyMatch(a -> Set.of("ROLE_MANAGER", "ROLE_CASHIER", "ROLE_WAITER", "ROLE_CHEF").contains(a.getAuthority()));
        if (!staff || (Set.of("marketing", "shifts", "settings").contains(type) && !manager)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private OperationRecord find(String type, Long id) {
        OperationRecord record = repository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy mục vận hành"));
        if (!type.equals(record.getRecordType()) || "settings".equals(type)) throw new NotFoundException("Không tìm thấy mục vận hành");
        return record;
    }

    private void checkVersion(OperationRecord record, Long version) {
        if (version == null || !version.equals(record.getVersion())) throw conflict(record.getId());
    }

    private ObjectOptimisticLockingFailureException conflict(Long id) {
        return new ObjectOptimisticLockingFailureException(OperationRecord.class, id == null ? 0L : id);
    }

    private Map<String, String> validate(String type, OperationRequest request) {
        if (request == null || request.fields() == null || request.fields().size() > 20) throw new BadRequestException("Thông tin biểu mẫu không hợp lệ");
        Map<String, String> fields = new LinkedHashMap<>();
        int size = 0;
        for (Map.Entry<String, String> entry : request.fields().entrySet()) {
            if (!FIELDS.get(type).contains(entry.getKey())) throw new BadRequestException("Biểu mẫu chứa trường không được hỗ trợ");
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            size += value.length();
            if (value.length() > 4000 || size > 20000) throw new BadRequestException("Nội dung quá dài, vui lòng rút gọn");
            fields.put(entry.getKey(), value);
        }
        for (String key : REQUIRED.getOrDefault(type, List.of())) {
            if (fields.getOrDefault(key, "").isBlank()) throw new BadRequestException("Vui lòng nhập đầy đủ các thông tin bắt buộc");
        }
        try {
            switch (type) {
                case "waitlist" -> { number(fields, "partySize", BigDecimal.ONE, new BigDecimal("500"), true); LocalTime.parse(fields.get("arrival")); }
                case "marketing" -> {
                    LocalDate start = LocalDate.parse(fields.get("startDate"));
                    LocalDate end = LocalDate.parse(fields.get("endDate"));
                    if (end.isBefore(start)) throw new BadRequestException("Ngày kết thúc phải từ ngày bắt đầu trở đi");
                    number(fields, "limit", BigDecimal.ONE, new BigDecimal("1000000000"), true);
                }
                case "shifts" -> { LocalDate.parse(fields.get("date")); LocalTime.parse(fields.get("start")); LocalTime.parse(fields.get("end")); }
                case "cleaning" -> { if (!fields.getOrDefault("due", "").isBlank()) LocalDateTime.parse(fields.get("due")); }
                case "maintenance" -> { LocalDateTime.parse(fields.get("reportedAt")); number(fields, "cost", BigDecimal.ZERO, new BigDecimal("1000000000000"), false); }
                case "settings" -> {
                    if (!fields.getOrDefault("opening", "").isBlank()) LocalTime.parse(fields.get("opening"));
                    if (!fields.getOrDefault("closing", "").isBlank()) LocalTime.parse(fields.get("closing"));
                    number(fields, "vat", BigDecimal.ZERO, new BigDecimal("100"), false);
                    number(fields, "serviceCharge", BigDecimal.ZERO, new BigDecimal("100"), false);
                    number(fields, "reservationWindow", BigDecimal.ZERO, new BigDecimal("3650"), true);
                }
                default -> { }
            }
        } catch (DateTimeParseException exception) { throw new BadRequestException("Ngày hoặc giờ trong biểu mẫu không hợp lệ"); }
        return fields;
    }

    private void number(Map<String, String> fields, String key, BigDecimal min, BigDecimal max, boolean integer) {
        String text = fields.getOrDefault(key, "");
        if (text.isBlank()) return;
        try {
            BigDecimal value = new BigDecimal(text);
            if (value.compareTo(min) < 0 || value.compareTo(max) > 0 || (integer && value.stripTrailingZeros().scale() > 0)) throw new NumberFormatException();
        } catch (NumberFormatException exception) { throw new BadRequestException("Giá trị số trong biểu mẫu không hợp lệ: " + key); }
    }

    private String encode(Map<String, String> fields) {
        try { return mapper.writeValueAsString(fields); }
        catch (Exception exception) { throw new BadRequestException("Không thể lưu nội dung biểu mẫu"); }
    }

    private OperationResponse response(OperationRecord record) {
        try {
            Map<String, String> fields = mapper.readValue(record.getFieldsJson(), new TypeReference<>() { });
            return new OperationResponse(record.getId(), record.getRecordType(), fields, record.isDone(), record.getVersion(), record.getCreatedAt(), record.getUpdatedAt());
        } catch (Exception exception) { throw new IllegalStateException("Không thể đọc dữ liệu vận hành đã lưu", exception); }
    }
}
