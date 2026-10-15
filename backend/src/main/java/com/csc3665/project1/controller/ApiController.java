package com.csc3665.project1.controller;
import service.DBService;
import dto.LoginResponse;
import dto.StaffRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;





@RestController
@RequestMapping("/api")
public class ApiController {
    // Demo uses frontend setUser for login state; pass user.id for schedule/PTO queries.
    // Implementation fix: imports must use com.csc3665.project1.service/dto to compile.
    // TODO (p.1): Complete admin staff creation/removal/password setting.
    // TODO (p.1): Provide each employee's next two weeks and coworkers.
    // TODO (pp.1-2): Accept work/PTO requests, return immediate decisions/reasons,
    // and provide annual vacation/sick usage.
    // Implementation note: API paths/DTOs are up to this project; match frontend fetch calls.

    private final DBService dbService;

    public ApiController(DBService dbService) {
        this.dbService = dbService;
    }


    @GetMapping("/auth/login")
    public LoginResponse login(String user, String pass) {
        return dbService.login(user, pass);
    }

    public record ChangePasswordRequest(int eid, String newPassword) {}

    @PostMapping("/auth/changePassword")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        dbService.changePassword(request.eid(), request.newPassword());
    }

    // Implementation note: the dashboard needs the created employee ID; it can be returned
    // here or obtained by reloading staff. The assignment does not prescribe a response DTO.
    @PostMapping("/admin/staff")
    public StaffRequest addStaff(@RequestBody StaffRequest request) {
        dbService.addStaff(request.name(), request.username(), request.password(), request.role());
        return request;
    }
}



/*
Common HTTP method annotations:
- @GetMapping — read data
- @PostMapping — create data
- @PutMapping — update data
- @DeleteMapping — delete data
*/
