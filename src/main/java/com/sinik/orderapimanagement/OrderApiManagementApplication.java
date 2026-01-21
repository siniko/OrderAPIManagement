package com.sinik.orderapimanagement;

import com.sinik.orderapimanagement.notification.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableConfigurationProperties(NotificationProperties.class)
@EnableRetry
@EnableAsync
@SpringBootApplication
public class OrderApiManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApiManagementApplication.class, args);
    }

}
