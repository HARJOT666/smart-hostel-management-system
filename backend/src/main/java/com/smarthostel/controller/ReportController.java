package com.smarthostel.controller;

import com.smarthostel.dto.ReportResponse;
import com.smarthostel.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public Map<String, String> list() {
        return reportService.listReports();
    }

    /** Output of the cursor procedure sp_hostel_report (read from DBMS_OUTPUT). */
    @GetMapping("/hostel-cursor-report")
    public List<String> hostelCursorReport() {
        return reportService.hostelCursorReport();
    }

    @GetMapping("/{key}")
    public ReportResponse run(@PathVariable String key) {
        return reportService.runReport(key);
    }
}
