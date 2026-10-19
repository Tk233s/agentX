package org.example.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 数据源配置：主库仍是 MySQL，知识库可以切到独立的 PostgreSQL。
 *
 * 声明了第二个 DataSource 之后，Spring Boot 的数据源自动配置会整体退让，
 * 所以主库必须在这里显式补一个 @Primary 的 bean，否则 MyBatis 找不到唯一数据源。
 */
@Configuration
public class RagDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConfigurationProperties("agentx.rag.datasource")
    public DataSourceProperties ragDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource ragDataSource(@Qualifier("ragDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate ragJdbcTemplate(@Qualifier("ragDataSource") DataSource ragDataSource) {
        return new JdbcTemplate(ragDataSource);
    }

    /**
     * 知识库的事务管理器，领域服务用 @Transactional("ragTransactionManager") 指定。
     */
    @Bean(name = "ragTransactionManager")
    public PlatformTransactionManager ragTransactionManager(@Qualifier("ragDataSource") DataSource ragDataSource) {
        return new DataSourceTransactionManager(ragDataSource);
    }
}
