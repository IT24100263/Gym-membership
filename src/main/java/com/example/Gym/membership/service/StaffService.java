package com.example.Gym.membership.service;

import com.gymsystem.gymmembershipmanagement.model.Staff;
import com.gymsystem.gymmembershipmanagement.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
// IMPORTANT: For password hashing, uncomment imports and PasswordEncoder injection
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service; // Already present

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    // === IMPORTANT: Uncomment and Inject PasswordEncoder for Hashing ===
    // private final PasswordEncoder passwordEncoder;

    @Autowired
    public StaffService(StaffRepository staffRepository /*, PasswordEncoder passwordEncoder */) {
        this.staffRepository = staffRepository;
        // this.passwordEncoder = passwordEncoder;
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    public Optional<Staff> getStaffById(String id) {
        return staffRepository.findById(id);
    }

    public Optional<Staff> getStaffByEmail(String email) {
        // Add trim() and null check for robustness, rely on repository's case-insensitivity
        if (email == null) {
            System.out.println("StaffService.getStaffByEmail - Input email is null.");
            return Optional.empty();
        }
        String trimmedEmail = email.trim();
        if (trimmedEmail.isEmpty()){
            System.out.println("StaffService.getStaffByEmail - Input email is blank after trimming.");
            return Optional.empty();
        }
        System.out.println("StaffService.getStaffByEmail - Calling repository for email: [" + trimmedEmail + "]");
        return staffRepository.findByEmail(trimmedEmail);
    }

    public Staff registerStaff(Staff staff) {
        // 1. Validation
        if (staff == null) {
            throw new IllegalArgumentException("Staff object cannot be null.");
        }
        if (staff.getEmail() == null || staff.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Staff email cannot be empty.");
        }
        String trimmedEmail = staff.getEmail().trim();
        staff.setEmail(trimmedEmail); // Use trimmed email going forward

        if (staffRepository.findByEmail(trimmedEmail).isPresent()) { // Check using trimmed email
            throw new IllegalArgumentException("Email already exists: " + trimmedEmail);
        }
        if (staff.getPassword() == null || staff.getPassword().trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        String trimmedPassword = staff.getPassword().trim(); // Trim password

        if (staff.getRole() == null || staff.getRole().isBlank()) {
            throw new IllegalArgumentException("Staff role cannot be empty.");
        }
        String trimmedRole = staff.getRole().trim().toUpperCase(); // Standardize role case
        staff.setRole(trimmedRole);


        // 2. === Password Hashing Placeholder ===
        // IMPORTANT: Replace plain text storage with hashing
        // String hashedPassword = passwordEncoder.encode(trimmedPassword);
        // staff.setPassword(hashedPassword);
        // Storing trimmed plain text for now (INSECURE)
        staff.setPassword(trimmedPassword);
        System.err.println("StaffService.registerStaff - WARNING: Storing staff password as plain text for " + trimmedEmail);


        // 3. Save
        return staffRepository.save(staff);
    }

    public Staff updateStaff(String id, Staff updatedStaffDetails) {
        if (updatedStaffDetails == null) throw new IllegalArgumentException("Updated staff details cannot be null.");

        updatedStaffDetails.setId(id); // Ensure ID consistency
        Staff existingStaff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + id));

        // Validate and Trim Email
        if (updatedStaffDetails.getEmail() == null || updatedStaffDetails.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Staff email cannot be empty.");
        }
        String trimmedEmail = updatedStaffDetails.getEmail().trim();

        // Prevent changing email to one that already exists (excluding self)
        Optional<Staff> staffWithEmail = staffRepository.findByEmail(trimmedEmail);
        if(staffWithEmail.isPresent() && !staffWithEmail.get().getId().equals(id)) {
            throw new IllegalArgumentException("Email " + trimmedEmail + " is already used by another staff member.");
        }
        existingStaff.setEmail(trimmedEmail); // Set trimmed email


        // Update fields - Handle password update separately/carefully
        if (updatedStaffDetails.getPassword() != null && !updatedStaffDetails.getPassword().isBlank()) {
            String newPassword = updatedStaffDetails.getPassword().trim();
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters long.");
            }
            // === Password Hashing Placeholder ===
            // String hashedPassword = passwordEncoder.encode(newPassword);
            // existingStaff.setPassword(hashedPassword);
            System.err.println("StaffService.updateStaff - WARNING: Updating staff password as plain text for ID " + id);
            existingStaff.setPassword(newPassword); // INSECURE DEMO
        } // else: keep existing password if new one is blank


        // Validate and Trim Role
        if (updatedStaffDetails.getRole() == null || updatedStaffDetails.getRole().isBlank()) {
            throw new IllegalArgumentException("Staff role cannot be empty.");
        }
        existingStaff.setRole(updatedStaffDetails.getRole().trim().toUpperCase());

        // Update other mutable fields
        existingStaff.setName(updatedStaffDetails.getName());
        // Update other staff-specific fields if any...

        return staffRepository.save(existingStaff);
    }

    public boolean deleteStaff(String id) {
        Optional<Staff> staffToDelete = staffRepository.findById(id);
        if (staffToDelete.isEmpty()) {
            System.out.println("StaffService.deleteStaff - Staff not found with ID: ["+id+"]");
            return false; // Indicate not found
        }

        // --- Business Logic Example Placeholder ---
        // Prevent deleting the only admin
        if ("ADMIN".equalsIgnoreCase(staffToDelete.get().getRole())) {
            long adminCount = staffRepository.findAll().stream()
                    .filter(s -> "ADMIN".equalsIgnoreCase(s.getRole()))
                    .count();
            if (adminCount <= 1) {
                System.err.println("StaffService.deleteStaff - Attempt to delete the last administrator denied for ID: ["+id+"]");
                throw new IllegalStateException("Cannot delete the last administrator.");
            }
        }
        // --- End Placeholder ---

        return staffRepository.deleteById(id);
    }

    // --- Login ---
    public Optional<Staff> login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            System.out.println("StaffService.login - Email or Password input is null.");
            return Optional.empty(); // Cannot login with null credentials
        }
        // Use service method which trims email
        Optional<Staff> staffOpt = getStaffByEmail(email); // Search using potentially trimmed email

        if (staffOpt.isPresent()) {
            Staff staff = staffOpt.get();
            String storedPassword = staff.getPassword(); // Password from the file/object

            // Detailed Logging
            System.out.println("StaffService.login - Found staff: " + staff.getEmail());
            System.out.println("StaffService.login - Comparing entered PW: [" + rawPassword + "] ("+ rawPassword.length() +" chars)");
            System.out.println("StaffService.login - With stored   PW: [" + storedPassword + "] ("+ (storedPassword != null ? storedPassword.length() : 0) +" chars)");

            // === Password Comparison (INSECURE - Replace with Hashing Check) ===
            if (storedPassword != null && storedPassword.equals(rawPassword)) {
                System.err.println("StaffService.login - WARNING: Performing plain text password check.");
                System.out.println("StaffService.login - Password MATCH.");
                return Optional.of(staff); // Login successful
            } else {
                System.out.println("StaffService.login - Password MISMATCH or Stored Password is NULL.");
            }
            // === End Password Comparison ===

        } else {
            // Logging already done in getStaffByEmail/repository if not found
            // System.out.println("StaffService.login - Staff not found for email: [" + email.trim() + "]"); // Redundant
        }
        // If not found or password mismatch, return empty
        return Optional.empty();
    }
}