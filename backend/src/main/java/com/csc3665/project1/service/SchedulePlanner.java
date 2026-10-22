package com.csc3665.project1.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SchedulePlanner {
    private static final int REST_MINUTES = 10 * 60;
    private static final int MINUTES_PER_DAY = 24 * 60;

    private SchedulePlanner() {}

    static List<Assignment> generate(
        LocalDate start,
        int dayCount,
        List<Integer> doctorIds,
        List<Integer> technicianIds
    ) {
        return generate(start, dayCount, doctorIds, technicianIds, List.of());
    }


    static List<Assignment> generate(
        LocalDate start,
        int dayCount,
        List<Integer> doctorIds,
        List<Integer> technicianIds,
        List<Assignment> existingAssignments
    ) {
        return generate(start, dayCount, doctorIds, technicianIds, existingAssignments, Set.of());
    }


    static List<Assignment> generate(
        LocalDate start,
        int dayCount,
        List<Integer> doctorIds,
        List<Integer> technicianIds,
        List<Assignment> existingAssignments,
        Set<Unavailability> unavailable
    ) {

        if (doctorIds.isEmpty()) {
            throw new IllegalStateException("Add at least one doctor before generating a schedule.");
        }
        if (technicianIds.size() < 2) {
            throw new IllegalStateException("At least two technicians are required for Sunday coverage.");
        }

        List<Shift> shifts = createShifts(start, dayCount);
        List<Assignment> assignments = new ArrayList<>(existingAssignments);
        Map<LocalDate, List<Shift>> shiftsByWeek = new HashMap<>();
        for (Shift shift : shifts) {
            shiftsByWeek.computeIfAbsent(weekStart(shift.date()), ignored -> new ArrayList<>()).add(shift);
        }

        shiftsByWeek.keySet().stream().sorted().forEach(week -> {
            List<Shift> weekShifts = shiftsByWeek.get(week);
            List<Shift> required = weekShifts.stream()
                .sorted(Comparator
                    .comparing((Shift shift) -> !shift.block().equals("Overnight"))
                    .thenComparing(Shift::date)
                    .thenComparing(Shift::block))
                .toList();

            for (Shift shift : required) {
                if (hasDoctor(assignments, shift, doctorIds)) {
                    continue;
                }
                int doctorId = selectEmployee(
                    doctorIds, assignments, shift, week, true, unavailable
                ).orElseThrow(() -> new IllegalStateException(
                    "Not enough doctors to cover " + shift.block() + " on " + shift.date()
                        + " while maintaining rest and weekly limits."
                ));
                assignments.add(shift.assignTo(doctorId));
            }

            addExtraDoctorShifts(weekShifts, doctorIds, assignments, week, unavailable);
            addTechnicianShifts(weekShifts, technicianIds, assignments, week, unavailable);
        });
        LocalDate endExclusive = start.plusDays(dayCount);
        return assignments.stream()
            .filter(assignment -> !assignment.date().isBefore(start)
                && assignment.date().isBefore(endExclusive))
            .toList();
    }

    private static List<Shift> createShifts(LocalDate start, int dayCount) {
        List<Shift> shifts = new ArrayList<>();
        for (int offset = 0; offset < dayCount; offset++) {
            LocalDate date = start.plusDays(offset);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                shifts.add(shift(date, "Sunday Shift", false));
            } else {
                shifts.add(shift(date, "Early Morning", true));
                shifts.add(shift(date, "8:30 Shift", false));
                shifts.add(shift(date, "Late Doctor", false));
            }
            shifts.add(shift(date, "Overnight", false));
        }
        return shifts;
    }

    static Assignment existingAssignment(
        int employeeId,
        LocalDate date,
        String block,
        boolean surgery
    ) {
        return shift(date, block, surgery).assignTo(employeeId);
    }

    private static Shift shift(LocalDate date, String block, boolean surgery) {
        return switch (block) {
            case "Early Morning" -> new Shift(
                date, block, surgery, 7 * 60 + 30, 18 * 60 + 30
            );
            case "8:30 Shift" -> new Shift(
                date, block, surgery, 8 * 60 + 30, 19 * 60 + 30
            );
            case "Late Doctor" -> new Shift(
                date, block, surgery, 9 * 60 + 30, 20 * 60 + 30
            );
            case "Sunday Shift" -> new Shift(
                date, block, surgery, 8 * 60, 20 * 60
            );
            case "Overnight" -> new Shift(
                date, block, surgery, 20 * 60, 32 * 60
            );
            default -> throw new IllegalStateException("Unknown shift block: " + block);
        };
    }

    private static void addExtraDoctorShifts(
        List<Shift> weekShifts,
        List<Integer> doctorIds,
        List<Assignment> assignments,
        LocalDate week,
        Set<Unavailability> unavailable
    ) {
        int currentAssignments = (int) assignments.stream()
            .filter(assignment -> doctorIds.contains(assignment.employeeId())
                && weekStart(assignment.date()).equals(week))
            .count();
        int targetAssignments = Math.max(currentAssignments, 4 * doctorIds.size());

        List<Shift> extras = new ArrayList<>();
        weekShifts.stream()
            .filter(shift -> shift.block().equals("Early Morning")
                && shift.date().getDayOfWeek() == DayOfWeek.SATURDAY)
            .forEach(extras::add);
        weekShifts.stream()
            .filter(shift -> shift.block().equals("Late Doctor")
                && shift.date().getDayOfWeek() == DayOfWeek.FRIDAY)
            .forEach(extras::add);
        weekShifts.stream()
            .filter(shift -> shift.block().equals("Early Morning")
                && shift.date().getDayOfWeek() != DayOfWeek.SATURDAY)
            .sorted(Comparator.comparing(Shift::date))
            .forEach(extras::add);
        weekShifts.stream()
            .filter(shift -> shift.block().equals("8:30 Shift")
                || shift.block().equals("Late Doctor"))
            .sorted(Comparator.comparing(Shift::date).thenComparing(Shift::block))
            .forEach(extras::add);

        int remaining = targetAssignments - currentAssignments;
        while (remaining > 0) {
            boolean added = false;
            for (Shift shift : extras) {
                if (remaining == 0) {
                    break;
                }
                var employee = selectEmployee(doctorIds, assignments, shift, week, true, unavailable);
                if (employee.isPresent()) {
                    assignments.add(shift.assignTo(employee.get()));
                    remaining--;
                    added = true;
                }
            }
            if (!added) {
                break;
            }
        }
    }

    private static void addTechnicianShifts(
        List<Shift> weekShifts,
        List<Integer> technicianIds,
        List<Assignment> assignments,
        LocalDate week,
        Set<Unavailability> unavailable
    ) {
        for (Shift shift : weekShifts) {
            int pinnedTechnicians = (int) assignments.stream()
                .filter(a -> a.date().equals(shift.date())
                    && a.block().equals(shift.block())
                    && technicianIds.contains(a.employeeId()))
                .count();
            int doctorCount = countAssignments(assignments, shift) - pinnedTechnicians;
            int technicianCount = (shift.block().equals("Overnight") ? 1
                : shift.block().equals("Sunday Shift") ? 2
                : doctorCount) - pinnedTechnicians;

            for (int i = 0; i < technicianCount; i++) {
                int technicianId = selectEmployee(
                    technicianIds, assignments, shift, week, false, unavailable
                ).orElseThrow(() -> new IllegalStateException(
                    "Not enough technicians to cover " + shift.block() + " on " + shift.date()
                        + " while maintaining 10 hours between shifts."
                ));
                assignments.add(shift.assignTo(technicianId));
            }
        }
    }

    private static java.util.Optional<Integer> selectEmployee(
        List<Integer> employees,
        List<Assignment> assignments,
        Shift shift,
        LocalDate week,
        boolean doctor,
        Set<Unavailability> unavailable
    ) {
        Set<Integer> assignedOnShift = assignments.stream()
            .filter(assignment -> assignment.date().equals(shift.date())
                && assignment.block().equals(shift.block()))
            .map(Assignment::employeeId)
            .collect(java.util.stream.Collectors.toSet());

        return employees.stream()
            .filter(id -> !assignedOnShift.contains(id))
            .filter(id -> !unavailable.contains(new Unavailability(id, shift.date())))
            .filter(id -> doctor ? doctorEligible(id, shift, week, assignments)
                : restCompatible(id, shift, assignments))
            .min(Comparator
                .comparingInt((Integer id) -> countWeeklyAssignments(id, week, assignments))
                .thenComparingInt(id -> countAssignments(id, assignments))
                .thenComparingInt(Integer::intValue));
    }

    private static boolean doctorEligible(
        int employeeId,
        Shift shift,
        LocalDate week,
        List<Assignment> assignments
    ) {
        if (!restCompatible(employeeId, shift, assignments)) {
            return false;
        }
        return assignments.stream()
            .filter(assignment -> assignment.employeeId() == employeeId
                && weekStart(assignment.date()).equals(week))
            .noneMatch(assignment ->
                (shift.surgery() && assignment.surgery())
                    || (shift.block().equals("Overnight")
                        && assignment.block().equals("Overnight"))
            );
    }

    private static boolean restCompatible(
        int employeeId,
        Shift shift,
        List<Assignment> assignments
    ) {
        return assignments.stream()
            .filter(assignment -> assignment.employeeId() == employeeId)
            .noneMatch(assignment ->
                shift.startMinute() < assignment.endMinute() + REST_MINUTES
                    && assignment.startMinute() < shift.endMinute() + REST_MINUTES
            );
    }

    static boolean conflictsWith(Assignment candidate, List<Assignment> others, boolean doctor) {
        return others.stream()
            .filter(o -> o.employeeId() == candidate.employeeId())
            .anyMatch(o -> {
                boolean tooClose = candidate.startMinute() < o.endMinute() + REST_MINUTES
                    && o.startMinute() < candidate.endMinute() + REST_MINUTES;
                boolean sameWeek = weekStart(o.date()).equals(weekStart(candidate.date()));
                boolean doubleRole = doctor && sameWeek
                    && ((candidate.surgery() && o.surgery())
                        || (candidate.block().equals("Overnight") && o.block().equals("Overnight")));
                return tooClose || doubleRole;
            });
    }


    private static boolean hasDoctor(List<Assignment> assignments, Shift shift, List<Integer> doctorIds) {
    return assignments.stream().anyMatch(a ->
        a.date().equals(shift.date())
            && a.block().equals(shift.block())
            && doctorIds.contains(a.employeeId()));
    }


    private static int countWeeklyAssignments(
        int employeeId,
        LocalDate week,
        List<Assignment> assignments
    ) {
        return (int) assignments.stream()
            .filter(assignment -> assignment.employeeId() == employeeId
                && weekStart(assignment.date()).equals(week))
            .count();
    }

    private static int countAssignments(int employeeId, List<Assignment> assignments) {
        return (int) assignments.stream()
            .filter(assignment -> assignment.employeeId() == employeeId)
            .count();
    }

    private static int countAssignments(List<Assignment> assignments, Shift shift) {
        return (int) assignments.stream()
            .filter(assignment -> assignment.date().equals(shift.date())
                && assignment.block().equals(shift.block()))
            .count();
    }

    private static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    record Assignment(
        int employeeId,
        LocalDate date,
        String block,
        boolean surgery,
        long startMinute,
        long endMinute
    ) {}

    record Unavailability(
        int employeeId, 
        LocalDate date
    ) {}


    private record Shift(
        LocalDate date,
        String block,
        boolean surgery,
        int startOffset,
        int endOffset
    ) {
        long startMinute() {
            return date.toEpochDay() * MINUTES_PER_DAY + startOffset;
        }

        long endMinute() {
            return date.toEpochDay() * MINUTES_PER_DAY + endOffset;
        }

        Assignment assignTo(int employeeId) {
            return new Assignment(
                employeeId, date, block, surgery, startMinute(), endMinute()
            );
        }
    }
}
