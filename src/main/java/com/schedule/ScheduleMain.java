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
        System.out.print("请输入第几个月（默认30天）：");
        int weekNumber;
        try {
            weekNumber = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("输入无效，默认第1个月。");
            weekNumber = 1;
        }

        while (true) {
            Scheduler scheduler = new Scheduler(weekNumber);
            scheduler.generateSchedule();

            Printer printer = new Printer(scheduler);
            // 控制台输出顺序：
            printer.printDailyShiftCounts();        // 1. 每天班次数量
            printer.printDailyShiftAssignments();   // 2. 每天班次人员
            printer.printDailyPersonSequence();     // 3. 每天员工班次顺序
            printer.printMatrix();                  // 矩阵（可选，保留）
            printer.printDailySequence();           // 员工班次顺序
            printer.printRestDays();                // 连续休息统计

            if (printer.askUserForExport()) {
                try {
                    ExcelExporter exporter = new ExcelExporter(scheduler);
                    exporter.exportToExcel("排班表_第" + weekNumber + "月.xlsx");
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
