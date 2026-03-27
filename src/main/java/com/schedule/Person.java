package com.schedule;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */

import java.util.HashSet;
import java.util.Set;

/**
 * 人员类（包含属性、可用班次更新、连续性检查等）
 */
public class Person {
    String name;
    boolean isFixedA1Only;   // 1号
    boolean isSpecial2;      // 2号：只能A1或A
    boolean forceDailyA1;    // 新增：强制每天A1
    Set<ShiftType> availableShifts; // 动态可用班次（按周调整）
    int workDays;            // 总工作天数
    int weekWorkDays;        // 本周已工作天数（用于周平衡）
    boolean isCurrentWeekA1Only; // 本周是否为轮值A1
    ShiftType yesterdayShift;     // 前一天所上班次（用于连续性检查）

    public Person(String name, boolean isFixedA1Only, boolean isSpecial2, boolean forceDailyA1) {
        this.name = name;
        this.isFixedA1Only = isFixedA1Only;
        this.isSpecial2 = isSpecial2;
        this.forceDailyA1 = forceDailyA1;
        this.availableShifts = new HashSet<>();
        this.workDays = 0;
        this.weekWorkDays = 0;
        this.isCurrentWeekA1Only = false;
        this.yesterdayShift = null;
        updateAvailableShifts();
    }

    public void updateAvailableShifts() {
        availableShifts.clear();
        if (forceDailyA1) {
            availableShifts.add(ShiftType.A1);
            return;
        }
        if (isFixedA1Only) {
            availableShifts.add(ShiftType.A1);
            return;
        }
        if (isCurrentWeekA1Only) {
            availableShifts.add(ShiftType.A1);
            return;
        }
        if (isSpecial2) {
            availableShifts.add(ShiftType.A1);
            availableShifts.add(ShiftType.A);
        } else {
            availableShifts.add(ShiftType.A1);
            availableShifts.add(ShiftType.A);
            availableShifts.add(ShiftType.B);
            availableShifts.add(ShiftType.C);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}