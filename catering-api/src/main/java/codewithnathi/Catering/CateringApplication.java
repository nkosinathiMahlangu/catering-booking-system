package codewithnathi.Catering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CateringApplication {

	public static void main(String[] args) {
		SpringApplication.run(CateringApplication.class, args);
	}

}
