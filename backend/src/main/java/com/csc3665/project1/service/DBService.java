package com.csc3665.project1.service;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.csc3665.project1.dto.LoginResponse;
import com.csc3665.project1.dto.PTODecision;
import com.csc3665.project1.dto.PTOResponse;
import com.csc3665.project1.dto.ScheduleEntry;
import com.csc3665.project1.dto.ScheduleResponse;
import com.csc3665.project1.dto.StaffResponse;


@Service
public class DBService {
    // Done: addPTO decides time off on submission (notice, sick-today, overlap, balance,
    // trial-schedule coverage), saves it, and regenerates the schedule when it applies.
    // getPTO reports calendar-year usage against eight vacation and four sick days.
    // TODO (p.1-2): addWorkRequest still only stores the request (approved=false). It needs
    // decision logic: a shift the employee isn't already on, conflicts with granted requests,
    // coverage, then rearranging the schedule.
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
                Set<SchedulePlanner.Unavailability> unavailable =
                    loadUnavailability(connection, start, end);
                List<SchedulePlanner.Assignment> assignments;
                try {
                    assignments = SchedulePlanner.generate(
                        start, 14, doctors, technicians, existingAssignments, unavailable
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


    private Set<SchedulePlanner.Unavailability> loadUnavailability(
        Connection connection,
        LocalDate from,
        LocalDate to
    ) throws SQLException {
        String sql = """
            SELECT r.eid, p.start_date, p.end_date
            FROM requests r, pto_requests p
            WHERE p.rid = r.rid AND r.approved = true
            AND p.start_date <= ? AND p.end_date >= ?
        """;
        Set<SchedulePlanner.Unavailability> unavailable = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDate(1, java.sql.Date.valueOf(to));
            statement.setDate(2, java.sql.Date.valueOf(from));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int eid = rs.getInt("eid");

                    LocalDate first = rs.getDate("start_date").toLocalDate();
                    LocalDate last = rs.getDate("end_date").toLocalDate();

                    for (LocalDate d = first.isBefore(from) ? from : first;
                        !d.isAfter(last) && !d.isAfter(to);
                        d = d.plusDays(1)) {
                        unavailable.add(new SchedulePlanner.Unavailability(eid, d));
                    }
                }
            }
        }
        return unavailable;
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

