package com.example.Gym.membership.controller;

import com.gymsystem.gymmembershipmanagement.model.Member;
import com.gymsystem.gymmembershipmanagement.model.PaymentRecord;
import com.gymsystem.gymmembershipmanagement.service.MemberService;
import com.gymsystem.gymmembershipmanagement.service.MembershipPlanService;
import com.gymsystem.gymmembershipmanagement.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final MembershipPlanService planService;
    private final PaymentService paymentService;

    @Autowired
    public MemberController(MemberService memberService, MembershipPlanService planService, PaymentService paymentService) {
        this.memberService = memberService;
        this.planService = planService;
        this.paymentService = paymentService;
    }

    private boolean isAdmin(HttpSession session) {
        Object role = session.getAttribute("userRole");
        return role != null && "ADMIN".equalsIgnoreCase(role.toString());
    }

    private void addAvailablePlansToModel(Model model) {
        model.addAttribute("availablePlans", planService.getAllPlans());
    }

    @GetMapping
    public String listMembers(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/";
        model.addAttribute("members", memberService.getAllMembers());
        return "members/list";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("member", new Member());
        model.addAttribute("isPaymentDue", false);
        addAvailablePlansToModel(model);
        return "members/register";
    }

    @GetMapping("/profile/{id}")
    public String showProfileForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        Member loggedInMemberFromSession = (Member) session.getAttribute("loggedInMember");
        boolean isAdminUser = isAdmin(session);
        boolean isOwnProfile = loggedInMemberFromSession != null && loggedInMemberFromSession.getId() != null && loggedInMemberFromSession.getId().equals(id);
        boolean canAccess = isOwnProfile || isAdminUser;

        if (!canAccess) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access Denied.");
            return "redirect:/";
        }

        Optional<Member> memberOpt = memberService.getMemberById(id);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            model.addAttribute("member", member);
            addAvailablePlansToModel(model);
            List<PaymentRecord> paymentHistory = paymentService.getPaymentsForMember(member.getId());
            PaymentRecord.PaymentStatus currentStatus = paymentService.getLatestPaymentStatusForMember(member.getId());
            model.addAttribute("paymentHistory", paymentHistory);
            model.addAttribute("paymentStatus", currentStatus);
            model.addAttribute("isPaymentDue", currentStatus != PaymentRecord.PaymentStatus.PAID);
            // Explicitly add loggedInMember to the model
            model.addAttribute("loggedInMember", loggedInMemberFromSession);
            model.addAttribute("isAdmin", isAdminUser); // Add isAdmin status to the model
            return "members/profile";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Member not found with ID: " + id);
            return isAdminUser ? "redirect:/members" : "redirect:/";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "members/login";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute Member member, RedirectAttributes redirectAttributes) {
        try {
            member.setId(UUID.randomUUID().toString());
            memberService.registerMember(member);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/members/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/members/register";
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
            return "redirect:/members/register";
        }
    }

    @PostMapping("/login")
    public String processMemberLogin(@RequestParam String email, @RequestParam String password, RedirectAttributes redirectAttributes, HttpSession session) {
        Optional<Member> memberOpt = memberService.login(email, password);

        if (memberOpt.isPresent()) {
            Member loggedInMember = memberOpt.get();
            session.setAttribute("loggedInMember", loggedInMember);
            session.setMaxInactiveInterval(1800);
            redirectAttributes.addFlashAttribute("successMessage", "Login successful! Welcome " + loggedInMember.getName());
            return "redirect:/members/profile/" + loggedInMember.getId();
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid email or password.");
            return "redirect:/members/login";
        }
    }

    @PostMapping("/profile/{id}")
    public String processProfileUpdate(@PathVariable String id, @ModelAttribute Member member, RedirectAttributes redirectAttributes, HttpSession session) {
        Member loggedInMemberFromSession = (Member) session.getAttribute("loggedInMember");
        boolean isAdminUser = isAdmin(session);
        boolean isOwnProfile = loggedInMemberFromSession != null && loggedInMemberFromSession.getId() != null && loggedInMemberFromSession.getId().equals(id);
        boolean canAccess = isOwnProfile || isAdminUser;
        if (!canAccess) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access Denied.");
            return "redirect:/";
        }

        try {
            memberService.updateMember(id, member);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/members/profile/" + id;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Update failed: " + e.getMessage());
            return "redirect:/members/profile/" + id;
        } catch (Exception e) {
            System.err.println("Error updating profile: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
            return "redirect:/members/profile/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteMember(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access Denied.");
            return "redirect:/";
        }
        try {
            boolean deleted = memberService.deleteMember(id);
            if (deleted) {
                redirectAttributes.addFlashAttribute("successMessage", "Member deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Could not delete member (Member not found).");
            }
        } catch (Exception e) {
            System.err.println("Error deleting member: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting member: " + e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/logout")
    public String memberLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("loggedInMember");
        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out.");
        return "redirect:/";
    }

    // Inside MemberController.java

    // ... (other imports and methods remain the same) ...

     // POST /members/payment/initiate - Creates a PENDING payment record
     @PostMapping("/payment/initiate")
     public String initiatePayment(HttpSession session, RedirectAttributes redirectAttributes) {
         Member loggedInMember = (Member) session.getAttribute("loggedInMember");
         if (loggedInMember == null || loggedInMember.getId() == null) {
             redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to make a payment.");
             return "redirect:/members/login";
         }
         String memberId = loggedInMember.getId();

         try {
              PaymentRecord pendingPayment = paymentService.initiatePaymentForMember(memberId);
              // ===>>> CHANGED: Redirect to the new payment form page with the paymentId <<<===
              return "redirect:/members/payment/form/" + pendingPayment.getPaymentId();
         } catch(IllegalStateException e) {
              redirectAttributes.addFlashAttribute("errorMessage", "Could not initiate payment: " + e.getMessage());
              return "redirect:/members/profile/" + memberId;
         } catch (Exception e) {
              System.err.println("Error initiating payment for member " + memberId + ": " + e.getMessage());
              e.printStackTrace();
              redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while initiating payment.");
               return "redirect:/members/profile/" + memberId;
         }
     }

    // ===>>> NEW: GET mapping to show the simulated payment form <<<===
    @GetMapping("/payment/form/{paymentId}")
    public String showPaymentForm(@PathVariable String paymentId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Member loggedInMember = (Member) session.getAttribute("loggedInMember");
        // In PaymentService, ensure you have a method like getPaymentById
        Optional<PaymentRecord> paymentOpt = paymentService.getPaymentById(paymentId); // Assuming this method exists

        // Authorization: Ensure logged-in member owns this payment record
        if (loggedInMember == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to proceed.");
            return "redirect:/members/login";
        }
        if (paymentOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment record not found.");
            return "redirect:/members/profile/" + loggedInMember.getId();
        }

        PaymentRecord paymentRecord = paymentOpt.get();
        if (!loggedInMember.getId().equals(paymentRecord.getMemberId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access to payment record.");
            return "redirect:/members/profile/" + loggedInMember.getId();
        }

        // Only show form if payment is PENDING or OVERDUE
        if (paymentRecord.getStatus() != PaymentRecord.PaymentStatus.PENDING &&
            paymentRecord.getStatus() != PaymentRecord.PaymentStatus.OVERDUE) {
            redirectAttributes.addFlashAttribute("infoMessage", "This payment has already been processed or is not currently due.");
            return "redirect:/members/profile/" + loggedInMember.getId();
        }

        model.addAttribute("paymentRecord", paymentRecord);
        // Add attributes for navbar (ensure your navbar fragment uses these)
        model.addAttribute("isMemberLoggedIn", true);
        model.addAttribute("loggedInUserName", loggedInMember.getName());
        model.addAttribute("loggedInMemberId", loggedInMember.getId());
        // model.addAttribute("isAdmin", isAdmin(session)); // If admin can somehow view this page

        return "payment/payment-form"; // Path to the new HTML template
    }

    // POST /members/payment/success/{id} and POST /members/payment/fail/{id}
    // should remain as previously defined to handle the form submissions from payment-form.html
    // ...
}