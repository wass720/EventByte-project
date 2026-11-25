package fr.soft_labs.eventbyte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class EventbyteApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventbyteApplication.class, args);
	}

}
