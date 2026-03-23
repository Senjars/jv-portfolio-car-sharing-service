package com.github.senjars.carsharing.repository;

import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarTypeRepository extends JpaRepository<CarType, Long> {

    Optional<CarType> findByTypeName(TypeName typeName);
}
