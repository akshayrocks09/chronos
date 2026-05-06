package com.chronos.config;

import com.chronos.entity.User;
import com.chronos.enums.UserRole;
import com.chronos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            if (adminPassword == null || adminPassword.trim().isEmpty() || "admin123".equals(adminPassword)) {
                log.error("CRITICAL SECURITY ERROR: ADMIN_PASSWORD environment variable is not set or is set to the insecure default 'admin123'.");
                log.error("The application will NOT start without a secure ADMIN_PASSWORD.");
                throw new IllegalStateException("ADMIN_PASSWORD must be provided via environment variable and cannot be 'admin123'.");
            }
            User admin = User.builder()
                    .username("admin")
                    .email("admin@chronos.local")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user (username=admin)");
        }
    }
}
