package com.flowboard.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.workspace.client.AuthServiceClient;
import com.flowboard.workspace.controller.WorkspaceController;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.enums.Role;
import com.flowboard.workspace.enums.Visibility;
import com.flowboard.workspace.service.WorkspaceService;
import com.flowboard.workspace.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WorkspaceService workspaceService;
    @MockBean private AuthServiceClient authServiceClient;
    @MockBean private JwtUtil jwtUtil;

    private WorkspaceResponseDTO mockWs(Long id) {
        return WorkspaceResponseDTO.builder()
                .workspaceId(id).name("WS").ownerId(1L).visibility(Visibility.PRIVATE).build();
    }

    @BeforeEach
    void setUp() {
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(1L); u.setEmail("u@e.com");
        when(authServiceClient.getUserByEmail("u@e.com")).thenReturn(u);
    }

    @Test
    void createWorkspace_returnsOk() throws Exception {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("WS"); req.setVisibility(Visibility.PRIVATE);
        when(workspaceService.createWorkspace(any(), eq(1L))).thenReturn(mockWs(5L));

        mockMvc.perform(post("/workspaces")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(5));
    }

    @Test
    void createWorkspace_invalidBody_returns400() throws Exception {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        mockMvc.perform(post("/workspaces")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWorkspace_noEmailAttr_returns500() throws Exception {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("WS"); req.setVisibility(Visibility.PRIVATE);
        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getWorkspace_returnsWs() throws Exception {
        when(workspaceService.getWorkspaceById(5L)).thenReturn(mockWs(5L));
        mockMvc.perform(get("/workspaces/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(5));
    }

    @Test
    void getMyWorkspaces_returnsList() throws Exception {
        when(workspaceService.getWorkspacesByUser(1L)).thenReturn(List.of(mockWs(1L)));
        mockMvc.perform(get("/workspaces/my").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getWorkspacesByUser_returnsList() throws Exception {
        when(workspaceService.getWorkspacesByUser(5L)).thenReturn(List.of(mockWs(1L)));
        mockMvc.perform(get("/workspaces/user/5"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicWorkspacesByUser_returnsList() throws Exception {
        when(workspaceService.getPublicWorkspacesByUser(5L)).thenReturn(List.of(mockWs(1L)));
        mockMvc.perform(get("/workspaces/user/5/public"))
                .andExpect(status().isOk());
    }

    @Test
    void updateWorkspace_returnsUpdated() throws Exception {
        WorkspaceRequestDTO req = new WorkspaceRequestDTO();
        req.setName("X"); req.setVisibility(Visibility.PRIVATE);
        when(workspaceService.updateWorkspace(eq(5L), any(), eq(1L))).thenReturn(mockWs(5L));

        mockMvc.perform(put("/workspaces/5")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteWorkspace_returnsOk() throws Exception {
        doNothing().when(workspaceService).deleteWorkspace(5L, 1L);
        mockMvc.perform(delete("/workspaces/5").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void addMember_returnsOk() throws Exception {
        when(workspaceService.addMember(eq(1L), eq(5L), eq("MEMBER"), eq(1L)))
                .thenReturn(MemberDTO.builder().userId(5L).role(Role.MEMBER).build());

        mockMvc.perform(post("/workspaces/1/members")
                        .requestAttr("userEmail", "u@e.com")
                        .param("userId", "5").param("role", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    void removeMember_returnsOk() throws Exception {
        doNothing().when(workspaceService).removeMember(1L, 5L, 1L);
        mockMvc.perform(delete("/workspaces/1/members/5").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void updateMemberRole_returnsOk() throws Exception {
        when(workspaceService.updateMemberRole(eq(1L), eq(5L), eq("ADMIN"), eq(1L)))
                .thenReturn(MemberDTO.builder().userId(5L).role(Role.ADMIN).build());

        mockMvc.perform(put("/workspaces/1/members/5/role")
                        .requestAttr("userEmail", "u@e.com")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getMembers_returnsList() throws Exception {
        when(workspaceService.getMembers(1L))
                .thenReturn(List.of(MemberDTO.builder().userId(5L).role(Role.MEMBER).build()));
        mockMvc.perform(get("/workspaces/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void checkMembership_returnsResult() throws Exception {
        when(workspaceService.checkMembership(1L, 5L))
                .thenReturn(new InternalMemberCheckDTO(true, "MEMBER"));

        mockMvc.perform(get("/workspaces/internal/1/members/5/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMember").value(true))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void listMembersInternal_returnsList() throws Exception {
        when(workspaceService.getMembers(1L))
                .thenReturn(List.of(MemberDTO.builder().userId(5L).role(Role.MEMBER).build()));
        mockMvc.perform(get("/workspaces/internal/1/members"))
                .andExpect(status().isOk());
    }
}
