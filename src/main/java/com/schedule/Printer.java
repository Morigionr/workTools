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
    private final Scheduler scheduler;

    public Printer(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void printMatrix() {
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> weekSchedule = scheduler.getWeekSchedule();

        System.out.print("员工 \\ D");
        for (int day = 1; day <= Scheduler.TOTAL_DAYS; day++) {
            System.out.printf("%4d", day);
        }
        System.out.println();

        for (Person p : people) {
            System.out.printf("%-8s", p.name);
            for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
                ShiftType shift = weekSchedule.get(day).personShift.get(p.name);
                if (shift == null) {
                    System.out.print("  休");
                } else {
                    System.out.printf("%4s", shift);
                }
            }
            System.out.println();
        }
    }

    /**
     * 输出每个员工一周的班次顺序（用 - 连接）
     */
    public void printDailySequence() {
        System.out.println("\n===== 员工7天班次顺序 =====");
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> weekSchedule = scheduler.getWeekSchedule();
        for (Person p : people) {
            StringBuilder sb = new StringBuilder(p.name + ": ");
            for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
                ShiftType shift = weekSchedule.get(day).personShift.get(p.name);
                if (shift == null) {
                    sb.append("休");
                } else {
                    sb.append(shift);
                }
                if (day < Scheduler.TOTAL_DAYS - 1) {
                    sb.append(" - ");
                }
            }
            System.out.println(sb.toString());
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
