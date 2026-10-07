package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint")
@Getter
@Setter
@NoArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    /** NULL until the warden assigns it (done by sp_assign_complaint). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private Staff assignedTo;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String priority = "Medium";

    /** Open, Assigned, In Progress, Resolved */
    @Column(nullable = false)
    private String status = "Open";

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    // Set by the Oracle trigger trg_complaint_status when status becomes 'Resolved'.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "resolved_date", insertable = false, updatable = false)
    private LocalDateTime resolvedDate;

    private String remarks;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "completion_image_path")
    private String completionImagePath;
}
