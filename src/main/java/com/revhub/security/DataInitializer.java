package com.revhub.security;

import com.revhub.model.Admin;
import com.revhub.model.UserRole;
import com.revhub.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!adminRepository.existsByEmail("admin@revhub.com")) {
            Admin admin = Admin.builder()
                    .email("admin@revhub.com")
                    .password(passwordEncoder.encode("password"))
                    .name("System Admin")
                    .role(UserRole.ADMIN)
                    .build();
            adminRepository.save(admin);
            System.out.println("Admin user seeded: admin@revhub.com / password");
        }
    }
}
