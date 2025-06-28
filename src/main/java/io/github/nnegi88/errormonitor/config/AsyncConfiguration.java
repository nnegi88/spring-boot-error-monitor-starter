package io.github.nnegi88.errormonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfiguration {
    // This enables async processing for error notifications
}