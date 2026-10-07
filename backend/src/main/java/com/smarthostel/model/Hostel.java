package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hostel")
@Getter
@Setter
@NoArgsConstructor
public class Hostel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hostel_id")
    private Long id;

    @Column(name = "hostel_name", nullable = false, unique = true)
    private String hostelName;

    @Column(name = "gender_type", nullable = false)
    private String genderType;

    @Column(nullable = false)
    private String location;

    @Column(name = "total_floors")
    private Integer totalFloors;
}
