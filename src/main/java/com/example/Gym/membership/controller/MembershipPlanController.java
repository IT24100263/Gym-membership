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
