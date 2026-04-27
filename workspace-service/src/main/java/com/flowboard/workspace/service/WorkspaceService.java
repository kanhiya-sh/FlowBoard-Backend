package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.*;
import java.util.List;

public interface WorkspaceService {
    WorkspaceResponseDTO createWorkspace(WorkspaceRequestDTO dto, Long ownerId);
    WorkspaceResponseDTO getWorkspaceById(Long workspaceId);
    List<WorkspaceResponseDTO> getWorkspacesByUser(Long userId);
    WorkspaceResponseDTO updateWorkspace(Long workspaceId, WorkspaceRequestDTO dto, Long requesterId);
    void deleteWorkspace(Long workspaceId, Long requesterId);
    MemberDTO addMember(Long workspaceId, Long userId, String role, Long requesterId);
    void removeMember(Long workspaceId, Long userId, Long requesterId);
    MemberDTO updateMemberRole(Long workspaceId, Long userId, String role, Long requesterId);
    List<MemberDTO> getMembers(Long workspaceId);
    // Internal check used by board-service
    InternalMemberCheckDTO checkMembership(Long workspaceId, Long userId);
}