package com.csc3665.project1.controller;

import org.springframework.web.bind.annotation.GetMapping;
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
}



/*
Common HTTP method annotations:
- @GetMapping — read data
- @PostMapping — create data
- @PutMapping — update data
- @DeleteMapping — delete data
*/