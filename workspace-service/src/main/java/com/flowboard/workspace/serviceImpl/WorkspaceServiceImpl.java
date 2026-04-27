package com.flowboard.workspace.serviceImpl;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.*;
import com.flowboard.workspace.enums.Role;
import com.flowboard.workspace.exception.ResourceNotFoundException;
import com.flowboard.workspace.exception.UnauthorizedException;
import com.flowboard.workspace.repository.*;
import com.flowboard.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    @Override
    @Transactional
    public WorkspaceResponseDTO createWorkspace(WorkspaceRequestDTO dto, Long ownerId) {

        Workspace workspace = Workspace.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .ownerId(ownerId)
                .visibility(dto.getVisibility())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Workspace saved = workspaceRepository.save(workspace);
        workspaceRepository.flush(); // Force DB write immediately

        log.info("Workspace saved to DB: id={}, name={}, owner={}",
                saved.getWorkspaceId(), saved.getName(), ownerId);

        // Creator automatically becomes ADMIN member
        WorkspaceMember owner = WorkspaceMember.builder()
                .workspaceId(saved.getWorkspaceId())
                .userId(ownerId)
                .role(Role.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(owner);

        log.info("WorkspaceMember saved: workspaceId={}, userId={}", saved.getWorkspaceId(), ownerId);

        return mapToResponseDTO(saved);
    }

    @Override
    public WorkspaceResponseDTO getWorkspaceById(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));
        return mapToResponseDTO(workspace);
    }

    @Override
    public List<WorkspaceResponseDTO> getWorkspacesByUser(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(m -> workspaceRepository.findById(m.getWorkspaceId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Workspace not found: " + m.getWorkspaceId())))
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceResponseDTO updateWorkspace(Long workspaceId, WorkspaceRequestDTO dto, Long requesterId) {
        Workspace existing = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        boolean isAdminMember = memberRepository.findByWorkspaceId(workspaceId).stream()
                .anyMatch(m -> m.getUserId().equals(requesterId) && m.getRole() == Role.ADMIN);

        if (!existing.getOwnerId().equals(requesterId) && !isAdminMember) {
            throw new UnauthorizedException("You do not have permission to update this workspace");
        }

        if (dto.getName() != null && !dto.getName().isBlank()) existing.setName(dto.getName());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getVisibility() != null) existing.setVisibility(dto.getVisibility());
        existing.setUpdatedAt(LocalDateTime.now());

        return mapToResponseDTO(workspaceRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long workspaceId, Long requesterId) {
        Workspace existing = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        if (!existing.getOwnerId().equals(requesterId)) {
            throw new UnauthorizedException("Only the workspace owner can delete it");
        }

        memberRepository.deleteAll(memberRepository.findByWorkspaceId(workspaceId));
        workspaceRepository.delete(existing);
        log.info("Workspace deleted: id={} by userId={}", workspaceId, requesterId);
    }

    @Override
    @Transactional
    public MemberDTO addMember(Long workspaceId, Long userId, String role, Long requesterId) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        boolean alreadyMember = memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
        if (alreadyMember) {
            throw new IllegalArgumentException(
                    "User " + userId + " is already a member of this workspace");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .role(Role.valueOf(role.toUpperCase()))
                .joinedAt(LocalDateTime.now())
                .build();

        return mapToMemberDTO(memberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Long workspaceId, Long userId, Long requesterId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        if (workspace.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove the workspace owner");
        }

        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found in workspace " + workspaceId));

        memberRepository.delete(member);
    }

    @Override
    @Transactional
    public MemberDTO updateMemberRole(Long workspaceId, Long userId, String role, Long requesterId) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found in workspace " + workspaceId));

        member.setRole(Role.valueOf(role.toUpperCase()));
        return mapToMemberDTO(memberRepository.save(member));
    }

    @Override
    public List<MemberDTO> getMembers(Long workspaceId) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        return memberRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::mapToMemberDTO)
                .toList();
    }

    @Override
    public InternalMemberCheckDTO checkMembership(Long workspaceId, Long userId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            log.warn("checkMembership: workspace {} does not exist", workspaceId);
            return new InternalMemberCheckDTO(false, null);
        }

        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(m -> {
                    log.debug("checkMembership: user {} IS member of workspace {} with role {}",
                            userId, workspaceId, m.getRole().name());
                    return new InternalMemberCheckDTO(true, m.getRole().name());
                })
                .orElseGet(() -> {
                    log.debug("checkMembership: user {} is NOT member of workspace {}",
                            userId, workspaceId);
                    return new InternalMemberCheckDTO(false, null);
                });
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────────

    private WorkspaceResponseDTO mapToResponseDTO(Workspace w) {
        return WorkspaceResponseDTO.builder()
                .workspaceId(w.getWorkspaceId())
                .name(w.getName())
                .description(w.getDescription())
                .ownerId(w.getOwnerId())
                .visibility(w.getVisibility())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    private MemberDTO mapToMemberDTO(WorkspaceMember m) {
        return MemberDTO.builder()
                .userId(m.getUserId())
                .role(m.getRole())
                .build();
    }
}
