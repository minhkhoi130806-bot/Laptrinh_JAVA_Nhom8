package com.restaurant.backend.dto;

// Phạm vi lấy danh sách với các bảng có thùng rác
//   ACTIVE = đang dùng (mặc định), TRASH = trong thùng rác, ALL = tất cả (dùng để tra tên)
public enum ListScope {
    ACTIVE,
    TRASH,
    ALL
}
