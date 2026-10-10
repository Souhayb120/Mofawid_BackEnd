package com.example.mofawid.config;



import com.example.mofawid.Enums.Role;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminInitializer(
            UserRepo userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userRepository.findByUsername("superadmin").isEmpty()) {

            User superAdmin = new User();

            superAdmin.setUsername("superadmin");
            superAdmin.setEmail("admin@mofawid.ma");

            superAdmin.setPassword(
                    passwordEncoder.encode("Admin@123")
            );

            superAdmin.setNom("Admin");
            superAdmin.setPrenom("Mofawid");

            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setActive(true);

            superAdmin.setTenant(null);

            userRepository.save(superAdmin);
        }
    }
}