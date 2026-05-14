package com.flowboard.board;

import com.flowboard.board.dto.*;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.enums.BoardRole;
import com.flowboard.board.enums.Visibility;
import com.flowboard.board.client.AuthServiceClient;
import com.flowboard.board.client.WorkspaceServiceClient;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
import com.flowboard.board.serviceImpl.BoardServiceImpl;
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
class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardMemberRepository boardMemberRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private WorkspaceServiceClient workspaceServiceClient;
    @InjectMocks private BoardServiceImpl boardService;

    private Board testBoard;

    @BeforeEach
    void setUp() {
        testBoard = Board.builder()
                .boardId(1L).name("Test Board")
                .workspaceId(1L).createdById(1L)
                .visibility(Visibility.PRIVATE).isClosed(false).build();
    }

    @Test
    void getBoardById_returnsBoard() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        Board result = boardService.getBoardById(1L);
        assertNotNull(result);
        assertEquals("Test Board", result.getName());
    }

    @Test
    void getBoardById_notFound_throws() {
        when(boardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> boardService.getBoardById(99L));
    }

    @Test
    void createBoard_savesBoard() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("New Board"); req.setWorkspaceId(1L);

        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(1L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user);
        when(workspaceServiceClient.checkMembership(anyLong(), anyLong()))
                .thenReturn(new WorkspaceMemberCheckDTO(true, "ADMIN"));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(boardMemberRepository.save(any(BoardMember.class))).thenReturn(new BoardMember());

        Board result = boardService.createBoard(req, "user@test.com");
        assertNotNull(result);
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    void getBoardsByWorkspace_returnsList() {
        when(boardRepository.findByWorkspaceId(1L)).thenReturn(List.of(testBoard));
        List<Board> results = boardService.getBoardsByWorkspace(1L);
        assertEquals(1, results.size());
    }

    @Test
    void deleteBoard_callsRepository() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        // owner deleting
        testBoard.setCreatedById(1L);
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(1L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user);
        BoardMember adminMember = BoardMember.builder().boardId(1L).userId(1L).role(BoardRole.ADMIN).build();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember));
        boardService.deleteBoard(1L, "user@test.com");
        verify(boardRepository).delete(testBoard);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private UserResponseDTO user(Long id) {
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(id);
        return u;
    }

    private BoardMember admin() {
        return BoardMember.builder().boardId(1L).userId(1L).role(BoardRole.ADMIN).build();
    }

    private void mockAuthAsAdmin() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(admin()));
    }

    // ─── resolveUserIdFromEmail branches ──────────────────────────────────────

    @Test
    void createBoard_authReturnsNull_throwsUnauthorized() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(null);

        assertThrows(com.flowboard.board.exception.UnauthorizedException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    @Test
    void createBoard_authFeignNotFound_throwsUnauthorized() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail(anyString())).thenThrow(nf);

        assertThrows(com.flowboard.board.exception.UnauthorizedException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    @Test
    void createBoard_authFeignError_throwsIllegalState() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        feign.FeignException fe = mock(feign.FeignException.class);
        when(fe.getMessage()).thenReturn("500");
        when(authServiceClient.getUserByEmail(anyString())).thenThrow(fe);

        assertThrows(IllegalStateException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    @Test
    void createBoard_authGenericException_throwsIllegalState() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        when(authServiceClient.getUserByEmail(anyString())).thenThrow(new RuntimeException("net"));

        assertThrows(IllegalStateException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    // ─── checkWorkspaceMembership branches via createBoard ────────────────────

    @Test
    void createBoard_notWorkspaceMember_throws() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));
        when(workspaceServiceClient.checkMembership(1L, 1L))
                .thenReturn(new WorkspaceMemberCheckDTO(false, null));

        assertThrows(com.flowboard.board.exception.UnauthorizedException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    @Test
    void createBoard_workspaceServiceReturnsNull_throws() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));
        when(workspaceServiceClient.checkMembership(1L, 1L)).thenReturn(null);

        assertThrows(com.flowboard.board.exception.UnauthorizedException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    @Test
    void createBoard_workspaceFeignNotFound_treatedAsNonMember() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));
        when(workspaceServiceClient.checkMembership(1L, 1L)).thenThrow(nf);

        assertThrows(com.flowboard.board.exception.UnauthorizedException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    @Test
    void createBoard_workspaceFeignError_throwsIllegalState() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        feign.FeignException fe = mock(feign.FeignException.class);
        when(fe.getMessage()).thenReturn("500");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));
        when(workspaceServiceClient.checkMembership(1L, 1L)).thenThrow(fe);

        assertThrows(IllegalStateException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    @Test
    void createBoard_workspaceGenericException_throwsIllegalState() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));
        when(workspaceServiceClient.checkMembership(1L, 1L)).thenThrow(new RuntimeException("boom"));

        assertThrows(IllegalStateException.class,
                () -> boardService.createBoard(req, "u@e.com"));
    }

    // ─── getBoardsByUser ──────────────────────────────────────────────────────

    @Test
    void getBoardsByUser_emptyMembership_returnsEmpty() {
        when(boardMemberRepository.findByUserId(1L)).thenReturn(List.of());
        assertEquals(0, boardService.getBoardsByUser(1L).size());
    }

    @Test
    void getBoardsByUser_returnsBoards() {
        when(boardMemberRepository.findByUserId(1L)).thenReturn(List.of(admin()));
        when(boardRepository.findAllById(List.of(1L))).thenReturn(List.of(testBoard));
        assertEquals(1, boardService.getBoardsByUser(1L).size());
    }

    // ─── updateBoard ──────────────────────────────────────────────────────────

    @Test
    void updateBoard_success() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("Renamed"); req.setDescription("D"); req.setBackground("blue");
        req.setVisibility(Visibility.PUBLIC); req.setWorkspaceId(1L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        mockAuthAsAdmin();
        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0));

        Board result = boardService.updateBoard(1L, req, "u@e.com");
        assertEquals("Renamed", result.getName());
    }

    @Test
    void updateBoard_blankName_keepsOld() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("  "); req.setVisibility(Visibility.PRIVATE); req.setWorkspaceId(1L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        mockAuthAsAdmin();
        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0));

        Board result = boardService.updateBoard(1L, req, "u@e.com");
        assertEquals("Test Board", result.getName());
    }

    @Test
    void updateBoard_closed_throws() {
        Board closed = Board.builder().boardId(1L).isClosed(true).workspaceId(1L).build();
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setVisibility(Visibility.PRIVATE); req.setWorkspaceId(1L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(closed));
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));

        assertThrows(IllegalStateException.class,
                () -> boardService.updateBoard(1L, req, "u@e.com"));
    }

    @Test
    void updateBoard_notAdmin_throws() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("X"); req.setVisibility(Visibility.PRIVATE); req.setWorkspaceId(1L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.board.exception.UnauthorizedException.class,
                () -> boardService.updateBoard(1L, req, "u@e.com"));
    }

    // ─── closeBoard ───────────────────────────────────────────────────────────

    @Test
    void closeBoard_success() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        mockAuthAsAdmin();
        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0));

        Board result = boardService.closeBoard(1L, "u@e.com");
        assertTrue(result.getIsClosed());
    }

    @Test
    void closeBoard_alreadyClosed_throws() {
        Board closed = Board.builder().boardId(1L).isClosed(true).workspaceId(1L).build();
        when(boardRepository.findById(1L)).thenReturn(Optional.of(closed));
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user(1L));

        assertThrows(IllegalStateException.class,
                () -> boardService.closeBoard(1L, "u@e.com"));
    }

    // ─── addMember / removeMember / updateMemberRole ──────────────────────────

    @Test
    void addMember_success() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        mockAuthAsAdmin();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.empty());
        BoardMember saved = BoardMember.builder()
                .boardMemberId(10L).boardId(1L).userId(5L).role(BoardRole.MEMBER).build();
        when(boardMemberRepository.save(any(BoardMember.class))).thenReturn(saved);

        BoardMemberResponseDTO result = boardService.addMember(1L, 5L, BoardRole.MEMBER, "u@e.com");
        assertEquals(5L, result.getUserId());
    }

    @Test
    void addMember_alreadyMember_throws() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        mockAuthAsAdmin();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L))
                .thenReturn(Optional.of(BoardMember.builder().userId(5L).role(BoardRole.MEMBER).build()));

        assertThrows(IllegalArgumentException.class,
                () -> boardService.addMember(1L, 5L, BoardRole.MEMBER, "u@e.com"));
    }

    @Test
    void removeMember_success() {
        BoardMember member = BoardMember.builder().boardId(1L).userId(5L).role(BoardRole.MEMBER).build();
        mockAuthAsAdmin();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.of(member));

        boardService.removeMember(1L, 5L, "u@e.com");
        verify(boardMemberRepository).delete(member);
    }

    @Test
    void removeMember_notFound_throws() {
        mockAuthAsAdmin();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.board.exception.ResourceNotFoundException.class,
                () -> boardService.removeMember(1L, 5L, "u@e.com"));
    }

    @Test
    void updateMemberRole_success() {
        BoardMember member = BoardMember.builder().boardId(1L).userId(5L).role(BoardRole.MEMBER).build();
        mockAuthAsAdmin();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.of(member));
        when(boardMemberRepository.save(any(BoardMember.class))).thenAnswer(inv -> inv.getArgument(0));

        BoardMemberResponseDTO result = boardService.updateMemberRole(1L, 5L, BoardRole.ADMIN, "u@e.com");
        assertEquals(BoardRole.ADMIN, result.getRole());
    }

    @Test
    void updateMemberRole_notFound_throws() {
        mockAuthAsAdmin();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.board.exception.ResourceNotFoundException.class,
                () -> boardService.updateMemberRole(1L, 5L, BoardRole.ADMIN, "u@e.com"));
    }

    // ─── getBoardMembers / getAssignableUsers ─────────────────────────────────

    @Test
    void getBoardMembers_returnsList() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(admin()));
        assertEquals(1, boardService.getBoardMembers(1L).size());
    }

    @Test
    void getBoardMembers_enrichesFromAuth() {
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(1L); u.setEmail("a@b.com"); u.setFullName("Alice");
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(admin()));
        when(authServiceClient.getUserById(1L)).thenReturn(u);

        List<BoardMemberResponseDTO> result = boardService.getBoardMembers(1L);
        assertEquals("Alice", result.get(0).getFullName());
    }

    @Test
    void getBoardMembers_authFailure_swallowedReturnsBareDTO() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(admin()));
        when(authServiceClient.getUserById(1L)).thenThrow(new RuntimeException("down"));

        List<BoardMemberResponseDTO> result = boardService.getBoardMembers(1L);
        assertEquals(1, result.size());
        assertNull(result.get(0).getFullName());
    }

    @Test
    void getAssignableUsers_unionsBoardAndWorkspaceMembers() {
        WorkspaceMemberDTO w = new WorkspaceMemberDTO();
        w.setUserId(2L); w.setEmail("b@c.com"); w.setFullName("Bob");

        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(admin()));
        when(workspaceServiceClient.listMembers(1L)).thenReturn(List.of(w));

        List<BoardMemberResponseDTO> result = boardService.getAssignableUsers(1L);
        assertEquals(2, result.size());
    }

    @Test
    void getAssignableUsers_workspaceFails_returnsBoardMembersOnly() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(admin()));
        when(workspaceServiceClient.listMembers(1L)).thenThrow(new RuntimeException("down"));

        List<BoardMemberResponseDTO> result = boardService.getAssignableUsers(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getAssignableUsers_skipsDuplicates() {
        WorkspaceMemberDTO w = new WorkspaceMemberDTO();
        w.setUserId(1L); // same as admin
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(admin()));
        when(workspaceServiceClient.listMembers(1L)).thenReturn(List.of(w));

        List<BoardMemberResponseDTO> result = boardService.getAssignableUsers(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getAssignableUsers_boardNotFound_throws() {
        when(boardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.board.exception.ResourceNotFoundException.class,
                () -> boardService.getAssignableUsers(99L));
    }

    // ─── checkBoardMembership ─────────────────────────────────────────────────

    @Test
    void checkBoardMembership_directMember_returnsTrue() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(admin()));

        BoardMemberCheckResponseDTO result = boardService.checkBoardMembership(1L, 1L);
        assertTrue(result.isMember());
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void checkBoardMembership_workspaceFallback_grantsMember() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.empty());
        when(workspaceServiceClient.checkMembership(1L, 5L))
                .thenReturn(new WorkspaceMemberCheckDTO(true, "MEMBER"));

        BoardMemberCheckResponseDTO result = boardService.checkBoardMembership(1L, 5L);
        assertTrue(result.isMember());
        assertEquals("MEMBER", result.getRole());
    }

    @Test
    void checkBoardMembership_notMember_returnsFalse() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.empty());
        when(workspaceServiceClient.checkMembership(1L, 5L))
                .thenReturn(new WorkspaceMemberCheckDTO(false, null));

        BoardMemberCheckResponseDTO result = boardService.checkBoardMembership(1L, 5L);
        assertFalse(result.isMember());
    }

    @Test
    void checkBoardMembership_workspaceFailure_returnsFalse() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 5L)).thenReturn(Optional.empty());
        when(workspaceServiceClient.checkMembership(1L, 5L))
                .thenThrow(new RuntimeException("down"));

        BoardMemberCheckResponseDTO result = boardService.checkBoardMembership(1L, 5L);
        assertFalse(result.isMember());
    }

    @Test
    void checkBoardMembership_boardMissing_returnsFalse() {
        when(boardRepository.findById(99L)).thenReturn(Optional.empty());

        BoardMemberCheckResponseDTO result = boardService.checkBoardMembership(99L, 1L);
        assertFalse(result.isMember());
    }
}
