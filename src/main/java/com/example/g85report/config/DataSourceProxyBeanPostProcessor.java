package com.example.g85report.config;

import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Wraps the Spring Boot auto-configured DataSource with a datasource-proxy
 * ProxyDataSource so that SqlCaptureListener can intercept every SQL execution.
 */
@Component
public class DataSourceProxyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
            return ProxyDataSourceBuilder
                    .create((DataSource) bean)
                    .listener(new SqlCaptureListener())
                    .build();
        }
        return bean;
    }
}
