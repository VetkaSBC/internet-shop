package org.nicetu.spb.productservice.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableR2dbcRepositories(basePackages = "org.nicetu.spb.productservice.repository")
@EnableR2dbcAuditing
@EnableTransactionManagement
public class R2dbcConfig {

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        new ClassPathResource("schema.sql")
                )
        );
        return initializer;
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager txManager) {
        return TransactionalOperator.create(txManager);
    }
}