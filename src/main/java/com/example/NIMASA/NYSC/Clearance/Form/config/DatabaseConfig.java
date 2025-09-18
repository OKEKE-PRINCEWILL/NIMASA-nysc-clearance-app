//package com.example.NIMASA.NYSC.Clearance.Form.config;
//
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//import javax.sql.DataSource;
//import java.util.Properties;
//
//@Slf4j
//@Configuration
//@EnableTransactionManagement
//public class DatabaseConfig {
//
//    @Value("${spring.datasource.url}")
//    private String jdbcUrl;
//
//    @Value("${spring.datasource.username}")
//    private String username;
//
//    @Value("${spring.datasource.password}")
//    private String password;
//
//    @Value("${spring.datasource.driver-class-name}")
//    private String driverClassName;
//
//    /**
//     * FIXED: Optimized DataSource configuration to prevent JDBC connection errors
//     */
//    @Bean
//    @Primary
//    public DataSource dataSource() {
//        HikariConfig config = new HikariConfig();
//
//        // Basic connection settings
//        config.setJdbcUrl(jdbcUrl);
//        config.setUsername(username);
//        config.setPassword(password);
//        config.setDriverClassName(driverClassName);
//
//        // CRITICAL: Connection pool settings to prevent "Unable to commit" errors
//        config.setMaximumPoolSize(5);           // Keep small to avoid overwhelming DB
//        config.setMinimumIdle(2);               // Always have connections ready
//        config.setConnectionTimeout(30000);     // 30 seconds to get connection
//        config.setIdleTimeout(300000);          // 5 minutes idle timeout
//        config.setMaxLifetime(900000);          // 15 minutes max connection lifetime
//        config.setLeakDetectionThreshold(60000); // 1 minute leak detection
//
//        // CRITICAL: Auto-commit and validation settings
//        config.setAutoCommit(true);             // Enable auto-commit by default
//        config.setConnectionTestQuery("SELECT 1"); // Simple validation query
//        config.setValidationTimeout(5000);     // 5 seconds for validation
//
//        // Connection initialization
//        config.setConnectionInitSql("SELECT 1");
//
//        // Pool naming for monitoring
//        config.setPoolName("NIMASAAuthPool");
//        config.setRegisterMbeans(true);
//
//        // CRITICAL: Additional PostgreSQL specific settings
//        config.addDataSourceProperty("cachePrepStmts", "true");
//        config.addDataSourceProperty("prepStmtCacheSize", "250");
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
//        config.addDataSourceProperty("useServerPrepStmts", "true");
//        config.addDataSourceProperty("rewriteBatchedStatements", "true");
//
//        // Connection recovery settings
//        config.addDataSourceProperty("socketTimeout", "30");
//        config.addDataSourceProperty("loginTimeout", "10");
//        config.addDataSourceProperty("connectTimeout", "10");
//
//        log.info("Configured HikariCP with maxPoolSize={}, minIdle={}, connectionTimeout={}ms",
//                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getConnectionTimeout());
//
//        return new HikariDataSource(config);
//    }
//
//    /**
//     * FIXED: EntityManagerFactory with proper transaction settings
//     */
//    @Bean
//    @Primary
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
//        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
//        em.setDataSource(dataSource);
//        em.setPackagesToScan("com.example.NIMASA.NYSC.Clearance.Form.model");
//
//        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//        vendorAdapter.setGenerateDdl(true);
//        vendorAdapter.setShowSql(false);
//        em.setJpaVendorAdapter(vendorAdapter);
//
//        // CRITICAL: Hibernate properties to prevent transaction issues
//        Properties jpaProperties = new Properties();
//        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
//        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
//        jpaProperties.put("hibernate.show_sql", "false");
//        jpaProperties.put("hibernate.format_sql", "false");
//
//        // CRITICAL: Transaction and connection management
//        jpaProperties.put("hibernate.connection.autocommit", "true");
//        jpaProperties.put("hibernate.connection.provider_disables_autocommit", "false");
//        jpaProperties.put("hibernate.current_session_context_class", "org.springframework.orm.hibernate5.SpringSessionContext");
//
//        // Batch processing (keep low to prevent connection issues)
//        jpaProperties.put("hibernate.jdbc.batch_size", "10");
//        jpaProperties.put("hibernate.order_inserts", "true");
//        jpaProperties.put("hibernate.order_updates", "true");
//        jpaProperties.put("hibernate.jdbc.batch_versioned_data", "true");
//
//        // Query optimization
//        jpaProperties.put("hibernate.query.plan_cache_max_size", "128");
//        jpaProperties.put("hibernate.query.plan_parameter_metadata_max_size", "32");
//
//        em.setJpaProperties(jpaProperties);
//
//        return em;
//    }
//
//    /**
//     * FIXED: Transaction manager with proper timeout settings
//     */
//    @Bean
//    @Primary
//    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
//        JpaTransactionManager transactionManager = new JpaTransactionManager();
//        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
//
//        // CRITICAL: Set transaction timeout to prevent hanging connections
//        transactionManager.setDefaultTimeout(30); // 30 seconds default timeout
//        transactionManager.setRollbackOnCommitFailure(true);
//
//        log.info("Configured JpaTransactionManager with 30 second timeout");
//
//        return transactionManager;
//    }
//
//    /**
//     * Health check method to verify database connectivity
//     */
//    @Bean
//    public DatabaseHealthChecker databaseHealthChecker(DataSource dataSource) {
//        return new DatabaseHealthChecker(dataSource);
//    }
//
//    /**
//     * Simple health checker for database connections
//     */
//    public static class DatabaseHealthChecker {
//        private final DataSource dataSource;
//
//        public DatabaseHealthChecker(DataSource dataSource) {
//            this.dataSource = dataSource;
//        }
//
//        public boolean isHealthy() {
//            try (var connection = dataSource.getConnection()) {
//                return connection.isValid(5); // 5 second timeout
//            } catch (Exception e) {
//                log.error("Database health check failed: {}", e.getMessage());
//                return false;
//            }
//        }
//    }
//}