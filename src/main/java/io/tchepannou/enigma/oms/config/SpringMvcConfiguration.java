package io.tchepannou.enigma.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
public class SpringMvcConfiguration extends WebMvcConfigurerAdapter {
    @Bean
    public RequestContextListener requestContextListener(){
        return new RequestContextListener();
    }
}
