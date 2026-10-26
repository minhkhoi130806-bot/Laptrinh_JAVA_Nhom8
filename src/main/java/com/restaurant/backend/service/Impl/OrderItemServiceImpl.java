package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.entity.Menuitem;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import com.restaurant.backend.entity.Recipe;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.RecipeRepository;
import com.restaurant.backend.service.OrderItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================================
 * LỚP THỰC THI NGHIỆP VỤ GỌI MÓN (OrderItemServiceImpl)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 & Tuần 4 - Nghiệp vụ Đơn hàng & Kiểm tra Kho)
 * 
 * Mục đích và yêu cầu trọng tâm:
 * 1. API thêm/sửa/xóa món trong đơn hàng.
 * 2. LƯU UNIT_PRICE LÚC GỌI: Khi khách gọi món, hệ thống tự động tìm giá
 *    niêm yết của món ăn trong bảng thực đơn (menu_item) và lưu cố định vào cột
 *    unit_price của chi tiết đơn hàng (order_item).
 * 3. Nếu khách gọi thêm món đã có trong đơn, hệ thống tự động cộng dồn số lượng.
 * 4. KẾ HOẠCH TUẦN 4 (Khôi): Chặn gọi món khi món đang ngưng bán (available = false)
 *    hoặc kho không đủ nguyên liệu theo công thức (Recipe) để chế biến -> trả mã lỗi 400.
 * =========================================================================
 */
