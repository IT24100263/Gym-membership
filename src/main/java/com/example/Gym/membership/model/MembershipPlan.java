package com.example.Gym.membership.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // Use BigDecimal for currency

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlan {
    private String id; // e.g., "P001", "P-BASIC"
    private String name; // e.g., "Basic Monthly", "Premium Annual"
    private String description;
    private int durationMonths; // Duration of the plan
    private BigDecimal price; // Use BigDecimal for accurate money handling

    // Add other relevant fields: features list, access restrictions, etc.

    // Helper method for CSV conversion
    public String toCsvString() {
        // Ensure consistent order and handle potential commas in description
        // Basic example - requires proper CSV escaping for production
        return String.join(",",
                id,
                name,
                "\"" + description.replace("\"", "\"\"") + "\"", // Basic quoting for description
                String.valueOf(durationMonths),
                price.toPlainString()); // Use toPlainString to avoid scientific notation
    }

    // Static factory method to create MembershipPlan from CSV
    public static MembershipPlan fromCsvString(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        // Basic CSV splitting - fragile if commas exist in quoted fields
        // A proper CSV library is recommended for robust parsing
        String[] fields = csvLine.split(",", 5); // Adjust limit
        if (fields.length < 5) {
            System.err.println("Warning: Skipping malformed CSV line for Plan: " + csvLine);
            return null;
        }
        try {
            String desc = fields[2];
            // Basic de-quoting (remove surrounding quotes if present)
            if (desc.startsWith("\"") && desc.endsWith("\"")) {
                desc = desc.substring(1, desc.length() - 1).replace("\"\"", "\"");
            }
            return new MembershipPlan(
                    fields[0],
                    fields[1],
                    desc,
                    Integer.parseInt(fields[3]),
                    new BigDecimal(fields[4])
            );
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse number fields for Plan CSV: " + csvLine + " - Error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Warning: Error parsing Plan CSV line: " + csvLine + " - Error: " + e.getMessage());
            return null;
        }
    }
}