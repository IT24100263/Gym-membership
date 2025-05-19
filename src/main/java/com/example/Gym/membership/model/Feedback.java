package com.example.Gym.membership.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.lang.NumberFormatException;
import java.lang.IllegalArgumentException; // <-- Added this import

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    public enum Status { NEW, READ, RESOLVED, ARCHIVED } // Possible feedback statuses

    private String feedbackId; // e.g., F001
    private String memberId; // Who submitted it
    private LocalDateTime submissionTime;
    private int rating; // Optional: e.g., 1-5 stars. Use 0 or -1 if not applicable.
    private String subject; // Optional: e.g., "Class Timetable", "Locker Room Cleanliness"
    private String comments;
    private Status status; // Track admin processing

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Helper method for CSV conversion
    public String toCsvString() {
        // Quote fields that might contain commas (subject, comments)
        // Ensure proper CSV escaping for robustness if needed
        return String.join(",",
                feedbackId != null ? feedbackId : "",
                memberId != null ? memberId : "",
                submissionTime != null ? submissionTime.format(ISO_FORMATTER) : "",
                String.valueOf(rating),
                "\"" + (subject != null ? subject.replace("\"", "\"\"") : "") + "\"",
                "\"" + (comments != null ? comments.replace("\"", "\"\"") : "") + "\"",
                status != null ? status.name() : "" // Store enum name as string, handle null status
        );
    }

    // Static factory method to create Feedback from CSV
    public static Feedback fromCsvString(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        // Need a more robust CSV parser for quoted fields
        // Simple split is highly likely to fail if comments/subject have commas
        // For DEMO purposes, assuming simple splitting works OR using a library is required.
        // Let's proceed with simple split, acknowledging its limitations.
        // Adjust split limit based on number of fields (7)
        String[] fields = csvLine.split(",", 7);
        if (fields.length < 7) {
            System.err.println("Warning: Skipping malformed CSV line for Feedback: " + csvLine + ". Expected 7 fields, got " + fields.length);
            return null;
        }
        try {
             // Basic de-quoting (remove surrounding quotes if present) - very basic!
            String subject = fields[4];
            if (subject.length() >= 2 && subject.startsWith("\"") && subject.endsWith("\"")) {
                subject = subject.substring(1, subject.length() - 1).replace("\"\"", "\"");
            }
             String comments = fields[5];
            if (comments.length() >= 2 && comments.startsWith("\"") && comments.endsWith("\"")) {
                comments = comments.substring(1, comments.length() - 1).replace("\"\"", "\"");
            }

            // Added trim() to handle potential whitespace around enum name
            Status statusEnum = Status.valueOf(fields[6].trim().toUpperCase());

            return new Feedback(
                    fields[0],                      // feedbackId
                    fields[1],                      // memberId
                    LocalDateTime.parse(fields[2]), // submissionTime (Assuming ISO format from save)
                    Integer.parseInt(fields[3]),    // rating
                    subject,                        // subject
                    comments,                       // comments
                    statusEnum                      // status
            );
        // Catch specific exceptions first
        } catch (DateTimeParseException e) {
             System.err.println("Warning: Could not parse Submission Time for Feedback CSV: '" + fields[2] + "' in line: " + csvLine + " - Error: " + e.getMessage());
             return null;
        } catch (NumberFormatException e) {
             System.err.println("Warning: Could not parse Rating for Feedback CSV: '" + fields[3] + "' in line: " + csvLine + " - Error: " + e.getMessage());
             return null;
        } catch (IllegalArgumentException e) { // Catch errors from Status.valueOf() or other potential issues
             System.err.println("Warning: Could not parse Status or invalid argument for Feedback CSV: '" + fields[6] + "' in line: " + csvLine + " - Error: " + e.getMessage());
             return null;
        } catch (Exception e) { // Catch any other unexpected errors during parsing
             System.err.println("Warning: Generic error parsing Feedback CSV line: " + csvLine + " - Error: " + e.getMessage());
             return null;
        }
    }
}