    // gives start and end date of all used PTO days of specified type in this year
    private int usedDays(Connection connection, int eid, String reason, int year) throws SQLException {
        String sql = """
            SELECT p.start_date, p.end_date
            FROM requests r, pto_requests p
            WHERE p.rid = r.rid AND r.eid = ? AND r.approved = true AND p.reason = ?
            AND p.start_date >= ? AND p.start_date < ?
        """;
        int used = 0;
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, eid);
            st.setString(2, reason);
            st.setDate(3, java.sql.Date.valueOf(LocalDate.of(year, 1, 1)));
            st.setDate(4, java.sql.Date.valueOf(LocalDate.of(year + 1, 1, 1)));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    used += (int) ChronoUnit.DAYS.between(start, end) + 1;
                }
            }
        }
        return used;
    }

    private boolean overlapsApprovedWork(Connection connection, int eid, LocalDate start, LocalDate end) throws SQLException {
        String sql = """
            SELECT COUNT(*) AS n
            FROM requests r, work_requests w 
            WHERE w.rid = r.rid AND r.eid = ? AND r.approved = true
            AND w.sdate BETWEEN ? AND ?
        """;
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, eid);
            st.setDate(2, java.sql.Date.valueOf(start));
            st.setDate(3, java.sql.Date.valueOf(end));
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getInt("n") > 0;
            }
        }
    }

    // Tests building schedule for the week to see if we will be short on staff by granting it
    private String coverageProblem(
        Connection connection,
        int eid,
        LocalDate start,
        LocalDate end
    ) throws SQLException {
        if (end.isBefore(LocalDate.now())) {
            return null;
        }

        String sql = "SELECT eid FROM employees WHERE LOWER(etype) = ? ORDER BY eid";
        List<Integer> doctors = loadEmployeeIds(connection, sql, "doctor");
        List<Integer> technicians = loadEmployeeIds(connection, sql, "technician");


        LocalDate monday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = end.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        int dayCount = (int) ChronoUnit.DAYS.between(monday, sunday) + 1;


        Set<SchedulePlanner.Unavailability> unavailable =
            loadUnavailability(connection, monday, sunday);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            unavailable.add(new SchedulePlanner.Unavailability(eid, d));
        }

        try {
            SchedulePlanner.generate(monday, dayCount, doctors, technicians, List.of(), unavailable);
            return null;
        } catch (IllegalStateException e) {
            return "We will be short on staff if we grant your request. " + e.getMessage();
        }
    }


    // check if an employee already got som time off during this period
    private boolean overlapsApproved(Connection connection, int eid, LocalDate start, LocalDate end) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM requests r, pto_requests p
            WHERE p.rid = r.rid AND r.eid = ? AND r.approved = true
            AND p.start_date <= ? AND p.end_date >= ?
        """;
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, eid);
            st.setDate(2, java.sql.Date.valueOf(end));
            st.setDate(3, java.sql.Date.valueOf(start));
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public PTODecision addPTO(int eid, String reason, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select valid start and end dates.");
        }
        if (!reason.equals("Vacation") && !reason.equals("Sick")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason must be either Vacation or Sick.");
        }

        LocalDate today = LocalDate.now();
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int allowed = reason.equals("Vacation") ? VACATION_DAYS : SICK_DAYS;

        try (Connection connection = dataSource.getConnection()) {
            //denial reason is null until proven otherwise
            String denialReason = null;

            if (startDate.getYear() != endDate.getYear()) {
                denialReason = "A request cannot be across two calendar years.";
            } else if (reason.equals("Vacation") && startDate.isBefore(today.plusWeeks(3))) {
                denialReason = "Request is too late.";
            } else if (reason.equals("Sick") && startDate.equals(today)) {
                denialReason = "A sick day cannot be requested for today.";
            } else if (overlapsApproved(connection, eid, startDate, endDate)) {
                denialReason = "These dates conflict with time off you were already granted.";
            } else if (overlapsApprovedWork(connection, eid, startDate, endDate)) {
                denialReason = "Your request cannot be granted because it conflicts with a shift request that was already granted.";
            } 
            else {
                int used = usedDays(connection, eid, reason, startDate.getYear());
                if (used + days > allowed) {
                    denialReason = "You only have " + (allowed - used) + " " + reason.toLowerCase()
                        + " day(s) left this year, but requested " + days + ".";
                }
            }

            if (denialReason == null) {
                denialReason = coverageProblem(connection, eid, startDate, endDate);
            }

            boolean approved = (denialReason == null);

            String requestSql = """
                INSERT INTO requests (eid, approved, request_timestamp)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                RETURNING rid
            """;
            String ptoSql = "INSERT INTO pto_requests (rid, reason, start_date, end_date) VALUES (?, ?, ?, ?)";

            connection.setAutoCommit(false);
            try (
                PreparedStatement requestStatement = connection.prepareStatement(requestSql);
                PreparedStatement ptoStatement = connection.prepareStatement(ptoSql)
            ) {
                requestStatement.setInt(1, eid);
                requestStatement.setBoolean(2, approved);
                int rid;
                try (ResultSet rs = requestStatement.executeQuery()) {
                    rs.next();
                    rid = rs.getInt("rid");
                }
                ptoStatement.setInt(1, rid);
                ptoStatement.setString(2, reason);
                ptoStatement.setDate(3, java.sql.Date.valueOf(startDate));
                ptoStatement.setDate(4, java.sql.Date.valueOf(endDate));
                ptoStatement.executeUpdate();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }

            if (approved) {
            boolean inSchedule = !startDate.isAfter(today.plusDays(13)) && !endDate.isBefore(today);
            String note = "";
            if (inSchedule) {
                try {
                    generateSchedule();
                    note = " Your schedule has been updated.";
                } catch (ResponseStatusException e) {
                    note = " Your time off is saved, but the schedule could not be rebuilt yet: " + e.getReason();
                }
            }
            return new PTODecision(true, "Approved: " + days + " " + reason.toLowerCase() + " day(s)." + note);
        }

            return new PTODecision(false, denialReason);
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to submit PTO request.", e);
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
