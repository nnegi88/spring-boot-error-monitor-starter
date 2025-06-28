package io.github.nnegi88.errormonitor.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RequestContext {
    private String url;
    private String httpMethod;
    private String userAgent;
    private String clientIp;
    private String referer;
    private Map<String, String> headers;
    private Map<String, String[]> parameters;
    private String requestBody;
    private int statusCode;
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public String getReferer() {
        return referer;
    }
    
    public void setReferer(String referer) {
        this.referer = referer;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Map<String, String[]> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, String[]> parameters) {
        this.parameters = parameters;
    }
    
    public String getRequestBody() {
        return requestBody;
    }
    
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestContext that = (RequestContext) o;
        return statusCode == that.statusCode &&
                Objects.equals(url, that.url) &&
                Objects.equals(httpMethod, that.httpMethod) &&
                Objects.equals(userAgent, that.userAgent) &&
                Objects.equals(clientIp, that.clientIp) &&
                Objects.equals(referer, that.referer) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(requestBody, that.requestBody);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(url, httpMethod, userAgent, clientIp, referer, headers, parameters, requestBody, statusCode);
    }
}