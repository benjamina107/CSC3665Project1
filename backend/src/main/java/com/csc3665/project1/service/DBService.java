package com.csc3665.project1.service;

import com.csc3665.project1.dto.LoginResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DBService {

    // TODO: Replace JdbcTemplate with JDBC Connection/PreparedStatement/ResultSet;
    // the project requirements explicitly disallow JdbcTemplates and Spring Data JPA.
    // TODO: Schema setup uses employees (not users), including the added first_login column.
    // Seed admin/admin with is_admin=true and first_login=true; etype must still satisfy
    // the Doctor/Technician CHECK constraint (document the admin's etype assumption).

    // TODO: Staff queries: SELECT eid, ename, euser, etype FROM employees WHERE is_admin=false.
    // Remove staff in a transaction: remove working rows and requests for eid before
    // employees; deleting requests cascades to pto_requests/work_requests. Check/rebuild
    // future coverage after removal and report if a valid schedule cannot be produced.
    // TODO: Admin password-setting query: UPDATE employees SET epass=? WHERE eid=?;
    // decide/document whether resetting staff passwords also sets first_login=true.

    // TODO: Generate/persist schedules in shifts + working, using employees for staffing
    // and approved requests + pto_requests/work_requests for availability/preferences.
    // Fix the surgery shift identity first (see TODO.txt); use exact schema block values.
    // Scheduling rules from the assignment:
    // - Typically four shifts per employee/week; at least 10 hours between shifts,
    //   including across week boundaries and overnight shifts ending the next day.
    // - Doctors: at most one surgery day and one overnight/week.
    // - Mon-Sat: cover Early Morning 07:30-18:30, 8:30 Shift 08:30-19:30,
    //   Late Doctor 09:30-20:30, surgery 07:30-18:30, and Overnight 20:00-08:00.
    // - Sunday: Sunday Shift 08:00-20:00 plus Overnight 20:00-08:00.
    // - At least one doctor per shift; exactly one on overnight and Sunday shifts.
    //   Prioritize extra doctors Saturday Early Morning and Friday Late Doctor,
    //   then remaining extra assignments on Early Morning shifts.
    // - At least one technician per doctor; exactly one technician overnight and
    //   two on Sunday daytime; distribute remaining technician work evenly.
    // TODO: Read the next two weeks by joining working -> shifts on the full shift key
    // and employees on eid. Join other working rows on the same shift key to return
    // opposite-role coworkers; include dates, block, surgery, hours, and days off.

    // TODO: PTO usage query: requests JOIN pto_requests USING(rid), filtered by eid,
    // approved=true and calendar year. Allow eight vacation and four sick days/year;
    // count overlapping days once and split cross-year ranges into their respective years.
    // Decide/document whether employees.vacation_days/sick_days represent entitlement
    // or remaining balance; avoid subtracting usage twice or carrying it into next year.
    // TODO: Request history: requests LEFT JOIN pto_requests/work_requests USING(rid),
    // scoped to the logged-in eid and ordered by request_timestamp, rid.
    // TODO: Submit PTO/work requests in a transaction: INSERT requests with server-side
    // timestamp RETURNING rid; INSERT pto_requests(reason='Sick'/'Vacation') or
    // work_requests(sdate, block), using the corrected shift key for work requests.
    // Validate date ranges, PTO balance, vacation notice >=21 days, and no sick day
    // for today (the requirement allows any other day; do not invent tomorrow-only rules).
    // Process first-come, first-served with serialization/locking so concurrent requests
    // cannot overdraw balances or approve conflicting schedules. Preserve earlier granted
    // requests and all coverage/rest/weekly rules when attempting to rearrange working.
    // Immediately return granted/denied and a specific denial reason; persist the decision
    // and commit schedule/balance changes only when granted. See TODO.txt for reason storage.

    private final JdbcTemplate jdbc;

    public DBService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LoginResponse login(String username, String password) {
        // TODO: SELECT is_admin too: map true to frontend role 'admin', otherwise map
        // Doctor/Technician to 'doctor'/'technician'; never use etype='admin'.
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
        // TODO: Require an authenticated account and update its own eid; validate the
        // password server-side and prevent bypassing the admin first-login requirement.
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
        // TODO: Validate fields and unique euser; map role to Doctor/Technician, insert
        // is_admin=false and an explicit first_login policy, and RETURNING eid to send
        // the created staff record back to the dashboard (never include epass).
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
