package com.example.Gym.membership.controller;

import com.example.Gym.membership.model.MembershipPlan;
import com.example.Gym.membership.service.MembershipPlanService;

// Use the correct import for HttpSession based on your Spring Boot version
import jakarta.servlet.http.HttpSession; // For Spring Boot 3+
// import javax.servlet.http.HttpSession; // For Spring Boot 2.x

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
// Explicitly import exceptions if needed
import java.lang.IllegalArgumentException;
import java.lang.RuntimeException;
import java.lang.IllegalStateException;


@Controller
@RequestMapping("/plans")
public class MembershipPlanController {

    private final MembershipPlanService planService;

    @Autowired
    public MembershipPlanController(MembershipPlanService planService) {
        this.planService = planService;
    }

    // --- Helper Method for Admin Check ---
    private boolean isAdmin(HttpSession session) {
        Object role = session.getAttribute("userRole");
        return role != null && "ADMIN".equalsIgnoreCase(role.toString());
    }

    // --- Display Pages ---

    @GetMapping
    public String listPlans(Model model, HttpSession session, @RequestParam(name = "context", required = false) String context) {
        boolean isAdminAccessing = isAdmin(session);
        boolean allowAdd = "admin".equalsIgnoreCase(context) && isAdminAccessing;

        model.addAttribute("plans", planService.getAllPlans());
        model.addAttribute("isAdmin", isAdminAccessing);
        model.addAttribute("allowAddPlan", allowAdd);

        System.out.println("[DEBUG] MembershipPlanController.listPlans - isAdmin: " + isAdminAccessing + ", context: " + context + ", allowAddPlan: " + allowAdd);
        return "plans/list";
    }

    // GET /plans/add - Show form to add a new plan (Admin Only)
    @GetMapping("/add")
    public String showAddPlanForm(Model model, HttpSession session) { // Added session
        // Security Check
        if (!isAdmin(session)) {
            // Optionally add flash message "Access Denied"
            return "redirect:/"; // Redirect non-admins
        }
        model.addAttribute("plan", new MembershipPlan()); // Empty plan object for form binding
        return "plans/add"; // Renders templates/plans/add.html
    }

    @GetMapping("/edit/{id}")
    public String showEditPlanForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) { return "redirect:/plans"; } // Security check
        Optional<MembershipPlan> planOpt = planService.getPlanById(id);
        if (planOpt.isPresent()) {
            model.addAttribute("plan", planOpt.get());
            return "plans/edit";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Plan not found with ID: " + id);
            return "redirect:/plans?context=admin";
        }
    }

    // --- Handle Form Submissions ---

    // POST /plans/add - Process adding a new plan (Admin Only)
    @PostMapping("/add")
    public String processAddPlan(@ModelAttribute MembershipPlan plan, RedirectAttributes redirectAttributes, HttpSession session) { // Added session
        // Security Check
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
            return "redirect:/";
        }

        try {
            // Service layer should perform detailed validation
            planService.addPlan(plan);
            redirectAttributes.addFlashAttribute("successMessage", "Plan added successfully!");
            // Redirect back to the plan list with admin context
            return "redirect:/plans?context=admin";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add plan: " + e.getMessage());
            // Add the submitted plan back to flash attributes to repopulate form
            redirectAttributes.addFlashAttribute("plan", plan);
            return "redirect:/plans/add"; // Redirect back to the add form GET mapping
        } catch (Exception e) {
            System.err.println("Error adding plan: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while adding the plan.");
            redirectAttributes.addFlashAttribute("plan", plan); // Keep data on unexpected error too
            return "redirect:/plans/add";
        }
    }

    // POST /plans/edit/{id} - Process editing (Admin Only)
    @PostMapping("/edit/{id}")
    public String processEditPlan(@PathVariable String id, @ModelAttribute MembershipPlan plan, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) { return "redirect:/"; } // Security check
        try {
            planService.updatePlan(id, plan);
            redirectAttributes.addFlashAttribute("successMessage", "Plan updated successfully!");
            return "redirect:/plans?context=admin";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update plan: " + e.getMessage());
            // Add plan object to flash attributes if you want edit form to retain failed input
            // redirectAttributes.addFlashAttribute("plan", plan);
            return "redirect:/plans/edit/" + id;
        } catch (Exception e) {
            System.err.println("Error updating plan ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while updating the plan.");
            return "redirect:/plans/edit/" + id;
        }
    }

    // POST /plans/delete/{id} - Process deleting (Admin Only)
    @PostMapping("/delete/{id}")
    public String deletePlan(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) { return "redirect:/"; } // Security check
        try {
            boolean deleted = planService.deletePlan(id);
            if (deleted) { redirectAttributes.addFlashAttribute("successMessage", "Plan deleted successfully!"); }
            else { redirectAttributes.addFlashAttribute("errorMessage", "Could not delete plan (Plan not found)."); }
        } catch (IllegalStateException e) { redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete plan: " + e.getMessage());
        } catch (Exception e) { System.err.println("Error deleting plan ID " + id + ": " + e.getMessage()); e.printStackTrace(); redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while deleting the plan."); }
        return "redirect:/plans?context=admin";
    }
}
