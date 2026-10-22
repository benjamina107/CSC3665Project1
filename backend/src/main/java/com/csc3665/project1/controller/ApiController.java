package com.csc3665.project1.controller;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.csc3665.project1.dto.LoginResponse;
import com.csc3665.project1.dto.PTODecision;
import com.csc3665.project1.dto.PTORequest;
import com.csc3665.project1.dto.PTOResponse;
import com.csc3665.project1.dto.ScheduleResponse;
import com.csc3665.project1.dto.StaffRequest;
import com.csc3665.project1.dto.StaffResponse;
import com.csc3665.project1.dto.WorkRequest;
import com.csc3665.project1.service.DBService;




@RestController
@RequestMapping("/api")
public class ApiController {
    // Done: /requests/pto returns an immediate PTODecision; /requests/my returns annual usage.
    // TODO (pp.1-2): /requests/work still returns a plain "awaiting approval" string; make it
    // return a granted/denied decision with a reason, like /requests/pto.
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
    public PTODecision addPTO(@RequestBody PTORequest request){
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
