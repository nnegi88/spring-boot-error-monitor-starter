package io.github.nnegi88.errormonitor.filter;

import io.github.nnegi88.errormonitor.model.ErrorEvent;

public interface ErrorFilter {
    boolean shouldReport(ErrorEvent event);
}