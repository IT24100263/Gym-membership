package com.example.Gym.membership.repository;

import com.example.Gym.membership.model.MembershipPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class MembershipPlanRepository {

    private final Path filePath;
    private final AtomicLong idCounter = new AtomicLong(0); // For simple ID generation like P1, P2...

    public MembershipPlanRepository(@Value("${data.file.plans}") String filePath) {
        this.filePath = Paths.get(filePath);
        initializeIdCounter();
    }

    private void initializeIdCounter() {
        long maxId = 0;
        try {
            if (Files.exists(filePath) && Files.size(filePath) > 0) {
                List<MembershipPlan> plans = findAllInternal();
                for (MembershipPlan plan : plans) {
                    try {
                        // Assuming ID format is like "P1", "P2", etc.
                        if (plan.getId() != null && plan.getId().toUpperCase().startsWith("P")) {
                            long currentId = Long.parseLong(plan.getId().substring(1));
                            if (currentId > maxId) {
                                maxId = currentId;
                            }
                        }
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        System.err.println("Warning: Could not parse ID for plan: " + plan.getId());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not read plans file to initialize ID counter: " + e.getMessage());
        }
        this.idCounter.set(maxId);
    }

    private List<MembershipPlan> findAllInternal() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        return lines.stream()
                .map(MembershipPlan::fromCsvString)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void saveAllInternal(List<MembershipPlan> plans) throws IOException {
        Files.createDirectories(filePath.getParent());
        List<String> lines = plans.stream()
                .map(MembershipPlan::toCsvString)
                .collect(Collectors.toList());
        Files.write(filePath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

