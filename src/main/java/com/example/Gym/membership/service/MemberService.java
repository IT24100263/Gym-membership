package com.example.Gym.membership.service;

import com.gymsystem.gymmembershipmanagement.model.Member;
import com.gymsystem.gymmembershipmanagement.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> getMemberById(String id) {
        return memberRepository.findById(id);
    }

    public Optional<Member> getMemberByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String trimmedEmail = email.trim();
        if (trimmedEmail.isEmpty()) {
            return Optional.empty();
        }
        return memberRepository.findByEmail(trimmedEmail);
    }

    public Member registerMember(Member member) {
        if (member == null || member.getEmail() == null || member.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Member email cannot be empty.");
        }
        String trimmedEmail = member.getEmail().trim();
        member.setEmail(trimmedEmail);

        if (memberRepository.findByEmail(trimmedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + trimmedEmail);
        }
        if (member.getPassword() == null || member.getPassword().trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        String trimmedPassword = member.getPassword().trim();
        member.setPassword(trimmedPassword); // Storing plain text password (INSECURE)

        if (member.getId() == null || member.getId().isEmpty()) {
            member.setId(UUID.randomUUID().toString());
        }
        return memberRepository.save(member);
    }

    public Member updateMember(String id, Member updatedMemberDetails) {
        if (updatedMemberDetails == null) {
            throw new IllegalArgumentException("Updated member details cannot be null.");
        }
        updatedMemberDetails.setId(id);
        Optional<Member> existingMemberOpt = memberRepository.findById(id);
        if (existingMemberOpt.isEmpty()) {
            throw new RuntimeException("Member not found with id: " + id);
        }
        Member existingMember = existingMemberOpt.get();

        if (updatedMemberDetails.getEmail() != null && !updatedMemberDetails.getEmail().trim().isEmpty()) {
            String trimmedEmail = updatedMemberDetails.getEmail().trim();
            Optional<Member> memberWithEmail = memberRepository.findByEmail(trimmedEmail);
            if (memberWithEmail.isPresent() && !memberWithEmail.get().getId().equals(id)) {
                throw new IllegalArgumentException("Email " + trimmedEmail + " is already used by another member.");
            }
            existingMember.setEmail(trimmedEmail);
        }

        if (updatedMemberDetails.getName() != null && !updatedMemberDetails.getName().trim().isEmpty()) {
            existingMember.setName(updatedMemberDetails.getName().trim());
        }
        if (updatedMemberDetails.getPhone() != null && !updatedMemberDetails.getPhone().trim().isEmpty()) {
            existingMember.setPhone(updatedMemberDetails.getPhone().trim());
        }
        if (updatedMemberDetails.getMembershipPlanId() != null && !updatedMemberDetails.getMembershipPlanId().trim().isEmpty()) {
            existingMember.setMembershipPlanId(updatedMemberDetails.getMembershipPlanId().trim());
        }

        if (updatedMemberDetails.getPassword() != null && !updatedMemberDetails.getPassword().trim().isEmpty()) {
            String newPassword = updatedMemberDetails.getPassword().trim();
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters long.");
            }
            existingMember.setPassword(newPassword); // Storing plain text password (INSECURE)
        }

        return memberRepository.save(existingMember);
    }

    public boolean deleteMember(String id) {
        Optional<Member> memberToDelete = memberRepository.findById(id);
        return memberToDelete.map(member -> memberRepository.deleteById(id)).orElse(false);
    }

    public Optional<Member> login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return Optional.empty();
        }
        Optional<Member> memberOpt = getMemberByEmail(email);

        return memberOpt.filter(member -> member.getPassword().equals(rawPassword)); // Plain text comparison (INSECURE)
    }
}