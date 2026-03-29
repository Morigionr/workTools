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
        System.out.print("请输入第几周（1-...）：");
        int weekNumber;
        try {
            weekNumber = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("输入无效，使用第1周。");
            weekNumber = 1;
        }

        while (true) {
            Scheduler scheduler = new Scheduler(weekNumber);
            scheduler.generateSchedule();

            Printer printer = new Printer(scheduler);
            printer.printMatrix();
            printer.printDailySequence();   // 新增调用

            if (printer.askUserForExport()) {
                try {
                    ExcelExporter exporter = new ExcelExporter(scheduler);
                    exporter.exportToExcel("排班表_第" + weekNumber + "周.xlsx");
                } catch (IOException e) {
                    System.err.println("导出Excel失败：" + e.getMessage());
                }
                break;
            } else {
                printer.waitForRegenerate();
            }
        }
        scanner.close();
    }
}
