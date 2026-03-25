package com.schedule;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */

import java.util.HashMap;
import java.util.Map;

/**
 * 每日排班记录类
 */
public class DaySchedule {

    public Map<ShiftType, String> assignments = new HashMap<>();
    public Map<String, ShiftType> personShift = new HashMap<>(); // 当天每个人上的班次
}
