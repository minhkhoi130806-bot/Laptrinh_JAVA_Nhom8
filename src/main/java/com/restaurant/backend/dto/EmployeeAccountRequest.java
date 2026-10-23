package com.restaurant.backend.dto;

import com.restaurant.backend.entity.EmployeeRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeAccountRequest(
        @NotNull(message = "Vui lòng chọn nhân viên")
        Long employeeId,

        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "Tên đăng nhập gồm 3-50 ký tự chữ, số, dấu chấm, gạch dưới hoặc gạch ngang"
        )
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
        String password,

        @NotNull(message = "Vui lòng chọn quyền nhân viên")
        EmployeeRole role
) {
}
