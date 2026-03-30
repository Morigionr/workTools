package com.schedule;

import java.util.*;

public class Scheduler {
    public static final int TOTAL_DAYS = 30;
    public static final int TOTAL_PEOPLE = 7;

    private List<Person> people;
    private List<DaySchedule> monthSchedule;
    private int weekNumber;

    private static final int[][] SPECIAL_REST_PAIRS = {
            {5, 6},   // 第6、7天
            {12, 13}, // 第13、14天
            {19, 20}, // 第20、21天
            {26, 27}  // 第27、28天
    };

    public Scheduler(int weekNumber) {
        this.weekNumber = weekNumber;
        people = new ArrayList<>();
        monthSchedule = new ArrayList<>(TOTAL_DAYS);
        for (int i = 0; i < TOTAL_DAYS; i++) {
            monthSchedule.add(new DaySchedule());
        }
        initPeople();
    }

    private void initPeople() {
        for (int i = 1; i <= TOTAL_PEOPLE; i++) {
            people.add(new Person("P" + i));
        }
    }

    public void generateSchedule() {
        // 重置统计
        for (Person p : people) {
            p.workDays = 0;
            p.yesterdayShift = null;
            p.shiftCount.put(ShiftType.A, 0);
            p.shiftCount.put(ShiftType.B, 0);
            p.shiftCount.put(ShiftType.C, 0);
        }

        // 初始分配
        for (int day = 0; day < TOTAL_DAYS; day++) {
            assignRequiredShifts(day);
            updateYesterdayShifts(day);
        }

        recalcShiftCounts();
        balanceShifts();
        enforceSpecialRest();
        balanceShifts();

        // 最终修复：确保每天 A、B、C 各一人
        verifyAndRepairDailyCoverage();

        // 修复后重新统计，并再次轻微均衡（可选）
        recalcShiftCounts();
        balanceShifts();
    }

    // ================== 新增：验证并修复每日岗位覆盖 ==================
    /**
     * 验证每天 A、B、C 是否各有一人，若缺失则尝试修复。
     * 修复策略：优先寻找当天未分配且能上该班次的人；若无，则与当天已分配的人交换。
     */
    private void verifyAndRepairDailyCoverage() {
        for (int day = 0; day < TOTAL_DAYS; day++) {
            DaySchedule ds = monthSchedule.get(day);
            for (ShiftType shift : new ShiftType[]{ShiftType.A, ShiftType.B, ShiftType.C}) {
                if (!ds.assignments.containsKey(shift)) {
                    // 尝试修复缺失的 shift
                    Person chosen = null;
                    // 1. 找当天空闲且能上该班次的人
                    for (Person p : people) {
                        if (!isAssignedOnDay(p, day) && p.availableShifts.contains(shift)) {
                            if (isShiftAllowedForPerson(p, day, shift)) {
                                chosen = p;
                                break;
                            }
                        }
                    }
                    if (chosen != null) {
                        assignShift(day, shift, chosen);
                        continue;
                    }
                    // 2. 与当天已分配的人交换
                    for (Person p : people) {
                        if (isAssignedOnDay(p, day)) {
                            ShiftType currentShift = getShiftOfPersonOnDay(p, day);
                            // 找另一个当天休息的人 q
                            for (Person q : people) {
                                if (q == p) continue;
                                if (!isAssignedOnDay(q, day) && q.availableShifts.contains(currentShift)) {
                                    if (isShiftAllowedForPerson(q, day, currentShift)) {
                                        // 将 p 的班次转移给 q
                                        assignShift(day, currentShift, q);
                                        // 让 p 上缺失的班次
                                        if (isShiftAllowedForPerson(p, day, shift)) {
                                            assignShift(day, shift, p);
                                            chosen = p;
                                            break;
                                        } else {
                                            // 回滚：删除 q 的分配
                                            ds.assignments.remove(currentShift);
                                            ds.personShift.remove(q.name);
                                            q.workDays--;
                                            q.shiftCount.put(currentShift, q.shiftCount.get(currentShift) - 1);
                                        }
                                    }
                                }
                            }
                            if (chosen != null) break;
                        }
                    }
                    if (chosen == null) {
                        System.err.println("严重错误：第" + (day+1) + "天无法修复" + shift + "岗位！");
                    }
                }
            }
        }
    }

