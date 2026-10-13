package com.csc3665.project1.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;





@RestController
@RequestMapping("/api")
public class ApiController {

    private final DBService dbService;

    public ApiController(DBService dbService) {
        this.dbService = dbService;
    }


    @GetMapping("/auth/login")
    public boolean login(String user, String pass) {
        return dbService.login(user, pass);
    }

    public record ChangePasswordRequest(int eid, String newPassword) {}

    @PostMapping("/auth/changePassword")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        dbService.changePassword(request.eid(), request.newPassword());
    }

    @PostMapping("/admin/staff")
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