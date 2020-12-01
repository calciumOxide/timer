package com.oxde.timer;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class TimerApplication {

    public static ApplicationContext context = null;

    public static void main(String[] args) {
        context = SpringApplication.run(TimerApplication.class, args);
    }
}
