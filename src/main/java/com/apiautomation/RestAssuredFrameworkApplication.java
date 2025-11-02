package com.apiautomation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;

@SpringBootApplication
public class RestAssuredFrameworkApplication {
    public static void main(String[] args) {
        // Configure RestAssured encoding globally at application startup
        RestAssured.config = RestAssured.config()
            .encoderConfig(EncoderConfig.encoderConfig()
                .encodeContentTypeAs("application/json", ContentType.JSON)
                .encodeContentTypeAs("application/xml", ContentType.XML)
                .encodeContentTypeAs("text/plain", ContentType.TEXT)
                .encodeContentTypeAs("application/x-www-form-urlencoded", ContentType.URLENC)
                .encodeContentTypeAs("multipart/form-data", ContentType.MULTIPART));
        
        SpringApplication.run(RestAssuredFrameworkApplication.class, args);
    }
}
