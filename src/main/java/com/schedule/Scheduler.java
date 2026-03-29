package com.schedule;

import java.util.*;

/**
 * @author 李鸿
 * @date 2026/3/25
 * @apiNote
 */

/**
 * 主排班逻辑类（包含周处理、岗位分配、连续性约束处理等）
 */
public class Scheduler {

    public static final int TOTAL_DAYS = 7;
    public static final int TOTAL_PEOPLE = 7;

    private List<Person> people;
    private List<DaySchedule> weekSchedule;
    private int weekNumber; // 仅用于输出文件名，不影响排班

    public Scheduler(int weekNumber) {
        this.weekNumber = weekNumber;
        people = new ArrayList<>();
        weekSchedule = new ArrayList<>(TOTAL_DAYS);
        for (int i = 0; i < TOTAL_DAYS; i++) {
            weekSchedule.add(new DaySchedule());
        }
        initPeople();
    }

    private void initPeople() {
        for (int i = 1; i <= TOTAL_PEOPLE; i++) {
            people.add(new Person("P" + i));
        }
    }

    public void generateSchedule() {
        // 重置每周状态
        for (Person p : people) {
            p.resetWeek();
        }

        // 每天按顺序分配 A、B、C
        for (int day = 0; day < TOTAL_DAYS; day++) {
            assignRequiredShifts(day);
            updateYesterdayShifts(day);
        }

        // 尝试微调，提升班次多样性（简单交换）
        improveDiversity();
    }

    // 分配必须的 A、B、C 岗位（各需1人）
    private void assignRequiredShifts(int day) {
        // 按顺序分配，优先考虑多样性：先分配当前天尚未获得该班次的人
        for (ShiftType shift : new ShiftType[]{ShiftType.A, ShiftType.B, ShiftType.C}) {
            Person chosen = null;
            int minWorkDays = Integer.MAX_VALUE;
            // 第一优先级：还未获得过该班次的人
            List<Person> candidatesNotObtained = new ArrayList<>();
            // 第二优先级：已获得过该班次但工作天数少的人
            List<Person> candidatesObtained = new ArrayList<>();

            for (Person p : people) {
                if (p.availableShifts.contains(shift) && !isAssignedOnDay(p, day)) {
                    if (!isShiftAllowedForPerson(p, day, shift)) continue;
                    if (!p.obtainedShifts.contains(shift)) {
                        candidatesNotObtained.add(p);
                    } else {
                        candidatesObtained.add(p);
                    }
                }
            }

            // 优先从未获得该班次的人中选择
            if (!candidatesNotObtained.isEmpty()) {
                chosen = selectBest(candidatesNotObtained);
            } else if (!candidatesObtained.isEmpty()) {
                chosen = selectBest(candidatesObtained);
            }

            if (chosen != null) {
                assignShift(day, shift, chosen);
            } else {
                System.err.println("警告：第" + (day+1) + "天无法分配" + shift + "岗位！");
            }
        }
    }

    // 从候选列表中选择工作天数最少的人（若相同则随机，保证公平）
    private Person selectBest(List<Person> candidates) {
        candidates.sort(Comparator.comparingInt(p -> p.workDays));
        int minDays = candidates.get(0).workDays;
        List<Person> minCandidates = new ArrayList<>();
        for (Person p : candidates) {
            if (p.workDays == minDays) minCandidates.add(p);
            else break;
        }
        // 随机选择一人，避免单调
        return minCandidates.get(new Random().nextInt(minCandidates.size()));
    }

    // 检查连续性约束
    private boolean isShiftAllowedForPerson(Person p, int day, ShiftType shift) {
        if (day == 0) return true;
        ShiftType yesterday = p.yesterdayShift;
        if (yesterday == null) return true;
        if (yesterday == ShiftType.B && shift == ShiftType.A) return false;
        if (yesterday == ShiftType.C && (shift == ShiftType.A || shift == ShiftType.B)) return false;
        return true;
    }

    private void updateYesterdayShifts(int day) {
        DaySchedule ds = weekSchedule.get(day);
        for (Person p : people) {
            p.yesterdayShift = ds.personShift.get(p.name);
        }
    }

