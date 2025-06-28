package io.github.nnegi88.errormonitor.analytics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.github.nnegi88.errormonitor.model.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorAggregatorTest {
    
    private ErrorAggregator aggregator;
    
    @BeforeEach
    void setUp() {
        aggregator = new ErrorAggregator();
    }
    
    @Test
    void testAggregate_SingleError() {
        // Given
        ErrorEvent event = ErrorEvent.builder()
                .exception(new NullPointerException("User not found"))
                .message("User not found")
                .severity(ErrorSeverity.ERROR)
                .build();
        
        // When
        aggregator.aggregate(event);
        
        // Then
        List<ErrorAnalytics.ErrorGroup> groups = aggregator.getErrorGroups();
        assertThat(groups).hasSize(1);
        
        ErrorAnalytics.ErrorGroup group = groups.get(0);
        assertThat(group.getErrorType()).isEqualTo("NullPointerException");
        assertThat(group.getPattern()).isEqualTo("User not found");
        assertThat(group.getCount()).isEqualTo(2); // Initial count is 1, plus the aggregation
    }
    
    @Test
    void testAggregate_MultipleErrorsSameType() {
        // Given
        ErrorEvent event1 = ErrorEvent.builder()
                .exception(new RuntimeException("Processing failed"))
                .message("Processing failed")
                .severity(ErrorSeverity.ERROR)
                .build();
        
        ErrorEvent event2 = ErrorEvent.builder()
                .exception(new RuntimeException("Processing failed"))
                .message("Processing failed")
                .severity(ErrorSeverity.CRITICAL)
                .build();
        
        // When
        aggregator.aggregate(event1);
        aggregator.aggregate(event2);
        
        // Then
        List<ErrorAnalytics.ErrorGroup> groups = aggregator.getErrorGroups();
        assertThat(groups).hasSize(1);
        
        ErrorAnalytics.ErrorGroup group = groups.get(0);
        assertThat(group.getCount()).isEqualTo(3); // Initial + 2 aggregations
    }
    
    @Test
    void testAggregate_DifferentErrorTypes() {
        // Given
        ErrorEvent event1 = ErrorEvent.builder()
                .exception(new NullPointerException("NPE"))
                .build();
        
        ErrorEvent event2 = ErrorEvent.builder()
                .exception(new IllegalArgumentException("Invalid arg"))
                .build();
        
        ErrorEvent event3 = ErrorEvent.builder()
                .exception(new RuntimeException("Runtime error"))
                .build();
        
        // When
        aggregator.aggregate(event1);
        aggregator.aggregate(event2);
        aggregator.aggregate(event3);
        
        // Then
        List<ErrorAnalytics.ErrorGroup> groups = aggregator.getErrorGroups();
        assertThat(groups).hasSize(3);
    }
    
    @Test
    void testAggregate_WithRequestContext() {
        // Given
        RequestContext context1 = new RequestContext();
        context1.setUrl("/api/users/1");
        
        RequestContext context2 = new RequestContext();
        context2.setUrl("/api/users/2");
        
        ErrorEvent event1 = ErrorEvent.builder()
                .exception(new RuntimeException("User error"))
                .requestContext(context1)
                .build();
        
        ErrorEvent event2 = ErrorEvent.builder()
                .exception(new RuntimeException("User error"))
                .requestContext(context2)
                .build();
        
        // When
        aggregator.aggregate(event1);
        aggregator.aggregate(event2);
        
        // Then
        List<ErrorAnalytics.ErrorGroup> groups = aggregator.getErrorGroups();
        assertThat(groups).hasSize(1);
        
        ErrorAnalytics.ErrorGroup group = groups.get(0);
        assertThat(group.getAffectedEndpoints()).containsExactlyInAnyOrder("/api/users/1", "/api/users/2");
    }
    
    @Test
    void testAggregate_NoException() {
        // Given
        ErrorEvent event = ErrorEvent.builder()
                .message("Custom error message")
                .severity(ErrorSeverity.WARNING)
                .build();
        
        // When
        aggregator.aggregate(event);
        
        // Then
        List<ErrorAnalytics.ErrorGroup> groups = aggregator.getErrorGroups();
        assertThat(groups).hasSize(1);
        
        ErrorAnalytics.ErrorGroup group = groups.get(0);
        assertThat(group.getErrorType()).isEqualTo("Unknown");
    }
    
    @Test
    void testGetTopErrorGroups() {
        // Given - Create errors with different frequencies
        for (int i = 0; i < 10; i++) {
            aggregator.aggregate(ErrorEvent.builder()
                    .exception(new RuntimeException("Most common error"))
                    .build());
        }
        
        for (int i = 0; i < 5; i++) {
            aggregator.aggregate(ErrorEvent.builder()
                    .exception(new NullPointerException("Second most common"))
                    .build());
        }
        
        for (int i = 0; i < 2; i++) {
            aggregator.aggregate(ErrorEvent.builder()
                    .exception(new IllegalArgumentException("Least common"))
                    .build());
        }
        
        // When
        List<ErrorAnalytics.ErrorGroup> topGroups = aggregator.getTopErrorGroups(2);
        
        // Then
        assertThat(topGroups).hasSize(2);
        assertThat(topGroups.get(0).getErrorType()).isEqualTo("RuntimeException");
        assertThat(topGroups.get(0).getCount()).isEqualTo(11); // Initial + 10
        assertThat(topGroups.get(1).getErrorType()).isEqualTo("NullPointerException");
        assertThat(topGroups.get(1).getCount()).isEqualTo(6); // Initial + 5
    }
    
    @Test
    void testGetTopErrorGroups_LimitExceedsGroups() {
        // Given
        aggregator.aggregate(ErrorEvent.builder()
                .exception(new RuntimeException("Error 1"))
                .build());
        
        aggregator.aggregate(ErrorEvent.builder()
                .exception(new NullPointerException("Error 2"))
                .build());
        
        // When
        List<ErrorAnalytics.ErrorGroup> topGroups = aggregator.getTopErrorGroups(10);
        
        // Then
        assertThat(topGroups).hasSize(2);
    }
    
    @Test
    void testClear() {
        // Given
        aggregator.aggregate(ErrorEvent.builder()
                .exception(new RuntimeException("Error"))
                .build());
        
        assertThat(aggregator.getErrorGroups()).isNotEmpty();
        
        // When
        aggregator.clear();
        
        // Then
        assertThat(aggregator.getErrorGroups()).isEmpty();
    }
    
    @Test
    void testSeverityDistribution() {
        // Given
        ErrorEvent criticalEvent = ErrorEvent.builder()
                .exception(new RuntimeException("Critical error"))
                .severity(ErrorSeverity.CRITICAL)
                .build();
        
        ErrorEvent errorEvent = ErrorEvent.builder()
                .exception(new RuntimeException("Critical error"))
                .severity(ErrorSeverity.ERROR)
                .build();
        
        ErrorEvent warningEvent = ErrorEvent.builder()
                .exception(new RuntimeException("Critical error"))
                .severity(ErrorSeverity.WARNING)
                .build();
        
        // When
        aggregator.aggregate(criticalEvent);
        aggregator.aggregate(errorEvent);
        aggregator.aggregate(errorEvent);
        aggregator.aggregate(warningEvent);
        
        // Then
        List<ErrorAnalytics.ErrorGroup> groups = aggregator.getErrorGroups();
        assertThat(groups).hasSize(1);
        
        ErrorAnalytics.ErrorGroup group = groups.get(0);
        assertThat(group.getSeverityDistribution())
                .containsEntry("CRITICAL", 1L)
                .containsEntry("ERROR", 2L)
                .containsEntry("WARNING", 1L);
    }
    
    @Test
    void testStackTraceGrouping() {
        // Given - Two errors with same exception type but different stack traces
        Exception exception1 = new RuntimeException("Error in method A");
        exception1.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("com.example.ServiceA", "methodA", "ServiceA.java", 10),
                new StackTraceElement("com.example.Controller", "handleRequest", "Controller.java", 20)
        });
        
        Exception exception2 = new RuntimeException("Error in method B");
        exception2.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("com.example.ServiceB", "methodB", "ServiceB.java", 15),
                new StackTraceElement("com.example.Controller", "handleRequest", "Controller.java", 20)
        });
        
        ErrorEvent event1 = ErrorEvent.builder()
                .exception(exception1)
                .build();
        
        ErrorEvent event2 = ErrorEvent.builder()
                .exception(exception2)
                .build();
        
        // When
        aggregator.aggregate(event1);
        aggregator.aggregate(event2);
        
        // Then
        List<ErrorAnalytics.ErrorGroup> groups = aggregator.getErrorGroups();
        // Should be grouped separately due to different stack traces
        assertThat(groups).hasSize(2);
    }
}