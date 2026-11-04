package com.restaurant.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Du lieu khong hop le (400)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // Khong tim thay tai nguyen (404)
    @ExceptionHandler({ResourceNotFoundException.class, NotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(
            RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", request);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleConcurrentUpdate(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Dữ liệu vừa được cập nhật. Hãy tải lại và thử lại thao tác", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), request);
    }

    // Body JSON sai dinh dang (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không hợp lệ",
                request
        );
    }

    // Tham so khong dung kieu du lieu (400)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Giá trị tham số '" + ex.getName() + "' không hợp lệ",
                request
        );
    }

    // File upload qua lon (413)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.CONTENT_TOO_LARGE,
                "Ảnh quá lớn (tối đa 5MB)",
                request
        );
    }

    // Vi pham rang buoc du lieu (409)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Dữ liệu vi phạm ràng buộc (trùng giá trị duy nhất hoặc đang được tham chiếu)",
                request
        );
    }

    // Loi truy cap CSDL (500)
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccess(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Lỗi CSDL khi gọi {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể truy cập dữ liệu. Vui lòng thử lại sau",
                request
        );
    }

    // Cong thanh toan ben ngoai loi/timeout (502)
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiError> handlePaymentGateway(
            PaymentGatewayException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
    }

    // Xu ly cac RuntimeException chua co handler rieng
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Lỗi xử lý {}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể hoàn tất thao tác. Vui lòng thử lại sau",
                request
        );
    }

    // Tao phan hoi loi dung chung
    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status, String message, HttpServletRequest request) {

        ApiError error = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }
}
