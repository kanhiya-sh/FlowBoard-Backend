package com.flowboard.workspace.serviceImpl;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.*;
import com.flowboard.workspace.enums.Role;
import com.flowboard.workspace.exception.ResourceNotFoundException;
import com.flowboard.workspace.repository.*;
import com.flowboard.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    @Override
    public WorkspaceResponseDTO createWorkspace(WorkspaceRequestDTO dto) {

        Workspace workspace = Workspace.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .ownerId(dto.getOwnerId())
                .visibility(dto.getVisibility())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Workspace saved = workspaceRepository.save(workspace);

        WorkspaceMember owner = WorkspaceMember.builder()
                .workspaceId(saved.getWorkspaceId())
                .userId(saved.getOwnerId())
                .role(Role.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(owner);

        return mapToResponseDTO(saved);
    }

    @Override
    public WorkspaceResponseDTO getWorkspaceById(Long workspaceId) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id " + workspaceId));

        return mapToResponseDTO(workspace);
    }

    @Override
    public List<WorkspaceResponseDTO> getWorkspacesByUser(Long userId) {

        List<WorkspaceMember> memberships = memberRepository.findByUserId(userId);

        return memberships.stream()
                .map(m -> workspaceRepository.findById(m.getWorkspaceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id " + m.getWorkspaceId())))
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    public WorkspaceResponseDTO updateWorkspace(Long workspaceId, WorkspaceRequestDTO dto) {

        Workspace existing = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id " + workspaceId));

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setVisibility(dto.getVisibility());
        existing.setUpdatedAt(LocalDateTime.now());

        Workspace updated = workspaceRepository.save(existing);

        return mapToResponseDTO(updated);
    }

    @Override
    public void deleteWorkspace(Long workspaceId) {

        Workspace existing = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id " + workspaceId));

        workspaceRepository.delete(existing);
    }

    @Override
    public MemberDTO addMember(Long workspaceId, Long userId, String role) {

        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id " + workspaceId));

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .role(Role.valueOf(role))
                .joinedAt(LocalDateTime.now())
                .build();

        WorkspaceMember saved = memberRepository.save(member);

        return mapToMemberDTO(saved);
    }

    @Override
    public void removeMember(Long workspaceId, Long userId) {

        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);

        WorkspaceMember member = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        memberRepository.delete(member);
    }

    @Override
    public MemberDTO updateMemberRole(Long workspaceId, Long userId, String role) {

        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);

        WorkspaceMember member = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        member.setRole(Role.valueOf(role));

        WorkspaceMember updated = memberRepository.save(member);

        return mapToMemberDTO(updated);
    }

    @Override
    public List<MemberDTO> getMembers(Long workspaceId) {

        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id " + workspaceId));

        return memberRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(this::mapToMemberDTO)
                .toList();
    }

    private WorkspaceResponseDTO mapToResponseDTO(Workspace workspace) {
        return WorkspaceResponseDTO.builder()
                .workspaceId(workspace.getWorkspaceId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerId(workspace.getOwnerId())
                .visibility(workspace.getVisibility())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    private MemberDTO mapToMemberDTO(WorkspaceMember member) {
        return MemberDTO.builder()
                .userId(member.getUserId())
                .role(member.getRole())
                .build();
    }
}