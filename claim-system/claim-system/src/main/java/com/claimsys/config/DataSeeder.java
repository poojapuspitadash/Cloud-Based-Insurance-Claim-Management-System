package com.claimsys.config;

import com.claimsys.model.AppUser;
import com.claimsys.model.Role;
import com.claimsys.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Creates the first employee account (and a demo customer when app.seed-demo-data=true). */
@Configuration
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seed(UserRepository users, PasswordEncoder encoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword,
                           @Value("${app.seed-demo-data:false}") boolean demo) {
        return args -> {
            if (!users.existsByEmail(adminEmail)) {
                users.save(new AppUser("Claims Officer", adminEmail, encoder.encode(adminPassword), null, Role.EMPLOYEE));
                log.info("Created employee account {}", adminEmail);
            }
            if (demo && !users.existsByEmail("rahul@example.com")) {
                users.save(new AppUser("Rahul Sharma", "rahul@example.com", encoder.encode("Customer@123"), "POL1001", Role.CUSTOMER));
                log.info("Created demo customer rahul@example.com");
            }
        };
    }
}
