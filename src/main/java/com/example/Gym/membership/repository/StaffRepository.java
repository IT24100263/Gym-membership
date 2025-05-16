package com.example.Gym.membership.repository;

import com.gymsystem.gymmembershipmanagement.model.Staff;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class StaffRepository {

    private final Path filePath;
    private final AtomicLong idCounter = new AtomicLong(0); // For simple ID generation like S1, S2...

    public StaffRepository(@Value("${data.file.staff}") String filePath) {
        this.filePath = Paths.get(filePath);
        // Ensure parent directory exists on startup (helps avoid initial save errors)
        try {
            if (this.filePath.getParent() != null) {
                 Files.createDirectories(this.filePath.getParent());
                 System.out.println("StaffRepository - Ensured data directory exists: " + this.filePath.getParent());
            } else {
                 System.err.println("StaffRepository - WARNING: Could not determine parent directory for path: " + this.filePath);
            }
        } catch (IOException e) {
             System.err.println("StaffRepository - WARNING: Could not create data directory on startup: " + e.getMessage());
        } catch (Exception e) {
             System.err.println("StaffRepository - ERROR: Unexpected error ensuring directory exists: " + e.getMessage());
             e.printStackTrace();
        }
        initializeIdCounter();
    }

    private void initializeIdCounter() {
        long maxId = 0;
         try {
             // Check if file exists and is not empty before trying to read
             if (Files.exists(filePath) && Files.size(filePath) > 0) {
                 System.out.println("StaffRepository.initializeIdCounter - Reading existing staff file for max ID...");
                 List<Staff> staffList = findAllInternal(); // Use internal method to avoid double logging
                 for (Staff staff : staffList) {
                     // Add null check for staff object itself, as parsing might fail
                     if (staff == null) {
                         System.err.println("StaffRepository.initializeIdCounter - WARNING: Found null staff object in list, skipping ID check.");
                         continue;
                     }
                     try {
                         // Assuming ID format "S1", "S2"...
                         if (staff.getId() != null && staff.getId().toUpperCase().startsWith("S")) {
                             // Attempt to parse the number part after 'S'
                             long currentId = Long.parseLong(staff.getId().substring(1));
                             if (currentId > maxId) {
                                 maxId = currentId;
                             }
                         } else {
                              System.err.println("StaffRepository.initializeIdCounter - WARNING: Staff ID format incorrect or missing: " + staff.getId());
                         }
                     } catch (NumberFormatException | IndexOutOfBoundsException e) {
                         System.err.println("StaffRepository.initializeIdCounter - WARNING: Could not parse numeric part of staff ID: '" + staff.getId() + "' - " + e.getMessage());
                     } catch (NullPointerException e) {
                         // This shouldn't happen due to the null check above, but safe to keep
                         System.err.println("StaffRepository.initializeIdCounter - WARNING: Encountered null Staff ID during ID initialization.");
                     }
                 }
                 System.out.println("StaffRepository.initializeIdCounter - Initialized ID counter to start after max ID: " + maxId);
             } else {
                  System.out.println("StaffRepository.initializeIdCounter - Staff file not found or empty. Initializing ID counter to 0.");
             }
         } catch (IOException e) {
             System.err.println("StaffRepository.initializeIdCounter - WARNING: Could not read staff file to initialize ID counter: " + e.getMessage());
         } catch (Exception e) { // Catch broader exceptions during init
              System.err.println("StaffRepository.initializeIdCounter - ERROR: Unexpected error during ID initialization: " + e.getMessage());
              e.printStackTrace(); // Print stack trace for unexpected errors
         }
         this.idCounter.set(maxId);
    }

    // Internal method to read all staff, includes logging
    private List<Staff> findAllInternal() throws IOException {
        System.out.println("StaffRepository.findAllInternal - Reading file: " + filePath); // Log file path attempt
        if (!Files.exists(filePath)) {
            System.out.println("StaffRepository.findAllInternal - Staff file NOT FOUND.");
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        System.out.println("StaffRepository.findAllInternal - Read " + lines.size() + " lines from staff file.");
        if (lines.isEmpty()) {
             System.out.println("StaffRepository.findAllInternal - Staff file is empty.");
             return new ArrayList<>(); // Return empty list immediately
        }

        // Process lines, relying on Staff.fromCsvString to handle parsing and its own logging
        return lines.stream()
                    .map(Staff::fromCsvString) // Call the static parsing method
                    .filter(java.util.Objects::nonNull) // Keep only successfully parsed staff
                    .collect(Collectors.toList());
    }

    // Internal method to save all staff, includes logging
    private void saveAllInternal(List<Staff> staffList) throws IOException {
        System.out.println("StaffRepository.saveAllInternal - Attempting to save " + staffList.size() + " staff records to: " + filePath);
        // Ensure parent directory exists (safe redundancy)
        if (filePath.getParent() != null) {
             Files.createDirectories(filePath.getParent());
        }

        // Convert staff objects to CSV strings for writing
        List<String> lines = staffList.stream()
                                    .map(Staff::toCsvString) // Call instance method for formatting
                                    .collect(Collectors.toList());
        Files.write(filePath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, // Create if doesn't exist
                StandardOpenOption.WRITE,  // Allow writing
                StandardOpenOption.TRUNCATE_EXISTING); // Overwrite existing content
        System.out.println("StaffRepository.saveAllInternal - Successfully saved " + lines.size() + " lines.");
    }

    // --- CRUD Methods ---

    // Public method uses internal method with error handling
    public List<Staff> findAll() {
        System.out.println("StaffRepository.findAll - Public method called.");
        try {
            return findAllInternal();
        } catch (IOException e) {
            System.err.println("StaffRepository.findAll - ERROR reading staff file: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for IO errors during read
            return new ArrayList<>(); // Return empty list on error
        } catch (Exception e) { // Catch unexpected errors during find
             System.err.println("StaffRepository.findAll - UNEXPECTED ERROR reading staff file: " + e.getMessage());
             e.printStackTrace();
             return new ArrayList<>();
        }
    }

    // Public method uses findAll with stream logic
    public Optional<Staff> findById(String id) {
         System.out.println("StaffRepository.findById - Searching for ID: [" + id + "]");
         if (id == null || id.isBlank()) return Optional.empty(); // Prevent searching for null/blank ID
         List<Staff> allStaff = findAll(); // Calls method with logging/error handling
         return allStaff.stream()
                .filter(staff -> staff != null && id.equals(staff.getId())) // Added null check for staff object
                .findFirst();
    }

     // Public method uses findAll with stream logic and logging
     public Optional<Staff> findByEmail(String email) {
         System.out.println("StaffRepository.findByEmail - Searching for email (case-insensitive): [" + email + "]");
         if (email == null || email.isBlank()) {
              System.out.println("StaffRepository.findByEmail - Search email is null or blank, returning empty.");
              return Optional.empty(); // Prevent searching for null/blank email
         }

         List<Staff> allStaff = findAll(); // Calls method with logging/error handling
         System.out.println("StaffRepository.findByEmail - Total staff records loaded: " + allStaff.size());

         // Trim the search email once before the stream
         String trimmedEmail = email.trim();

         Optional<Staff> result = allStaff.stream()
                .filter(staff -> { // Added block for clarity and null checks
                    if (staff == null || staff.getEmail() == null) {
                        // System.out.println("StaffRepository.findByEmail - Skipping null staff or staff with null email."); // Verbose
                        return false; // Skip null staff or staff with null email
                    }
                    boolean match = staff.getEmail().equalsIgnoreCase(trimmedEmail);
                    // Optional verbose logging inside filter:
                    // System.out.println("StaffRepository.findByEmail - Comparing Filter: [" + trimmedEmail + "] with [" + staff.getEmail() + "] -> Match: " + match);
                    return match;
                })
                .findFirst();

         if(result.isPresent()) {
              System.out.println("StaffRepository.findByEmail - FOUND match for email: [" + trimmedEmail + "]");
         } else {
              System.out.println("StaffRepository.findByEmail - NO match found for email: [" + trimmedEmail + "]");
         }
         return result;
    }

    // Public save method uses internal methods with error handling
    public Staff save(Staff staff) {
         System.out.println("StaffRepository.save - Attempting to save staff: " + (staff != null ? staff.getEmail() : "null object"));
         if (staff == null) {
             System.err.println("StaffRepository.save - ERROR: Attempted to save null staff object.");
             return null;
         }
         try {
            List<Staff> staffList = findAllInternal(); // Get current list (logs read attempt)
            String staffId = staff.getId(); // Get ID before potentially setting it

            if (staffId == null || staffId.trim().isEmpty()) {
                // Create new staff
                long newIdNum = idCounter.incrementAndGet();
                staff.setId("S" + newIdNum);
                System.out.println("StaffRepository.save - Creating new staff with ID: " + staff.getId());
                staffList.add(staff);
            } else {
                // Update existing staff
                 System.out.println("StaffRepository.save - Attempting to update staff with ID: " + staffId);
                 final String finalStaffId = staffId; // Need final variable for lambda
                Optional<Staff> existingOpt = staffList.stream()
                        .filter(s -> s != null && finalStaffId.equals(s.getId())) // Added null check
                        .findFirst();

                if (existingOpt.isPresent()) {
                    System.out.println("StaffRepository.save - Found existing staff. Replacing entry.");
                    staffList.remove(existingOpt.get());
                    staffList.add(staff); // Add the updated staff object
                } else {
                    // Handle case where ID was provided but didn't match - treating as new add here
                    System.err.println("StaffRepository.save - WARNING: Provided staff ID [" + staffId + "] not found for update. Adding as new.");
                    staffList.add(staff); // Or throw exception if update must find existing
                }
            }
            saveAllInternal(staffList); // Save the modified list (logs save attempt)
            return staff;
        } catch (IOException e) {
             System.err.println("StaffRepository.save - ERROR saving staff: " + e.getMessage());
             e.printStackTrace(); // Print stack trace for save errors
             return null; // Indicate failure
        } catch (Exception e) { // Catch unexpected errors during save
             System.err.println("StaffRepository.save - UNEXPECTED ERROR saving staff: " + e.getMessage());
             e.printStackTrace();
             return null;
        }
    }

     // Public delete method uses internal methods with error handling
     public boolean deleteById(String id) {
          System.out.println("StaffRepository.deleteById - Attempting to delete staff with ID: [" + id + "]");
          if (id == null || id.isBlank()) {
                System.out.println("StaffRepository.deleteById - ID is null or blank. Cannot delete.");
                return false;
          }
         try {
            List<Staff> staffList = findAllInternal(); // Get current list
            // Use removeIf which returns true if the list was modified
            boolean removed = staffList.removeIf(staff -> staff != null && id.equals(staff.getId())); // Added null check

            if (removed) {
                System.out.println("StaffRepository.deleteById - Found and removed staff ID: [" + id + "]. Saving updated list.");
                saveAllInternal(staffList); // Save the list only if something was removed
            } else {
                 System.out.println("StaffRepository.deleteById - Staff ID [" + id + "] not found. No changes made.");
            }
            return removed;
        } catch (IOException e) {
             System.err.println("StaffRepository.deleteById - ERROR during delete/save operation for ID [" + id + "]: " + e.getMessage());
             e.printStackTrace();
             return false;
        } catch (Exception e) {
             System.err.println("StaffRepository.deleteById - UNEXPECTED ERROR during delete for ID [" + id + "]: " + e.getMessage());
             e.printStackTrace();
             return false;
        }
    }
}