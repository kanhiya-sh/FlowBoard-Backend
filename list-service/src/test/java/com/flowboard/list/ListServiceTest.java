package com.flowboard.list;

import com.flowboard.list.dto.*;
import com.flowboard.list.entity.TaskList;
import com.flowboard.list.client.AuthServiceClient;
import com.flowboard.list.client.BoardServiceClient;
import com.flowboard.list.repository.ListRepository;
import com.flowboard.list.serviceImpl.ListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListServiceTest {

    @Mock private ListRepository listRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private BoardServiceClient boardServiceClient;
    @InjectMocks private ListServiceImpl listService;

    private TaskList testList;

    @BeforeEach
    void setUp() {
        testList = TaskList.builder()
                .listId(1L).name("To Do").boardId(1L).position(0).isArchived(false).build();
    }

    private void mockUserAndMembership(String role) {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(10L);
        user.setEmail("user@test.com");
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(user);
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, role));
    }

    @Test
    void createList_savesAndReturns() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("To Do"); req.setBoardId(1L);

        mockUserAndMembership("MEMBER");
        when(listRepository.save(any(TaskList.class))).thenReturn(testList);
        when(listRepository.findMaxPositionByBoardId(1L)).thenReturn(Optional.empty());

        TaskList result = listService.createList(req, "user@test.com");
        assertNotNull(result);
        assertEquals("To Do", result.getName());
        verify(listRepository).save(any(TaskList.class));
    }

    @Test
    void getListById_notFound_throws() {
        when(listRepository.findByListId(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> listService.getListById(99L));
    }

    @Test
    void getListsByBoard_returnsList() {
        when(listRepository.findByBoardIdOrderByPosition(1L)).thenReturn(List.of(testList));
        List<TaskList> result = listService.getListsByBoardOrdered(1L);
        assertEquals(1, result.size());
    }

    @Test
    void deleteList_callsRepository() {
        mockUserAndMembership("ADMIN");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        listService.deleteList(1L, "user@test.com");
        verify(listRepository).delete(testList);
    }

    @Test
    void updateList_updatesName() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("Updated");

        mockUserAndMembership("MEMBER");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenReturn(testList);

        TaskList result = listService.updateList(1L, req, "user@test.com");
        assertNotNull(result);
        verify(listRepository).save(any());
    }

    // ─── resolveUserIdFromEmail branches ──────────────────────────────────────

    @Test
    void createList_authReturnsNull_throwsUnauthorized() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(null);

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    @Test
    void createList_authFeignNotFound_throwsUnauthorized() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail("user@test.com")).thenThrow(nf);

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    @Test
    void createList_authFeignError_throwsIllegalState() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        feign.FeignException fe = mock(feign.FeignException.class);
        when(fe.getMessage()).thenReturn("500");
        when(authServiceClient.getUserByEmail("user@test.com")).thenThrow(fe);

        assertThrows(IllegalStateException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    @Test
    void createList_authGenericException_throwsIllegalState() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com"))
                .thenThrow(new RuntimeException("net"));

        assertThrows(IllegalStateException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    // ─── Board membership branches ────────────────────────────────────────────

    @Test
    void createList_notBoardMember_throwsUnauthorized() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(false, null));

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    @Test
    void createList_boardServiceReturnsNull_treatedAsNonMember() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(boardServiceClient.checkBoardMembership(1L, 10L)).thenReturn(null);

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    @Test
    void createList_boardFeignNotFound_treatedAsNonMember() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(boardServiceClient.checkBoardMembership(1L, 10L)).thenThrow(nf);

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    @Test
    void createList_boardFeignError_throwsIllegalState() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        feign.FeignException fe = mock(feign.FeignException.class);
        when(fe.getMessage()).thenReturn("500");
        when(boardServiceClient.checkBoardMembership(1L, 10L)).thenThrow(fe);

        assertThrows(IllegalStateException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    @Test
    void createList_boardGenericException_throwsIllegalState() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X"); req.setBoardId(1L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(IllegalStateException.class,
                () -> listService.createList(req, "user@test.com"));
    }

    // ─── createList success - position ────────────────────────────────────────

    @Test
    void createList_setsNextPositionFromExisting() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("New"); req.setBoardId(1L); req.setColor("#abc");
        mockUserAndMembership("MEMBER");
        when(listRepository.findMaxPositionByBoardId(1L)).thenReturn(Optional.of(5));
        when(listRepository.save(any(TaskList.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskList result = listService.createList(req, "user@test.com");
        assertEquals(6, result.getPosition());
    }

    // ─── getListById / getListsByBoard ────────────────────────────────────────

    @Test
    void getListById_success() {
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        TaskList r = listService.getListById(1L);
        assertEquals("To Do", r.getName());
    }

    @Test
    void getListsByBoard_success() {
        when(listRepository.findByBoardId(1L)).thenReturn(List.of(testList));
        assertEquals(1, listService.getListsByBoard(1L).size());
    }

    @Test
    void getArchivedLists_returnsList() {
        when(listRepository.findByBoardIdAndIsArchivedOrderByPosition(1L, true))
                .thenReturn(List.of(testList));
        assertEquals(1, listService.getArchivedLists(1L).size());
    }

    // ─── updateList branches ──────────────────────────────────────────────────

    @Test
    void updateList_archivedList_throws() {
        TaskList archived = TaskList.builder()
                .listId(1L).boardId(1L).isArchived(true).build();
        ListRequestDTO req = new ListRequestDTO();
        req.setName("X");
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class,
                () -> listService.updateList(1L, req, "user@test.com"));
    }

    @Test
    void updateList_blankName_keepsOld() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("   "); req.setColor("#new");
        mockUserAndMembership("MEMBER");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskList result = listService.updateList(1L, req, "user@test.com");
        assertEquals("To Do", result.getName());
        assertEquals("#new", result.getColor());
    }

    @Test
    void updateList_nullName_keepsOld() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName(null); req.setColor(null);
        mockUserAndMembership("MEMBER");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskList result = listService.updateList(1L, req, "user@test.com");
        assertEquals("To Do", result.getName());
    }

    // ─── deleteList branches ──────────────────────────────────────────────────

    @Test
    void deleteList_notAdmin_throws() {
        mockUserAndMembership("MEMBER");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.deleteList(1L, "user@test.com"));
    }

    // ─── reorderLists ─────────────────────────────────────────────────────────

    @Test
    void reorderLists_emptyIds_throws() {
        ReorderRequestDTO req = new ReorderRequestDTO();
        req.setOrderedListIds(List.of());
        mockUserAndMembership("MEMBER");

        assertThrows(IllegalArgumentException.class,
                () -> listService.reorderLists(1L, req, "user@test.com"));
    }

    @Test
    void reorderLists_nullIds_throws() {
        ReorderRequestDTO req = new ReorderRequestDTO();
        req.setOrderedListIds(null);
        mockUserAndMembership("MEMBER");

        assertThrows(IllegalArgumentException.class,
                () -> listService.reorderLists(1L, req, "user@test.com"));
    }

    @Test
    void reorderLists_duplicateIds_throws() {
        ReorderRequestDTO req = new ReorderRequestDTO();
        req.setOrderedListIds(List.of(1L, 1L));
        mockUserAndMembership("MEMBER");
        when(listRepository.findByBoardId(1L)).thenReturn(List.of(testList));

        assertThrows(IllegalArgumentException.class,
                () -> listService.reorderLists(1L, req, "user@test.com"));
    }

    @Test
    void reorderLists_mismatchedIds_throws() {
        ReorderRequestDTO req = new ReorderRequestDTO();
        req.setOrderedListIds(List.of(99L));
        mockUserAndMembership("MEMBER");
        when(listRepository.findByBoardId(1L)).thenReturn(List.of(testList));

        assertThrows(IllegalArgumentException.class,
                () -> listService.reorderLists(1L, req, "user@test.com"));
    }

    @Test
    void reorderLists_success_updatesPositions() {
        TaskList l1 = TaskList.builder().listId(1L).boardId(1L).position(1).isArchived(false).build();
        TaskList l2 = TaskList.builder().listId(2L).boardId(1L).position(2).isArchived(false).build();

        ReorderRequestDTO req = new ReorderRequestDTO();
        req.setOrderedListIds(List.of(2L, 1L));
        mockUserAndMembership("MEMBER");
        when(listRepository.findByBoardId(1L)).thenReturn(List.of(l1, l2));
        when(listRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<TaskList> result = listService.reorderLists(1L, req, "user@test.com");
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getListId());
        assertEquals(1L, result.get(1).getListId());
    }

    @Test
    void reorderLists_ignoresArchivedLists() {
        TaskList active = TaskList.builder().listId(1L).boardId(1L).position(1).isArchived(false).build();
        TaskList archived = TaskList.builder().listId(2L).boardId(1L).position(2).isArchived(true).build();

        ReorderRequestDTO req = new ReorderRequestDTO();
        req.setOrderedListIds(List.of(1L));
        mockUserAndMembership("MEMBER");
        when(listRepository.findByBoardId(1L)).thenReturn(List.of(active, archived));
        when(listRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<TaskList> result = listService.reorderLists(1L, req, "user@test.com");
        assertEquals(1, result.size());
    }

    // ─── archive/unarchive ────────────────────────────────────────────────────

    @Test
    void archiveList_success() {
        mockUserAndMembership("MEMBER");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskList result = listService.archiveList(1L, "user@test.com");
        assertTrue(result.getIsArchived());
    }

    @Test
    void archiveList_alreadyArchived_throws() {
        TaskList already = TaskList.builder().listId(1L).boardId(1L).isArchived(true).build();
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(already));

        assertThrows(IllegalStateException.class,
                () -> listService.archiveList(1L, "user@test.com"));
    }

    @Test
    void unarchiveList_success() {
        TaskList archived = TaskList.builder().listId(1L).boardId(1L).isArchived(true).build();
        mockUserAndMembership("MEMBER");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(archived));
        when(listRepository.findMaxPositionByBoardId(1L)).thenReturn(Optional.of(3));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskList result = listService.unarchiveList(1L, "user@test.com");
        assertFalse(result.getIsArchived());
        assertEquals(4, result.getPosition());
    }

    @Test
    void unarchiveList_notArchived_throws() {
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));

        assertThrows(IllegalStateException.class,
                () -> listService.unarchiveList(1L, "user@test.com"));
    }

    // ─── moveList ─────────────────────────────────────────────────────────────

    @Test
    void moveList_sameBoard_throws() {
        MoveListRequestDTO req = new MoveListRequestDTO();
        req.setTargetBoardId(1L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));

        assertThrows(IllegalArgumentException.class,
                () -> listService.moveList(1L, req, "user@test.com"));
    }

    @Test
    void moveList_success() {
        MoveListRequestDTO req = new MoveListRequestDTO();
        req.setTargetBoardId(2L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, "ADMIN"));
        when(boardServiceClient.checkBoardMembership(2L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, "MEMBER"));
        when(listRepository.findMaxPositionByBoardId(2L)).thenReturn(Optional.of(2));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskList result = listService.moveList(1L, req, "user@test.com");
        assertEquals(2L, result.getBoardId());
        assertEquals(3, result.getPosition());
    }

    @Test
    void moveList_notAdminOnSource_throws() {
        MoveListRequestDTO req = new MoveListRequestDTO();
        req.setTargetBoardId(2L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, "MEMBER"));

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.moveList(1L, req, "user@test.com"));
    }

    @Test
    void moveList_notMemberOnTarget_throws() {
        MoveListRequestDTO req = new MoveListRequestDTO();
        req.setTargetBoardId(2L);
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, "ADMIN"));
        when(boardServiceClient.checkBoardMembership(2L, 10L))
                .thenReturn(new BoardMemberCheckDTO(false, null));

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.moveList(1L, req, "user@test.com"));
    }

    // ─── validateBoardMembership ─────────────────────────────────────────────

    @Test
    void validateBoardMembership_success() {
        mockUserAndMembership("MEMBER");

        // should NOT throw
        listService.validateBoardMembership(1L, "user@test.com");
    }

    @Test
    void validateBoardMembership_notMember_throws() {
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(10L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(u);
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(false, null));

        assertThrows(com.flowboard.list.exception.UnauthorizedException.class,
                () -> listService.validateBoardMembership(1L, "user@test.com"));
    }
}
