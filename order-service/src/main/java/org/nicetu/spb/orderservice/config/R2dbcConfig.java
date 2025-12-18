package org.nicetu.spb.orderservice.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableR2dbcRepositories(basePackages = "org.nicetu.spb.orderservice.repository")
@EnableR2dbcAuditing
@EnableTransactionManagement
public class R2dbcConfig {

    @Value("${spring.r2dbc.schema:}")
    private String schemaLocation;

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        if (schemaLocation != null && !schemaLocation.isEmpty()) {
            initializer.setDatabasePopulator(
                    new ResourceDatabasePopulator(
                            new ClassPathResource(schemaLocation)
                    )
            );
        }

        return initializer;
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager txManager) {
        return TransactionalOperator.create(txManager);
    }
}