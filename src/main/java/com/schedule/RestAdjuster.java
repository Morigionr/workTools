package com.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */

/**
 * 负责确保每人至少有一次连续2天休息的调整类
 */
public class RestAdjuster {

    private final Scheduler scheduler;

    public RestAdjuster(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void ensureConsecutiveRestDays() {
        for (int attempt = 0; attempt < 50; attempt++) {
            boolean allHaveConsecutive = true;
            for (Person p : scheduler.getPeople()) {
                if (p.forceDailyA1) continue; // 跳过
                if (!hasConsecutiveRestDays(p)) {
                    allHaveConsecutive = false;
                    if (createConsecutiveRestForPerson(p)) {
                        rebuildState();
                        break;
                    }
                }
            }
            if (allHaveConsecutive) break;
        }
        for (Person p : scheduler.getPeople()) {
            if (p.forceDailyA1) continue;
            if (!hasConsecutiveRestDays(p)) {
                System.err.println("警告：" + p.name + " 仍无连续2天休息，可能无法完全满足。");
            }
        }
    }

    private boolean hasConsecutiveRestDays(Person p) {
        boolean[] work = new boolean[Scheduler.TOTAL_DAYS];
        for (int d = 0; d < Scheduler.TOTAL_DAYS; d++) {
            work[d] = scheduler.isAssignedOnDay(p, d);
        }
        for (int d = 0; d < Scheduler.TOTAL_DAYS - 1; d++) {
            if (!work[d] && !work[d+1]) return true;
        }
        return false;
    }

    private boolean createConsecutiveRestForPerson(Person p) {
        List<Integer> restDays = new ArrayList<>();
        for (int d = 0; d < Scheduler.TOTAL_DAYS; d++) {
            if (!scheduler.isAssignedOnDay(p, d)) restDays.add(d);
        }
        if (restDays.size() < 2) return false;
        for (int i = 0; i < restDays.size() - 1; i++) {
            if (restDays.get(i) + 1 == restDays.get(i+1)) return true;
        }
        List<Integer> workDays = new ArrayList<>();
        for (int d = 0; d < Scheduler.TOTAL_DAYS; d++) {
            if (scheduler.isAssignedOnDay(p, d)) workDays.add(d);
        }
        for (int workDay : workDays) {
            int neighbor1 = workDay - 1;
            int neighbor2 = workDay + 1;
            for (int neighbor : new int[]{neighbor1, neighbor2}) {
                if (neighbor >= 0 && neighbor < Scheduler.TOTAL_DAYS && !scheduler.isAssignedOnDay(p, neighbor)) {
                    ShiftType shiftAtWork = getShiftOfPersonOnDay(p, workDay);
                    for (Person q : scheduler.getPeople()) {
                        if (q == p) continue;
                        if (scheduler.isAssignedOnDay(q, neighbor)) {
                            ShiftType shiftAtNeighbor = getShiftOfPersonOnDay(q, neighbor);
                            if (!canPersonTakeShiftOnDay(p, neighbor, shiftAtWork)) continue;
                            if (!canPersonTakeShiftOnDay(q, workDay, shiftAtNeighbor)) continue;
                            swapShifts(p, workDay, q, neighbor);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ShiftType getShiftOfPersonOnDay(Person p, int day) {
        return scheduler.getMonthSchedule().get(day).personShift.get(p.name);
    }

    private boolean canPersonTakeShiftOnDay(Person p, int day, ShiftType shift) {
        if (!p.availableShifts.contains(shift)) return false;
        if (day == 0) return true;
        ShiftType yesterday = p.yesterdayShift;
        if (yesterday == ShiftType.B && (shift == ShiftType.A1 || shift == ShiftType.A)) return false;
        if (yesterday == ShiftType.C && (shift == ShiftType.A1 || shift == ShiftType.A || shift == ShiftType.B)) return false;
        return true;
    }

    private void swapShifts(Person p1, int day1, Person p2, int day2) {
        DaySchedule ds1 = scheduler.getMonthSchedule().get(day1);
        DaySchedule ds2 = scheduler.getMonthSchedule().get(day2);
        ShiftType shift1 = ds1.personShift.get(p1.name);
        ShiftType shift2 = ds2.personShift.get(p2.name);
        ds1.assignments.remove(shift1);
        ds1.personShift.remove(p1.name);
        ds2.assignments.remove(shift2);
        ds2.personShift.remove(p2.name);
        ds1.assignments.put(shift2, p2.name);
        ds1.personShift.put(p2.name, shift2);
        ds2.assignments.put(shift1, p1.name);
        ds2.personShift.put(p1.name, shift1);
    }

    private void rebuildState() {
        for (Person p : scheduler.getPeople()) {
            p.workDays = 0;
            p.yesterdayShift = null;
        }
        for (int day = 0; day < Scheduler.TOTAL_DAYS; day++) {
            DaySchedule ds = scheduler.getMonthSchedule().get(day);
            for (Map.Entry<String, ShiftType> entry : ds.personShift.entrySet()) {
                String name = entry.getKey();
                Person p = findPersonByName(name);
                p.workDays++;
            }
            // 重新计算昨天班次
            for (Person p : scheduler.getPeople()) {
                if (day == 0) {
                    p.yesterdayShift = null;
                } else {
                    p.yesterdayShift = getShiftOfPersonOnDay(p, day - 1);
                }
            }
        }
    }

    private Person findPersonByName(String name) {
        for (Person p : scheduler.getPeople()) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }
}
