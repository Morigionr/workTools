package com.schedule;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 人员类（包含属性、可用班次更新、连续性检查等）
 */
public class Person {
    String name;
    Set<ShiftType> availableShifts;   // 始终包含 A,B,C
    int workDays;                     // 本周工作天数
    ShiftType yesterdayShift;         // 前一天所上班次（用于连续性检查）
    Set<ShiftType> obtainedShifts;    // 本周已获得的班次种类

    public Map<ShiftType, Integer> shiftCount;

    public Person(String name) {
        this.name = name;
        this.availableShifts = new HashSet<>();
        this.availableShifts.add(ShiftType.A);
        this.availableShifts.add(ShiftType.B);
        this.availableShifts.add(ShiftType.C);
        this.workDays = 0;
        this.yesterdayShift = null;
        this.shiftCount = new HashMap<>();
        this.shiftCount.put(ShiftType.A, 0);
        this.shiftCount.put(ShiftType.B, 0);
        this.shiftCount.put(ShiftType.C, 0);
    }

    // 重置每周状态（在生成新一周排班前调用）
    public void resetWeek() {
        workDays = 0;
        yesterdayShift = null;
        obtainedShifts.clear();
    }

    @Override
    public String toString() {
        return name;
    }
}