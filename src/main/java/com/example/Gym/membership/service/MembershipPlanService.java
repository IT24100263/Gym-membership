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

    public MembershipPlan updatePlan(String id, MembershipPlan updatedPlanDetails) {
        updatedPlanDetails.setId(id); // Ensure ID is consistent
        if (planRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Plan not found with id: " + id); // Custom exception preferred
        }
        // Add validation for updated details
        if (updatedPlanDetails.getPrice() == null || updatedPlanDetails.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (updatedPlanDetails.getDurationMonths() <= 0) {
            throw new IllegalArgumentException("Duration must be positive.");
        }

        // Optional: Check if name is being changed to one that already exists (excluding itself)
        boolean nameCollision = planRepository.findAll().stream()
                .anyMatch(p -> !p.getId().equals(id) && p.getName().equalsIgnoreCase(updatedPlanDetails.getName()));
        if (nameCollision) {
            throw new IllegalArgumentException("Another plan with the name '" + updatedPlanDetails.getName() + "' already exists.");
        }

        return planRepository.save(updatedPlanDetails);
    }

    public boolean deletePlan(String id) {
        // **Business Logic Example:** Prevent deletion if plan is in use by members
        // boolean isInUse = memberRepository.findAll().stream()
        //                    .anyMatch(member -> id.equals(member.getMembershipPlanId()));
        // if (isInUse) {
        //     throw new IllegalStateException("Cannot delete plan ID " + id + " as it is currently assigned to members.");
        // }

        // If the check above is implemented, remove the simple delete from repository and call it here.
        if (planRepository.findById(id).isEmpty()) {
            System.err.println("Attempted to delete non-existent plan ID: " + id);
            return false; // Or throw not found exception
        }

        return planRepository.deleteById(id); // Keep this if no dependency check is needed or handled differently
    }
}