@Service
@Transactional
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RecipeRepository recipeRepository;

    // Tiêm các Repository cần thiết qua Constructor
    public OrderItemServiceImpl(
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository,
            MenuItemRepository menuItemRepository,
            RecipeRepository recipeRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.recipeRepository = recipeRepository;
    }

    /**
     * BƯỚC 1: THÊM MÓN VÀO ĐƠN HÀNG (Gọi món)
     * Đã bổ sung kiểm tra available và kiểm tra nguyên liệu tồn kho theo Recipe (Tuần 4 - Khôi)
     */
    @Override
    public OrderItem create(OrderItem orderItem) {
        // Kiểm tra xem ID kép (gồm orderId và menuItemId) có được truyền hay không
        if (orderItem.getId() == null) {
            throw new BadRequestException("Thiếu thông tin mã đơn hàng hoặc mã món ăn");
        }

        Integer maDonHang = orderItem.getId().getOrderId();
        Integer maMonAn = orderItem.getId().getMenuItemId();

        if (maDonHang == null || maMonAn == null) {
            throw new BadRequestException("Mã đơn hàng và mã món ăn không được để trống");
        }

        // Bước 1.1: Kiểm tra xem đơn hàng có tồn tại trong CSDL hay không
        Order donHang = orderRepository.findById(maDonHang)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng có mã ID: " + maDonHang));

        // Nếu đơn hàng đã thanh toán (PAID) hoặc đã hủy (CANCELLED) thì không cho phép gọi thêm món
        if ("PAID".equalsIgnoreCase(donHang.getStatus())) {
            throw new BadRequestException("Đơn hàng này đã thanh toán xong, không thể gọi thêm món");
        }
        if ("CANCELLED".equalsIgnoreCase(donHang.getStatus())) {
            throw new BadRequestException("Đơn hàng này đã bị hủy, không thể gọi thêm món");
        }

        // Bước 1.2: Tìm món ăn trong bảng menu_item để lấy thông tin món và giá tiền
        Menuitem monAn = menuItemRepository.findById(Long.valueOf(maMonAn))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy món ăn có mã ID: " + maMonAn));

        // Bước 1.2.1 (Tuần 4 - Khôi): Chặn gọi món khi món đang tạm ngừng bán (available = false)
        if (Boolean.FALSE.equals(monAn.getAvailable())) {
            throw new BadRequestException("Món '" + monAn.getNameVi() + "' hiện đang tạm ngưng phục vụ, không thể gọi món!");
        }

        // Bước 1.3: Đảm bảo số lượng gọi ít nhất là 1
        if (orderItem.getQuantity() == null || orderItem.getQuantity() <= 0) {
            orderItem.setQuantity(1);
        }

        int soLuongGoiThem = orderItem.getQuantity();

        // Bước 1.3.1 (Tuần 4 - Khôi): Chặn gọi món khi kho thiếu nguyên liệu theo công thức (Recipe)
        List<Recipe> danhSachCongThuc = recipeRepository.findByMenuItem(Long.valueOf(maMonAn));
        if (danhSachCongThuc != null && !danhSachCongThuc.isEmpty()) {
            for (Recipe recipe : danhSachCongThuc) {
                Ingredient nguyenLieu = recipe.getIngredient();
                if (nguyenLieu != null) {
                    // Lấy số lượng nguyên liệu cần cho 1 phần ăn
                    BigDecimal dinhLuongMotPhan = (recipe.getQuantityNeeded() != null) 
                            ? recipe.getQuantityNeeded() 
                            : BigDecimal.ZERO;
                    // Lượng cần dùng cho số lượng món khách đang muốn gọi
                    BigDecimal luongCanDung = dinhLuongMotPhan.multiply(BigDecimal.valueOf(soLuongGoiThem));

                    // Lấy tồn kho hiện tại của nguyên liệu
                    BigDecimal tonKhoHienTai = (nguyenLieu.getStockQty() != null) 
                            ? nguyenLieu.getStockQty() 
                            : BigDecimal.ZERO;

                    // Nếu tồn kho không đủ để chế biến -> Báo lỗi mã 400 (BadRequest)
                    if (tonKhoHienTai.compareTo(luongCanDung) < 0) {
                        throw new BadRequestException("Món '" + monAn.getNameVi() + "' không đủ nguyên liệu '" 
                                + nguyenLieu.getName() + "' trong kho để chế biến (cần: " 
                                + luongCanDung + " " + nguyenLieu.getUnit() + ", trong kho chỉ còn: " 
                                + tonKhoHienTai + " " + nguyenLieu.getUnit() + ")");
                    }
                }
            }
        }

        // Bước 1.4: LƯU ĐƠN GIÁ (unit_price) TẠI THỜI ĐIỂM GỌI MÓN
        // Nếu client không gửi đơn giá hoặc gửi đơn giá <= 0, ta lấy trực tiếp giá từ thực đơn
        if (orderItem.getUnitPrice() == null || orderItem.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            orderItem.setUnitPrice(monAn.getPrice());
        }

        // Bước 1.5: Kiểm tra xem món ăn này đã có sẵn trong đơn hàng hay chưa
        Optional<OrderItem> monDaCo = orderItemRepository.findById(orderItem.getId());

        if (monDaCo.isPresent()) {
            // Nếu món này đã có: Ta cộng dồn số lượng thêm vào số lượng cũ
            OrderItem monCu = monDaCo.get();
            int soLuongMoi = monCu.getQuantity() + orderItem.getQuantity();
            monCu.setQuantity(soLuongMoi);

            // Cập nhật lại ghi chú nếu có ghi chú mới
            if (orderItem.getNote() != null && !orderItem.getNote().trim().isEmpty()) {
                monCu.setNote(orderItem.getNote());
            }

            // Lưu lại và trả về
            return orderItemRepository.save(monCu);
        }

        // Nếu món này chưa có trong đơn: Lưu mới vào CSDL
        return orderItemRepository.save(orderItem);
    }

    /**
     * BƯỚC 2: LẤY TẤT CẢ CHI TIẾT MÓN ĂN
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getAll() {
        return orderItemRepository.findAll();
    }

    /**
     * BƯỚC 3: LẤY CÁC MÓN ĂN CỦA 1 ĐƠN HÀNG CỤ THỂ
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getByOrderId(Integer orderId) {
        return orderItemRepository.findByIdOrderId(orderId);
    }

    /**
     * BƯỚC 4: TÌM CHI TIẾT MÓN ĂN THEO KHÓA CHÍNH KÉP (orderId + menuItemId)
     */
    @Override
    @Transactional(readOnly = true)
    public OrderItem getById(OrderItemId id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy món ăn này trong đơn hàng (orderId=" 
                        + id.getOrderId() + ", menuItemId=" + id.getMenuItemId() + ")"));
    }

    /**
     * BƯỚC 5: CẬP NHẬT MÓN TRONG ĐƠN (Sửa số lượng, sửa ghi chú)
     */
    @Override
    public OrderItem update(OrderItemId id, OrderItem duLieuMoi) {
        OrderItem monHienTai = getById(id);

        // Nếu người dùng nhập số lượng <= 0, ta hiểu là khách muốn hủy món này khỏi đơn
        if (duLieuMoi.getQuantity() != null && duLieuMoi.getQuantity() <= 0) {
            orderItemRepository.delete(monHienTai);
            return monHienTai;
        }

        // Cập nhật số lượng mới
        if (duLieuMoi.getQuantity() != null) {
            monHienTai.setQuantity(duLieuMoi.getQuantity());
        }

        // Cập nhật đơn giá nếu có chỉ định mới
        if (duLieuMoi.getUnitPrice() != null && duLieuMoi.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            monHienTai.setUnitPrice(duLieuMoi.getUnitPrice());
        }

        // Cập nhật ghi chú mới (ví dụ: "ít đá", "không cay")
        if (duLieuMoi.getNote() != null) {
            monHienTai.setNote(duLieuMoi.getNote());
        }

        return orderItemRepository.save(monHienTai);
    }

    /**
     * BƯỚC 6: XÓA MÓN ĂN KHỎI ĐƠN HÀNG (Khách hủy món)
     */
    @Override
    public void delete(OrderItemId id) {
        OrderItem monHienTai = getById(id);
        orderItemRepository.delete(monHienTai);
    }
}
