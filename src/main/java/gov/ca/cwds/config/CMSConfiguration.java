package gov.ca.cwds.config;

import gov.ca.cwds.data.auth.UserIdDao;
import gov.ca.cwds.data.persistence.auth.AssignmentUnit;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Created by TPT2 on 10/27/2017.
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = UserIdDao.class)
@EntityScan(basePackageClasses = AssignmentUnit.class)
public class CMSConfiguration {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  public DataSource dataSource() {
    return dataSourceProperties().initializeDataSourceBuilder().build();
  }

  @Bean
  @Primary
  @ConfigurationProperties("spring.jpa")
  public HibernateJpaVendorAdapter jpaVendorAdapter() {
    return new HibernateJpaVendorAdapter();
  }

  @Bean
  @Primary
  @ConfigurationProperties("spring.jpa")
  public JpaProperties jpaProperties() {
    return new JpaProperties();
  }

  @Bean
  @Primary
  @Autowired
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setJpaPropertyMap(jpaProperties().getHibernateProperties(dataSource));
    em.setPackagesToScan("gov.ca.cwds.data.persistence.auth", "gov.ca.cwds.data.auth");
    em.setPersistenceUnitName("default");
    em.setJpaVendorAdapter(jpaVendorAdapter());
    return em;
  }

  @Bean
  @Primary
  @Autowired
  public PlatformTransactionManager tokenTransactionManager(DataSource dataSource) {
    JpaTransactionManager transactionManager
        = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(
        entityManagerFactory(dataSource).getObject());
    return transactionManager;
  }
}
