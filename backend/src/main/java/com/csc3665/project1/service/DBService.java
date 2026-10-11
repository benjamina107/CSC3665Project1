package com.csc3665.project1.service;

import com.csc3665.project1.dto.LoginResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DBService {

    private final JdbcTemplate jdbc;

    public DBService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LoginResponse login(String username, String password) {
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
}
