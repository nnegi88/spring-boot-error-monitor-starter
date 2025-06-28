package io.github.nnegi88.errormonitor.config;

import io.github.nnegi88.errormonitor.core.DefaultErrorMonitor;
import io.github.nnegi88.errormonitor.core.DefaultErrorProcessor;
import io.github.nnegi88.errormonitor.core.ErrorMonitor;
import io.github.nnegi88.errormonitor.core.ErrorProcessor;
import io.github.nnegi88.errormonitor.filter.CompositeErrorFilter;
import io.github.nnegi88.errormonitor.filter.DefaultErrorFilter;
import io.github.nnegi88.errormonitor.filter.ErrorFilter;
import io.github.nnegi88.errormonitor.filter.PackageErrorFilter;
import io.github.nnegi88.errormonitor.filter.RateLimitingErrorFilter;
import io.github.nnegi88.errormonitor.interceptor.ErrorMonitorFilter;
import io.github.nnegi88.errormonitor.interceptor.GlobalExceptionHandler;
import io.github.nnegi88.errormonitor.notification.CompositeNotificationClient;
import io.github.nnegi88.errormonitor.notification.NotificationClient;
import io.github.nnegi88.errormonitor.notification.slack.SlackClient;
import io.github.nnegi88.errormonitor.notification.teams.TeamsClient;
import io.github.nnegi88.errormonitor.notification.template.DefaultSlackMessageTemplate;
import io.github.nnegi88.errormonitor.notification.template.DefaultTeamsMessageTemplate;
import io.github.nnegi88.errormonitor.notification.template.SlackMessageTemplate;
import io.github.nnegi88.errormonitor.notification.template.TeamsMessageTemplate;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import io.github.nnegi88.errormonitor.metrics.MicrometerErrorMetrics;
import io.github.nnegi88.errormonitor.metrics.NoOpErrorMetrics;
import io.github.nnegi88.errormonitor.util.RequestContextExtractor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(ErrorMonitorProperties.class)
@ConditionalOnProperty(prefix = "spring.error-monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ErrorMonitorAutoConfiguration {
    
    @Value("${spring.application.name:unknown}")
    private String applicationName;
    
    @Value("${spring.profiles.active:default}")
    private String environment;
    
    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RequestContextExtractor requestContextExtractor() {
        return new RequestContextExtractor();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SlackMessageTemplate slackMessageTemplate(ErrorMonitorProperties properties) {
        return new DefaultSlackMessageTemplate(properties, applicationName, environment);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TeamsMessageTemplate teamsMessageTemplate(ErrorMonitorProperties properties) {
        return new DefaultTeamsMessageTemplate(properties, applicationName, environment);
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "packageErrorFilter")
    public PackageErrorFilter packageErrorFilter(ErrorMonitorProperties properties) {
        return new PackageErrorFilter(properties.getFiltering());
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "rateLimitingErrorFilter")
    public RateLimitingErrorFilter rateLimitingErrorFilter(ErrorMonitorProperties properties) {
        return new RateLimitingErrorFilter(properties.getRateLimiting());
    }
    
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "errorFilter")
    public ErrorFilter errorFilter(ErrorMonitorProperties properties, 
                                 @Autowired(required = false) PackageErrorFilter packageFilter,
                                 @Autowired(required = false) RateLimitingErrorFilter rateLimitingFilter,
                                 @Autowired(required = false) List<ErrorFilter> customFilters) {
        CompositeErrorFilter compositeFilter = new CompositeErrorFilter();
        
        // Add built-in filters
        if (packageFilter != null) {
            compositeFilter.addFilter(packageFilter);
        }
        if (rateLimitingFilter != null) {
            compositeFilter.addFilter(rateLimitingFilter);
        }
        
        // Add custom filters (excluding the built-in ones)
        if (customFilters != null) {
            customFilters.stream()
                .filter(f -> !(f instanceof PackageErrorFilter) && !(f instanceof RateLimitingErrorFilter))
                .forEach(compositeFilter::addFilter);
        }
        
        return compositeFilter;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public NotificationClient notificationClient(ErrorMonitorProperties properties,
                                               WebClient.Builder webClientBuilder,
                                               SlackMessageTemplate slackTemplate,
                                               TeamsMessageTemplate teamsTemplate,
                                               List<NotificationClient> customClients) {
        
        CompositeNotificationClient compositeClient = new CompositeNotificationClient();
        
        String platform = properties.getNotification().getPlatform().toLowerCase();
        
        if ("slack".equals(platform) || "both".equals(platform)) {
            compositeClient.addClient(new SlackClient(webClientBuilder, properties, slackTemplate));
        }
        
        if ("teams".equals(platform) || "both".equals(platform)) {
            compositeClient.addClient(new TeamsClient(webClientBuilder, properties, teamsTemplate));
        }
        
        // Add custom clients
        customClients.forEach(compositeClient::addClient);
        
        return compositeClient;
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "spring.error-monitor.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ErrorMetrics micrometerErrorMetrics(@Autowired(required = false) MeterRegistry meterRegistry) {
        if (meterRegistry != null) {
            return new MicrometerErrorMetrics(meterRegistry);
        }
        return new NoOpErrorMetrics();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
    public ErrorMetrics noOpErrorMetrics() {
        return new NoOpErrorMetrics();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ErrorProcessor errorProcessor(NotificationClient notificationClient, 
                                       ErrorFilter errorFilter,
                                       ErrorMonitorProperties properties,
                                       ErrorMetrics errorMetrics) {
        return new DefaultErrorProcessor(notificationClient, errorFilter, properties, errorMetrics, applicationName, environment);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ErrorMonitor errorMonitor(ErrorProcessor errorProcessor) {
        return new DefaultErrorMonitor(errorProcessor);
    }
    
    @Configuration
    @ConditionalOnWebApplication
    public static class WebConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public GlobalExceptionHandler globalExceptionHandler(ErrorProcessor errorProcessor, 
                                                           RequestContextExtractor contextExtractor) {
            return new GlobalExceptionHandler(errorProcessor, contextExtractor);
        }
        
        @Bean
        public FilterRegistrationBean<ErrorMonitorFilter> errorMonitorFilterRegistration(
                ErrorProcessor errorProcessor, RequestContextExtractor contextExtractor) {
            FilterRegistrationBean<ErrorMonitorFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new ErrorMonitorFilter(errorProcessor, contextExtractor));
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registration;
        }
    }
}