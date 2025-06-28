package io.github.nnegi88.errormonitor.filter;

import io.github.nnegi88.errormonitor.model.ErrorEvent;

import java.util.ArrayList;
import java.util.List;

public class CompositeErrorFilter implements ErrorFilter {
    
    private final List<ErrorFilter> filters = new ArrayList<>();
    
    public void addFilter(ErrorFilter filter) {
        filters.add(filter);
    }
    
    @Override
    public boolean shouldReport(ErrorEvent event) {
        // All filters must approve for the event to be reported
        return filters.stream().allMatch(filter -> filter.shouldReport(event));
    }
}