package com.csc3665.project1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SchedulePlannerTest {
    private static final LocalDate START = LocalDate.of(2026, 10, 19);
    private static final List<Integer> DOCTORS =
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    private static final List<Integer> TECHNICIANS =
        List.of(101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112);

    @Test
    void generatesCoveredBalancedShiftsWithRestAndWeeklyLimits() {
        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 14, DOCTORS, TECHNICIANS
        );

        for (int day = 0; day < 14; day++) {
            LocalDate date = START.plusDays(day);
            List<SchedulePlanner.Assignment> daily = assignments.stream()
                .filter(assignment -> assignment.date().equals(date))
                .toList();
            List<String> requiredBlocks = date.getDayOfWeek() == DayOfWeek.SUNDAY
                ? List.of("Sunday Shift", "Overnight")
                : List.of("Early Morning", "8:30 Shift", "Late Doctor", "Overnight");

            for (String block : requiredBlocks) {
                List<SchedulePlanner.Assignment> onShift = daily.stream()
                    .filter(assignment -> assignment.block().equals(block))
                    .toList();
                long doctorCount = onShift.stream()
                    .filter(assignment -> DOCTORS.contains(assignment.employeeId()))
                    .count();
                long technicianCount = onShift.stream()
                    .filter(assignment -> TECHNICIANS.contains(assignment.employeeId()))
                    .count();
                assertTrue(doctorCount >= 1, block + " has a doctor on " + date);
                if (block.equals("Sunday Shift") || block.equals("Overnight")) {
                    assertEquals(1, doctorCount);
                }
                assertEquals(
                    block.equals("Overnight") ? 1 : block.equals("Sunday Shift") ? 2 : doctorCount,
                    technicianCount
                );
                if (block.equals("Early Morning")) {
                    assertTrue(onShift.stream().allMatch(SchedulePlanner.Assignment::surgery));
                }
            }
        }

        Map<Integer, List<SchedulePlanner.Assignment>> byEmployee = assignments.stream()
            .collect(Collectors.groupingBy(SchedulePlanner.Assignment::employeeId));
        for (List<SchedulePlanner.Assignment> employeeShifts : byEmployee.values()) {
            List<SchedulePlanner.Assignment> sorted = employeeShifts.stream()
                .sorted((left, right) -> Long.compare(left.startMinute(), right.startMinute()))
                .toList();
            for (int i = 1; i < sorted.size(); i++) {
                assertTrue(
                    sorted.get(i).startMinute() - sorted.get(i - 1).endMinute() >= 600,
                    "Assignments must have at least 10 hours of rest."
                );
            }
        }

        for (int offset = 0; offset < 14; offset++) {
            LocalDate week = START.plusDays(offset)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (int doctor : DOCTORS) {
                List<SchedulePlanner.Assignment> weeklyShifts = assignments.stream()
                    .filter(assignment -> assignment.employeeId() == doctor
                        && assignment.date().with(
                            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                        ).equals(week))
                    .toList();
                assertTrue(weeklyShifts.stream().filter(SchedulePlanner.Assignment::surgery).count() <= 1);
                assertTrue(weeklyShifts.stream()
                    .filter(assignment -> assignment.block().equals("Overnight")).count() <= 1);
            }
        }

        assertTrue(countDoctors(assignments, START.with(
            TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)
        ), "Early Morning") >= 2);
        assertTrue(countDoctors(assignments, START.with(
            TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)
        ), "Late Doctor") >= 2);
        assertFalse(assignments.isEmpty());
    }

    @Test
    void rejectsStaffingThatCannotCoverEachNightInAWeek() {
        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> SchedulePlanner.generate(
                START, 7, List.of(1, 2, 3, 4, 5, 6), TECHNICIANS
            )
        );

        assertTrue(error.getMessage().contains("Not enough doctors"));
    }

    @Test
    void respectsRestFromAssignmentsJustOutsideTheGeneratedWindow() {
        LocalDate priorDay = START.minusDays(1);
        SchedulePlanner.Assignment existingNight = SchedulePlanner.existingAssignment(
            DOCTORS.get(0), priorDay, "Overnight", false
        );

        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 7, DOCTORS, TECHNICIANS, List.of(existingNight)
        );

        assertFalse(assignments.stream().anyMatch(assignment ->
            assignment.employeeId() == DOCTORS.get(0)
                && assignment.date().equals(START)
                && !assignment.block().equals("Overnight")
        ));
    }

    @Test
    void neverAssignsAnEmployeeOnADateTheyAreUnavailable() {
        LocalDate off = START.plusDays(2);
        Set<SchedulePlanner.Unavailability> unavailable = Set.of(
            new SchedulePlanner.Unavailability(DOCTORS.get(0), off),
            new SchedulePlanner.Unavailability(TECHNICIANS.get(0), off)
        );

        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 14, DOCTORS, TECHNICIANS, List.of(), unavailable
        );

        assertFalse(assignments.stream().anyMatch(assignment ->
            assignment.date().equals(off)
                && (assignment.employeeId() == DOCTORS.get(0)
                    || assignment.employeeId() == TECHNICIANS.get(0))
        ));
        assertFalse(assignments.isEmpty());
    }

    @Test
    void failsWhenEveryDoctorIsUnavailableOnOneDay() {
        LocalDate off = START.plusDays(2);
        Set<SchedulePlanner.Unavailability> unavailable = DOCTORS.stream()
            .map(id -> new SchedulePlanner.Unavailability(id, off))
            .collect(Collectors.toSet());

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> SchedulePlanner.generate(START, 14, DOCTORS, TECHNICIANS, List.of(), unavailable)
        );

        assertTrue(error.getMessage().contains("Not enough doctors"));
    }

    @Test
    void aPinnedOvernightDoctorIsTheOnlyOvernightDoctorThatNight() {
        LocalDate night = START.plusDays(1);
        SchedulePlanner.Assignment pinned = SchedulePlanner.existingAssignment(
            DOCTORS.get(4), night, "Overnight", false
        );

        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 14, DOCTORS, TECHNICIANS, List.of(pinned)
        );

        assertTrue(assignments.stream().anyMatch(assignment ->
            assignment.employeeId() == DOCTORS.get(4)
                && assignment.date().equals(night)
                && assignment.block().equals("Overnight")));
        assertEquals(1, countDoctors(assignments, night, "Overnight"));
    }

    @Test
    void aPinnedOvernightTechnicianIsTheOnlyOvernightTechnicianThatNight() {
        LocalDate night = START.plusDays(1);
        SchedulePlanner.Assignment pinned = SchedulePlanner.existingAssignment(
            TECHNICIANS.get(5), night, "Overnight", false
        );

        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 14, DOCTORS, TECHNICIANS, List.of(pinned)
        );

        List<SchedulePlanner.Assignment> technicians = assignments.stream()
            .filter(assignment -> assignment.date().equals(night)
                && assignment.block().equals("Overnight")
                && TECHNICIANS.contains(assignment.employeeId()))
            .toList();
        assertEquals(1, technicians.size());
        assertEquals(TECHNICIANS.get(5), technicians.get(0).employeeId());
    }

    @Test
    void aPinnedSundayTechnicianCountsTowardTheTwoTechnicians() {
        LocalDate sunday = START.plusDays(6);
        SchedulePlanner.Assignment pinned = SchedulePlanner.existingAssignment(
            TECHNICIANS.get(7), sunday, "Sunday Shift", false
        );

        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 14, DOCTORS, TECHNICIANS, List.of(pinned)
        );

        List<Integer> technicians = assignments.stream()
            .filter(assignment -> assignment.date().equals(sunday)
                && assignment.block().equals("Sunday Shift")
                && TECHNICIANS.contains(assignment.employeeId()))
            .map(SchedulePlanner.Assignment::employeeId)
            .toList();
        assertEquals(2, technicians.size());
        assertTrue(technicians.contains(TECHNICIANS.get(7)));
    }

    @Test
    void aPinnedSurgeryDoctorKeepsThatShiftAndGetsNoSecondSurgeryThatWeek() {
        LocalDate wednesday = START.plusDays(2);
        SchedulePlanner.Assignment pinned = SchedulePlanner.existingAssignment(
            DOCTORS.get(3), wednesday, "Early Morning", true
        );

        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 14, DOCTORS, TECHNICIANS, List.of(pinned)
        );

        assertTrue(assignments.stream().anyMatch(assignment ->
            assignment.employeeId() == DOCTORS.get(3)
                && assignment.date().equals(wednesday)
                && assignment.block().equals("Early Morning")));
        assertEquals(1, assignments.stream()
            .filter(assignment -> assignment.employeeId() == DOCTORS.get(3)
                && assignment.surgery()
                && !assignment.date().isAfter(START.plusDays(6)))
            .count());
    }

    @Test
    void conflictsWithFlagsRestViolationsAndASecondOvernightForDoctors() {
        SchedulePlanner.Assignment monNight = SchedulePlanner.existingAssignment(
            1, START, "Overnight", false
        );
        SchedulePlanner.Assignment tueMorning = SchedulePlanner.existingAssignment(
            1, START.plusDays(1), "Early Morning", true
        );
        SchedulePlanner.Assignment thuNight = SchedulePlanner.existingAssignment(
            1, START.plusDays(3), "Overnight", false
        );
        SchedulePlanner.Assignment otherDoctor = SchedulePlanner.existingAssignment(
            2, START.plusDays(1), "Early Morning", true
        );

        // Overnight ends Tuesday 8:00, Early Morning starts Tuesday 7:30.
        assertTrue(SchedulePlanner.conflictsWith(tueMorning, List.of(monNight), true));
        // A second overnight in the same week: a doctor rule only.
        assertTrue(SchedulePlanner.conflictsWith(thuNight, List.of(monNight), true));
        assertFalse(SchedulePlanner.conflictsWith(thuNight, List.of(monNight), false));
        // Someone else's shift never conflicts.
        assertFalse(SchedulePlanner.conflictsWith(otherDoctor, List.of(monNight), true));
    }

    private long countDoctors(
        List<SchedulePlanner.Assignment> assignments,
        LocalDate date,
        String block
    ) {
        return assignments.stream()
            .filter(assignment -> assignment.date().equals(date)
                && assignment.block().equals(block)
                && DOCTORS.contains(assignment.employeeId()))
            .count();
    }
}
