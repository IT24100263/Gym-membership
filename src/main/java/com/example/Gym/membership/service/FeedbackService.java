package com.example.Gym.membership.service;

import com.gymsystem.gymmembershipmanagement.model.Feedback;
import com.gymsystem.gymmembershipmanagement.repository.FeedbackRepository;
import com.gymsystem.gymmembershipmanagement.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository; // To check if member exists

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository, MemberRepository memberRepository) {
        this.feedbackRepository = feedbackRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Allows a member to submit feedback.
     * Requires memberId to be set on the feedback object.
     * @param feedback The Feedback object to submit.
     * @return The saved Feedback object with generated ID, timestamp, and status.
     * @throws RuntimeException if the member ID is invalid or not found.
     * @throws IllegalArgumentException if comments are empty.
     */
    public Feedback submitFeedback(Feedback feedback) {
        // Validation
        if (feedback.getMemberId() == null || memberRepository.findById(feedback.getMemberId()).isEmpty()) {
            throw new RuntimeException("Invalid or non-existent Member ID provided for feedback: " + feedback.getMemberId());
        }
        if (feedback.getComments() == null || feedback.getComments().trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback comments cannot be empty.");
        }
        if (feedback.getSubject() == null || feedback.getSubject().trim().isEmpty()) {
             feedback.setSubject("General Feedback"); // Default subject
         }
         if (feedback.getRating() < 0 || feedback.getRating() > 5) { // Assuming 1-5 rating, 0 for N/A
             System.err.println("Warning: Invalid rating provided ("+ feedback.getRating() +"). Setting to 0.");
             feedback.setRating(0); // Default rating if invalid
         }

        // Set server-side defaults (overwrite client-side attempts)
        feedback.setFeedbackId(null); // Ensure repository generates ID
        feedback.setSubmissionTime(LocalDateTime.now());
        feedback.setStatus(Feedback.Status.NEW);

        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll(); // Repository sorts by date desc
    }

    public List<Feedback> getFeedbackByMember(String memberId) {
         // Optional: check if member exists first
         if (memberRepository.findById(memberId).isEmpty()) {
             throw new RuntimeException("Cannot get feedback for non-existent Member ID: " + memberId);
         }
        return feedbackRepository.findByMemberId(memberId);
    }

    public Optional<Feedback> getFeedbackById(String feedbackId) {
        return feedbackRepository.findById(feedbackId);
    }

    public List<Feedback> getFeedbackByStatus(Feedback.Status status) {
         return feedbackRepository.findByStatus(status);
    }


    /**
     * Updates the status of a feedback item (Admin action).
     * @param feedbackId The ID of the feedback to update.
     * @param newStatus The new status to set.
     * @return The updated Feedback object.
     * @throws RuntimeException if feedback is not found.
     */
    public Feedback updateFeedbackStatus(String feedbackId, Feedback.Status newStatus) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with ID: " + feedbackId));

        feedback.setStatus(newStatus);
        return feedbackRepository.save(feedback); // Save updates the existing record
    }


    /**
     * Deletes a feedback item (Admin action).
     * @param feedbackId The ID of the feedback to delete.
     * @return true if deleted, false otherwise.
     */
    public boolean deleteFeedback(String feedbackId) {
        // Optional: Add checks if needed before deletion
        if (feedbackRepository.findById(feedbackId).isEmpty()) {
             System.err.println("Attempt to delete non-existent feedback ID: " + feedbackId);
             return false;
        }
        return feedbackRepository.deleteById(feedbackId);
    }
}