    // 从当前排班表重新统计所有人的 shiftCount 和 workDays
    private void recalcShiftCounts() {
        // 重置所有人的计数
        for (Person p : people) {
            p.workDays = 0;
            p.shiftCount.put(ShiftType.A, 0);
            p.shiftCount.put(ShiftType.B, 0);
            p.shiftCount.put(ShiftType.C, 0);
        }
        // 遍历所有天
        for (int day = 0; day < TOTAL_DAYS; day++) {
            DaySchedule ds = monthSchedule.get(day);
            for (Map.Entry<String, ShiftType> entry : ds.personShift.entrySet()) {
                String name = entry.getKey();
                ShiftType shift = entry.getValue();
                Person p = findPersonByName(name);
                p.workDays++;
                p.shiftCount.put(shift, p.shiftCount.get(shift) + 1);
            }
        }
    }

    private Person findPersonByName(String name) {
        for (Person p : people) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }

    // 分配必须的 A、B、C
    private void assignRequiredShifts(int day) {
        for (ShiftType shift : new ShiftType[]{ShiftType.A, ShiftType.B, ShiftType.C}) {
            Person chosen = null;
            int minCount = Integer.MAX_VALUE;

            for (Person p : people) {
                if (p.availableShifts.contains(shift) && !isAssignedOnDay(p, day)) {
                    if (!isShiftAllowedForPerson(p, day, shift)) continue;
                    int currentCount = p.shiftCount.get(shift);
                    if (currentCount < minCount) {
                        minCount = currentCount;
                        chosen = p;
                    }
                }
            }

            if (chosen != null) {
                assignShift(day, shift, chosen);
            } else {
                // 回退：忽略连续性约束
                for (Person p : people) {
                    if (p.availableShifts.contains(shift) && !isAssignedOnDay(p, day)) {
                        chosen = p;
                        break;
                    }
                }
                if (chosen != null) {
                    assignShift(day, shift, chosen);
                } else {
                    System.err.println("严重错误：第" + (day+1) + "天无法分配" + shift + "岗位！");
                }
            }
        }
    }

    // 交换调整，使每个人的A、B、C次数更均衡
    private void balanceShifts() {
        boolean improved = true;
        int maxAttempts = 100;
        int attempts = 0;
        while (improved && attempts < maxAttempts) {
            recalcShiftCounts(); // 确保计数最新
            improved = false;
            attempts++;
            // 找出最不均衡的人
            Person p1 = null;
            double maxImbalance = 0;
            for (Person p : people) {
                double imbalance = computeImbalance(p);
                if (imbalance > maxImbalance) {
                    maxImbalance = imbalance;
                    p1 = p;
                }
            }
            if (p1 == null) break;
            // 寻找可交换的人
            for (Person p2 : people) {
                if (p2 == p1) continue;
                if (trySwap(p1, p2)) {
                    improved = true;
                    break;
                }
            }
        }
    }

    private double computeImbalance(Person p) {
        int a = p.shiftCount.get(ShiftType.A);
        int b = p.shiftCount.get(ShiftType.B);
        int c = p.shiftCount.get(ShiftType.C);
        double avg = (a + b + c) / 3.0;
        return (Math.pow(a - avg, 2) + Math.pow(b - avg, 2) + Math.pow(c - avg, 2)) / 3;
    }

    private boolean trySwap(Person p1, Person p2) {
        List<Integer> commonDays = new ArrayList<>();
        for (int day = 0; day < TOTAL_DAYS; day++) {
            ShiftType s1 = getShiftOfPersonOnDay(p1, day);
            ShiftType s2 = getShiftOfPersonOnDay(p2, day);
            if (s1 != null && s2 != null && s1 != s2) {
                commonDays.add(day);
            }
        }
        if (commonDays.isEmpty()) return false;

        Random rand = new Random();
        int day = commonDays.get(rand.nextInt(commonDays.size()));
        ShiftType s1 = getShiftOfPersonOnDay(p1, day);
        ShiftType s2 = getShiftOfPersonOnDay(p2, day);
        if (isSwapAllowed(p1, p2, day, s1, s2)) {
            swapShifts(p1, day, p2, day);
            return true;
        }
        return false;
    }

