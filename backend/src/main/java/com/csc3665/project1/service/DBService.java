package com.csc3665.project1.service;
import com.csc3665.project1.dto.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DBService {
    // TODO (pp.1-2): Allow requests for work dates/times and time off. Track/display
    // calendar-year usage against eight vacation days and four sick days per employee.
    // TODO (p.2): Process first-come, first-served; grant feasible requests and rearrange
    // the schedule, deny conflicts with earlier granted requests, and immediately explain
    // denials. Require three weeks' vacation notice; sick days cannot be today.
    // Implementation notes: requests + pto_requests/work_requests hold request data.
    // pto_requests.reason uses Sick/Vacation. Persist usage/schedule changes on approval.

    private final DataSource dataSource;
    private static final int VACATION_DAYS = 8;
    private static final int SICK_DAYS = 4;

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

    public PTOResponse getPTO(int eid){
        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        String sql = """
        SELECT COALESCE(SUM (p.end_date - p.start_date + 1), 0) as used
        FROM requests r, pto_requests p
        WHERE r.eid = ?
        AND p.rid = r.rid
        AND p.reason = ?
        AND r.approved = true
        AND p.start_date >= ?
        AND p.end_date < ?
        """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ){
            statement.setInt(1, eid);
            statement.setString(2, "Vacation");
            statement.setDate(3, java.sql.Date.valueOf(yearStart));
            statement.setDate(4, java.sql.Date.valueOf(yearStart.plusYears(1)));
            int vacationUsed;
            try (ResultSet rs = statement.executeQuery()){
                rs.next();
                vacationUsed = rs.getInt("used");
            }

            statement.setString(2, "Sick");
            int sickUsed;
            try (ResultSet rs = statement.executeQuery()){
                rs.next();
                sickUsed = rs.getInt("used");
            }
            return new PTOResponse(
            new PTOResponse.PTOBalance(vacationUsed, VACATION_DAYS),
            new PTOResponse.PTOBalance(sickUsed, SICK_DAYS)
        );
        }

        catch(SQLException e){
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to load PTO balances.",
                e
            );
        }

    }

    public String addPTO(
        int eid,
        String reason,
        LocalDate startDate,
        LocalDate endDate
    ){

        //NEED CHECKS FOR CURRENT TIME SO THAT YOU CANT SCHEDULE LESS THAN 3 WEEKS IN ADVANCE
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Select valid start and end dates."
            );
        }
        if (!"Vacation".equals(reason) && !"Sick".equals(reason)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Reason must be Vacation or Sick."
            );
        }

        String sql = """
            INSERT INTO requests (eid, approved, request_timestamp)
            VALUES (?, false, CURRENT_TIMESTAMP)
            RETURNING rid
        """;
        String ptoSql = """
        INSERT INTO pto_requests (rid, reason, start_date, end_date) 
        VALUES (?, ?, ?, ?)
        """;

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (
                PreparedStatement requestStatement = connection.prepareStatement(sql);
                PreparedStatement ptoStatement = connection.prepareStatement(ptoSql)
            ) {
                requestStatement.setInt(1, eid);
                

                int rid;
                try (ResultSet rs = requestStatement.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No request ID was returned.");
                    }
                    rid = rs.getInt("rid");
                }

                ptoStatement.setInt(1, rid);
                ptoStatement.setString(2, reason);
                ptoStatement.setDate(3, java.sql.Date.valueOf(startDate));
                ptoStatement.setDate(4, java.sql.Date.valueOf(endDate));
                ptoStatement.executeUpdate();

                connection.commit();
                return "PTO request submitted. Awaiting approval.";
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unable to submit PTO request.", e
            );
        }
    }

    public String addWorkRequest(
        int eid,
        LocalDate sDate,
        String block
    ){
        if (sDate == null || block == null || block.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Select a shift date and block."
            );
        }

        String shiftSql = "SELECT * FROM shifts WHERE sdate = ? AND block = ?";
        String requestSql = """
            INSERT INTO requests (eid, approved, request_timestamp)
            VALUES (?, false, CURRENT_TIMESTAMP)
            RETURNING rid
            """;
        String workSql = "INSERT INTO work_requests (rid, sdate, block) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (
                PreparedStatement shiftStatement = connection.prepareStatement(shiftSql);
                PreparedStatement requestStatement = connection.prepareStatement(requestSql);
                PreparedStatement workStatement = connection.prepareStatement(workSql)
            ) {
                shiftStatement.setDate(1, java.sql.Date.valueOf(sDate));
                shiftStatement.setString(2, block);
                try (ResultSet rs = shiftStatement.executeQuery()) {
                    if (!rs.next()) {
                        throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "The selected shift does not exist."
                        );
                    }
                }

                requestStatement.setInt(1, eid);
                int rid;
                try (ResultSet rs = requestStatement.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No request ID was returned.");
                    }
                    rid = rs.getInt("rid");
                }

                workStatement.setInt(1, rid);
                workStatement.setDate(2, java.sql.Date.valueOf(sDate));
                workStatement.setString(3, block);
                workStatement.executeUpdate();

                connection.commit();
                return "Work request submitted. Awaiting approval.";
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unable to submit work request.", e
            );
        }
    }
}
