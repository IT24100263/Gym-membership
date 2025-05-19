package com.example.Gym.membership.repository;

import com.gymsystem.gymmembershipmanagement.model.Feedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Repository
public class FeedbackRepository {

    private final Path filePath;
    private final AtomicLong idCounter = new AtomicLong(0); // For ID generation like F1, F2...

    public FeedbackRepository(@Value("${data.file.feedback}") String filePath) {
        this.filePath = Paths.get(filePath);
        initializeIdCounter();
    }

    private void initializeIdCounter() {
         long maxId = 0;
         try {
             if (Files.exists(filePath) && Files.size(filePath) > 0) {
                 List<Feedback> feedbackList = findAllInternal();
                 for (Feedback feedback : feedbackList) {
                     try {
                         // Assuming ID format "F1", "F2"...
                         if (feedback.getFeedbackId() != null && feedback.getFeedbackId().toUpperCase().startsWith("F")) {
                             long currentId = Long.parseLong(feedback.getFeedbackId().substring(1));
                             if (currentId > maxId) {
                                 maxId = currentId;
                             }
                         }
                     } catch (NumberFormatException | IndexOutOfBoundsException e) {
                         System.err.println("Warning: Could not parse ID for feedback: " + feedback.getFeedbackId());
                     }
                 }
             }
         } catch (IOException e) {
             System.err.println("Warning: Could not read feedback file to initialize ID counter: " + e.getMessage());
         }
         this.idCounter.set(maxId);
    }


    private List<Feedback> findAllInternal() throws IOException {
         if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        // IMPORTANT: Standard Files.readAllLines and String.split() will likely break
        // if comments/subjects contain commas. A proper CSV parsing library (like OpenCSV or Apache Commons CSV)
        // should be used here for robustness in a real application.
        // We proceed with the simple (fragile) approach for consistency in this example.
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        return lines.stream()
                    .map(Feedback::fromCsvString)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
    }

    private void saveAllInternal(List<Feedback> feedbackList) throws IOException {
        Files.createDirectories(filePath.getParent());
        // Use the CSV conversion method from the Feedback model
        List<String> lines = feedbackList.stream()
                                    .map(Feedback::toCsvString)
                                    .collect(Collectors.toList());
         // IMPORTANT: Ensure toCsvString correctly handles quoting/escaping for saving.
        Files.write(filePath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // --- CRUD Methods ---

    public List<Feedback> findAll() {
        try {
            // Sort by submission time descending (most recent first)
             return findAllInternal().stream()
                     .sorted(Comparator.comparing(Feedback::getSubmissionTime).reversed())
                     .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading feedback file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

     public Optional<Feedback> findById(String feedbackId) {
        return findAll().stream() // findAll already sorts, if needed unsorted, call findAllInternal
                .filter(feedback -> feedback.getFeedbackId().equals(feedbackId))
                .findFirst();
    }

    // Find all feedback submitted by a specific member
    public List<Feedback> findByMemberId(String memberId) {
         return findAll().stream()
                .filter(feedback -> feedback.getMemberId().equals(memberId))
                // Sorting is already done by findAll()
                .collect(Collectors.toList());
    }

     // Find feedback by status
    public List<Feedback> findByStatus(Feedback.Status status) {
         return findAll().stream()
                 .filter(feedback -> feedback.getStatus() == status)
                 // Sorting is already done by findAll()
                 .collect(Collectors.toList());
    }


    public Feedback save(Feedback feedback) {
         try {
            List<Feedback> feedbackList = findAllInternal(); // Get unsorted list for modification
            if (feedback.getFeedbackId() == null || feedback.getFeedbackId().trim().isEmpty()) {
                // Create new feedback
                long newIdNum = idCounter.incrementAndGet();
                feedback.setFeedbackId("F" + newIdNum);
                // Set default status and time if not already set
                if (feedback.getSubmissionTime() == null) {
                     feedback.setSubmissionTime(LocalDateTime.now());
                }
                if (feedback.getStatus() == null) {
                    feedback.setStatus(Feedback.Status.NEW);
                }
                feedbackList.add(feedback);
            } else {
                // Update existing feedback (e.g., admin changing status)
                Optional<Feedback> existingOpt = feedbackList.stream()
                        .filter(f -> f.getFeedbackId().equals(feedback.getFeedbackId()))
                        .findFirst();
                if (existingOpt.isPresent()) {
                    feedbackList.remove(existingOpt.get());
                    feedbackList.add(feedback); // Add the updated object
                } else {
                    System.err.println("Warning: Trying to update non-existent feedback ID: " + feedback.getFeedbackId());
                     feedbackList.add(feedback); // Or throw exception
                }
            }
            saveAllInternal(feedbackList);
            return feedback;
        } catch (IOException e) {
             System.err.println("Error saving feedback: " + e.getMessage());
             return null; // Indicate failure
        }
    }

    public boolean deleteById(String feedbackId) {
        try {
            List<Feedback> feedbackList = findAllInternal();
            boolean removed = feedbackList.removeIf(feedback -> feedback.getFeedbackId().equals(feedbackId));
            if (removed) {
                saveAllInternal(feedbackList);
            }
            return removed;
        } catch (IOException e) {
             System.err.println("Error deleting feedback: " + e.getMessage());
             return false;
        }
    }
}