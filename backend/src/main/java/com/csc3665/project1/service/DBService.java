package com.csc3665.project1.service;

import com.csc3665.project1.dto.LoginResponse;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DBService {

    // TODO (assignment p.2): Finish replacing jdbc.query/update with plain JDBC;
    // the React/Spring Boot option disallows JdbcTemplates and Spring Data JPA.
    // Implementation note: DataSource is already injected; methods still use old jdbc calls.
    // Accounts use employees, including the user's added first_login column.

    // TODO (p.1): Provide admin/admin and force the admin's first-login password change.
    // TODO (p.1): Let only the admin create/remove staff and set their passwords.
    // Implementation notes: use is_admin for the admin role; etype must satisfy the CHECK.
    // Staff listing can use employees WHERE is_admin=false. Related working/requests rows
    // must be handled when removing an employee because of their foreign keys.

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

    public DBService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public LoginResponse login(String username, String password) {
        // TODO (p.1): Support doctor, technician, and administrator logins.
        // Implementation: map is_admin=true to admin; Doctor/Technician to lowercase UI roles.
        List<LoginResponse> results = jdbc.query(
            "SELECT eid, ename, etype, first_login FROM employees WHERE euser = ? AND epass = ?",
            (rs, rowNum) -> new LoginResponse(
                rs.getInt("eid"),
                rs.getString("ename"),
                rs.getString("etype"),
                rs.getBoolean("first_login")
            ),
            username,
            password
        );

        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        return results.get(0);
    }

    public void changePassword(int eid, String newPassword) {
        int updated = jdbc.update(
            "UPDATE employees SET epass = ?, first_login = false WHERE eid = ?",
            newPassword,
            eid
        );
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found.");
        }
    }

    public void addStaff(String name, String username, String password, String role) {
        // TODO (p.1): Complete the administrator's create-doctor/technician operation.
        // Implementation: match Doctor/Technician in the schema and supply the UI's employee ID.
        int updated = jdbc.update(
            "INSERT INTO employees (ename, euser, epass, etype) VALUES (?, ?, ?, ?)",
            name,
            username,
            password,
            role
        );
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add staff.");
        }
    }
}
