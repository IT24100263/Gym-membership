package com.example.Gym.membership.controller;

import com.gymsystem.gymmembershipmanagement.model.Staff;
import com.gymsystem.gymmembershipmanagement.model.PaymentRecord; // Import PaymentRecord
import com.gymsystem.gymmembershipmanagement.service.StaffService;
import com.gymsystem.gymmembershipmanagement.service.PaymentService; // Import PaymentService

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.lang.IllegalArgumentException;
import java.lang.RuntimeException;
import java.lang.IllegalStateException;


@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;
    private final PaymentService paymentService; // <-- Inject PaymentService

    private static final List<String> AVAILABLE_ROLES = Arrays.asList("ADMIN", "FRONT_DESK", "TRAINER");

    @Autowired
    public StaffController(StaffService staffService, PaymentService paymentService) { // Add to constructor
        this.staffService = staffService;
        this.paymentService = paymentService; // Initialize
    }

    // --- Helper Method for Admin Check ---
    private boolean isAdmin(HttpSession session) {
        Object role = session.getAttribute("userRole");
        return role != null && "ADMIN".equalsIgnoreCase(role.toString());
    }

    // --- Helper to add roles to model ---
    private void addRolesToModel(Model model) {
         model.addAttribute("availableRoles", AVAILABLE_ROLES);
    }


    // --- Staff Management Endpoints (as before, ensure security checks) ---
    @GetMapping
    public String listStaff(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
        model.addAttribute("staffList", staffService.getAllStaff());
        return "staff/list";
    }

    @GetMapping("/add")
    public String showAddStaffForm(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
        model.addAttribute("staff", new Staff());
        addRolesToModel(model);
        return "staff/add";
    }

     @GetMapping("/edit/{id}")
    public String showEditStaffForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
         if (!isAdmin(session)) return "redirect:/";
        Optional<Staff> staffOpt = staffService.getStaffById(id);
        if (staffOpt.isPresent()) {
            model.addAttribute("staff", staffOpt.get());
             addRolesToModel(model);
            return "staff/edit";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff not found with ID: " + id);
            return "redirect:/staff";
        }
    }

    @GetMapping("/login")
    public String showStaffLoginForm() {
        return "staff/login";
    }

    @GetMapping("/dashboard")
    public String showStaffDashboard(Model model, HttpSession session ) {
        Object user = session.getAttribute("loggedInStaff");
        String userRole = (String) session.getAttribute("userRole");
        if (user == null || !(user instanceof Staff)) { return "redirect:/staff/login"; }
        model.addAttribute("isStaffLoggedIn", true);
        model.addAttribute("staffName", ((Staff) user).getName());
        model.addAttribute("isAdmin", "ADMIN".equalsIgnoreCase(userRole));
        return "staff/dashboard";
    }

    @PostMapping("/add")
    public String processAddStaff(@ModelAttribute Staff staff, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
        try {
            staffService.registerStaff(staff);
            redirectAttributes.addFlashAttribute("successMessage", "Staff member added successfully!");
            return "redirect:/staff";
        } catch (IllegalArgumentException e) { redirectAttributes.addFlashAttribute("errorMessage", "Failed to add staff: " + e.getMessage()); return "redirect:/staff/add"; } catch (Exception e) { System.err.println("Error adding staff: " + e.getMessage()); e.printStackTrace(); redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred."); return "redirect:/staff/add"; }
    }

    @PostMapping("/edit/{id}")
    public String processEditStaff(@PathVariable String id, @ModelAttribute Staff staff, RedirectAttributes redirectAttributes, HttpSession session) {
         if (!isAdmin(session)) return "redirect:/";
         try {
             staffService.updateStaff(id, staff);
             redirectAttributes.addFlashAttribute("successMessage", "Staff member updated successfully!");
             return "redirect:/staff";
         } catch (RuntimeException e) { redirectAttributes.addFlashAttribute("errorMessage", "Failed to update staff: " + e.getMessage()); return "redirect:/staff/edit/" + id; } catch (Exception e) { System.err.println("Error updating staff: " + e.getMessage()); e.printStackTrace(); redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred."); return "redirect:/staff/edit/" + id; }
    }

    @PostMapping("/delete/{id}")
    public String deleteStaff(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
         try {
            boolean deleted = staffService.deleteStaff(id);
            if (deleted) { redirectAttributes.addFlashAttribute("successMessage", "Staff member deleted successfully!"); }
             else { redirectAttributes.addFlashAttribute("errorMessage", "Could not delete staff member (maybe not found)."); }
        } catch (IllegalStateException e) { redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete staff: " + e.getMessage()); } catch (Exception e) { System.err.println("Error deleting staff: " + e.getMessage()); e.printStackTrace(); redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while deleting staff."); }
        return "redirect:/staff";
    }

    @PostMapping("/login")
    public String processStaffLogin(@RequestParam String email, @RequestParam String password, RedirectAttributes redirectAttributes, HttpSession session ) {
        System.out.println("Attempting staff login for email: " + email);
        Optional<Staff> staffOpt = staffService.login(email, password);
        if (staffOpt.isPresent()) {
            Staff loggedInStaff = staffOpt.get();
            System.out.println("Staff login successful for: " + loggedInStaff.getName() + " with role " + loggedInStaff.getRole());
            session.setAttribute("loggedInStaff", loggedInStaff);
            session.setAttribute("userRole", loggedInStaff.getRole());
            session.setMaxInactiveInterval(1800);
            redirectAttributes.addFlashAttribute("successMessage", "Staff login successful! Welcome " + loggedInStaff.getName());
            return "redirect:/staff/dashboard";
        } else {
            System.out.println("Staff login failed for email: " + email);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid email or password.");
            return "redirect:/staff/login";
        }
    }

     @GetMapping("/logout")
     public String staffLogout(HttpSession session, RedirectAttributes redirectAttributes) {
         session.removeAttribute("loggedInStaff");
         session.removeAttribute("userRole");
         redirectAttributes.addFlashAttribute("successMessage", "You have been logged out.");
         return "redirect:/";
     }

    // --- ADDED: Payment Management Endpoints (Admin Only) ---

    @GetMapping("/payments")
    public String listPayments(Model model, HttpSession session,
                               @RequestParam(required = false) String statusFilter) {
        if (!isAdmin(session)) return "redirect:/"; // Security check

        List<PaymentRecord> payments;
        PaymentRecord.PaymentStatus filter = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                filter = PaymentRecord.PaymentStatus.valueOf(statusFilter.toUpperCase());
                payments = paymentService.getPaymentRecordsByStatus(filter);
                 model.addAttribute("currentFilter", filter.name());
            } catch (IllegalArgumentException e) {
                 System.err.println("Invalid payment status filter: " + statusFilter);
                 payments = paymentService.getAllPaymentRecords(); // Default to all on bad filter
                 model.addAttribute("filterError", "Invalid status: " + statusFilter);
            }
        } else {
            payments = paymentService.getAllPaymentRecords(); // Default: show all
        }

        model.addAttribute("payments", payments);
        model.addAttribute("statuses", PaymentRecord.PaymentStatus.values()); // For filter dropdown

        return "payments/admin-list"; // Path to the new admin view template
    }

    @PostMapping("/payments/mark-paid/{id}")
    public String markAsPaid(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
         if (!isAdmin(session)) return "redirect:/"; // Security check

         try {
             paymentService.markPaymentAsPaid(id);
             redirectAttributes.addFlashAttribute("successMessage", "Payment record " + id + " marked as PAID.");
         } catch (IllegalStateException e) {
              redirectAttributes.addFlashAttribute("errorMessage", "Could not mark as paid: " + e.getMessage());
         } catch (Exception e) {
             System.err.println("Error marking payment " + id + " as paid: " + e.getMessage());
             e.printStackTrace();
              redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
         }
        // Redirect back to the payment list
        return "redirect:/staff/payments";
    }
}