package com.csc3665.project1.service;
import com.csc3665.project1.dto.LoginResponse;
import com.csc3665.project1.dto.ScheduleEntry;
import com.csc3665.project1.dto.ScheduleResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.csc3665.project1.dto.StaffResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DBService {
    // TODO (p.1): Initialize a database-backed admin/admin account with first_login=true.
    // Implementation notes: use etype = 'Admin' for the admin role.

    // DONE (p.1): Generate typically four shifts per employee per week, with at least 10
    // hours between shifts; doctors have at most one surgery day and one overnight per week.
    // DONE (p.1): Cover each required shift with a doctor:
    // Mon-Sat: Early Morning 07:30-18:30, 8.30 Shift 08:30-19:30,
    // Late Doctor 09:30-20:30, and Overnight 20:00-08:00.
    // Sunday: Sunday Shift 08:00-20:00 and Overnight 20:00-08:00.
    // No more than one doctor is assigned overnight or on Sunday. Extra doctor shifts
    // prioritize Saturday Early Morning and Friday Late Doctor, then other Early Mornings.
    // DONE (p.1): Assign technicians to cover each doctor's shift, one technician overnight,
    // and two on Sunday Shift; balance technician assignments across available staff.
    // Assumption: the two Sunday technicians cover the Sunday Shift; Sunday Overnight has one.
    // DONE (p.1): getSchedule() returns the next two weeks and opposite-role coworkers;
    // surgery shifts are highlighted using shifts.surgery.
    // The generator uses the existing shifts/working/employees tables and changes no schema.
    // Surgery reuses Early Morning with surgery=true. Since surgery is stored on the shift,
    // that indicator applies to every doctor assigned to Early Morning, not one individual.

    // TODO (pp.1-2): Allow requests for work dates/times and time off. Track/display
    // calendar-year usage against eight vacation days and four sick days per employee.
    // TODO (p.2): Process first-come, first-served; grant feasible requests and rearrange
    // the schedule, deny conflicts with earlier granted requests, and immediately explain
    // denials. Require three weeks' vacation notice; sick days cannot be today.
    // Implementation notes: requests + pto_requests/work_requests hold request data.
    // pto_requests.reason uses Sick/Vacation. Persist usage/schedule changes on approval.

    private final DataSource dataSource;

    public DBService(DataSource dataSource) {
        this.dataSource = dataSource;
    }



    public LoginResponse login(String username, String password) {
        String sql = """
            SELECT eid, ename, etype, first_login
            FROM employees
            WHERE euser = ? AND epass = ?
            """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password."
                    );
                }

                String role = rs.getString("etype").toLowerCase(java.util.Locale.ROOT);

                return new LoginResponse(
                    rs.getInt("eid"),
                    rs.getString("ename"),
                    role,
                    rs.getBoolean("first_login")
                );
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to log in due to a database error.",
                e
            );
        }
    }
    
    public void changePassword(int eid, String newPassword) {
        String sql = "UPDATE employees SET epass = ?, first_login = false WHERE eid = ?";

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, newPassword);
            statement.setInt(2, eid);

            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Employee not found."
                );
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to change password due to a database error.",
                e
            );
        }
    }

    public void setStaffPassword(int eid, String newPassword) {
        String sql = """
            UPDATE employees
            SET epass = ?, first_login = true
            WHERE eid = ? AND LOWER(etype) IN ('doctor', 'technician')
            """;
        
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, newPassword);
            statement.setInt(2, eid);

            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Employee not found."
                );
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to change password due to a database error.",
                e
            );
        }
    }

    public int addStaff(String name, String username, String password, String role) {
        if ("doctor".equalsIgnoreCase(role)) {
            role = "Doctor";
        } else if ("technician".equalsIgnoreCase(role)) {
            role = "Technician";
        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Role must be doctor or technician."
            );
        }
        String sql = "INSERT INTO employees (ename, euser, epass, etype) VALUES (?, ?, ?, ?)";

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, name);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setString(4, role);

            int updatedRows = statement.executeUpdate();
            if (updatedRows != 1) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to add staff."
                );
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Employee created, but no ID was returned."
            );
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to add staff due to a database error.",
                e
            );
        }
    }

    public void generateSchedule() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(13);
        String selectEmployees = """
            SELECT eid
            FROM employees
            WHERE LOWER(etype) = ?
            ORDER BY eid
            """;
        String deleteAssignments = "DELETE FROM working WHERE sdate BETWEEN ? AND ?";
        String selectExistingAssignments = """
            SELECT w.eid, w.sdate, w.block, s.surgery
            FROM working w
            JOIN shifts s ON s.sdate = w.sdate AND s.block = w.block
            WHERE w.sdate BETWEEN ? AND ? AND (w.sdate < ? OR w.sdate > ?)
            """;
        String upsertShift = """
            INSERT INTO shifts (surgery, block, day_of_week, sdate)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (block, sdate) DO UPDATE
            SET surgery = EXCLUDED.surgery, day_of_week = EXCLUDED.day_of_week
            """;
        String insertAssignment = "INSERT INTO working (eid, sdate, block) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                List<Integer> doctors = loadEmployeeIds(connection, selectEmployees, "doctor");
                List<Integer> technicians = loadEmployeeIds(connection, selectEmployees, "technician");
                LocalDate constraintStart = start
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusDays(1);
                LocalDate constraintEnd = end
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1);
                List<SchedulePlanner.Assignment> existingAssignments =
                    loadExistingAssignments(
                        connection,
                        selectExistingAssignments,
                        constraintStart,
                        constraintEnd,
                        start,
                        end
                    );
                List<SchedulePlanner.Assignment> assignments;
                try {
                    assignments = SchedulePlanner.generate(
                        start, 14, doctors, technicians, existingAssignments
                    );
                } catch (IllegalStateException e) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
                }

                try (PreparedStatement delete = connection.prepareStatement(deleteAssignments)) {
                    delete.setDate(1, java.sql.Date.valueOf(start));
                    delete.setDate(2, java.sql.Date.valueOf(end));
                    delete.executeUpdate();
                }

                Map<String, SchedulePlanner.Assignment> shifts = new LinkedHashMap<>();
                for (SchedulePlanner.Assignment assignment : assignments) {
                    shifts.putIfAbsent(
                        assignment.date() + "|" + assignment.block(), assignment
                    );
                }
                try (
                    PreparedStatement shiftStatement = connection.prepareStatement(upsertShift);
                    PreparedStatement assignmentStatement = connection.prepareStatement(insertAssignment)
                ) {
                    for (SchedulePlanner.Assignment assignment : shifts.values()) {
                        shiftStatement.setBoolean(1, assignment.surgery());
                        shiftStatement.setString(2, assignment.block());
                        shiftStatement.setString(
                            3,
                            assignment.date().getDayOfWeek().getDisplayName(
                                TextStyle.FULL, Locale.ENGLISH
                            )
                        );
                        shiftStatement.setDate(4, java.sql.Date.valueOf(assignment.date()));
                        shiftStatement.addBatch();
                    }
                    shiftStatement.executeBatch();

                    for (SchedulePlanner.Assignment assignment : assignments) {
                        assignmentStatement.setInt(1, assignment.employeeId());
                        assignmentStatement.setDate(2, java.sql.Date.valueOf(assignment.date()));
                        assignmentStatement.setString(3, assignment.block());
                        assignmentStatement.addBatch();
                    }
                    assignmentStatement.executeBatch();
                }
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate the schedule due to a database error.",
                e
            );
        }
    }

    private List<Integer> loadEmployeeIds(
        Connection connection,
        String sql,
        String role
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role);
            try (ResultSet rs = statement.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("eid"));
                }
                return ids;
            }
        }
    }

    private List<SchedulePlanner.Assignment> loadExistingAssignments(
        Connection connection,
        String sql,
        LocalDate constraintStart,
        LocalDate constraintEnd,
        LocalDate start,
        LocalDate end
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(constraintStart));
            statement.setDate(2, java.sql.Date.valueOf(constraintEnd));
            statement.setDate(3, java.sql.Date.valueOf(start));
            statement.setDate(4, java.sql.Date.valueOf(end));
            try (ResultSet rs = statement.executeQuery()) {
                List<SchedulePlanner.Assignment> assignments = new ArrayList<>();
                while (rs.next()) {
                    assignments.add(SchedulePlanner.existingAssignment(
                        rs.getInt("eid"),
                        rs.getDate("sdate").toLocalDate(),
                        rs.getString("block"),
                        rs.getBoolean("surgery")
                    ));
                }
                return assignments;
            }
        }
    }

    // Next two weeks (today .. today + 13 days) of shifts for one employee, plus the
    // opposite-role employees on each of those shifts. Read-only; the schedule itself is
    // whatever is in the working table, so rearrangements from granted requests show up
    // automatically on the next call.
    public ScheduleResponse getSchedule(int eid) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(13);

        String sql = """
            SELECT s.sdate, s.day_of_week, s.block, s.surgery, e2.ename AS coworker
            FROM working me
            JOIN employees mine ON mine.eid = me.eid
            JOIN shifts s ON s.sdate = me.sdate AND s.block = me.block
            LEFT JOIN working w2 ON w2.sdate = me.sdate AND w2.block = me.block
            LEFT JOIN employees e2 ON e2.eid = w2.eid
                AND e2.etype IN ('Doctor', 'Technician')
                AND e2.etype <> mine.etype
            WHERE me.eid = ? AND s.sdate BETWEEN ? AND ?
            ORDER BY s.sdate,
                CASE s.block
                    WHEN 'Early Morning' THEN 1
                    WHEN '8:30 Shift'    THEN 2
                    WHEN 'Late Doctor'   THEN 3
                    WHEN 'Sunday Shift'  THEN 4
                    WHEN 'Overnight'     THEN 5
                    ELSE 6
                END,
                e2.ename
            """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, eid);
            statement.setDate(2, java.sql.Date.valueOf(start));
            statement.setDate(3, java.sql.Date.valueOf(end));

            // One entry per (date, block); the join repeats it once per coworker.
            Map<String, ScheduleEntry> entries = new LinkedHashMap<>();

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getDate("sdate").toLocalDate().toString();
                    String block = rs.getString("block");
                    String key = date + "|" + block;

                    ScheduleEntry entry = entries.get(key);
                    if (entry == null) {
                        entry = new ScheduleEntry(
                            date,
                            rs.getString("day_of_week"),
                            block,
                            rs.getBoolean("surgery"),
                            new ArrayList<>()
                        );
                        entries.put(key, entry);
                    }

                    String coworker = rs.getString("coworker");
                    if (coworker != null && !entry.coworkers().contains(coworker)) {
                        entry.coworkers().add(coworker);
                    }
                }
            }

            return new ScheduleResponse(
                start.toString(),
                end.toString(),
                new ArrayList<>(entries.values())
            );
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to load schedule.",
                e
            );
        }
    }

    public List<StaffResponse> getStaff() {
        String sql = """
            SELECT eid, ename, euser, etype
            FROM employees
            WHERE etype IN ('Doctor', 'Technician')
            """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()
        ) {
            List<StaffResponse> staff = new ArrayList<>();

            while (rs.next()) {
                staff.add(new StaffResponse(
                    rs.getInt("eid"),
                    rs.getString("ename"),
                    rs.getString("euser"),
                    rs.getString("etype").toLowerCase(java.util.Locale.ROOT)
                ));
            }

            return staff;
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to load staff.",
                e
            );
        }
    }

    public void removeStaff(int eid) {
    String sql = """
        DELETE FROM employees
        WHERE eid = ? AND LOWER(etype) IN ('doctor', 'technician')
        """;

    try (
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)
    ) {
        statement.setInt(1, eid);

        if (statement.executeUpdate() == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Staff member not found."
            );
        }
    } catch (SQLException e) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unable to remove staff.",
            e
        );
    }
}
}
