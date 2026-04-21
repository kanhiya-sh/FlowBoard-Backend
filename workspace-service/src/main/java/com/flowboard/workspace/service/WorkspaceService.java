package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.*;

import java.util.List;

public interface WorkspaceService {

    WorkspaceResponseDTO createWorkspace(WorkspaceRequestDTO dto);

    WorkspaceResponseDTO getWorkspaceById(Long workspaceId);

    List<WorkspaceResponseDTO> getWorkspacesByUser(Long userId);

    WorkspaceResponseDTO updateWorkspace(Long workspaceId, WorkspaceRequestDTO dto);

    void deleteWorkspace(Long workspaceId);

    MemberDTO addMember(Long workspaceId, Long userId, String role);

    void removeMember(Long workspaceId, Long userId);

    MemberDTO updateMemberRole(Long workspaceId, Long userId, String role);

    List<MemberDTO> getMembers(Long workspaceId);
}