    private boolean isSwapAllowed(Person p1, Person p2, int day, ShiftType s1, ShiftType s2) {
        // 检查 p1 在 day 上换为 s2 的连续性
        ShiftType p1Yesterday = (day > 0) ? getShiftOfPersonOnDay(p1, day - 1) : null;
        ShiftType p1Tomorrow = (day < TOTAL_DAYS - 1) ? getShiftOfPersonOnDay(p1, day + 1) : null;
        if (p1Yesterday != null) {
            if (p1Yesterday == ShiftType.B && s2 == ShiftType.A) return false;
            if (p1Yesterday == ShiftType.C && (s2 == ShiftType.A || s2 == ShiftType.B)) return false;
        }
        if (p1Tomorrow != null) {
            if (s2 == ShiftType.B && p1Tomorrow == ShiftType.A) return false;
            if (s2 == ShiftType.C && (p1Tomorrow == ShiftType.A || p1Tomorrow == ShiftType.B)) return false;
        }
        // 检查 p2 在 day 上换为 s1
        ShiftType p2Yesterday = (day > 0) ? getShiftOfPersonOnDay(p2, day - 1) : null;
        ShiftType p2Tomorrow = (day < TOTAL_DAYS - 1) ? getShiftOfPersonOnDay(p2, day + 1) : null;
        if (p2Yesterday != null) {
            if (p2Yesterday == ShiftType.B && s1 == ShiftType.A) return false;
            if (p2Yesterday == ShiftType.C && (s1 == ShiftType.A || s1 == ShiftType.B)) return false;
        }
        if (p2Tomorrow != null) {
            if (s1 == ShiftType.B && p2Tomorrow == ShiftType.A) return false;
            if (s1 == ShiftType.C && (p2Tomorrow == ShiftType.A || p2Tomorrow == ShiftType.B)) return false;
        }
        return true;
    }

    // 强制连续休息
    private void enforceSpecialRest() {
        boolean[] satisfied = new boolean[TOTAL_PEOPLE];
        for (int i = 0; i < TOTAL_PEOPLE; i++) {
            if (hasSpecialConsecutiveRest(people.get(i))) {
                satisfied[i] = true;
            }
        }

        int maxAttempts = 200;
        int attempts = 0;
        Random rand = new Random();

        while (attempts < maxAttempts) {
            int unsatisfiedIndex = -1;
            for (int i = 0; i < TOTAL_PEOPLE; i++) {
                if (!satisfied[i]) {
                    unsatisfiedIndex = i;
                    break;
                }
            }
            if (unsatisfiedIndex == -1) break;

            Person p = people.get(unsatisfiedIndex);
            List<int[]> availablePairs = new ArrayList<>();
            for (int[] pair : SPECIAL_REST_PAIRS) {
                if (isConsecutiveRestOnPair(p, pair)) {
                    satisfied[unsatisfiedIndex] = true;
                    break;
                } else {
                    availablePairs.add(pair);
                }
            }
            if (satisfied[unsatisfiedIndex]) continue;

            if (availablePairs.isEmpty()) break;

            int[] targetPair = availablePairs.get(rand.nextInt(availablePairs.size()));
            int d1 = targetPair[0];
            int d2 = targetPair[1];

            if (makePersonRestOnPair(p, d1, d2)) {
                satisfied[unsatisfiedIndex] = true;
                recalcShiftCounts(); // 交换后重新统计
            }
            attempts++;
        }

        // 最终检查
        for (Person p : people) {
            if (!hasSpecialConsecutiveRest(p)) {
                System.err.println("警告：" + p.name + " 未能在指定日期对中实现连续两天休息。");
            }
        }
    }

    private boolean isConsecutiveRestOnPair(Person p, int[] pair) {
        return !isAssignedOnDay(p, pair[0]) && !isAssignedOnDay(p, pair[1]);
    }

    private boolean hasSpecialConsecutiveRest(Person p) {
        for (int[] pair : SPECIAL_REST_PAIRS) {
            if (isConsecutiveRestOnPair(p, pair)) return true;
        }
        return false;
    }

    private boolean makePersonRestOnPair(Person p, int d1, int d2) {
        boolean rest1 = !isAssignedOnDay(p, d1);
        boolean rest2 = !isAssignedOnDay(p, d2);
        if (rest1 && rest2) return true;
        if (rest1) {
            return swapWorkToRest(p, d2);
        } else if (rest2) {
            return swapWorkToRest(p, d1);
        } else {
            if (swapWorkToRest(p, d1)) {
                return swapWorkToRest(p, d2);
            }
            return false;
        }
    }

