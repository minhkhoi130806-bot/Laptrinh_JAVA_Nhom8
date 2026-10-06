package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
 
    @Column(nullable = false, unique = true, length = 15)
    private String phone;
 
    @Column(length = 100)
    private String email;
 
    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;
 
    // 1 khách - N lượt đặt bàn
    @OneToMany(mappedBy = "customer")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Reservation> reservations = new ArrayList<>();
}
 