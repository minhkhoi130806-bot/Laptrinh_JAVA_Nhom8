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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gọi món chỉ được thay đổi khi đơn còn mở; đơn giá được chốt từ thực đơn. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RecipeRepository recipeRepository;
    private final EntityManager entityManager;

    public OrderItemServiceImpl(
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository,
            MenuItemRepository menuItemRepository,
            RecipeRepository recipeRepository,
            EntityManager entityManager) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.recipeRepository = recipeRepository;
        this.entityManager = entityManager;
    }

    @Override
    public OrderItem create(OrderItem input) {
        if (input == null) throw new BadRequestException("Thiếu thông tin món gọi");
        validateId(input.getId());
        Integer orderId = input.getId().getOrderId();
        Integer menuItemId = input.getId().getMenuItemId();
        lockOpenOrder(orderId);

        Menuitem menuItem = requireAvailableMenuItem(menuItemId);
        if (input.getQuantity() == null || input.getQuantity() <= 0) {
            throw new BadRequestException("Số lượng món phải lớn hơn 0");
        }
        String note = normalizeNote(input.getNote());
        OrderItem existing = orderItemRepository.findById(input.getId()).orElse(null);
        if (existing != null) entityManager.refresh(existing, LockModeType.PESSIMISTIC_WRITE);
        int quantity;
        try {
            quantity = Math.addExact(existing == null ? 0 : existing.getQuantity(), input.getQuantity());
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Số lượng món vượt giới hạn cho phép");
        }
        validateOrderStock(orderId, menuItemId, quantity);

        if (existing != null) {
            existing.setQuantity(quantity);
            if (note != null) existing.setNote(note);
            // Gọi thêm cùng món vẫn giữ đơn giá đã chốt của dòng cũ.
            return orderItemRepository.save(existing);
        }

        input.setUnitPrice(menuItem.getPrice());
        input.setNote(note);
        return orderItemRepository.save(input);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getAll() {
        return orderItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getByOrderId(Integer orderId) {
        return orderItemRepository.findByIdOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItem getById(OrderItemId id) {
        validateId(id);
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy món ăn này trong đơn hàng"));
    }

    @Override
    public OrderItem update(OrderItemId id, OrderItem input) {
        validateId(id);
        if (input == null) throw new BadRequestException("Thiếu thông tin món cần sửa");
        lockOpenOrder(id.getOrderId());
        OrderItem existing = getById(id);
        entityManager.refresh(existing, LockModeType.PESSIMISTIC_WRITE);
        int quantity = input.getQuantity() == null ? existing.getQuantity() : input.getQuantity();
        if (quantity <= 0) {
            orderItemRepository.delete(existing);
            return existing;
        }

        if (quantity > existing.getQuantity()) {
            requireAvailableMenuItem(id.getMenuItemId());
            validateOrderStock(id.getOrderId(), id.getMenuItemId(), quantity);
        }
        // Giảm số lượng hoặc sửa ghi chú vẫn được phép nếu kho vừa thiếu/món vừa ngừng bán.
        String note = input.getNote() == null ? existing.getNote() : normalizeNote(input.getNote());
        existing.setQuantity(quantity);
        existing.setNote(note);
        // Không cho client thay đơn giá đã chốt bằng thao tác sửa số lượng/ghi chú.
        return orderItemRepository.save(existing);
    }

    @Override
    public void delete(OrderItemId id) {
        validateId(id);
        lockOpenOrder(id.getOrderId());
        orderItemRepository.delete(getById(id));
    }

    private void lockOpenOrder(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng có mã ID: " + orderId));
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        if (!"OPEN".equalsIgnoreCase(order.getStatus()) && !"SERVING".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Đơn hàng đã kết thúc, không thể thay đổi món");
        }
    }

    private Menuitem requireAvailableMenuItem(Integer menuItemId) {
        Menuitem item = menuItemRepository.findById(menuItemId.longValue())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy món ăn có mã ID: " + menuItemId));
        if (Boolean.TRUE.equals(item.getDeleted()) || !Boolean.TRUE.equals(item.getAvailable())) {
            throw new BadRequestException("Món '" + item.getNameVi() + "' hiện đang tạm ngưng phục vụ");
        }
        if (item.getPrice() == null || item.getPrice().signum() < 0) {
            throw new BadRequestException("Giá món '" + item.getNameVi() + "' không hợp lệ");
        }
        return item;
    }

    /** Cộng nhu cầu chung của toàn bộ đơn, kể cả các món dùng chung một nguyên liệu. */
    private void validateOrderStock(Integer orderId, Integer changedMenuItemId, int changedQuantity) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem line : orderItemRepository.findByOrderIdForUpdate(orderId)) {
            entityManager.refresh(line, LockModeType.PESSIMISTIC_WRITE);
            quantities.put(line.getId().getMenuItemId(), line.getQuantity());
        }
        quantities.put(changedMenuItemId, changedQuantity);

        Map<Long, BigDecimal> required = new HashMap<>();
        Map<Long, Ingredient> ingredients = new HashMap<>();
        for (Map.Entry<Integer, Integer> line : quantities.entrySet()) {
            for (Recipe recipe : recipeRepository.findByMenuItem(line.getKey().longValue())) {
                Ingredient ingredient = recipe.getIngredient();
                if (ingredient == null || ingredient.getId() == null
                        || recipe.getQuantityNeeded() == null || recipe.getQuantityNeeded().signum() <= 0) {
                    throw new BadRequestException("Công thức món ăn không hợp lệ. Vui lòng kiểm tra lại nguyên liệu");
                }
                required.merge(ingredient.getId(), recipe.getQuantityNeeded()
                        .multiply(BigDecimal.valueOf(line.getValue())), BigDecimal::add);
                ingredients.put(ingredient.getId(), ingredient);
            }
        }
        for (Map.Entry<Long, BigDecimal> requirement : required.entrySet()) {
            Ingredient ingredient = ingredients.get(requirement.getKey());
            BigDecimal stock = ingredient.getStockQty() == null ? BigDecimal.ZERO : ingredient.getStockQty();
            if (stock.compareTo(requirement.getValue()) < 0) {
                throw new BadRequestException("Đơn hàng không đủ nguyên liệu '" + ingredient.getName()
                        + "' (tổng cần: " + requirement.getValue() + " " + ingredient.getUnit()
                        + ", trong kho: " + stock + " " + ingredient.getUnit() + ")");
            }
        }
    }

    private static void validateId(OrderItemId id) {
        if (id == null || id.getOrderId() == null || id.getMenuItemId() == null
                || id.getOrderId() <= 0 || id.getMenuItemId() <= 0) {
            throw new BadRequestException("Mã đơn hàng và mã món ăn không hợp lệ");
        }
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) return null;
        String value = note.trim();
        if (value.length() > 255) throw new BadRequestException("Ghi chú món không được quá 255 ký tự");
        return value;
    }
}