    private boolean swapWorkToRest(Person p, int workDay) {
        if (!isAssignedOnDay(p, workDay)) return true;
        ShiftType shiftToMove = getShiftOfPersonOnDay(p, workDay);
        for (Person q : people) {
            if (q == p) continue;
            if (!isAssignedOnDay(q, workDay)) {
                for (int otherDay = 0; otherDay < TOTAL_DAYS; otherDay++) {
                    if (otherDay == workDay) continue;
                    if (isAssignedOnDay(q, otherDay) && !isAssignedOnDay(p, otherDay)) {
                        ShiftType shiftToReceive = getShiftOfPersonOnDay(q, otherDay);
                        if (isSwapAllowedForRest(p, q, workDay, otherDay, shiftToMove, shiftToReceive)) {
                            swapShifts(p, workDay, q, otherDay);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isSwapAllowedForRest(Person p, Person q, int workDay, int otherDay,
                                         ShiftType shiftP, ShiftType shiftQ) {
        // 简化连续性检查（与之前相同，略）
        if (!isShiftAllowedForPerson(p, otherDay, shiftQ)) return false;
        if (!isShiftAllowedForPerson(q, workDay, shiftP)) return false;
        // 检查 workDay 前后
        if (workDay > 0) {
            ShiftType prevP = getShiftOfPersonOnDay(p, workDay - 1);
            ShiftType prevQ = getShiftOfPersonOnDay(q, workDay - 1);
            if (prevP == ShiftType.B && shiftP == ShiftType.A) return false;
            if (prevP == ShiftType.C && (shiftP == ShiftType.A || shiftP == ShiftType.B)) return false;
            if (prevQ == ShiftType.B && shiftP == ShiftType.A) return false;
            if (prevQ == ShiftType.C && (shiftP == ShiftType.A || shiftP == ShiftType.B)) return false;
        }
        if (workDay < TOTAL_DAYS - 1) {
            ShiftType nextP = getShiftOfPersonOnDay(p, workDay + 1);
            ShiftType nextQ = getShiftOfPersonOnDay(q, workDay + 1);
            if (shiftP == ShiftType.B && nextP == ShiftType.A) return false;
            if (shiftP == ShiftType.C && (nextP == ShiftType.A || nextP == ShiftType.B)) return false;
            if (shiftP == ShiftType.B && nextQ == ShiftType.A) return false;
            if (shiftP == ShiftType.C && (nextQ == ShiftType.A || nextQ == ShiftType.B)) return false;
        }
        // 检查 otherDay 前后
        if (otherDay > 0) {
            ShiftType prevP = getShiftOfPersonOnDay(p, otherDay - 1);
            ShiftType prevQ = getShiftOfPersonOnDay(q, otherDay - 1);
            if (prevP == ShiftType.B && shiftQ == ShiftType.A) return false;
            if (prevP == ShiftType.C && (shiftQ == ShiftType.A || shiftQ == ShiftType.B)) return false;
            if (prevQ == ShiftType.B && shiftQ == ShiftType.A) return false;
            if (prevQ == ShiftType.C && (shiftQ == ShiftType.A || shiftQ == ShiftType.B)) return false;
        }
        if (otherDay < TOTAL_DAYS - 1) {
            ShiftType nextP = getShiftOfPersonOnDay(p, otherDay + 1);
            ShiftType nextQ = getShiftOfPersonOnDay(q, otherDay + 1);
            if (shiftQ == ShiftType.B && nextP == ShiftType.A) return false;
            if (shiftQ == ShiftType.C && (nextP == ShiftType.A || nextP == ShiftType.B)) return false;
            if (shiftQ == ShiftType.B && nextQ == ShiftType.A) return false;
            if (shiftQ == ShiftType.C && (nextQ == ShiftType.A || nextQ == ShiftType.B)) return false;
        }
        return true;
    }

    private boolean isShiftAllowedForPerson(Person p, int day, ShiftType shift) {
        if (day == 0) return true;
        ShiftType yesterday = p.yesterdayShift;
        if (yesterday == null) return true;
        if (yesterday == ShiftType.B && shift == ShiftType.A) return false;
        if (yesterday == ShiftType.C && (shift == ShiftType.A || shift == ShiftType.B)) return false;
        return true;
    }

    private void updateYesterdayShifts(int day) {
        DaySchedule ds = monthSchedule.get(day);
        for (Person p : people) {
            p.yesterdayShift = ds.personShift.get(p.name);
        }
    }

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
        p.shiftCount.put(shift, p.shiftCount.get(shift) + 1);
    }

    private ShiftType getShiftOfPersonOnDay(Person p, int day) {
        return monthSchedule.get(day).personShift.get(p.name);
    }

    private void swapShifts(Person p1, int day1, Person p2, int day2) {
        DaySchedule ds1 = monthSchedule.get(day1);
        DaySchedule ds2 = monthSchedule.get(day2);
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
        // 注意：此处不更新 shiftCount，由后续 recalcShiftCounts() 完成
    }

    public List<Person> getPeople() {
        return people;
    }

    public List<DaySchedule> getMonthSchedule() {
        return monthSchedule;
    }

    public int getWeekNumber() {
        return weekNumber;
    }
}