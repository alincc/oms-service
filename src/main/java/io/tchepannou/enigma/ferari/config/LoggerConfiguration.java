package io.tchepannou.enigma.ferari.config;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.logger.KVLoggerAspect;
import io.tchepannou.core.logger.KVLoggerFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.time.Clock;

@Configuration
public class LoggerConfiguration {

    @Bean
    @Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public KVLogger logger (){
        return new KVLogger();
    }

    @Bean
    public KVLoggerFilter loggerFilter(){
        return new KVLoggerFilter(logger(), Clock.systemUTC());
    }

    @Bean
    public FilterRegistrationBean loggerFilterRegistrationBean(){
        final FilterRegistrationBean bean = new FilterRegistrationBean();
        bean.setFilter(loggerFilter());
        bean.addUrlPatterns("/v1/*");
        return bean;
    }

    @Bean
    public KVLoggerAspect loggerAspect(){
        return new KVLoggerAspect(logger());
    }
}
