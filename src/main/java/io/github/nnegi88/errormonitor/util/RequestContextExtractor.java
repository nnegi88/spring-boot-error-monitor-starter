package io.github.nnegi88.errormonitor.util;

import io.github.nnegi88.errormonitor.model.RequestContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RequestContextExtractor {
    
    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };
    
    public RequestContext extractContext(HttpServletRequest request) {
        RequestContext context = new RequestContext();
        
        context.setUrl(request.getRequestURL().toString());
        context.setHttpMethod(request.getMethod());
        context.setUserAgent(request.getHeader("User-Agent"));
        context.setClientIp(extractClientIp(request));
        context.setHeaders(extractHeaders(request));
        context.setParameters(request.getParameterMap());
        
        return context;
    }
    
    public Optional<HttpServletRequest> extractHttpServletRequest(WebRequest webRequest) {
        if (webRequest instanceof ServletWebRequest) {
            return Optional.of(((ServletWebRequest) webRequest).getRequest());
        }
        return Optional.empty();
    }
    
    private String extractClientIp(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs (in case of proxy chain)
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
    
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            headers.put(headerName, request.getHeader(headerName));
        });
        return headers;
    }
}