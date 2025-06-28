package io.github.nnegi88.errormonitor.interceptor;

import io.github.nnegi88.errormonitor.core.ErrorProcessor;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.RequestContext;
import io.github.nnegi88.errormonitor.util.RequestContextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "spring.error-monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ErrorMonitorFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorMonitorFilter.class);
    
    private final ErrorProcessor errorProcessor;
    private final RequestContextExtractor requestContextExtractor;
    
    public ErrorMonitorFilter(ErrorProcessor errorProcessor, RequestContextExtractor requestContextExtractor) {
        this.errorProcessor = errorProcessor;
        this.requestContextExtractor = requestContextExtractor;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            filterChain.doFilter(request, response);
            
            // Check for HTTP error status codes
            if (response.getStatus() >= 400) {
                handleHttpError(request, response);
            }
        } catch (Exception ex) {
            // This will catch any unhandled exceptions during request processing
            handleException(request, response, ex);
            throw ex;
        }
    }
    
    private void handleHttpError(HttpServletRequest request, HttpServletResponse response) {
        try {
            String message = String.format("HTTP Error %d for %s %s", 
                response.getStatus(), 
                request.getMethod(), 
                request.getRequestURI());
            
            ErrorEvent errorEvent = new ErrorEvent();
            errorEvent.setMessage(message);
            
            RequestContext requestContext = requestContextExtractor.extractContext(request);
            requestContext.setStatusCode(response.getStatus());
            errorEvent.setRequestContext(requestContext);
            
            // Only process 5xx errors by default (server errors)
            if (response.getStatus() >= 500) {
                errorProcessor.processError(errorEvent);
            }
        } catch (Exception ex) {
            logger.error("Error while processing HTTP error", ex);
        }
    }
    
    private void handleException(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        try {
            ErrorEvent errorEvent = new ErrorEvent(ex);
            RequestContext requestContext = requestContextExtractor.extractContext(request);
            errorEvent.setRequestContext(requestContext);
            
            errorProcessor.processError(errorEvent);
        } catch (Exception processingEx) {
            logger.error("Error while processing exception", processingEx);
        }
    }
}