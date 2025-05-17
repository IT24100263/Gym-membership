package com.example.Gym.membership.setup; // Adjust package if you placed it elsewhere

import com.gymsystem.gymmembershipmanagement.model.Staff;
import com.gymsystem.gymmembershipmanagement.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component // Make it a Spring-managed component
public class DataLoader implements CommandLineRunner {

    private final StaffService staffService;

    // Define the exact credentials to use
    private static final String ADMIN_EMAIL = "admin@gym.com";
    private static final String ADMIN_PASSWORD = "password123"; // INSECURE - For setup only!
    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    public DataLoader(StaffService staffService) {
        this.staffService = staffService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DataLoader - Checking for existing admin users...");
        // Check if any admin user already exists
        List<Staff> existingStaff = staffService.getAllStaff(); // Use service which uses repository
        boolean adminExists = existingStaff.stream()
                                        .anyMatch(s -> ADMIN_ROLE.equalsIgnoreCase(s.getRole()));

        if (!adminExists) {
            System.out.println("***** DataLoader: No ADMIN found. Creating initial administrator... *****");
            try {
                // Check if the specific email already exists (maybe as non-admin)
                if(staffService.getStaffByEmail(ADMIN_EMAIL).isPresent()) {
                     System.out.println("***** DataLoader: User with email " + ADMIN_EMAIL + " already exists. Skipping initial admin creation. *****");
                     return; // Exit if email exists, even if not admin
                }

                // !!! IMPORTANT: Password should be HASHED by the service !!!
                Staff admin = new Staff();
                admin.setName("Default Admin");
                admin.setEmail(ADMIN_EMAIL);
                admin.setPassword(ADMIN_PASSWORD); // Pass plain password to service
                admin.setRole(ADMIN_ROLE);

                // registerStaff should handle validation and saving (and ideally hashing)
                staffService.registerStaff(admin);

                System.out.println("***** DataLoader: Initial administrator created with email: " + ADMIN_EMAIL + " *****");
                System.out.println("***** DataLoader: Default Password: " + ADMIN_PASSWORD + " *****");
                System.out.println("***** PLEASE CHANGE THE DEFAULT PASSWORD IMMEDIATELY AFTER LOGIN *****");

            } catch (IllegalArgumentException e) {
                 System.err.println("***** DataLoader: FAILED to create initial administrator (Validation Failed): " + e.getMessage() + " *****");
            } catch (Exception e) {
                System.err.println("***** DataLoader: FAILED to create initial administrator (Unexpected Error): " + e.getMessage() + " *****");
                e.printStackTrace(); // Print stack trace for unexpected errors
            }
        } else {
            System.out.println("DataLoader: Admin user already exists. Skipping initial admin creation.");
        }
    }
}