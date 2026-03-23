package com.github.senjars.carsharing.repository;

import com.github.senjars.carsharing.model.car.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
}
