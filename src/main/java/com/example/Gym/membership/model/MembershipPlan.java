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
