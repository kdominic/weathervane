package com.assignment.spring.persistence.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "weather")
@EntityListeners(AuditingEntityListener.class)
public class WeatherEntity {
    @Id
    @Column(name = "weather_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "temperature", columnDefinition = "NUMERIC(5,2)")
    private Double temperature;

    @CreatedDate
    @Column(name = "creation_date")
    private LocalDateTime creationDate;
}
