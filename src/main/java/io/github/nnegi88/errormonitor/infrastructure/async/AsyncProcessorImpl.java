package io.github.nnegi88.errormonitor.infrastructure.async;

import io.github.nnegi88.errormonitor.domain.port.AsyncProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Implementation of AsyncProcessor using a ThreadPoolExecutor with bounded queue.
 * Provides async processing with configurable queue size and thread pool settings.
 */
public class AsyncProcessorImpl implements AsyncProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessorImpl.class);
    
    private final ExecutorService executorService;
    private final BlockingQueue<Runnable> taskQueue;
    private final int maxQueueSize;
    private volatile boolean shutdown = false;
    
    public AsyncProcessorImpl() {
        this(256, 1, 4);
    }
    
    public AsyncProcessorImpl(int queueSize, int corePoolSize, int maximumPoolSize) {
        this.maxQueueSize = queueSize;
        this.taskQueue = new LinkedBlockingQueue<>(queueSize);
        
        this.executorService = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                60L, TimeUnit.SECONDS,
                taskQueue,
                this::createThread,
                new ThreadPoolExecutor.DiscardOldestPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        if (!e.isShutdown()) {
                            logger.warn("Task queue is full, discarding oldest task");
                            super.rejectedExecution(r, e);
                        }
                    }
                }
        );
        
        logger.debug("AsyncProcessor initialized with queue size: {}, core threads: {}, max threads: {}", 
                    queueSize, corePoolSize, maximumPoolSize);
    }
    
    @Override
    public CompletableFuture<Void> processAsync(Runnable task) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncProcessor is shutdown"));
        }
        
        return CompletableFuture.runAsync(task, executorService)
                .exceptionally(throwable -> {
                    logger.error("Async task execution failed", throwable);
                    return null;
                });
    }
    
    @Override
    public <T> CompletableFuture<T> processAsync(Callable<T> task) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncProcessor is shutdown"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService)
        .exceptionally(throwable -> {
            logger.error("Async callable execution failed", throwable);
            throw new CompletionException(throwable);
        });
    }
    
    @Override
    public boolean canAcceptTasks() {
        return !shutdown && taskQueue.remainingCapacity() > 0;
    }
    
    @Override
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        shutdown = true;
        logger.info("Shutting down AsyncProcessor gracefully...");
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("AsyncProcessor did not terminate within 10 seconds, forcing shutdown");
                shutdownNow();
            } else {
                logger.info("AsyncProcessor shutdown completed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for AsyncProcessor shutdown", e);
            shutdownNow();
        }
    }
    
    @Override
    public void shutdownNow() {
        if (shutdown) {
            return;
        }
        
        shutdown = true;
        logger.info("Forcing AsyncProcessor shutdown...");
        
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            logger.info("AsyncProcessor forced shutdown completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted during forced shutdown", e);
        }
    }
    
    private Thread createThread(Runnable r) {
        Thread thread = new Thread(r, "notification-async-" + System.nanoTime());
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> 
                logger.error("Uncaught exception in async thread: {}", t.getName(), e));
        return thread;
    }
}