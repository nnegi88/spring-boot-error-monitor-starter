package io.github.nnegi88.errormonitor.filter;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DefaultErrorFilter implements ErrorFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorFilter.class);
    
    private final ErrorMonitorProperties properties;
    
    public DefaultErrorFilter(ErrorMonitorProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public boolean shouldReport(ErrorEvent event) {
        // Check excluded exceptions
        if (isExcludedException(event)) {
            logger.debug("Exception excluded by configuration: {}", event.getException());
            return false;
        }
        
        // Check enabled packages
        if (!isFromEnabledPackage(event)) {
            logger.debug("Exception not from enabled package: {}", event.getException());
            return false;
        }
        
        // Check severity
        if (!meetsSeverityThreshold(event)) {
            logger.debug("Event does not meet severity threshold: {}", event.getSeverity());
            return false;
        }
        
        return true;
    }
    
    private boolean isExcludedException(ErrorEvent event) {
        if (event.getException() == null) {
            return false;
        }
        
        List<String> excludedExceptions = properties.getFiltering().getExcludedExceptions();
        if (excludedExceptions == null || excludedExceptions.isEmpty()) {
            return false;
        }
        
        String exceptionClassName = event.getException().getClass().getName();
        return excludedExceptions.contains(exceptionClassName);
    }
    
    private boolean isFromEnabledPackage(ErrorEvent event) {
        List<String> enabledPackages = properties.getFiltering().getEnabledPackages();
        if (enabledPackages == null || enabledPackages.isEmpty()) {
            // If no packages specified, allow all
            return true;
        }
        
        if (event.getException() == null) {
            return true;
        }
        
        StackTraceElement[] stackTrace = event.getException().getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return true;
        }
        
        // Check if any stack trace element is from an enabled package
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            for (String enabledPackage : enabledPackages) {
                if (className.startsWith(enabledPackage)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean meetsSeverityThreshold(ErrorEvent event) {
        String minimumSeverityStr = properties.getFiltering().getMinimumSeverity();
        try {
            ErrorSeverity minimumSeverity = ErrorSeverity.valueOf(minimumSeverityStr.toUpperCase());
            return event.getSeverity().ordinal() >= minimumSeverity.ordinal();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid minimum severity configuration: {}", minimumSeverityStr);
            return true;
        }
    }
}