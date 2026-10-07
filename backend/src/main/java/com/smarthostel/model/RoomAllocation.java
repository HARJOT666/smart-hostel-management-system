package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Read-only from Java's point of view: allocations are created by the
 * Oracle procedure sp_allocate_room and closed by sp_vacate_room.
 */
@Entity
@Table(name = "room_allocation")
@Getter
@Setter
@NoArgsConstructor
public class RoomAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allocated_by")
    private Staff allocatedBy;

    @Column(name = "bed_number", nullable = false)
    private Integer bedNumber;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(name = "vacate_date")
    private LocalDate vacateDate;

    /** Active, Vacated or Cancelled */
    @Column(nullable = false)
    private String status;
}
