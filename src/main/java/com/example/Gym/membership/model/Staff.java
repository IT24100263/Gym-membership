package com.example.Gym.membership.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Staff extends User {

    // Constructor
    public Staff(String id, String name, String email, String password, String role) {
        super(id, name, email, password, role);
    }

    // --- CSV Handling specific to Staff ---
    public String toCsvString() {
        // Order: id, name, email, password, role
        return String.join(",",
                id != null ? id : "",
                name != null ? name.replace(",",";") : "", // Basic escaping
                email != null ? email : "",
                password != null ? password : "", // HASHED password ideally
                role != null ? role : ""
        );
    }

    public static Staff fromCsvString(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        // Expect 5 fields
        String[] fields = csvLine.split(",", 5);
        if (fields.length < 5) {
            System.err.println("Staff.fromCsvString - WARNING: Skipping malformed CSV line for Staff: \"" + csvLine + "\". Expected 5 fields, got " + fields.length);
            return null;
        }
        try {
            String id = fields[0];
            String name = fields[1].replace(";",","); // Basic un-escaping
            String email = fields[2]; // Email is at index 2
            String password = fields[3];
            String role = fields[4];

            // Logging the parsed email
            System.out.println("Staff.fromCsvString - Parsed Email: [" + email + "] from line: [" + csvLine + "]");

            if (email == null || email.trim().isEmpty()) {
                 System.err.println("Staff.fromCsvString - WARNING: Skipping staff record with blank email in line: \"" + csvLine + "\"");
                 return null;
            }
            // Create the Staff object using the constructor, trimming email/role
            return new Staff(id, name, email.trim(), password, role.trim());
        } catch (ArrayIndexOutOfBoundsException e) {
             System.err.println("Staff.fromCsvString - ERROR: Array index out of bounds while parsing Staff CSV line: \"" + csvLine + "\" - Error: " + e.getMessage());
             return null;
        }
        catch (Exception e) {
             System.err.println("Staff.fromCsvString - ERROR: Unexpected error parsing Staff CSV line: \"" + csvLine + "\" - Error: " + e.getMessage());
             return null;
        }
    }
}