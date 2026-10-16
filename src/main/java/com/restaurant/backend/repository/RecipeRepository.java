package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Recipe;
import com.restaurant.backend.entity.RecipeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, RecipeId> {

    @Query("select r from Recipe r join fetch r.ingredient where r.id.menuItemId = :menuItemId order by r.ingredient.name")
    List<Recipe> findByMenuItem(@Param("menuItemId") Long menuItemId);

    // Xóa ngay lập tức (trước khi insert lại) để không vướng khóa chính kép
    @Modifying(flushAutomatically = true)
    @Query("delete from Recipe r where r.id.menuItemId = :menuItemId")
    void deleteByMenuItem(@Param("menuItemId") Long menuItemId);
}
