package com.flowboard.workspace.controller;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public WorkspaceResponseDTO createWorkspace(@Valid @RequestBody WorkspaceRequestDTO dto) {
        return workspaceService.createWorkspace(dto);
    }

    @GetMapping("/{id}")
    public WorkspaceResponseDTO getWorkspace(@PathVariable Long id) {
        return workspaceService.getWorkspaceById(id);
    }

    @GetMapping("/user/{userId}")
    public List<WorkspaceResponseDTO> getUserWorkspaces(@PathVariable Long userId) {
        return workspaceService.getWorkspacesByUser(userId);
    }

    @PutMapping("/{id}")
    public WorkspaceResponseDTO updateWorkspace(@PathVariable Long id,
                                                @Valid @RequestBody WorkspaceRequestDTO dto) {
        return workspaceService.updateWorkspace(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id);
    }

    @PostMapping("/{id}/members")
    public MemberDTO addMember(@PathVariable Long id,
                               @RequestParam Long userId,
                               @RequestParam String role) {
        return workspaceService.addMember(id, userId, role);
    }

    @GetMapping("/{id}/members")
    public List<MemberDTO> getMembers(@PathVariable Long id) {
        return workspaceService.getMembers(id);
    }
}