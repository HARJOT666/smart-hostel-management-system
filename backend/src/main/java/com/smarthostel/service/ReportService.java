package com.smarthostel.service;

import com.smarthostel.dto.ReportResponse;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.repository.PlsqlRepository;
import com.smarthostel.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reports are always computed live from Oracle (not cached). */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final PlsqlRepository plsqlRepository;

    /** key -> title of every available report */
    public Map<String, String> listReports() {
        Map<String, String> result = new LinkedHashMap<>();
        ReportRepository.REPORTS.forEach((key, value) -> result.put(key, value[0]));
        return result;
    }

    @Transactional(readOnly = true)
    public ReportResponse runReport(String key) {
        String[] report = ReportRepository.REPORTS.get(key);
        if (report == null) {
            throw new ResourceNotFoundException("Report", key);
        }
        List<Map<String, Object>> rows = reportRepository.runReport(report[1]);
        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
        return new ReportResponse(key, report[0], columns, rows);
    }

    /** Text report printed by the cursor procedure sp_hostel_report. */
    @Transactional(readOnly = true)
    public List<String> hostelCursorReport() {
        return plsqlRepository.hostelReport();
    }
}
