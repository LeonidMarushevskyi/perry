package gov.ca.cwds;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import gov.ca.cwds.idm.service.cognito.CognitoProperties;
import gov.ca.cwds.idm.service.cognito.SearchProperties;
import gov.ca.cwds.security.jwt.JwtService;

@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class, SessionAutoConfiguration.class,
    LiquibaseAutoConfiguration.class})
@ComponentScan("gov.ca.cwds")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EntityScan("gov.ca.cwds.data.persistence.auth")
@EnableConfigurationProperties({PerryProperties.class, CognitoProperties.class,
    SearchProperties.class})
public class PerryApplication {

  @Bean
  public RestTemplate client() {
    return new RestTemplate();
  }

  @Bean
  @Autowired
  public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    final JpaTransactionManager txManager = new JpaTransactionManager();
    txManager.setEntityManagerFactory(entityManagerFactory);
    return txManager;
  }

  @Bean
  @Autowired
  public JwtService jwtService(PerryProperties perryProperties) {
    return new JwtService(perryProperties.getJwt());
  }

  public static void main(String[] args) {
    SpringApplication.run(PerryApplication.class, args);
  }

}
