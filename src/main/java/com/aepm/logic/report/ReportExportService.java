package com.aepm.logic.report;

import com.aepm.model.User;
import com.aepm.model.KpiResult;
import com.aepm.ui.manager.ManagerReportExporter;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Exports a caller-supplied snapshot of matching records, never a fresh unfiltered query. */
public final class ReportExportService {
    private ReportExportService() {}

    public static JsonObject record(User employee, KpiResult result) {
        Objects.requireNonNull(employee, "employee");
        Objects.requireNonNull(result, "result");
        JsonObject row = new JsonObject();
        row.addProperty("result_id", result.getResultId());
        row.addProperty("employee_number", employee.getEmployeeId());
        row.addProperty("full_name", employee.getFullName());
        row.addProperty("department", employee.getDepartment());
        row.addProperty("account_status", employee.getAccountStatus());
        row.addProperty("evaluation_start_date", Objects.toString(result.getEvaluationStartDate(), ""));
        row.addProperty("evaluation_end_date", Objects.toString(result.getEvaluationEndDate(), ""));
        row.addProperty("productivity_score", result.getProductivityScore());
        row.addProperty("timeliness_score", result.getTimelinessScore());
        row.addProperty("completion_rate", result.getCompletionRate());
        row.addProperty("attendance_score", result.getAttendanceScore());
        row.addProperty("overall_kpi_score", result.getOverallKpiScore());
        row.addProperty("performance_status", result.getPerformanceStatus());
        row.addProperty("config_id", result.getConfigId());
        row.addProperty("calculated_at", Objects.toString(result.getCalculatedAt(), ""));
        return row;
    }

    /** Preserves an existing destination if validation or rendering fails. */
    public static void write(Path destination, List<JsonObject> rows,
                             List<String> metadata, boolean pdf) throws IOException {
        if (rows.isEmpty()) throw new IllegalArgumentException("No report records to export.");
        List<JsonObject> snapshot = rows.stream().map(JsonObject::deepCopy).toList();
        List<String> notes = List.copyOf(metadata);
        Path target = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".aepm-report-", pdf ? ".pdf" : ".xlsx");
        try {
            if (pdf) ManagerReportExporter.writePdf(temporary, snapshot, notes);
            else ManagerReportExporter.writeExcel(temporary, snapshot, notes);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
