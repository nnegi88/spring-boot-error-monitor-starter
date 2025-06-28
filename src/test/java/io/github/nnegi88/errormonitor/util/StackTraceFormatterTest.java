package io.github.nnegi88.errormonitor.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StackTraceFormatterTest {

    @Test
    void testFormatWithNullThrowable() {
        String result = StackTraceFormatter.format(null, 10);
        assertThat(result).isEmpty();
    }
    
    @Test
    void testFormatWithShortStackTrace() {
        Exception exception = new RuntimeException("Test error");
        exception.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.example.Service", "method1", "Service.java", 10),
            new StackTraceElement("com.example.Controller", "method2", "Controller.java", 20)
        });
        
        String result = StackTraceFormatter.format(exception, 10);
        
        assertThat(result).contains("RuntimeException: Test error");
        assertThat(result).contains("com.example.Service.method1(Service.java:10)");
        assertThat(result).contains("com.example.Controller.method2(Controller.java:20)");
        assertThat(result).doesNotContain("more lines truncated");
    }
    
    @Test
    void testFormatWithLongStackTrace() {
        Exception exception = new RuntimeException("Test error");
        StackTraceElement[] stackTrace = new StackTraceElement[20];
        for (int i = 0; i < 20; i++) {
            stackTrace[i] = new StackTraceElement(
                "com.example.Class" + i, 
                "method" + i, 
                "Class" + i + ".java", 
                i * 10
            );
        }
        exception.setStackTrace(stackTrace);
        
        String result = StackTraceFormatter.format(exception, 5);
        
        assertThat(result).contains("RuntimeException: Test error");
        assertThat(result).contains("com.example.Class0.method0(Class0.java:0)");
        assertThat(result).contains("com.example.Class1.method1(Class1.java:10)");
        assertThat(result).contains("... 16 more lines truncated");
        
        // Should not contain lines beyond the limit
        assertThat(result).doesNotContain("com.example.Class5");
    }
    
    @Test
    void testFormatWithNestedCause() {
        Exception cause = new IllegalArgumentException("Root cause");
        cause.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.example.Validator", "validate", "Validator.java", 15)
        });
        
        Exception exception = new RuntimeException("Wrapper error", cause);
        exception.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.example.Service", "process", "Service.java", 42)
        });
        
        String result = StackTraceFormatter.format(exception, 10);
        
        assertThat(result).contains("RuntimeException: Wrapper error");
        assertThat(result).contains("com.example.Service.process(Service.java:42)");
        assertThat(result).contains("Caused by: java.lang.IllegalArgumentException: Root cause");
        assertThat(result).contains("com.example.Validator.validate(Validator.java:15)");
    }
    
    @Test
    void testFormatWithExactLimit() {
        Exception exception = new RuntimeException("Test error");
        exception.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.example.A", "methodA", "A.java", 1),
            new StackTraceElement("com.example.B", "methodB", "B.java", 2),
            new StackTraceElement("com.example.C", "methodC", "C.java", 3)
        });
        
        // Limit exactly matches number of lines
        String result = StackTraceFormatter.format(exception, 4); // 1 for exception + 3 for stack
        
        assertThat(result).contains("RuntimeException: Test error");
        assertThat(result).contains("com.example.A.methodA(A.java:1)");
        assertThat(result).contains("com.example.B.methodB(B.java:2)");
        assertThat(result).contains("com.example.C.methodC(C.java:3)");
        assertThat(result).doesNotContain("more lines truncated");
    }
    
    @Test
    void testFormatWithZeroLimit() {
        Exception exception = new RuntimeException("Test error");
        exception.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.example.Service", "method", "Service.java", 10)
        });
        
        String result = StackTraceFormatter.format(exception, 0);
        
        // Should still show something even with 0 limit
        assertThat(result).contains("more lines truncated");
    }
    
    @Test
    void testFormatPreservesOriginalFormatting() {
        Exception exception = new RuntimeException("Multi\nline\nerror message");
        exception.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.example.Service", "method", "Service.java", 10)
        });
        
        String result = StackTraceFormatter.format(exception, 10);
        
        assertThat(result).contains("Multi\nline\nerror message");
    }
}