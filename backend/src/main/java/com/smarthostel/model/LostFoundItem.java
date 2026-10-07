package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "lost_found_item")
@Getter
@Setter
@NoArgsConstructor
public class LostFoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by")
    private Student reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by")
    private Student claimedBy;

    /** Lost or Found */
    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    private String description;

    private String location;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    /** Open, Claimed, Closed */
    @Column(nullable = false)
    private String status = "Open";

    @Column(name = "image_path")
    private String imagePath;
}
