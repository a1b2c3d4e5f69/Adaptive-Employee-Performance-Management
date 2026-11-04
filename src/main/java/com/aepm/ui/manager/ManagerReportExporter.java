package com.aepm.ui.manager;

import com.google.gson.JsonObject;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.fontbox.ttf.TrueTypeCollection;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ManagerReportExporter {

    private ManagerReportExporter() {
    }

    private static final String[] SCORE_KEYS = {
            "productivity_score",
            "timeliness_score",
            "completion_rate",
            "attendance_score",
            "overall_kpi_score"
    };

    private static final String[] SCORE_LABELS = {
            "Productivity",
            "Timeliness",
            "Completion",
            "Attendance",
            "Overall KPI"
    };

    private static final String[] HEADERS = {
            "Result ID",
            "Employee No.",
            "Employee",
            "Department",
            "Account Status",
            "Period Start",
            "Period End",
            "Productivity",
            "Timeliness",
            "Completion",
            "Attendance",
            "Overall KPI",
            "Performance",
            "Config ID",
            "KPI Configuration",
            "Calculated At (MYT)"
    };

    private static String text(JsonObject row, String key) {
        return !row.has(key) || row.get(key).isJsonNull()
                ? ""
                : row.get(key).getAsString();
    }

    private static Double number(JsonObject row, String key) {
        if (!row.has(key) || row.get(key).isJsonNull()) {
            return null;
        }

        double value = row.get(key).getAsDouble();

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Invalid KPI score in report."
            );
        }

        return value;
    }

    private static String calculated(JsonObject row) {
        String value = text(row, "calculated_at");

        if (value.isBlank()) {
            return "";
        }

        return OffsetDateTime.parse(value)
                .atZoneSameInstant(ZoneId.of("Asia/Kuala_Lumpur"))
                .format(DateTimeFormatter.ofPattern(
                        "dd MMM yyyy HH:mm:ss",
                        Locale.ENGLISH
                ));
    }

    public static void writeExcel(
            Path destination,
            List<JsonObject> rows,
            List<String> metadata
    ) throws IOException {

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "No report records to export."
            );
        }

        try (XSSFWorkbook book = new XSSFWorkbook()) {
            Sheet info = book.createSheet("Report Information");
            info.setColumnWidth(0, 110 * 256);

            CellStyle wrap = book.createCellStyle();
            wrap.setWrapText(true);

            List<String> notes = new ArrayList<>();
            notes.add("AEPM — Employee Performance & KPI Report");
            notes.addAll(metadata);
            notes.add("Exported records: " + rows.size());
            notes.add(
                    "All matching report pages are included. "
                    + "Scores are numeric; missing scores are blank."
            );

            int line = 0;

            for (String note : notes) {
                Row row = info.createRow(line++);
                row.setHeightInPoints(36);

                Cell cell = row.createCell(0);
                cell.setCellValue(note);
                cell.setCellStyle(wrap);
            }

            Sheet sheet = book.createSheet("Performance Reports");
            sheet.createFreezePane(3, 1);

            CellStyle header = book.createCellStyle();
            header.setFillForegroundColor(
                    IndexedColors.ROYAL_BLUE.getIndex()
            );
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setWrapText(true);

            org.apache.poi.ss.usermodel.Font headerFont =
                    book.createFont();

            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(headerFont);

            CellStyle decimal = book.createCellStyle();
            decimal.setDataFormat(
                    book.createDataFormat().getFormat("0.00")
            );

            CellStyle dateStyle = book.createCellStyle();
            dateStyle.setDataFormat(
                    book.createDataFormat().getFormat("dd mmm yyyy")
            );

            Row titles = sheet.createRow(0);
            titles.setHeightInPoints(32);

            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = titles.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(header);

                int width = i == 2 || i == 14
                        ? 30
                        : i == 15 ? 26 : 18;

                sheet.setColumnWidth(i, width * 256);
            }

            int index = 1;

            for (JsonObject record : rows) {
                Row row = sheet.createRow(index++);

                String[] firstKeys = {
                        "result_id",
                        "employee_number",
                        "full_name",
                        "department",
                        "account_status"
                };

                for (int i = 0; i < firstKeys.length; i++) {
                    row.createCell(i).setCellValue(
                            text(record, firstKeys[i])
                    );
                }

                for (int i = 0; i < 2; i++) {
                    String key = i == 0
                            ? "evaluation_start_date"
                            : "evaluation_end_date";

                    Cell cell = row.createCell(5 + i);
                    cell.setCellValue(
                            LocalDate.parse(text(record, key))
                    );
                    cell.setCellStyle(dateStyle);
                }

                for (int i = 0; i < SCORE_KEYS.length; i++) {
                    Cell cell = row.createCell(7 + i);
                    Double score = number(record, SCORE_KEYS[i]);

                    if (score != null) {
                        cell.setCellValue(score);
                    }

                    cell.setCellStyle(decimal);
                }

                row.createCell(12).setCellValue(
                        text(record, "performance_status")
                );
                row.createCell(13).setCellValue(
                        text(record, "config_id")
                );
                row.createCell(14).setCellValue(
                        text(record, "config_name")
                );
                row.createCell(15).setCellValue(
                        calculated(record)
                );
            }

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            0, rows.size(), 0, HEADERS.length - 1
                    )
            );

            sheet.setRepeatingRows(
                    new CellRangeAddress(0, 0, -1, -1)
            );

            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            try (OutputStream out = Files.newOutputStream(destination)) {
                book.write(out);
            }
        }
    }

    public static void writePdf(
            Path destination,
            List<JsonObject> rows,
            List<String> metadata
    ) throws IOException {

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "No report records to export."
            );
        }

        try (PdfWriter writer = new PdfWriter()) {
            writer.document.getDocumentInformation().setTitle(
                    "AEPM Performance & KPI Report"
            );
            writer.document.getDocumentInformation().setAuthor("AEPM");

            writer.newPage();

            for (String note : metadata) {
                writer.paragraph(
                        note, 10, new Color(71, 85, 105)
                );
            }

            writer.paragraph(
                    "Exported records: " + rows.size(),
                    10,
                    Color.DARK_GRAY
            );

            writer.y -= 12;

            int index = 0;

            for (JsonObject row : rows) {
                writer.ensureSpace(210);

                writer.paragraph(
                        ++index + ". "
                        + text(row, "full_name")
                        + " | "
                        + text(row, "employee_number"),
                        13,
                        new Color(15, 23, 42)
                );

                writer.paragraph(
                        "Department: " + text(row, "department")
                        + " | Account: " + text(row, "account_status"),
                        10,
                        Color.DARK_GRAY
                );

                writer.paragraph(
                        "Evaluation: "
                        + text(row, "evaluation_start_date")
                        + " to "
                        + text(row, "evaluation_end_date"),
                        10,
                        Color.DARK_GRAY
                );

                writer.paragraph(
                        "Configuration: " + text(row, "config_name")
                        + " [#" + text(row, "config_id") + "]",
                        10,
                        Color.DARK_GRAY
                );

                writer.ensureSpace(55);

                float width = writer.contentWidth / 5;

                writer.stream.setNonStrokingColor(
                        new Color(239, 246, 255)
                );
                writer.stream.addRect(
                        writer.margin,
                        writer.y - 42,
                        writer.contentWidth,
                        48
                );
                writer.stream.fill();

                for (int i = 0; i < SCORE_KEYS.length; i++) {
                    writer.draw(
                            SCORE_LABELS[i],
                            writer.margin + i * width + 6,
                            writer.y - 8,
                            9,
                            new Color(51, 65, 85)
                    );

                    Double score = number(row, SCORE_KEYS[i]);

                    writer.draw(
                            score == null
                                    ? "—"
                                    : String.format(
                                            Locale.ENGLISH,
                                            "%.2f",
                                            score
                                    ),
                            writer.margin + i * width + 6,
                            writer.y - 28,
                            13,
                            new Color(29, 78, 216)
                    );
                }

                writer.y -= 55;

                writer.paragraph(
                        "Performance: "
                        + text(row, "performance_status")
                        + " | Result ID: "
                        + text(row, "result_id"),
                        10,
                        Color.DARK_GRAY
                );

                writer.paragraph(
                        "Calculated (MYT): " + calculated(row),
                        9,
                        new Color(100, 116, 139)
                );

                writer.y -= 10;
            }

            writer.finishPage();
            writer.document.save(destination.toFile());
        }
    }

    private static final class PdfWriter implements AutoCloseable {

        final PDDocument document = new PDDocument();
        final List<PDFont> fonts = new ArrayList<>();
        final List<TrueTypeCollection> collections = new ArrayList<>();
        final Map<Integer, PDFont> fontCache = new HashMap<>();

        final float margin = 36;
        final float contentWidth = PDRectangle.A4.getWidth() - 72;

        PDPageContentStream stream;
        float y;
        int pageNumber;

        PdfWriter() throws IOException {
            try {
                String windows = System.getenv("WINDIR");

                if (windows != null) {
                    Path directory = Path.of(windows, "Fonts");

                    loadFont(directory.resolve("segoeui.ttf"));

                    if (fonts.isEmpty()) {
                        loadFont(directory.resolve("arial.ttf"));
                    }

                    Path cjk = directory.resolve("msyh.ttc");

                    if (!Files.isRegularFile(cjk)) {
                        cjk = directory.resolve("simsun.ttc");
                    }

                    if (Files.isRegularFile(cjk)) {
                        TrueTypeCollection collection =
                                new TrueTypeCollection(cjk.toFile());

                        collections.add(collection);

                        collection.processAllFonts(font ->
                                fonts.add(
                                        PDType0Font.load(
                                                document,
                                                font,
                                                true
                                        )
                                )
                        );
                    }
                }

                if (fonts.isEmpty()) {
                    loadFont(Path.of(
                            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
                    ));
                }

                if (fonts.isEmpty()) {
                    throw new IOException(
                            "No supported PDF font found. "
                            + "Install Segoe UI or DejaVu Sans."
                    );
                }
            } catch (Exception error) {
                close();

                if (error instanceof IOException io) {
                    throw io;
                }

                throw new IOException(
                        "Unable to load report fonts.",
                        error
                );
            }
        }

        void loadFont(Path path) throws IOException {
            if (Files.isRegularFile(path)) {
                fonts.add(
                        PDType0Font.load(document, path.toFile())
                );
            }
        }

        PDFont font(int codePoint) throws IOException {
            PDFont cached = fontCache.get(codePoint);

            if (cached != null) {
                return cached;
            }

            String value = new String(
                    Character.toChars(codePoint)
            );

            for (PDFont candidate : fonts) {
                try {
                    candidate.encode(value);
                    fontCache.put(codePoint, candidate);
                    return candidate;
                } catch (IllegalArgumentException ignored) {
                }
            }

            throw new IOException(
                    "PDF font cannot display character U+"
                    + Integer.toHexString(codePoint)
                            .toUpperCase(Locale.ROOT)
                    + ". Export Excel instead or install a font "
                    + "supporting this character."
            );
        }

        float width(int codePoint, float size) throws IOException {
            return font(codePoint).getStringWidth(
                    new String(Character.toChars(codePoint))
            ) * size / 1000;
        }

        void draw(
                String text,
                float x,
                float baseline,
                float size,
                Color color
        ) throws IOException {

            stream.setNonStrokingColor(color);

            for (int codePoint : text.codePoints().toArray()) {
                String part = new String(
                        Character.toChars(codePoint)
                );

                stream.beginText();
                stream.setFont(font(codePoint), size);
                stream.newLineAtOffset(x, baseline);
                stream.showText(part);
                stream.endText();

                x += width(codePoint, size);
            }
        }

        void paragraph(
                String input,
                float size,
                Color color
        ) throws IOException {

            String clean = input.replaceAll(
                    "[\\p{Cntrl}\\p{Zl}\\p{Zp}]",
                    " "
            );

            StringBuilder line = new StringBuilder();
            float occupied = 0;

            for (int codePoint : clean.codePoints().toArray()) {
                float advance = width(codePoint, size);

                if (occupied + advance > contentWidth
                        && !line.isEmpty()) {

                    ensureSpace(size + 6);
                    draw(
                            line.toString(),
                            margin,
                            y,
                            size,
                            color
                    );

                    y -= size + 5;
                    line.setLength(0);
                    occupied = 0;
                }

                line.appendCodePoint(codePoint);
                occupied += advance;
            }

            ensureSpace(size + 6);
            draw(line.toString(), margin, y, size, color);
            y -= size + 6;
        }

        void ensureSpace(float height) throws IOException {
            if (y - height < 44) {
                newPage();
            }
        }

        void newPage() throws IOException {
            finishPage();

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            stream = new PDPageContentStream(document, page);
            pageNumber++;
            y = page.getMediaBox().getHeight() - 42;

            draw(
                    "AEPM | Performance & KPI Reports",
                    margin,
                    y,
                    17,
                    new Color(29, 78, 216)
            );

            y -= 30;
        }

        void finishPage() throws IOException {
            if (stream != null) {
                draw(
                        "Manager report | Page " + pageNumber,
                        margin,
                        24,
                        9,
                        new Color(100, 116, 139)
                );

                stream.close();
                stream = null;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                if (stream != null) {
                    stream.close();
                }
            } finally {
                try {
                    document.close();
                } finally {
                    for (TrueTypeCollection collection : collections) {
                        collection.close();
                    }
                }
            }
        }
    }
}