package com.nnegi88.errormonitor.benchmark.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    @GetMapping("/normal")
    public ResponseEntity<String> normalRequest() {
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/error")
    public ResponseEntity<String> errorRequest() {
        throw new RuntimeException("Benchmark test error");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}