package com.smarthostel.dto;

/**
 * One row of StaffRepository.findMaintenanceOptions (Spring Data interface projection:
 * Spring fills these getters from the SQL column aliases).
 */
public interface MaintenanceOption {

    Long getId();

    String getName();

    String getSpecialization();

    Integer getWorkload();
}
