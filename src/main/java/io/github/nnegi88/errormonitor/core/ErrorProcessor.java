package io.github.nnegi88.errormonitor.core;

import io.github.nnegi88.errormonitor.model.ErrorEvent;

public interface ErrorProcessor {
    void processError(ErrorEvent errorEvent);
    boolean shouldProcess(ErrorEvent errorEvent);
}