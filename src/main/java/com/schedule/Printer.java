package com.schedule;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */

import java.util.List;
import java.util.Scanner;

/**
 *  负责输出
 */
public class Printer {
    private Scheduler scheduler;

    public Printer(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void printMatrix() {
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        // 打印表头：员工\日期 1 2 3 ... 30
        System.out.print("员工\\日期");
        for (int day = 1; day <= Scheduler.TOTAL_DAYS; day++) {
            System.out.printf("%4d", day);
        }
        System.out.println();

        // 打印每个人的班次矩阵
        for (Person p : people) {
            System.out.printf("%-8s", p.name); // 左对齐，宽度8
            for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
                ShiftType shift = monthSchedule.get(day).personShift.get(p.name);
                if (shift == null) {
                    System.out.print("  休");
                } else {
                    System.out.printf("%4s", shift);
                }
            }
            System.out.println();
        }
    }

    public boolean askUserForExport() {
        System.out.println("\n排班结果如上，是否导出为Excel？(y/n)");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim().toLowerCase();
        return "y".equals(input);
    }

    public void waitForRegenerate() {
        System.out.println("按回车键重新生成排班...");
        new Scanner(System.in).nextLine();
    }
}