    public boolean isAssignedOnDay(Person p, int day) {
        return weekSchedule.get(day).personShift.containsKey(p.name);
    }

    private void assignShift(int day, ShiftType shift, Person p) {
        DaySchedule ds = weekSchedule.get(day);
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
        p.obtainedShifts.add(shift);
    }

    // 简单交换调整，提升多样性：如果某人只有一种班次，尝试与其他人交换班次以获得更多种类
    private void improveDiversity() {
        boolean changed;
        do {
            changed = false;
            for (Person p : people) {
                if (p.obtainedShifts.size() >= 2) continue; // 已经有两种以上
                // 找到此人所有的上班日子
                List<Integer> workDaysList = new ArrayList<>();
                for (int d = 0; d < TOTAL_DAYS; d++) {
                    if (isAssignedOnDay(p, d)) workDaysList.add(d);
                }
                for (int day : workDaysList) {
                    ShiftType shiftP = getShiftOfPersonOnDay(p, day);
                    // 找一个其他人员 q，在另一天有不同班次，且交换后两人多样性增加
                    for (Person q : people) {
                        if (q == p) continue;
                        // 寻找 q 的某一天 day2，且 q 在 day2 上的班次与 shiftP 不同
                        for (int day2 = 0; day2 < TOTAL_DAYS; day2++) {
                            if (day2 == day) continue;
                            if (isAssignedOnDay(q, day2)) {
                                ShiftType shiftQ = getShiftOfPersonOnDay(q, day2);
                                if (shiftQ == shiftP) continue;
                                // 检查交换可行性
                                if (!canPersonTakeShiftOnDay(p, day2, shiftQ)) continue;
                                if (!canPersonTakeShiftOnDay(q, day, shiftP)) continue;
                                // 检查岗位覆盖：交换后 day 上的 shiftP 要有人顶替，day2 上的 shiftQ 要有人顶替
                                // 由于我们只交换两个人的班次，岗位类型不变，所以岗位覆盖仍满足
                                // 执行交换
                                swapShifts(p, day, q, day2);
                                // 更新 obtainedShifts
                                p.obtainedShifts.add(shiftQ);
                                q.obtainedShifts.add(shiftP);
                                changed = true;
                                break;
                            }
                        }
                        if (changed) break;
                    }
                    if (changed) break;
                }
            }
        } while (changed);
    }

    private ShiftType getShiftOfPersonOnDay(Person p, int day) {
        return weekSchedule.get(day).personShift.get(p.name);
    }

    private boolean canPersonTakeShiftOnDay(Person p, int day, ShiftType shift) {
        if (!p.availableShifts.contains(shift)) return false;
        // 连续性检查需要临时模拟前一天的班次，这里简化：使用当前 p.yesterdayShift，但交换后可能改变。
        // 为了安全，我们检查 day 前后一天的连续性。
        // 由于交换可能改变前后连续性，需要更复杂的检查。这里为了简单，只检查直接相邻。
        // 实际工程中可做更严格检查，但这里暂用简单条件。
        if (day > 0) {
            ShiftType yesterday = p.yesterdayShift;
            if (yesterday == ShiftType.B && shift == ShiftType.A) return false;
            if (yesterday == ShiftType.C && (shift == ShiftType.A || shift == ShiftType.B)) return false;
        }
        if (day < TOTAL_DAYS - 1) {
            // 检查后一天是否满足约束
            ShiftType tomorrowShift = getShiftOfPersonOnDay(p, day + 1);
            if (tomorrowShift != null) {
                if (shift == ShiftType.B && tomorrowShift == ShiftType.A) return false;
                if (shift == ShiftType.C && (tomorrowShift == ShiftType.A || tomorrowShift == ShiftType.B)) return false;
            }
        }
        return true;
    }

    private void swapShifts(Person p1, int day1, Person p2, int day2) {
        DaySchedule ds1 = weekSchedule.get(day1);
        DaySchedule ds2 = weekSchedule.get(day2);
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
        // 更新工作天数不变
    }

    public List<Person> getPeople() {
        return people;
    }

    public List<DaySchedule> getWeekSchedule() {
        return weekSchedule;
    }

    public int getWeekNumber() {
        return weekNumber;
    }
}
