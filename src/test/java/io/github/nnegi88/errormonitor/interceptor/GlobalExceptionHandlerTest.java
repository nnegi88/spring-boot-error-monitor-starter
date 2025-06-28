package io.github.nnegi88.errormonitor.interceptor;

import io.github.nnegi88.errormonitor.core.ErrorProcessor;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.RequestContext;
import io.github.nnegi88.errormonitor.util.RequestContextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private ErrorProcessor errorProcessor;
    
    @Mock
    private RequestContextExtractor requestContextExtractor;
    
    @Mock
    private HttpServletRequest httpServletRequest;
    
    @Mock
    private WebRequest webRequest;
    
    private GlobalExceptionHandler globalExceptionHandler;
    
    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler(errorProcessor, requestContextExtractor);
    }
    
    @Test
    void testHandleAllExceptions() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        RequestContext requestContext = new RequestContext();
        requestContext.setUrl("/api/test");
        requestContext.setHttpMethod("GET");
        requestContext.setClientIp("127.0.0.1");
        
        when(requestContextExtractor.extractHttpServletRequest(webRequest))
            .thenReturn(Optional.of(httpServletRequest));
        when(requestContextExtractor.extractContext(httpServletRequest))
            .thenReturn(requestContext);
        
        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleAllExceptions(exception, webRequest);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        
        // Verify error processing was called
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(errorProcessor).processError(eventCaptor.capture());
        
        ErrorEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getException()).isEqualTo(exception);
        assertThat(capturedEvent.getMessage()).isEqualTo("Test error");
        assertThat(capturedEvent.getRequestContext()).isNotNull();
        assertThat(capturedEvent.getRequestContext().getUrl()).isEqualTo("/api/test");
    }
    
    @Test
    void testHandleAllExceptionsWithoutRequest() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        
        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleAllExceptions(exception, null);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        // Verify error processing was called
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(errorProcessor).processError(eventCaptor.capture());
        
        ErrorEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getException()).isEqualTo(exception);
        assertThat(capturedEvent.getRequestContext()).isNull();
    }
    
    @Test
    void testHandleAllExceptionsNoHttpServletRequest() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        when(requestContextExtractor.extractHttpServletRequest(webRequest))
            .thenReturn(Optional.empty());
        
        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleAllExceptions(exception, webRequest);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        // Verify error processing was called
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(errorProcessor).processError(eventCaptor.capture());
        
        ErrorEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getException()).isEqualTo(exception);
        assertThat(capturedEvent.getRequestContext()).isNull();
    }
    
    @Test
    void testHandleAllExceptionsWithCustomStatus() {
        // Given
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        class BadRequestException extends RuntimeException {
            public BadRequestException(String message) {
                super(message);
            }
        }
        
        BadRequestException exception = new BadRequestException("Bad request");
        
        when(requestContextExtractor.extractHttpServletRequest(webRequest))
            .thenReturn(Optional.empty());
        
        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleAllExceptions(exception, webRequest);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    void testErrorProcessingFailure() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        doThrow(new RuntimeException("Processing failed")).when(errorProcessor).processError(any());
        
        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleAllExceptions(exception, webRequest);
        
        // Then - Should still return response even if processing fails
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }
    
    @Test
    void testRequestContextExtraction() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        RequestContext requestContext = new RequestContext();
        requestContext.setUrl("/api/users/123");
        requestContext.setHttpMethod("POST");
        requestContext.setClientIp("192.168.1.100");
        requestContext.setUserAgent("Mozilla/5.0");
        requestContext.setReferer("https://example.com");
        
        when(requestContextExtractor.extractHttpServletRequest(webRequest))
            .thenReturn(Optional.of(httpServletRequest));
        when(requestContextExtractor.extractContext(httpServletRequest))
            .thenReturn(requestContext);
        
        // When
        globalExceptionHandler.handleAllExceptions(exception, webRequest);
        
        // Then
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(errorProcessor).processError(eventCaptor.capture());
        
        ErrorEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getRequestContext()).isEqualTo(requestContext);
        assertThat(capturedEvent.getRequestContext().getUrl()).isEqualTo("/api/users/123");
        assertThat(capturedEvent.getRequestContext().getHttpMethod()).isEqualTo("POST");
        assertThat(capturedEvent.getRequestContext().getClientIp()).isEqualTo("192.168.1.100");
        assertThat(capturedEvent.getRequestContext().getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(capturedEvent.getRequestContext().getReferer()).isEqualTo("https://example.com");
    }
}