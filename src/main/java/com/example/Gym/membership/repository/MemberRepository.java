package com.example.Gym.membership.repository;

import com.gymsystem.gymmembershipmanagement.model.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class MemberRepository {

    private final Path filePath;

    public MemberRepository(@Value("${data.file.members}") String filePath) {
        this.filePath = Paths.get(filePath);
        try {
            Files.createDirectories(this.filePath.getParent());
        } catch (IOException e) {
            System.err.println("Could not create directory for members file: " + e.getMessage());
        }
    }

    private List<Member> findAllInternal() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        return lines.stream()
                .map(Member::fromCsvString)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void saveAllInternal(List<Member> members) throws IOException {
        Files.write(filePath, members.stream()
                        .map(Member::toCsvString)
                        .collect(Collectors.toList()),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public List<Member> findAll() {
        try {
            return findAllInternal();
        } catch (IOException e) {
            System.err.println("Error reading members file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<Member> findById(String id) {
        return findAll().stream()
                .filter(member -> member.getId().equals(id))
                .findFirst();
    }

    public Optional<Member> findByEmail(String email) {
        return findAll().stream()
                .filter(member -> member.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public Member save(Member member) {
        try {
            List<Member> members = findAllInternal();
            if (member.getId() == null || member.getId().isEmpty()) {
                member.setId(UUID.randomUUID().toString());
                members.add(member);
            } else {
                members.removeIf(m -> m.getId().equals(member.getId()));
                members.add(member);
            }
            saveAllInternal(members);
            return member;
        } catch (IOException e) {
            System.err.println("Error saving member: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteById(String id) {
        try {
            List<Member> members = findAllInternal();
            boolean removed = members.removeIf(member -> member.getId().equals(id));
            if (removed) {
                saveAllInternal(members);
            }
            return removed;
        } catch (IOException e) {
            System.err.println("Error deleting member: " + e.getMessage());
            return false;
        }
    }
}