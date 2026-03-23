package com.github.senjars.carsharing.model.car;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@SQLDelete(sql = "UPDATE cars SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private CarType type;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private int inventory;

    @Column(name = "daily_fee", nullable = false)
    private BigDecimal dailyFee;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;
}
