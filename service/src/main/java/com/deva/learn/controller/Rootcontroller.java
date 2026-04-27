package com.deva.learn.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class Rootcontroller {

    @PostMapping("/start-scan")
    ResponseEntity<String> startTask(){
        return ResponseEntity.status(HttpStatus.OK).body("Okayy.. creating 2");
    }
}
