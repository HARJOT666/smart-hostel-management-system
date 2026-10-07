package com.smarthostel.controller;

import com.smarthostel.dto.AdminDashboardResponse;
import com.smarthostel.dto.MaintenanceDashboardResponse;
import com.smarthostel.dto.StudentDashboardResponse;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public AdminDashboardResponse admin() {
        return dashboardService.adminDashboard();
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentDashboardResponse student(@AuthenticationPrincipal AppUserDetails user) {
        return dashboardService.studentDashboard(user.getId());
    }

    @GetMapping("/maintenance")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public MaintenanceDashboardResponse maintenance(@AuthenticationPrincipal AppUserDetails user) {
        return dashboardService.maintenanceDashboard(user.getId());
    }
}
