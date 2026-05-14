package com.flowboard.workspace;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.enums.Role;
import com.flowboard.workspace.enums.Visibility;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.client.AuthServiceClient;
import com.flowboard.workspace.messaging.NotificationPublisher;
import com.flowboard.workspace.repository.WorkspaceRepository;
import com.flowboard.workspace.serviceImpl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private NotificationPublisher notificationPublisher;
    @InjectMocks private WorkspaceServiceImpl workspaceService;

    private Workspace testWorkspace;
    private WorkspaceMember ownerMember;

    @BeforeEach
    void setUp() {
        testWorkspace = new Workspace();
        testWorkspace.setWorkspaceId(1L);
        testWorkspace.setName("Test Workspace");
        testWorkspace.setOwnerId(1L);
        testWorkspace.setVisibility(Visibility.PRIVATE);

        ownerMember = new WorkspaceMember();
        ownerMember.setWorkspaceId(1L);
        ownerMember.setUserId(1L);
        ownerMember.setRole(Role.ADMIN);
    }

    @Test
    void createWorkspace_savesAndReturns() {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("New Workspace");

        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(ownerMember);

        WorkspaceResponseDTO result = workspaceService.createWorkspace(req, 1L);
        assertNotNull(result);
        verify(workspaceRepository).save(any(Workspace.class));
    }

    @Test
    void getWorkspaceById_returnsWorkspace() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        WorkspaceResponseDTO result = workspaceService.getWorkspaceById(1L);
        assertNotNull(result);
        assertEquals("Test Workspace", result.getName());
    }

    @Test
    void getWorkspaceById_notFound_throws() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> workspaceService.getWorkspaceById(99L));
    }

    @Test
    void deleteWorkspace_ownerCanDelete() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        workspaceService.deleteWorkspace(1L, 1L);
        verify(workspaceRepository).delete(testWorkspace);
    }

    @Test
    void deleteWorkspace_nonOwner_throws() {
        testWorkspace.setOwnerId(2L);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> workspaceService.deleteWorkspace(1L, 1L));
    }

    // ─── getWorkspacesByUser ──────────────────────────────────────────────────

    @Test
    void getWorkspacesByUser_returnsList() {
        when(workspaceMemberRepository.findByUserId(1L)).thenReturn(List.of(ownerMember));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        List<WorkspaceResponseDTO> result = workspaceService.getWorkspacesByUser(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getWorkspacesByUser_workspaceMissing_throws() {
        when(workspaceMemberRepository.findByUserId(1L)).thenReturn(List.of(ownerMember));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.getWorkspacesByUser(1L));
    }

    @Test
    void getPublicWorkspacesByUser_filtersToPublicOnly() {
        Workspace publicWs = new Workspace();
        publicWs.setWorkspaceId(2L); publicWs.setVisibility(Visibility.PUBLIC); publicWs.setOwnerId(1L);
        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspaceId(2L); m2.setUserId(1L); m2.setRole(Role.MEMBER);

        when(workspaceMemberRepository.findByUserId(1L)).thenReturn(List.of(ownerMember, m2));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace)); // PRIVATE
        when(workspaceRepository.findById(2L)).thenReturn(Optional.of(publicWs));

        List<WorkspaceResponseDTO> result = workspaceService.getPublicWorkspacesByUser(1L);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getWorkspaceId());
    }

    @Test
    void getPublicWorkspacesByUser_skipsMissingWorkspaces() {
        when(workspaceMemberRepository.findByUserId(1L)).thenReturn(List.of(ownerMember));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.empty());

        assertEquals(0, workspaceService.getPublicWorkspacesByUser(1L).size());
    }

    // ─── updateWorkspace ──────────────────────────────────────────────────────

    @Test
    void updateWorkspace_ownerCanUpdate() {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("Renamed"); req.setDescription("Desc"); req.setVisibility(Visibility.PUBLIC);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponseDTO result = workspaceService.updateWorkspace(1L, req, 1L);
        assertEquals("Renamed", result.getName());
    }

    @Test
    void updateWorkspace_adminMemberCanUpdate() {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("Renamed"); req.setVisibility(Visibility.PUBLIC);
        testWorkspace.setOwnerId(99L);
        WorkspaceMember admin = new WorkspaceMember();
        admin.setWorkspaceId(1L); admin.setUserId(5L); admin.setRole(Role.ADMIN);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceId(1L)).thenReturn(List.of(admin));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponseDTO result = workspaceService.updateWorkspace(1L, req, 5L);
        assertEquals("Renamed", result.getName());
    }

    @Test
    void updateWorkspace_nonOwnerNonAdmin_throws() {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("X"); req.setVisibility(Visibility.PUBLIC);
        testWorkspace.setOwnerId(99L);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceId(1L)).thenReturn(List.of());

        assertThrows(com.flowboard.workspace.exception.UnauthorizedException.class,
                () -> workspaceService.updateWorkspace(1L, req, 5L));
    }

    @Test
    void updateWorkspace_blankName_keepsOld() {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("   "); req.setVisibility(Visibility.PUBLIC);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponseDTO result = workspaceService.updateWorkspace(1L, req, 1L);
        assertEquals("Test Workspace", result.getName());
    }

    @Test
    void updateWorkspace_notFound_throws() {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("X"); req.setVisibility(Visibility.PRIVATE);
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.updateWorkspace(99L, req, 1L));
    }

    // ─── deleteWorkspace ──────────────────────────────────────────────────────

    @Test
    void deleteWorkspace_notFound_throws() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.deleteWorkspace(99L, 1L));
    }

    @Test
    void deleteWorkspace_otherUser_throws() {
        testWorkspace.setOwnerId(1L);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        assertThrows(com.flowboard.workspace.exception.UnauthorizedException.class,
                () -> workspaceService.deleteWorkspace(1L, 99L));
    }

    // ─── addMember ────────────────────────────────────────────────────────────

    @Test
    void addMember_success_publishesNotification() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 5L)).thenReturn(false);
        WorkspaceMember saved = new WorkspaceMember();
        saved.setWorkspaceId(1L); saved.setUserId(5L); saved.setRole(Role.MEMBER);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(saved);

        MemberDTO result = workspaceService.addMember(1L, 5L, "MEMBER", 1L);
        assertEquals(5L, result.getUserId());
        verify(notificationPublisher).publish(any());
    }

    @Test
    void addMember_alreadyMember_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 5L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> workspaceService.addMember(1L, 5L, "MEMBER", 1L));
    }

    @Test
    void addMember_workspaceNotFound_throws() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.addMember(99L, 5L, "MEMBER", 1L));
    }

    @Test
    void addMember_authClientFails_doesNotBreak() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 5L)).thenReturn(false);
        WorkspaceMember saved = new WorkspaceMember();
        saved.setWorkspaceId(1L); saved.setUserId(5L); saved.setRole(Role.MEMBER);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(saved);
        when(authServiceClient.getUserById(5L)).thenThrow(new RuntimeException("down"));

        MemberDTO result = workspaceService.addMember(1L, 5L, "MEMBER", 1L);
        assertEquals(5L, result.getUserId());
    }

    // ─── removeMember ─────────────────────────────────────────────────────────

    @Test
    void removeMember_success() {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(1L); member.setUserId(5L); member.setRole(Role.MEMBER);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 5L)).thenReturn(Optional.of(member));

        workspaceService.removeMember(1L, 5L, 1L);
        verify(workspaceMemberRepository).delete(member);
    }

    @Test
    void removeMember_owner_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        assertThrows(IllegalArgumentException.class,
                () -> workspaceService.removeMember(1L, 1L, 1L));
    }

    @Test
    void removeMember_memberNotFound_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.removeMember(1L, 5L, 1L));
    }

    @Test
    void removeMember_workspaceNotFound_throws() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.removeMember(99L, 5L, 1L));
    }

    // ─── updateMemberRole ─────────────────────────────────────────────────────

    @Test
    void updateMemberRole_success() {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(1L); member.setUserId(5L); member.setRole(Role.MEMBER);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 5L)).thenReturn(Optional.of(member));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberDTO result = workspaceService.updateMemberRole(1L, 5L, "ADMIN", 1L);
        assertEquals(Role.ADMIN, result.getRole());
    }

    @Test
    void updateMemberRole_memberNotFound_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.updateMemberRole(1L, 5L, "ADMIN", 1L));
    }

    // ─── getMembers ───────────────────────────────────────────────────────────

    @Test
    void getMembers_returnsList() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceId(1L)).thenReturn(List.of(ownerMember));

        assertEquals(1, workspaceService.getMembers(1L).size());
    }

    @Test
    void getMembers_workspaceNotFound_throws() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.workspace.exception.ResourceNotFoundException.class,
                () -> workspaceService.getMembers(99L));
    }

    @Test
    void getMembers_resolvesUserDetailsFromAuth() {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(1L); user.setEmail("a@b.com"); user.setFullName("Alice"); user.setUsername("alice");
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceId(1L)).thenReturn(List.of(ownerMember));
        when(authServiceClient.getUserById(1L)).thenReturn(user);

        List<MemberDTO> result = workspaceService.getMembers(1L);
        assertEquals("Alice", result.get(0).getFullName());
    }

    // ─── checkMembership ──────────────────────────────────────────────────────

    @Test
    void checkMembership_isMember_returnsTrue() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(ownerMember));

        InternalMemberCheckDTO result = workspaceService.checkMembership(1L, 1L);
        assertTrue(result.isMember());
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void checkMembership_notMember_returnsFalse() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        InternalMemberCheckDTO result = workspaceService.checkMembership(1L, 99L);
        assertFalse(result.isMember());
        assertNull(result.getRole());
    }

    @Test
    void checkMembership_workspaceMissing_returnsFalse() {
        when(workspaceRepository.existsById(99L)).thenReturn(false);

        InternalMemberCheckDTO result = workspaceService.checkMembership(99L, 1L);
        assertFalse(result.isMember());
    }
}
