package be.vlaanderen.omgeving.oddtoolkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OddtoolkitApplication {

  public static void main(String[] args) {
    SpringApplication.run(OddtoolkitApplication.class, args);
  }

}
