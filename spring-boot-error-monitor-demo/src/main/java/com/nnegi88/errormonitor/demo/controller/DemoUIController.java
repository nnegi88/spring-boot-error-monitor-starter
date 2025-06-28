package com.nnegi88.errormonitor.demo.controller;

import com.nnegi88.errormonitor.demo.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DemoUIController {
    
    private final ScheduledTaskService scheduledTaskService;
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("taskExecutionCount", scheduledTaskService.getTaskExecutionCount());
        return "index";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}