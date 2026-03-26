package com.itfuliang.openclawproject;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.itfuliang.openclawproject.mapper") // 显式扫描 Mapper
public class OpenclawProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenclawProjectApplication.class, args);
    }

}
