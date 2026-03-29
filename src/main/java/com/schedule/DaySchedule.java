package com.schedule;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 李鸿
 * @date 2026/3/29
 * @apiNote
 */
    public class DaySchedule {
    public Map<ShiftType, String> assignments = new HashMap<>();
    public Map<String, ShiftType> personShift = new HashMap<>();
}
