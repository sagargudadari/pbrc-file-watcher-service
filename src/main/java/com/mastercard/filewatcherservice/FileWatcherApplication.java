package com.mastercard.filewatcherservice;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableBatchProcessing
public class FileWatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileWatcherApplication.class, args);
    }

    @Bean
    public KieContainer kieContainer() {
        return KieServices.Factory.get().getKieClasspathContainer();
    }
}
