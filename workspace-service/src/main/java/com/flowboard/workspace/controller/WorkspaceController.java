package com.flowboard.workspace.controller;

import com.flowboard.workspace.client.AuthServiceClient;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final AuthServiceClient authServiceClient;

    /**
     * Reads email from request attribute (set by JwtAuthenticationFilter)
     * then calls Auth service (via Feign) to get the full user object with userId.
     */
    private Long resolveUserId(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) {
            throw new IllegalStateException("User email not found in request. Is JWT filter running?");
        }
        try {
            UserResponseDTO user = authServiceClient.getUserByEmail(email);
            if (user == null || user.getUserId() == null) {
                throw new IllegalStateException("Could not resolve user from Auth service for email: " + email);
            }
            return user.getUserId();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to resolve userId for email {}: {}", email, e.getMessage());
            throw new IllegalStateException("Auth service unavailable: " + e.getMessage());
        }
    }

    // ─── Workspace CRUD ──────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<WorkspaceResponseDTO> createWorkspace(
            @Valid @RequestBody WorkspaceRequestDTO dto,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(workspaceService.createWorkspace(dto, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> getWorkspace(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getWorkspaceById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<WorkspaceResponseDTO>> getMyWorkspaces(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(workspaceService.getWorkspacesByUser(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkspaceResponseDTO>> getWorkspacesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(workspaceService.getWorkspacesByUser(userId));
    }

    @GetMapping("/user/{userId}/public")
    public ResponseEntity<List<WorkspaceResponseDTO>> getPublicWorkspacesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(workspaceService.getPublicWorkspacesByUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceRequestDTO dto,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(workspaceService.updateWorkspace(id, dto, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteWorkspace(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        workspaceService.deleteWorkspace(id, userId);
        return ResponseEntity.ok("Workspace deleted successfully");
    }

    // ─── Workspace Members ───────────────────────────────────────────────────────

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberDTO> addMember(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String role,
            HttpServletRequest request) {
        Long requesterId = resolveUserId(request);
        return ResponseEntity.ok(workspaceService.addMember(id, userId, role, requesterId));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<String> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long requesterId = resolveUserId(request);
        workspaceService.removeMember(id, userId, requesterId);
        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<MemberDTO> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam String role,
            HttpServletRequest request) {
        Long requesterId = resolveUserId(request);
        return ResponseEntity.ok(workspaceService.updateMemberRole(id, userId, role, requesterId));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberDTO>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getMembers(id));
    }

    // ─── Internal Endpoint (called by Board Service — no JWT needed) ─────────────

    @GetMapping("/internal/{workspaceId}/members/{userId}/check")
    public ResponseEntity<InternalMemberCheckDTO> checkMembership(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(workspaceService.checkMembership(workspaceId, userId));
    }

    // Internal service-to-service members listing used by board-service to build
    // the "assignable users" set for a board. Mirrors the public /members
    // endpoint but lives under /internal/* so it bypasses gateway JWT filtering.
    @GetMapping("/internal/{workspaceId}/members")
    public ResponseEntity<List<MemberDTO>> listMembersInternal(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(workspaceService.getMembers(workspaceId));
    }
}
