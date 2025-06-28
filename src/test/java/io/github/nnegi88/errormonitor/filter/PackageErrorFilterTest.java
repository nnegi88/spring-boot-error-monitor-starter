package io.github.nnegi88.errormonitor.filter;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageErrorFilterTest {

    private PackageErrorFilter packageErrorFilter;
    private ErrorMonitorProperties.FilteringProperties filteringProperties;
    
    @BeforeEach
    void setUp() {
        filteringProperties = new ErrorMonitorProperties.FilteringProperties();
    }
    
    @Test
    void testEnabledPackages() {
        filteringProperties.setEnabledPackages(Arrays.asList("com.example", "com.myapp"));
        // Clear the default minimum severity
        filteringProperties.setMinimumSeverity(null);
        packageErrorFilter = new PackageErrorFilter(filteringProperties);
        
        // Create an exception with stack trace from enabled package
        Exception exceptionFromEnabledPackage = new RuntimeException("Test error");
        StackTraceElement[] stackTrace = new StackTraceElement[] {
            new StackTraceElement("com.example.TestClass", "testMethod", "TestClass.java", 10),
            new StackTraceElement("com.myapp.AppClass", "appMethod", "AppClass.java", 20)
        };
        exceptionFromEnabledPackage.setStackTrace(stackTrace);
        
        // Should report errors from enabled packages
        assertThat(packageErrorFilter.shouldReport(createErrorEvent(exceptionFromEnabledPackage))).isTrue();
        
        // Create an exception with stack trace from other package
        Exception exceptionFromOtherPackage = new RuntimeException("Test error");
        StackTraceElement[] otherStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.other.OtherClass", "otherMethod", "OtherClass.java", 10)
        };
        exceptionFromOtherPackage.setStackTrace(otherStackTrace);
        
        // Should not report errors from other packages
        assertThat(packageErrorFilter.shouldReport(createErrorEvent(exceptionFromOtherPackage))).isFalse();
    }
    
    @Test
    void testExcludedExceptions() {
        filteringProperties.setExcludedExceptions(Arrays.asList(
            "java.lang.IllegalArgumentException",
            "java.lang.IllegalStateException"
        ));
        // Clear the default minimum severity
        filteringProperties.setMinimumSeverity(null);
        packageErrorFilter = new PackageErrorFilter(filteringProperties);
        
        // Should not report excluded exceptions
        assertThat(packageErrorFilter.shouldReport(createErrorEvent(new IllegalArgumentException()))).isFalse();
        assertThat(packageErrorFilter.shouldReport(createErrorEvent(new IllegalStateException()))).isFalse();
        
        // Should report other exceptions when no enabled packages are configured
        assertThat(packageErrorFilter.shouldReport(createErrorEvent(new RuntimeException()))).isTrue();
    }
    
    @Test
    void testMinimumSeverity() {
        filteringProperties.setMinimumSeverity("HIGH");
        packageErrorFilter = new PackageErrorFilter(filteringProperties);
        
        // Should report HIGH and CRITICAL severity
        assertThat(packageErrorFilter.shouldReport(createErrorEventWithSeverity(ErrorSeverity.HIGH))).isTrue();
        assertThat(packageErrorFilter.shouldReport(createErrorEventWithSeverity(ErrorSeverity.CRITICAL))).isTrue();
        
        // Should not report LOW and MEDIUM severity
        assertThat(packageErrorFilter.shouldReport(createErrorEventWithSeverity(ErrorSeverity.LOW))).isFalse();
        assertThat(packageErrorFilter.shouldReport(createErrorEventWithSeverity(ErrorSeverity.MEDIUM))).isFalse();
    }
    
    @Test
    void testCombinedFilters() {
        filteringProperties.setEnabledPackages(Arrays.asList("com.example"));
        filteringProperties.setExcludedExceptions(Arrays.asList("java.lang.IllegalStateException"));
        filteringProperties.setMinimumSeverity("MEDIUM");
        packageErrorFilter = new PackageErrorFilter(filteringProperties);
        
        // Create an exception from enabled package with HIGH severity
        Exception importantException = new RuntimeException("Important error");
        importantException.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.example.ImportantClass", "method", "ImportantClass.java", 10)
        });
        ErrorEvent event1 = createErrorEvent(importantException);
        event1.setSeverity(ErrorSeverity.HIGH);
        assertThat(packageErrorFilter.shouldReport(event1)).isTrue();
        
        // Should fail on excluded exception even if from enabled package
        Exception excludedException = new IllegalStateException("Excluded error");
        excludedException.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.example.SomeClass", "method", "SomeClass.java", 10)
        });
        ErrorEvent event2 = createErrorEvent(excludedException);
        event2.setSeverity(ErrorSeverity.HIGH);
        assertThat(packageErrorFilter.shouldReport(event2)).isFalse();
        
        // Should fail on severity even if from enabled package
        Exception lowSeverityException = new RuntimeException("Low severity");
        lowSeverityException.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.example.TestClass", "method", "TestClass.java", 10)
        });
        ErrorEvent event3 = createErrorEvent(lowSeverityException);
        event3.setSeverity(ErrorSeverity.LOW);
        assertThat(packageErrorFilter.shouldReport(event3)).isFalse();
        
        // Should fail on package
        Exception otherPackageException = new RuntimeException("Other package");
        otherPackageException.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("org.other.OtherClass", "method", "OtherClass.java", 10)
        });
        ErrorEvent event4 = createErrorEvent(otherPackageException);
        event4.setSeverity(ErrorSeverity.HIGH);
        assertThat(packageErrorFilter.shouldReport(event4)).isFalse();
    }
    
    @Test
    void testNoFiltersConfigured() {
        // Clear the default minimum severity
        filteringProperties.setMinimumSeverity(null);
        packageErrorFilter = new PackageErrorFilter(filteringProperties);
        
        // Should report all events when no filters are configured
        assertThat(packageErrorFilter.shouldReport(createErrorEvent(new RuntimeException()))).isTrue();
        assertThat(packageErrorFilter.shouldReport(createErrorEventWithSeverity(ErrorSeverity.LOW))).isTrue();
    }
    
    @Test
    void testNullException() {
        packageErrorFilter = new PackageErrorFilter(filteringProperties);
        
        ErrorEvent event = new ErrorEvent();
        event.setApplicationName("test-app");
        
        // Should report events without exception
        assertThat(packageErrorFilter.shouldReport(event)).isTrue();
    }
    
    private ErrorEvent createErrorEvent(Exception exception) {
        ErrorEvent event = new ErrorEvent();
        event.setApplicationName("test-app");
        event.setEnvironment("test");
        event.setException(exception);
        event.setSeverity(ErrorSeverity.MEDIUM);
        return event;
    }
    
    private ErrorEvent createErrorEventWithSeverity(ErrorSeverity severity) {
        ErrorEvent event = createErrorEvent(new RuntimeException());
        event.setSeverity(severity);
        return event;
    }
    
}