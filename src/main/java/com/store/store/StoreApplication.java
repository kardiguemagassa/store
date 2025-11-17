package com.store.store;

import com.store.store.dto.contact.ContactInfoDto;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;


@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(value = {ContactInfoDto.class})
public class StoreApplication { public static void main(String[] args) {SpringApplication.run(StoreApplication.class, args);}
}