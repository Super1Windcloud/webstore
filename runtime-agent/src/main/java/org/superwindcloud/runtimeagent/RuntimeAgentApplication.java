package org.superwindcloud.runtimeagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RuntimeAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(RuntimeAgentApplication.class, args);
  }
}
