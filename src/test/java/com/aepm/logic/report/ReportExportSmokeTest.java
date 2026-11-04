package com.aepm.logic.report;

import com.aepm.model.*;
import com.aepm.util.SessionManager;
import com.aepm.ui.admin.AdminReportController;
import com.aepm.ui.manager.ManagerReportController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Standalone integration checks; no database or visible application window required. */
public final class ReportExportSmokeTest {
    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                checkController(new AdminReportController(), 1);
                checkController(new ManagerReportController(), 2);
                done.complete(null);
            } catch (Throwable failure) { done.completeExceptionally(failure); }
        });
        try { done.get(); }
        finally { Platform.exit(); }
        System.out.println("PASS: Admin/Manager PDF and Excel, combined filters, empty results, invalid periods, numeric scores and destination preservation.");
    }

    private static void checkController(Object controller, long role) throws Exception {
        User signedIn = new User();
        signedIn.setRoleId(role);
        var current = SessionManager.class.getDeclaredField("currentUser");
        current.setAccessible(true);
        Object previous = current.get(null);
        Path directory = Files.createTempDirectory("aepm-export-test-");
        try {
            current.set(null, signedIn);
            TextField search = new TextField("Alice");
            ComboBox<String> departments = new ComboBox<>();
            departments.setValue("Engineering");
            ComboBox<String> statuses = new ComboBox<>();
            statuses.setValue("Good");
            DatePicker start = new DatePicker(LocalDate.of(2026, 2, 1));
            DatePicker end = new DatePicker(LocalDate.of(2026, 2, 28));
            set(controller, "searchField", search);
            set(controller, "departmentFilterComboBox", departments);
            set(controller, "statusFilterComboBox", statuses);
            set(controller, "startDatePicker", start);
            set(controller, "endDatePicker", end);
            for (String name : List.of("totalRecordsLabel", "averageKpiLabel", "atRiskCountLabel", "normalGoodCountLabel", "messageLabel")) {
                set(controller, name, new Label());
            }
            var allField = controller.getClass().getDeclaredField("allRows");
            allField.setAccessible(true);
            @SuppressWarnings("unchecked")
            javafx.collections.ObservableList<Object> all =
                    (javafx.collections.ObservableList<Object>) allField.get(controller);
            all.add(row(controller, "Alice MATCH", "Engineering", 2, "Good"));
            all.add(row(controller, "Bob EXCLUDED", "Engineering", 2, "Good"));
            all.add(row(controller, "Alice WRONG_DEPT", "Sales", 2, "Good"));
            all.add(row(controller, "Alice WRONG_PERIOD", "Engineering", 1, "Good"));
            all.add(row(controller, "Alice WRONG_STATUS", "Engineering", 2, "At Risk"));
            set(controller, "filteredRows", new FilteredList<>(all));
            for (boolean pdf : List.of(true, false)) {
                Path output = directory.resolve(pdf ? "report.pdf" : "report.xlsx");
                export(controller, output, pdf);
                if (pdf) {
                    try (var document = Loader.loadPDF(output.toFile())) {
                        String text = new PDFTextStripper().getText(document);
                        require(text.contains("Alice MATCH"), "PDF missing matching employee");
                        require(!text.contains("EXCLUDED") && !text.contains("WRONG_"), "PDF leaked filtered records");
                    }
                } else {
                    try (var input = Files.newInputStream(output); var book = new XSSFWorkbook(input)) {
                        var sheet = book.getSheet("Performance Reports");
                        require(sheet.getLastRowNum() == 1, "Excel must contain exactly one result");
                        require(sheet.getRow(1).getCell(2).getStringCellValue().equals("Alice MATCH"), "Wrong Excel employee");
                        require(sheet.getRow(1).getCell(11).getNumericCellValue() == 88.5, "KPI must remain numeric");
                    }
                }
                byte[] before = Files.readAllBytes(output);
                search.setText("No matching employee");
                expectRejected(controller, output, pdf);
                require(Arrays.equals(before, Files.readAllBytes(output)), "Empty export overwrote destination");
                search.setText("Alice");
                start.setValue(LocalDate.of(2026, 3, 1));
                expectRejected(controller, output, pdf);
                require(Arrays.equals(before, Files.readAllBytes(output)), "Invalid range overwrote destination");
                start.setValue(LocalDate.of(2026, 2, 1));
            }
        } finally {
            current.set(null, previous);
            try (var files = Files.list(directory)) {
                for (Path file : files.toList()) Files.delete(file);
            }
            Files.delete(directory);
        }
    }

    private static Object row(Object controller, String name, String department, int month, String status) throws Exception {
        User employee = new User();
        employee.setFullName(name);
        employee.setEmployeeId("EMP-" + name);
        employee.setDepartment(department);
        employee.setAccountStatus("Active");
        KpiResult result = new KpiResult();
        result.setResultId(1L);
        result.setOverallKpiScore(88.5);
        result.setPerformanceStatus(status);
        result.setEvaluationStartDate(LocalDate.of(2026, month, 1));
        result.setEvaluationEndDate(LocalDate.of(2026, month, 28));
        Class<?> type = Class.forName(controller.getClass().getName() + "$ReportRow");
        return type.getConstructor(User.class, KpiResult.class).newInstance(employee, result);
    }

    private static void export(Object controller, Path path, boolean pdf) throws Exception {
        controller.getClass().getMethod(pdf ? "exportPdf" : "exportExcel", Path.class).invoke(controller, path);
    }

    private static void expectRejected(Object controller, Path path, boolean pdf) throws Exception {
        try { export(controller, path, pdf); }
        catch (java.lang.reflect.InvocationTargetException expected) {
            if (expected.getCause() instanceof IllegalArgumentException) return;
            throw expected;
        }
        throw new AssertionError("Invalid/empty export must be rejected");
    }

    private static void set(Object owner, String name, Object value) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
