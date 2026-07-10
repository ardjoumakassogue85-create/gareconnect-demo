package com.hackathon.gares.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

@Configuration
public class DataSourceConfig {

    private static final String H2_URL = "jdbc:h2:mem:gares-dev;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    @Bean
    public DataSource dataSource(Environment environment) {
        String supabaseUrl = environment.getProperty("SUPABASE_DB_URL");
        String supabaseUser = environment.getProperty("SUPABASE_DB_USER", "postgres");
        String supabasePassword = environment.getProperty("SUPABASE_DB_PASSWORD", "");

        if (hasText(supabaseUrl)) {
            System.out.println("Database: trying Supabase with user " + maskUser(supabaseUser));
            HikariDataSource supabase = createDataSource(
                    "SupabasePool",
                    supabaseUrl,
                    supabaseUser,
                    supabasePassword,
                    "org.postgresql.Driver"
            );
            supabase.setConnectionTimeout(3000);

            try (Connection ignored = supabase.getConnection()) {
                System.out.println("Database: Supabase connected");
                return supabase;
            } catch (SQLException exception) {
                supabase.close();
                throw new IllegalStateException(buildSupabaseErrorMessage(exception, supabaseUser), exception);
            }
        } else {
            System.out.println("Database: Supabase not configured, using H2 local");
        }

        return createDataSource("H2LocalPool", H2_URL, "sa", "", "org.h2.Driver");
    }

    private HikariDataSource createDataSource(String poolName, String url, String username, String password, String driverClassName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName(poolName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildSupabaseErrorMessage(SQLException exception, String username) {
        String message = exception.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("password authentication failed")) {
            return "Database: Supabase refused the login for user " + maskUser(username)
                    + ". Check SUPABASE_DB_USER and SUPABASE_DB_PASSWORD in backend/.env. "
                    + "For the Supabase pooler, the user usually looks like postgres.<project-ref>.";
        }

        return "Database: Supabase configured but unavailable. Check SUPABASE_DB_URL, SUPABASE_DB_USER and SUPABASE_DB_PASSWORD.";
    }

    private String maskUser(String username) {
        if (!hasText(username)) {
            return "<empty>";
        }

        int dotIndex = username.indexOf('.');
        if (dotIndex > 0 && dotIndex < username.length() - 1) {
            return username.substring(0, dotIndex + 1) + "***";
        }

        return username;
    }
}
