package io.github.nnegi88.errormonitor.interceptor;

import io.github.nnegi88.errormonitor.core.ErrorProcessor;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.RequestContext;
import io.github.nnegi88.errormonitor.util.RequestContextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@ControllerAdvice
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "spring.error-monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final ErrorProcessor errorProcessor;
    private final RequestContextExtractor requestContextExtractor;
    
    public GlobalExceptionHandler(ErrorProcessor errorProcessor, RequestContextExtractor requestContextExtractor) {
        this.errorProcessor = errorProcessor;
        this.requestContextExtractor = requestContextExtractor;
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        logger.error("Unhandled exception caught", ex);
        
        ErrorEvent errorEvent = new ErrorEvent(ex);
        
        // Extract request context if available
        if (request != null) {
            Optional<HttpServletRequest> httpRequest = requestContextExtractor.extractHttpServletRequest(request);
            httpRequest.ifPresent(req -> {
                RequestContext requestContext = requestContextExtractor.extractContext(req);
                errorEvent.setRequestContext(requestContext);
            });
        }
        
        // Determine status code
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ResponseStatus responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        if (responseStatus != null) {
            status = responseStatus.value();
        }
        
        // Process the error asynchronously
        try {
            errorProcessor.processError(errorEvent);
        } catch (Exception processingEx) {
            logger.error("Error while processing error event", processingEx);
        }
        
        // Return appropriate response
        return new ResponseEntity<>(createErrorResponse(ex, status), status);
    }
    
    private Object createErrorResponse(Exception ex, HttpStatus status) {
        return new ErrorResponse(
            status.value(),
            status.getReasonPhrase(),
            ex.getMessage(),
            System.currentTimeMillis()
        );
    }
    
    static class ErrorResponse {
        private final int status;
        private final String error;
        private final String message;
        private final long timestamp;
        
        public ErrorResponse(int status, String error, String message, long timestamp) {
            this.status = status;
            this.error = error;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public int getStatus() {
            return status;
        }
        
        public String getError() {
            return error;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}