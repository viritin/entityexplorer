package in.virit.entityexplorerit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point of the Spring Boot application for development time.
 * 
 * This starts the project in development mode.
 */
@SpringBootApplication
public class DevModeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevModeApplication.class, args);
    }

}
