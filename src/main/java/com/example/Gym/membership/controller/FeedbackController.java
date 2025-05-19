
package com.example.Gym.membership.controller;

import com.gymsystem.gymmembershipmanagement.model.Feedback;
import com.gymsystem.gymmembershipmanagement.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.lang.IllegalArgumentException;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // Helper to get Status values for forms/filters
    private List<String> getStatusNames() {
        return Arrays.stream(Feedback.Status.values())
                     .map(Enum::name)
                     .collect(Collectors.toList());
    }

    // --- Display Pages ---

    // GET /feedback/submit - Show feedback form (Member action)
    @GetMapping("/submit")
    public String showFeedbackForm(Model model /*, Principal principal */) {
         // Security: Get logged-in member ID from Principal/SecurityContext
         // String memberId = getMemberIdFromPrincipal(principal); // Needs implementation
         // if (memberId == null) { return "redirect:/login"; } // Redirect if not logged in

         Feedback feedback = new Feedback();
         // feedback.setMemberId(memberId); // Pre-populate member ID
         model.addAttribute("feedback", feedback);
         // Corresponds to templates/feedback/submit.html
         return "feedback/submit";
    }

    // GET /feedback - Show list of all feedback (Admin action)
    @GetMapping
    public String listAllFeedback(Model model, @RequestParam(required = false) String statusFilter) {
        List<Feedback> feedbackList;
        Feedback.Status filter = null;

         if (statusFilter != null && !statusFilter.isBlank()) {
             try {
                filter = Feedback.Status.valueOf(statusFilter.toUpperCase());
                feedbackList = feedbackService.getFeedbackByStatus(filter);
                model.addAttribute("currentFilter", filter.name());
             } catch (IllegalArgumentException e) {
                  System.err.println("Invalid status filter provided: " + statusFilter);
                  feedbackList = feedbackService.getAllFeedback(); // Show all if filter is invalid
                  model.addAttribute("filterError", "Invalid status filter: " + statusFilter);
             }
         } else {
             feedbackList = feedbackService.getAllFeedback(); // Default: show all
         }

        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("statuses", getStatusNames()); // For filter dropdown
         // Corresponds to templates/feedback/list.html
        return "feedback/list";
    }

    // --- Handle Actions ---

    // POST /feedback/submit - Process feedback submission

    @PostMapping("/submit")
    public String processFeedbackSubmission(@ModelAttribute Feedback feedback, RedirectAttributes redirectAttributes /*, Principal principal */) {
         // Security: Re-validate member ID against logged-in user
         // String loggedInMemberId = getMemberIdFromPrincipal(principal);
         // if (loggedInMemberId == null || !loggedInMemberId.equals(feedback.getMemberId())) {
         //     redirectAttributes.addFlashAttribute("errorMessage", "Authentication error.");
         //     return "redirect:/feedback/submit";
         // }

         // *** Temporary memberId setting until security is implemented ***
          if (feedback.getMemberId() == null || feedback.getMemberId().isBlank()){
              // In a real app, this MUST come from the logged-in user's session/token
              feedback.setMemberId("M-TEMP-USER"); // Placeholder - Replace with actual logged-in user ID
               System.err.println("WARNING: Using placeholder Member ID 'M-TEMP-USER' for feedback submission.");
          }
          // *** End Temporary setting ***


         try {
             feedbackService.submitFeedback(feedback);
             redirectAttributes.addFlashAttribute("successMessage", "Thank you for your feedback!");
             // Redirect to member dashboard or home page
             return "redirect:/";
         } catch (RuntimeException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Submission failed: " + e.getMessage());
             return "redirect:/feedback/submit"; // Back to form
         } catch (Exception e) {
             System.err.println("Error submitting feedback: " + e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
             return "redirect:/feedback/submit";
         }
    }

    // POST /feedback/delete/{id} - Delete feedback (Admin action)
    @PostMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable String id, RedirectAttributes redirectAttributes) {
         // Security: Add check for Admin role here
         try {
            boolean deleted = feedbackService.deleteFeedback(id);
             if (deleted) {
                redirectAttributes.addFlashAttribute("successMessage", "Feedback deleted successfully!");
             } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Feedback not found or could not be deleted.");
             }
         } catch (Exception e) {
              System.err.println("Error deleting feedback: " + e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while deleting feedback.");
         }
         return "redirect:/feedback"; // Redirect back to the feedback list
    }

     // POST /feedback/status/{id} - Update feedback status (Admin action)
    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable String id, @RequestParam String newStatus, RedirectAttributes redirectAttributes) {
        // Security: Add check for Admin role here
         try {
             Feedback.Status statusEnum = Feedback.Status.valueOf(newStatus.toUpperCase());
             feedbackService.updateFeedbackStatus(id, statusEnum);
             redirectAttributes.addFlashAttribute("successMessage", "Feedback status updated successfully!");
         } catch (IllegalArgumentException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Invalid status value provided.");
         } catch (RuntimeException e) {
              redirectAttributes.addFlashAttribute("errorMessage", "Update failed: " + e.getMessage());
         } catch (Exception e) {
             System.err.println("Error updating feedback status: " + e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
         }
         return "redirect:/feedback"; // Redirect back to the feedback list
    }
}