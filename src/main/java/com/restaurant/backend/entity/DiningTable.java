package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
 
import java.util.ArrayList;
import java.util.List;
 
@Entity
@Table(name = "dining_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiningTable {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "table_code", nullable = false, unique = true, length = 20)
    private String tableCode;
 
    @Column(nullable = false)
    private Integer capacity;
 
    @Column(nullable = false, length = 50)
    private String zone;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.AVAILABLE;
 
    // 1 bàn - N lượt đặt bàn
    @OneToMany(mappedBy = "table")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Reservation> reservations = new ArrayList<>();
}