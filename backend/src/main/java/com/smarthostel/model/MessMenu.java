package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mess_menu")
@Getter
@Setter
@NoArgsConstructor
public class MessMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    /** MON ... SUN */
    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek;

    /** Breakfast, Lunch, Snacks, Dinner */
    @Column(name = "meal_type", nullable = false)
    private String mealType;

    @Column(name = "menu_items", nullable = false)
    private String menuItems;
}
