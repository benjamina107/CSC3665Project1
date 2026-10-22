package com.csc3665.project1.controller;
import com.csc3665.project1.service.DBService;
import com.csc3665.project1.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;




@RestController
@RequestMapping("/api")
public class ApiController {
    // TODO (pp.1-2): Accept work/PTO requests, return immediate decisions/reasons,
    // and provide annual vacation/sick usage.
    // Implementation note: API paths/DTOs are up to this project; match frontend fetch calls.

    private final DBService dbService;

    public ApiController(DBService dbService) {
        this.dbService = dbService;
    }


    @GetMapping("/schedule")
    public ScheduleResponse getSchedule(@RequestParam int eid) {
        return dbService.getSchedule(eid);
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
    
    @PostMapping("/admin/staff")
    public StaffResponse addStaff(@RequestBody StaffRequest request) {
        int id = dbService.addStaff(request.name(), request.username(), request.password(), request.role());
        return new StaffResponse(id, request.name(), request.username(), request.role());
    }

    @GetMapping("/admin/staff")
    public List<StaffResponse> getStaff() {
        return dbService.getStaff();
    }

    @PostMapping("/admin/schedule/generate")
    public void generateSchedule() {
        dbService.generateSchedule();
    }

    @DeleteMapping("/admin/staff/{eid}")
    public void removeStaff(@PathVariable int eid) {
        dbService.removeStaff(eid);
    }

    public record SetPasswordRequest(String newPassword) {}

    @PutMapping("/admin/staff/{eid}/password")
    public void setStaffPassword(
        @PathVariable int eid,
        @RequestBody SetPasswordRequest request
    ) {
        dbService.setStaffPassword(eid, request.newPassword());
    }

    @GetMapping("/requests/my")
    public PTOResponse getPTO(@RequestParam int eid) {
        return dbService.getPTO(eid);
    }

    @PostMapping("/requests/pto")
    public String addPTO(@RequestBody PTORequest request){
        return dbService.addPTO(
            request.eid(),
            request.reason(),
            request.startDate(),
            request.endDate()
        );
    }

     @PostMapping("/requests/work")
    public String addWorkRequest(@RequestBody WorkRequest request){
        return dbService.addWorkRequest(
            request.eid(),
            request.sdate(),
            request.block()
        );
    }
}



/*
Common HTTP method annotations:
- @GetMapping — read data
- @PostMapping — create data
- @PutMapping — update data
- @DeleteMapping — delete data
*/
