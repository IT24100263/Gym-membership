package com.example.Gym.membership.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a gym member.
 * Includes essential fields for registration, login, and management.
 */
@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Generates no-args constructor
@AllArgsConstructor // Lombok: Generates all-args constructor
public class Member {

    private String id; // Unique identifier (e.g., M001)
    private String name;
    private String email;
    private String password; // Stores the hashed password
    private String phone;
    private String membershipPlanId; // Foreign key to MembershipPlan

    public String toCsvString() {
        return String.join(",",
                id != null ? id : "",
                name != null ? name.replace(",", ";") : "", // Basic escaping
                email != null ? email : "",
                password != null ? password : "", // Store HASHED password
                phone != null ? phone : "",
                membershipPlanId != null ? membershipPlanId : ""
        );
    }

    public static Member fromCsvString(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        String[] fields = csvLine.split(",", 6);
        if (fields.length < 6) {
            System.err.println("Warning: Skipping malformed CSV line for Member: \"" + csvLine + "\". Expected 6 fields, got " + fields.length);
            return null;
        }
        try {
            String name = fields[1].replace(";", ",");
            return new Member(fields[0], name, fields[2], fields[3], fields[4], fields[5]);
        } catch (Exception e) {
            System.err.println("Warning: Error parsing Member CSV line: \"" + csvLine + "\" - Error: " + e.getMessage());
            return null;
        }
    }
}