package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Admin, Warden or Maintenance */
    @Column(nullable = false)
    private String role;

    /** Only for maintenance staff: Electrical, Plumbing, Carpentry, Housekeeping, Network */
    private String specialization;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
