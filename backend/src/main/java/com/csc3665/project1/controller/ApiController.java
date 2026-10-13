package com.csc3665.project1.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;





@RestController
@RequestMapping("/api")
public class ApiController {

    // TODO: Import service.DBService and dto.LoginResponse; define/import StaffRequest
    // and reuse dto.ChangePasswordRequest instead of duplicating it inside this class.
    // TODO: Establish server-side authentication; restrict staff create/remove/password
    // actions to employees.is_admin=true, and scope schedule/request APIs to the caller.
    // TODO: Add GET /admin/staff, DELETE /admin/staff/{eid}, and
    // POST /admin/staff/{eid}/reset-password accepting a password to set.
    // TODO: Add GET /schedule for the logged-in employee's next two weeks.
    // Add POST /requests for PTO and work preferences; return an immediate decision/reason.
    // Add GET /requests/my for history and current-calendar-year PTO usage/allowance.
    // Endpoint names are proposed contracts; keep the frontend and DTOs consistent.

    private final DBService dbService;

    public ApiController(DBService dbService) {
        this.dbService = dbService;
    }


    @GetMapping("/auth/login")
    // TODO: Return LoginResponse rather than boolean (DBService already returns it);
    // normalize roles using is_admin, and expose first_login as isFirstLogin.
    public boolean login(String user, String pass) {
        return dbService.login(user, pass);
    }

    public record ChangePasswordRequest(int eid, String newPassword) {}

    @PostMapping("/auth/changePassword")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        dbService.changePassword(request.eid(), request.newPassword());
    }

    @PostMapping("/admin/staff")
    // TODO: Return the new staff DTO rather than void to match the frontend JSON read.
    public void addStaff(@RequestBody StaffRequest request) {
        dbService.addStaff(request.name(), request.username(), request.password(), request.role());
    }
}



/*
Common HTTP method annotations:
- @GetMapping — read data
- @PostMapping — create data
- @PutMapping — update data
- @DeleteMapping — delete data
*/
