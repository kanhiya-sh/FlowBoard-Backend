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
}
