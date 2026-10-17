package com.csc3665.project1.service;
import com.csc3665.project1.dto.LoginResponse;
import com.csc3665.project1.dto.PTOResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.csc3665.project1.dto.StaffResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate; 

@Service
public class DBService {
    // TODO (p.1): Initialize a database-backed admin/admin account with first_login=true.
    // Implementation notes: use etype = 'Admin' for the admin role.

    // TODO (p.1): Schedule typically four shifts/employee/week, with at least 10 hours
    // between shifts; no doctor has two surgery days or two overnight shifts in a week.
    // TODO (p.1): Cover these shifts with at least one doctor each:
    // Mon-Sat: Early Morning 07:30-18:30, 8.30 Shift 08:30-19:30,
    // Late Doctor 09:30-20:30, surgeon 07:30-18:30, and Overnight 20:00-08:00.
    // Sunday: Sunday Shift 08:00-20:00 and Overnight 20:00-08:00.
    // Never two doctors overnight or on Sunday. Prioritize extra doctor shifts on
    // Saturday Early Morning and Friday Late Doctor, then prefer Early Morning.
    // TODO (p.1): Provide at least one technician per doctor, only one overnight, two on
    // Sundays, and spread the remaining technician scheduling as evenly as possible.
    // The document says "two technicians on Sundays"; clarify its interaction with the
    // overnight rule in Assumptions rather than adding a new staffing rule here.
    // TODO (p.1): Return each employee's next two weeks and opposite-role coworkers;
    // display doctors' surgery shifts in a different color.
    // Implementation notes: use shifts/working/employees and exact schema block values.
    // The current (block, sdate) key cannot represent separate regular Early Morning
    // and surgery rows on the same date; account for this when implementing coverage.

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
        SELECT COALESCE(SUM (r.end_date - r.start_date + 1), 0) as used
        FROM requests r, pto_requests p
        WHERE r.eid = ?
        AND p.rid = r.rid
        AND p.reason = ?
        AND r.approved = true
        AND r.start_date >= ?
        AND r.end_date < ?
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
}

