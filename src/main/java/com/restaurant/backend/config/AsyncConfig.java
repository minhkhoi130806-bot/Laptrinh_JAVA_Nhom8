package com.restaurant.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Kích hoạt xử lý bất đồng bộ (@Async) cho các tác vụ như gửi email thông báo. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
