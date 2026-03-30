package com.schedule;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ExcelExporter {
    private final Scheduler scheduler;

    public ExcelExporter(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void exportToExcel(String fileName) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // 1. 排班表（矩阵）
        Sheet scheduleSheet = workbook.createSheet("排班表");
        fillScheduleSheet(scheduleSheet);

        // 2. 班次顺序（每个员工的30天顺序）
        Sheet sequenceSheet = workbook.createSheet("班次顺序");
        fillSequenceSheet(sequenceSheet);

        // 3. 连续休息统计
        Sheet restSheet = workbook.createSheet("连续休息统计");
        fillRestSheet(restSheet);

        // 4. 每日班次汇总（A、B、C人员）
        Sheet dailySummarySheet = workbook.createSheet("每日班次汇总");
        fillDailySummarySheet(dailySummarySheet);

        // 5. 每日员工班次顺序
        Sheet dailyPersonSeqSheet = workbook.createSheet("每日员工班次顺序");
        fillDailyPersonSequenceSheet(dailyPersonSeqSheet);

        // 6. 每日班次数量统计（新增）
        Sheet dailyCountSheet = workbook.createSheet("每日班次数量统计");
        fillDailyCountSheet(dailyCountSheet);

        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            workbook.write(fileOut);
        }
        workbook.close();
        System.out.println("Excel文件已导出：" + fileName);
    }

    private void fillScheduleSheet(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("员工\\日期");
        for (int day = 1; day <= Scheduler.TOTAL_DAYS; day++) {
            headerRow.createCell(day).setCellValue(day);
        }

        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        int rowNum = 1;
        for (Person p : people) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.name);
            for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
                ShiftType shift = monthSchedule.get(day).personShift.get(p.name);
                String cellValue = (shift == null) ? "休" : shift.name();
                row.createCell(day + 1).setCellValue(cellValue);
            }
        }

        for (int i = 0; i <= Scheduler.TOTAL_DAYS; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void fillSequenceSheet(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("员工");
        headerRow.createCell(1).setCellValue("30天班次顺序");

        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        int rowNum = 1;
        for (Person p : people) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.name);

            StringBuilder sb = new StringBuilder();
            for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
                ShiftType shift = monthSchedule.get(day).personShift.get(p.name);
                sb.append(shift == null ? "休" : shift.name());
                if (day < Scheduler.TOTAL_DAYS - 1) {
                    sb.append(" - ");
                }
            }
            row.createCell(1).setCellValue(sb.toString());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void fillRestSheet(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("员工");
        headerRow.createCell(1).setCellValue("连续休息日期对");

        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        int[][] pairs = {
                {6, 7}, {13, 14}, {20, 21}, {27, 28}
        };

        int rowNum = 1;
        for (Person p : people) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.name);

            List<String> satisfiedPairs = new ArrayList<>();
            for (int[] pair : pairs) {
                int day1 = pair[0] - 1;
                int day2 = pair[1] - 1;
                boolean rest1 = monthSchedule.get(day1).personShift.get(p.name) == null;
                boolean rest2 = monthSchedule.get(day2).personShift.get(p.name) == null;
                if (rest1 && rest2) {
                    satisfiedPairs.add("第" + pair[0] + "-" + pair[1] + "天");
                }
            }
            String cellValue = satisfiedPairs.isEmpty() ? "无" : String.join("、", satisfiedPairs);
            row.createCell(1).setCellValue(cellValue);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void fillDailySummarySheet(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("日期");
        headerRow.createCell(1).setCellValue("A班");
        headerRow.createCell(2).setCellValue("B班");
        headerRow.createCell(3).setCellValue("C班");

        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
            Row row = sheet.createRow(day + 1);
            row.createCell(0).setCellValue("第" + (day + 1) + "天");

            DaySchedule ds = monthSchedule.get(day);
            String aPerson = ds.assignments.getOrDefault(ShiftType.A, "空缺");
            String bPerson = ds.assignments.getOrDefault(ShiftType.B, "空缺");
            String cPerson = ds.assignments.getOrDefault(ShiftType.C, "空缺");

            row.createCell(1).setCellValue(aPerson);
            row.createCell(2).setCellValue(bPerson);
            row.createCell(3).setCellValue(cPerson);
        }

        for (int i = 0; i <= 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void fillDailyPersonSequenceSheet(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("日期");
        headerRow.createCell(1).setCellValue("当天员工班次顺序（P1-P7）");

        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
            Row row = sheet.createRow(day + 1);
            row.createCell(0).setCellValue("第" + (day + 1) + "天");

            StringBuilder sb = new StringBuilder();
            for (Person p : people) {
                ShiftType shift = monthSchedule.get(day).personShift.get(p.name);
                sb.append(shift == null ? "休" : shift.name());
                sb.append("-");
            }
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            row.createCell(1).setCellValue(sb.toString());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * 新增：每日班次数量统计（每天A、B、C各1次）
     */
    private void fillDailyCountSheet(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("日期");
        headerRow.createCell(1).setCellValue("A班次数");
        headerRow.createCell(2).setCellValue("B班次数");
        headerRow.createCell(3).setCellValue("C班次数");

        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
            Row row = sheet.createRow(day + 1);
            row.createCell(0).setCellValue("第" + (day + 1) + "天");

            DaySchedule ds = monthSchedule.get(day);
            int aCount = ds.assignments.containsKey(ShiftType.A) ? 1 : 0;
            int bCount = ds.assignments.containsKey(ShiftType.B) ? 1 : 0;
            int cCount = ds.assignments.containsKey(ShiftType.C) ? 1 : 0;

            row.createCell(1).setCellValue(aCount);
            row.createCell(2).setCellValue(bCount);
            row.createCell(3).setCellValue(cCount);
        }

        for (int i = 0; i <= 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}