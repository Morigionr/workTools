package com.schedule;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */
public class ScheduleMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            Scheduler scheduler = new Scheduler();
            scheduler.generateSchedule();

            Printer printer = new Printer(scheduler);
            printer.printMatrix();

            if (printer.askUserForExport()) {
                try {
                    ExcelExporter exporter = new ExcelExporter(scheduler);
                    exporter.exportToExcel("排班表.xlsx");
                } catch (IOException e) {
                    System.err.println("导出Excel失败：" + e.getMessage());
                }
                break; // 导出后退出程序
            } else {
                printer.waitForRegenerate();
                // 循环重新生成
            }
        }
        scanner.close();
    }
}
