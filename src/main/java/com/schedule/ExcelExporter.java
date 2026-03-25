package com.schedule;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */
public class ExcelExporter {
    private Scheduler scheduler;

    public ExcelExporter(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void exportToExcel(String fileName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("排班表");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("员工\\日期");
        for (int day = 1; day <= Scheduler.TOTAL_DAYS; day++) {
            headerRow.createCell(day).setCellValue(day);
        }

        // 填充数据
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        int rowNum = 1;
        for (Person p : people) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.name);
            for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
                ShiftType shift = monthSchedule.get(day).personShift.get(p.name);
                String cellValue = (shift == null) ? "休息" : shift.name();
                row.createCell(day + 1).setCellValue(cellValue);
            }
        }

        // 自动调整列宽
        for (int i = 0; i <= Scheduler.TOTAL_DAYS; i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入文件
        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            workbook.write(fileOut);
        }
        workbook.close();
        System.out.println("Excel文件已导出：" + fileName);
    }
}
