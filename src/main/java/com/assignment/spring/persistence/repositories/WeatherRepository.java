package com.assignment.spring.persistence.repositories;

import com.assignment.spring.persistence.entities.WeatherEntity;
import org.springframework.data.repository.CrudRepository;

public interface WeatherRepository extends CrudRepository<WeatherEntity, Integer> {
}
