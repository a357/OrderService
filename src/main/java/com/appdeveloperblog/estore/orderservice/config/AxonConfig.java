package com.appdeveloperblog.estore.orderservice.config;

import com.thoughtworks.xstream.XStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonConfig {

    @Bean
    public XStream xStream() {
        XStream xStream = new XStream();

        xStream.allowTypesByWildcard(new String[] {
                "ch.qos.logback.**",
                "com.appdeveloperblog.estore.core.**",
                "com.appdeveloperblog.estore.orderservice.**"
        });
        return xStream;
    }
}