package com.restaurant.backend.security;

import com.restaurant.backend.config.SecurityConfig;
import com.restaurant.backend.controller.EmployeeController;
import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.entity.Employee;
import com.restaurant.backend.entity.EmployeeRole;
import com.restaurant.backend.mapper.EmployeeMapper;
import com.restaurant.backend.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(StaffAccessTest.TestConfig.class)
@WebAppConfiguration
class StaffAccessTest {
    @Autowired WebApplicationContext context;
    @Autowired EmployeeService employeeService;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(employeeService);
        Employee employee = new Employee();
        employee.setId(7L);
        employee.setFullName("Nhân viên A");
        employee.setUsername("staff-a");
        employee.setRole(EmployeeRole.WAITER);
        employee.setPhone("0901234567");
        employee.setPassword("never-expose-this");
        employee.setSalary(new BigDecimal("10000000"));
        when(employeeService.getAll(any())).thenReturn(List.of(employee));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "CASHIER", "WAITER", "CHEF"})
    void staffCanLookUpNamesWithoutPersonalOrSalaryData(String role) throws Exception {
        mvc.perform(get("/api/employees/lookup").with(user("staff").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].fullName").value("Nhân viên A"))
                .andExpect(jsonPath("$[0].deleted").value(false))
                .andExpect(jsonPath("$[0].salary").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].username").doesNotExist());
        verify(employeeService).getAll(ListScope.ALL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASHIER", "WAITER", "CHEF", "CUSTOMER"})
    void nonManagersCannotReadOrWriteEmployeeAdministration(String role) throws Exception {
        mvc.perform(get("/api/employees").with(user("staff").roles(role))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/employees/7").with(user("staff").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(employeeService);
    }

    @Test
    void managerRetainsFullEmployeeReadAccess() throws Exception {
        mvc.perform(get("/api/employees").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].salary").value(10000000));
    }

    @Test
    void lookupRequiresStaffAuthentication() throws Exception {
        mvc.perform(get("/api/employees/lookup")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/employees/lookup").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(employeeService);
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtFilter.class, RateLimitFilter.class,
            JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, EmployeeController.class, EmployeeMapper.class})
    static class TestConfig {
        @Bean JwtUtil jwtUtil() { return mock(JwtUtil.class); }
        @Bean EmployeeService employeeService() { return mock(EmployeeService.class); }
    }
}
