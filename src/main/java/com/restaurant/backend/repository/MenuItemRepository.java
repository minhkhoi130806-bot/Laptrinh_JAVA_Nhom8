package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Menuitem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<Menuitem, Long> {

    // join fetch để lấy luôn tên nhóm, không bị lỗi lazy khi map sang Response
    @Query("select m from Menuitem m join fetch m.category order by m.id desc")
    List<Menuitem> findAllWithCategory();

    @Query("select m from Menuitem m join fetch m.category where m.id = :id")
    Optional<Menuitem> findWithCategoryById(@Param("id") Long id);

    // Số món (kể cả thùng rác) trong 1 nhóm - dùng chặn xóa nhóm đang có món
    long countByCategoryId(Long categoryId);

    // Số món đang dùng (chưa vào thùng rác) trong 1 nhóm
    long countByCategoryIdAndDeleted(Long categoryId, Boolean deleted);

    // Mỗi dòng: [categoryId, số món đang bán chưa vào thùng rác]
    @Query("select m.category.id, count(m) from Menuitem m where m.deleted = false group by m.category.id")
    List<Object[]> countActiveByCategory();
}
