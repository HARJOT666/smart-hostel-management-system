package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(name = "floor_no")
    private Integer floorNo;

    @Column(name = "room_type")
    private String roomType;

    @Column(nullable = false)
    private Integer capacity;

    // Maintained ONLY by the Oracle trigger trg_allocation_occupancy.
    // Java never writes it; Hibernate re-reads it after insert/update.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "occupied_count", insertable = false, updatable = false)
    private Integer occupiedCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    public int getAvailableBeds() {
        return capacity - (occupiedCount == null ? 0 : occupiedCount);
    }
}
