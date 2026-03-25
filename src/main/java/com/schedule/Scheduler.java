package com.schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */

/**
 * 主排班逻辑类（包含周处理、岗位分配、连续性约束处理等）
 */
public class Scheduler {

    public static final int TOTAL_DAYS = 30;
    public static final int TOTAL_PEOPLE = 9;
    public static final int WORK_DAYS_PER_CYCLE = 5;
    public static final int REST_DAYS_PER_CYCLE = 2;
    private static final double WORK_RATIO = (double) WORK_DAYS_PER_CYCLE / (WORK_DAYS_PER_CYCLE + REST_DAYS_PER_CYCLE);
    public static final int IDEAL_WORK_DAYS = (int) Math.round(TOTAL_DAYS * WORK_RATIO);
    private static final int DAYS_PER_WEEK = 7;

    private List<Person> people;
    private List<DaySchedule> monthSchedule;

    public Scheduler() {
        people = new ArrayList<>();
        monthSchedule = new ArrayList<>(TOTAL_DAYS);
        for (int i = 0; i < TOTAL_DAYS; i++) {
            monthSchedule.add(new DaySchedule());
        }
        initPeople();
    }

    private void initPeople() {
        people.add(new Person("P1", true, false));
        people.add(new Person("P2", false, true));
        for (int i = 3; i <= TOTAL_PEOPLE; i++) {
            people.add(new Person("P" + i, false, false));
        }
    }

    public void generateSchedule() {
        // 按周生成基础排班
        int weekStart = 0;
        int weekNum = 0;
        while (weekStart < TOTAL_DAYS) {
            int weekEnd = Math.min(weekStart + DAYS_PER_WEEK, TOTAL_DAYS) - 1;
            int rotatingIndex = (weekNum % (TOTAL_PEOPLE - 1)) + 1; // 从P2~P9轮流
            Person rotatingPerson = people.get(rotatingIndex);
            rotatingPerson.isCurrentWeekA1Only = true;
            for (Person p : people) {
                p.updateAvailableShifts();
            }
            for (int day = weekStart; day <= weekEnd; day++) {
                boolean isWeekend = (day % DAYS_PER_WEEK == 5) || (day % DAYS_PER_WEEK == 6);
                assignRequiredShifts(day, isWeekend);
                if (!isWeekend) {
                    assignA1Shifts(day, rotatingPerson);
                }
                updateYesterdayShifts(day);
            }
            rotatingPerson.isCurrentWeekA1Only = false;
            for (Person p : people) {
                p.weekWorkDays = 0;
            }
            weekStart += DAYS_PER_WEEK;
            weekNum++;
        }

        // 后处理：确保每人有连续2天休息
        RestAdjuster restAdjuster = new RestAdjuster(this);
        restAdjuster.ensureConsecutiveRestDays();
    }

    private void assignRequiredShifts(int day, boolean isWeekend) {
        for (ShiftType shift : new ShiftType[]{ShiftType.A, ShiftType.B, ShiftType.C}) {
            Person chosen = null;
            int minWorkDays = Integer.MAX_VALUE;
            for (Person p : people) {
                if (p.availableShifts.contains(shift) && !isAssignedOnDay(p, day)) {
                    if (p.isSpecial2 && shift != ShiftType.A) continue;
                    if (p.isFixedA1Only) continue;
                    if (p.isCurrentWeekA1Only) continue;
                    if (!isShiftAllowedForPerson(p, day, shift)) continue;
                    if (p.workDays < minWorkDays) {
                        minWorkDays = p.workDays;
                        chosen = p;
                    }
                }
            }
            if (chosen != null) {
                assignShift(day, shift, chosen);
            } else {
                System.err.println("警告：第" + (day+1) + "天无法分配" + shift + "岗位！");
            }
        }
    }

    private void assignA1Shifts(int day, Person rotatingPerson) {
        if (rotatingPerson != null && !isAssignedOnDay(rotatingPerson, day)) {
            if (rotatingPerson.weekWorkDays < WORK_DAYS_PER_CYCLE &&
                    isShiftAllowedForPerson(rotatingPerson, day, ShiftType.A1)) {
                assignShift(day, ShiftType.A1, rotatingPerson);
            }
        }
        List<Person> candidates = new ArrayList<>(people);
        candidates.sort(Comparator.comparingInt(p -> p.workDays));
        boolean changed;
        do {
            changed = false;
            for (Person p : candidates) {
                if (isAssignedOnDay(p, day)) continue;
                if (!p.availableShifts.contains(ShiftType.A1)) continue;
                if (!isShiftAllowedForPerson(p, day, ShiftType.A1)) continue;
                assignShift(day, ShiftType.A1, p);
                changed = true;
                break;
            }
        } while (changed);
    }

    private boolean isShiftAllowedForPerson(Person p, int day, ShiftType shift) {
        if (day == 0) return true;
        ShiftType yesterday = p.yesterdayShift;
        if (yesterday == null) return true;
        if (yesterday == ShiftType.B && (shift == ShiftType.A1 || shift == ShiftType.A)) {
            return false;
        }
        if (yesterday == ShiftType.C && (shift == ShiftType.A1 || shift == ShiftType.A || shift == ShiftType.B)) {
            return false;
        }
        return true;
    }

    private void updateYesterdayShifts(int day) {
        DaySchedule ds = monthSchedule.get(day);
        for (Person p : people) {
            p.yesterdayShift = ds.personShift.get(p.name);
        }
    }

    /**
     * 判断某人在某天是否已被分配任何班次（公开供 RestAdjuster 调用）
     */
    public boolean isAssignedOnDay(Person p, int day) {
        return monthSchedule.get(day).personShift.containsKey(p.name);
    }

    private void assignShift(int day, ShiftType shift, Person p) {
        DaySchedule ds = monthSchedule.get(day);
        if (ds.assignments.containsKey(shift)) {
            System.err.println("冲突：第" + (day+1) + "天" + shift + "已被占用！");
            return;
        }
        if (ds.personShift.containsKey(p.name)) {
            System.err.println("冲突：第" + (day+1) + "天" + p.name + "已被安排！");
            return;
        }
        ds.assignments.put(shift, p.name);
        ds.personShift.put(p.name, shift);
        p.workDays++;
        p.weekWorkDays++;
    }

    public List<Person> getPeople() {
        return people;
    }

    public List<DaySchedule> getMonthSchedule() {
        return monthSchedule;
    }
}
