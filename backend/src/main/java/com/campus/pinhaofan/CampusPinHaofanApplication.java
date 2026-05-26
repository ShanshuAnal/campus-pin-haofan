package com.campus.pinhaofan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.campus.pinhaofan.mapper")
@SpringBootApplication
public class CampusPinHaofanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusPinHaofanApplication.class, args);
    }
}
