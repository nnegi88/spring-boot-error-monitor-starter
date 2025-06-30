package io.github.nnegi88.errormonitor.domain.port;

import java.util.concurrent.CompletableFuture;

/**
 * Port interface for asynchronous processing operations.
 * Abstracts away the specific async implementation details.
 */
public interface AsyncProcessor {
    
    /**
     * Process a task asynchronously.
     * 
     * @param task the task to execute
     * @return a CompletableFuture that completes when the task is done
     */
    CompletableFuture<Void> processAsync(Runnable task);
    
    /**
     * Process a task asynchronously with a return value.
     * 
     * @param task the task to execute
     * @param <T> the return type
     * @return a CompletableFuture containing the task result
     */
    <T> CompletableFuture<T> processAsync(java.util.concurrent.Callable<T> task);
    
    /**
     * Check if the processor can accept more tasks.
     * 
     * @return true if more tasks can be accepted
     */
    boolean canAcceptTasks();
    
    /**
     * Get the current queue size.
     * 
     * @return the number of pending tasks
     */
    int getQueueSize();
    
    /**
     * Shutdown the processor gracefully.
     * Waits for currently executing tasks to complete.
     */
    void shutdown();
    
    /**
     * Shutdown the processor immediately.
     * Attempts to stop all executing tasks.
     */
    void shutdownNow();
}