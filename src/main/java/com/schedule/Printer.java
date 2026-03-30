package com.schedule;

import java.util.*;

public class Printer {
    private final Scheduler scheduler;

    public Printer(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void printMatrix() {
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();

        System.out.print("员工\\日期");
        for (int day = 1; day <= Scheduler.TOTAL_DAYS; day++) {
            System.out.printf("%4d", day);
        }
        System.out.println();

        for (Person p : people) {
            System.out.printf("%-8s", p.name);
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

    /**
     * 1. 每天有什么班次（固定A、B、C各一次，仅输出确认）
     */
    public void printDailyShiftCounts() {
        System.out.println("\n===== 每日班次数量统计 =====");
        for (int day = 1; day <= Scheduler.TOTAL_DAYS; day++) {
            System.out.printf("第%2d天: A班:1次, B班:1次, C班:1次%n", day);
        }
    }

    /**
     * 2. 每个班哪个人（每天A、B、C分别由谁上）
     */
    public void printDailyShiftAssignments() {
        System.out.println("\n===== 每日班次人员分配 =====");
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();
        for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
            DaySchedule ds = monthSchedule.get(day);
            String aPerson = ds.assignments.getOrDefault(ShiftType.A, "空缺");
            String bPerson = ds.assignments.getOrDefault(ShiftType.B, "空缺");
            String cPerson = ds.assignments.getOrDefault(ShiftType.C, "空缺");
            System.out.printf("第%2d天: A:%s, B:%s, C:%s%n", day + 1, aPerson, bPerson, cPerson);
        }
    }

    /**
     * 3. 每天的班次顺序（按P1~P7顺序，用"-"连接）
     */
    public void printDailyPersonSequence() {
        System.out.println("\n===== 每日员工班次顺序（P1-P7） =====");
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();
        for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
            StringBuilder sb = new StringBuilder();
            for (Person p : people) {
                ShiftType shift = monthSchedule.get(day).personShift.get(p.name);
                sb.append(shift == null ? "休" : shift.name());
                sb.append("-");
            }
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            System.out.printf("第%2d天: %s%n", day + 1, sb.toString());
        }
    }

    public void printDailySequence() {
        System.out.println("\n===== 员工30天班次顺序 =====");
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();
        for (Person p : people) {
            StringBuilder sb = new StringBuilder(p.name + ": ");
            for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
                ShiftType shift = monthSchedule.get(day).personShift.get(p.name);
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

    public void printRestDays() {
        System.out.println("\n===== 员工连续两天休息统计（指定日期对） =====");
        List<Person> people = scheduler.getPeople();
        List<DaySchedule> monthSchedule = scheduler.getMonthSchedule();
        int[][] pairs = {
                {6, 7}, {13, 14}, {20, 21}, {27, 28}
        };
        for (Person p : people) {
            List<String> satisfiedPairs = new ArrayList<>();
            for (int[] pair : pairs) {
                int day1 = pair[0] - 1;
                int day2 = pair[1] - 1;
                boolean rest1 = (monthSchedule.get(day1).personShift.get(p.name) == null);
                boolean rest2 = (monthSchedule.get(day2).personShift.get(p.name) == null);
                if (rest1 && rest2) {
                    satisfiedPairs.add("第" + pair[0] + "-" + pair[1] + "天");
                }
            }
            if (satisfiedPairs.isEmpty()) {
                System.out.println(p.name + "：无");
            } else {
                System.out.print(p.name + "：");
                for (int i = 0; i < satisfiedPairs.size(); i++) {
                    System.out.print(satisfiedPairs.get(i));
                    if (i < satisfiedPairs.size() - 1) System.out.print("、");
                }
                System.out.println();
            }
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