package io.github.nnegi88.errormonitor.filter;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RateLimitingErrorFilter implements ErrorFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingErrorFilter.class);
    
    private final ErrorMonitorProperties.RateLimitingProperties rateLimitingProperties;
    private final ConcurrentLinkedQueue<Instant> errorTimestamps = new ConcurrentLinkedQueue<>();
    private final AtomicInteger burstCounter = new AtomicInteger(0);
    private final AtomicReference<Instant> burstWindowStart = new AtomicReference<>();
    
    public RateLimitingErrorFilter(ErrorMonitorProperties.RateLimitingProperties rateLimitingProperties) {
        this.rateLimitingProperties = rateLimitingProperties;
    }
    
    @Override
    public boolean shouldReport(ErrorEvent event) {
        Instant now = Instant.now();
        int maxErrorsPerMinute = rateLimitingProperties.getMaxErrorsPerMinute();
        int burstLimit = rateLimitingProperties.getBurstLimit();
        
        // Check if rate limiting is disabled
        if (maxErrorsPerMinute < 0 || burstLimit < 0) {
            return true;
        }
        
        // Clean up old timestamps
        cleanupOldTimestamps(now);
        
        // Check rate limit (per minute)
        if (errorTimestamps.size() >= maxErrorsPerMinute) {
            logger.warn("Rate limit exceeded: {} errors in the last minute", errorTimestamps.size());
            return false;
        }
        
        // Check burst limit (per second)
        Instant currentBurstWindow = burstWindowStart.get();
        if (currentBurstWindow == null || now.minusSeconds(1).isAfter(currentBurstWindow)) {
            // Reset burst window
            burstWindowStart.set(now);
            burstCounter.set(0);
        }
        
        if (burstCounter.get() >= burstLimit) {
            logger.warn("Burst limit exceeded: {} errors in rapid succession", burstCounter.get());
            return false;
        }
        
        // Record this error
        errorTimestamps.offer(now);
        burstCounter.incrementAndGet();
        
        return true;
    }
    
    private void cleanupOldTimestamps(Instant now) {
        Instant oneMinuteAgo = now.minusSeconds(60);
        
        while (!errorTimestamps.isEmpty()) {
            Instant oldest = errorTimestamps.peek();
            if (oldest != null && oldest.isBefore(oneMinuteAgo)) {
                errorTimestamps.poll();
            } else {
                break;
            }
        }
    }
}