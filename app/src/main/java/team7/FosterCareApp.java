package team7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FosterCareApp {
    public static void main(String[] args) {
        SpringApplication.run(FosterCareApp.class, args);
    }
}
