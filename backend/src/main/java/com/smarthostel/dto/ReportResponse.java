package com.smarthostel.dto;

import java.util.List;
import java.util.Map;

public record ReportResponse(String key, String title, List<String> columns, List<Map<String, Object>> rows) {
}
