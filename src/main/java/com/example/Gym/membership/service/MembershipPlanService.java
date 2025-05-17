package com.example.Gym.membership.service;

import com.example.Gym.membership.model.MembershipPlan;
import com.example.Gym.membership.repository.MembershipPlanRepository;
// Optional: Inject MemberRepository if needed for checks (like checking if plan is in use)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MembershipPlanService {

    private final MembershipPlanRepository planRepository;
    // Optional: private final MemberRepository memberRepository;

    @Autowired
    public MembershipPlanService(MembershipPlanRepository planRepository /*, MemberRepository memberRepository */) {
        this.planRepository = planRepository;
        // this.memberRepository = memberRepository;
    }

    public List<MembershipPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public Optional<MembershipPlan> getPlanById(String id) {
        return planRepository.findById(id);
    }

    public MembershipPlan addPlan(MembershipPlan plan) {
        // Add validation: e.g., check if name already exists? Price > 0? Duration valid?
        if (plan.getPrice() == null || plan.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (plan.getDurationMonths() <= 0) {
            throw new IllegalArgumentException("Duration must be positive.");
        }
        // Check for duplicate names (case-insensitive example)
        boolean nameExists = planRepository.findAll().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(plan.getName()));
        if(nameExists) {
            throw new IllegalArgumentException("Plan name '" + plan.getName() + "' already exists.");
        }

        return planRepository.save(plan);
    }

