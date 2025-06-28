package io.github.nnegi88.errormonitor.filter;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PackageErrorFilter implements ErrorFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(PackageErrorFilter.class);
    
    private final ErrorMonitorProperties.FilteringProperties filteringProperties;
    
    public PackageErrorFilter(ErrorMonitorProperties.FilteringProperties filteringProperties) {
        this.filteringProperties = filteringProperties;
    }
    
    @Override
    public boolean shouldReport(ErrorEvent event) {
        // Check enabled packages
        if (!isFromEnabledPackage(event)) {
            logger.debug("Error event filtered out - not from enabled package: {}", 
                event.getException() != null ? event.getException().getClass().getName() : "null");
            return false;
        }
        
        // Check excluded exceptions
        if (isExcludedException(event)) {
            logger.debug("Error event filtered out - excluded exception type: {}", 
                event.getException() != null ? event.getException().getClass().getName() : "null");
            return false;
        }
        
        // Check minimum severity
        if (!meetsSeverityThreshold(event)) {
            logger.debug("Error event filtered out - below severity threshold: {}", event.getSeverity());
            return false;
        }
        
        return true;
    }
    
    private boolean isFromEnabledPackage(ErrorEvent event) {
        List<String> enabledPackages = filteringProperties.getEnabledPackages();
        
        // If no packages configured, allow all
        if (enabledPackages == null || enabledPackages.isEmpty()) {
            return true;
        }
        
        // If no exception, allow it
        if (event.getException() == null) {
            return true;
        }
        
        String exceptionClassName = event.getException().getClass().getName();
        
        // Check if exception is from any enabled package
        for (String packagePrefix : enabledPackages) {
            if (exceptionClassName.startsWith(packagePrefix)) {
                return true;
            }
        }
        
        // Also check stack trace for enabled packages
        StackTraceElement[] stackTrace = event.getException().getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                for (String packagePrefix : enabledPackages) {
                    if (className.startsWith(packagePrefix)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean isExcludedException(ErrorEvent event) {
        List<String> excludedExceptions = filteringProperties.getExcludedExceptions();
        
        // If no exclusions configured, don't exclude anything
        if (excludedExceptions == null || excludedExceptions.isEmpty()) {
            return false;
        }
        
        // If no exception, don't exclude
        if (event.getException() == null) {
            return false;
        }
        
        String exceptionClassName = event.getException().getClass().getName();
        
        // Check if exception type is in exclusion list
        return excludedExceptions.contains(exceptionClassName);
    }
    
    private boolean meetsSeverityThreshold(ErrorEvent event) {
        String minimumSeverityStr = filteringProperties.getMinimumSeverity();
        
        // If no minimum severity configured, allow all
        if (minimumSeverityStr == null || minimumSeverityStr.isEmpty()) {
            return true;
        }
        
        // If event has no severity, allow it
        if (event.getSeverity() == null) {
            return true;
        }
        
        try {
            ErrorSeverity minimumSeverity = ErrorSeverity.valueOf(minimumSeverityStr.toUpperCase());
            return event.getSeverity().ordinal() >= minimumSeverity.ordinal();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid minimum severity configured: {}", minimumSeverityStr);
            return true;
        }
    }
}