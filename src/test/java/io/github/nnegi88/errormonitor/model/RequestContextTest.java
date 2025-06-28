package io.github.nnegi88.errormonitor.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextTest {

    private RequestContext requestContext;
    
    @BeforeEach
    void setUp() {
        requestContext = new RequestContext();
    }
    
    @Test
    void testAllFields() {
        requestContext.setUrl("/api/users/123");
        requestContext.setHttpMethod("GET");
        requestContext.setClientIp("192.168.1.1");
        requestContext.setUserAgent("Mozilla/5.0");
        requestContext.setReferer("https://example.com");
        requestContext.setStatusCode(404);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("Content-Type", "application/json");
        requestContext.setHeaders(headers);
        
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("page", new String[]{"1"});
        parameters.put("size", new String[]{"10"});
        requestContext.setParameters(parameters);
        
        assertThat(requestContext.getUrl()).isEqualTo("/api/users/123");
        assertThat(requestContext.getHttpMethod()).isEqualTo("GET");
        assertThat(requestContext.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(requestContext.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(requestContext.getReferer()).isEqualTo("https://example.com");
        assertThat(requestContext.getStatusCode()).isEqualTo(404);
        assertThat(requestContext.getHeaders()).containsExactlyEntriesOf(headers);
        assertThat(requestContext.getParameters()).containsExactlyEntriesOf(parameters);
    }
    
    @Test
    void testEmptyContext() {
        assertThat(requestContext.getUrl()).isNull();
        assertThat(requestContext.getHttpMethod()).isNull();
        assertThat(requestContext.getClientIp()).isNull();
        assertThat(requestContext.getUserAgent()).isNull();
        assertThat(requestContext.getReferer()).isNull();
        assertThat(requestContext.getStatusCode()).isEqualTo(0);
        assertThat(requestContext.getHeaders()).isNull();
        assertThat(requestContext.getParameters()).isNull();
    }
    
    @Test
    void testEqualsAndHashCode() {
        RequestContext context1 = new RequestContext();
        context1.setUrl("/test");
        context1.setHttpMethod("POST");
        
        RequestContext context2 = new RequestContext();
        context2.setUrl("/test");
        context2.setHttpMethod("POST");
        
        RequestContext context3 = new RequestContext();
        context3.setUrl("/test");
        context3.setHttpMethod("GET");
        
        assertThat(context1).isEqualTo(context2);
        assertThat(context1).isNotEqualTo(context3);
        assertThat(context1.hashCode()).isEqualTo(context2.hashCode());
    }
}